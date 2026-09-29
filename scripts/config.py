from pathlib import Path
import base64
import subprocess
import shutil
import os

ROOT = Path(__file__).resolve().parent.parent
RESTRICTED_NATIVE_LIBS = {"jfxwebkit.dll"}
ADMIN_LOGIN_KEY = "GOBITECH_ADMIN_LOGIN"
ADMIN_PASSWORD_KEY = "GOBITECH_ADMIN_PASSWORD"


def _read_gradle_properties():
    props = {}
    with open(ROOT / "gradle.properties") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#"):
                k, _, v = line.partition("=")
                props[k.strip()] = v.strip()
    return props


def _read_dotenv():
    values = {}
    env_file = ROOT / ".env"
    if not env_file.is_file():
        return values
    for number, original in enumerate(env_file.read_text(encoding="utf-8-sig").splitlines(), 1):
        line = original.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[7:].strip()
        key, separator, value = line.partition("=")
        if not separator or not key.strip():
            raise ValueError(f"Linha inválida no .env ({number})")
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in ("'", '"'):
            value = value[1:-1]
        values[key.strip()] = value
    return values


def _runtime_credential(key: str):
    # No GitHub Actions os valores vêm diretamente de Secrets como variáveis de
    # ambiente. Localmente, o .env é o fallback e nunca entra no pacote.
    value = os.environ.get(key) or _read_dotenv().get(key)
    if not value or not value.strip():
        raise EnvironmentError(
            f"{key} não definido. Configure a variável de ambiente ou o arquivo .env."
        )
    if "\n" in value or "\r" in value:
        raise ValueError(f"{key} não pode conter quebra de linha")
    return value


def _base64_java_option(property_name: str, value: str):
    encoded = base64.b64encode(value.encode("utf-8")).decode("ascii")
    return f"-D{property_name}={encoded}"


def validate_runtime_credentials():
    _runtime_credential(ADMIN_LOGIN_KEY)
    _runtime_credential(ADMIN_PASSWORD_KEY)


_gradle_props = _read_gradle_properties()

APP_NAME = _gradle_props["appDisplayName"]

# appVersion (base, x.x.x) + appPatch (contador, 0 = sem patch) compostos numa
# única string — mesma lógica de build.gradle.kts. Ver gradle.properties.
_patch_number = int(_gradle_props.get("appPatch", "0") or 0)
APP_VERSION = _gradle_props["appVersion"] if _patch_number == 0 else f"{_gradle_props['appVersion']}.{_patch_number}"

MAIN_CLASS = _gradle_props["appMainClass"]
VENDOR = _gradle_props["appVendor"]
LINUX_MENU_GROUP = _gradle_props["appLinuxMenuGroup"]
ICON_PATH = "src/main/resources/assets/app_ico.ico" if os.name == "nt" else "src/main/resources/assets/app_ico.png"
JAVAFX_VERSION = "25.0.1"

# Upgrade code fixo do MSI (Windows) — precisa ser o mesmo em toda geração de MSI
# pra o Windows tratar uma nova versão como upgrade da anterior em vez de instalar
# um produto "diferente" ao lado.
UPGRADE_UUID = "e3a2b1c4-7d5f-4a8e-9c6b-2f1d0a3e7b8c"


def get_platform():
    if os.name == "nt":
        return "windows"
    return "linux"


def javafx_dir():
    javafx_modules_home = os.environ.get("JAVAFX_MODULES_HOME")
    if not javafx_modules_home:
        raise EnvironmentError(
            "Variável de ambiente JAVAFX_MODULES_HOME não definida. "
            "Defina-a apontando para a pasta que contém "
            f"{get_platform()}-{JAVAFX_VERSION}/."
        )
    return Path(javafx_modules_home) / f"{get_platform()}-{JAVAFX_VERSION}"


def _java_home():
    jh = Path(os.environ.get("JAVA_HOME", ""))
    if jh.name == "bin":
        jh = jh.parent
    return str(jh)


def run_gradle(*tasks):
    gradlew = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    env = {**os.environ, "JAVA_HOME": _java_home()}
    subprocess.run([str(gradlew), *tasks], cwd=ROOT, check=True, env=env)


def find_jar():
    jars = list((ROOT / "build" / "libs").glob("*.jar"))
    if not jars:
        raise FileNotFoundError("Nenhum JAR encontrado em build/libs/")
    return jars[0]


def prepare_temp():
    temp_dir = ROOT / "temp"
    shutil.rmtree(temp_dir, ignore_errors=True)
    # O jpackage copia tudo que estiver em --input para app/. Por isso o JAR e
    # o .env ficam isolados dos módulos JavaFX e da imagem gerada pelo jlink.
    (temp_dir / "input").mkdir(parents=True)
    (temp_dir / "javafx").mkdir(parents=True)
    return temp_dir


def copy_javafx(temp_dir: Path):
    jfx = javafx_dir()
    javafx_temp = temp_dir / "javafx"
    shutil.copytree(jfx / "lib", javafx_temp / "lib")
    bin_dir = jfx / "bin"
    if bin_dir.exists():
        shutil.copytree(bin_dir, javafx_temp / "bin")


def run_jlink(temp_dir: Path):
    jdk_jmods = Path(_java_home()) / "jmods"
    sep = ";" if os.name == "nt" else ":"
    javafx_lib = temp_dir / "javafx" / "lib"
    module_path = f"{javafx_lib}{sep}{jdk_jmods}"
    app_jar = temp_dir / "input" / "app.jar"
    java_bin = Path(_java_home()) / "bin"
    jdeps_cmd = str(java_bin / "jdeps")
    base_modules = {
        "javafx.controls", "java.sql", "jdk.zipfs", "java.logging", "java.xml",
        "jdk.charsets",   # necessário para codepages tipo Cp860, Cp437 (ESC/POS)
    }
    try:
        jdeps = subprocess.run(
            [jdeps_cmd, "--print-module-deps", "--ignore-missing-deps",
             "--module-path", str(javafx_lib),
             str(app_jar)],
            capture_output=True, text=True, cwd=ROOT, check=True
        )
        lines = [l for l in jdeps.stdout.strip().splitlines() if not l.startswith("Warning:")]
        found = set()
        if lines:
            found.update(lines[-1].split(","))
        found.discard("")
        modules = ",".join(sorted(base_modules | found))
    except subprocess.CalledProcessError:
        modules = ",".join(sorted(base_modules))
    jlink_cmd = str(java_bin / "jlink")
    subprocess.run(
        [
            jlink_cmd,
            "--module-path", module_path,
            "--add-modules", modules,
            "--no-header-files",
            "--no-man-pages",
            "--output", str(temp_dir / "runtime")
        ],
        cwd=ROOT, check=True
    )


def copy_natives(temp_dir: Path):
    ext = ".dll" if os.name == "nt" else ".so"
    target = "bin" if os.name == "nt" else "lib"
    javafx_temp = temp_dir / "javafx"
    source = javafx_temp / "bin" if (javafx_temp / "bin").exists() else javafx_temp / "lib"
    for native in source.glob(f"*{ext}"):
        if native.name.lower() in RESTRICTED_NATIVE_LIBS:
            print(f"Ignorando native restrito: {native.name}")
            continue
        dest = temp_dir / "runtime" / target / native.name
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy(native, dest)


def run_jpackage(temp_dir: Path, pkg_type: str, extra_args: list = None):
    dist_dir = ROOT / "dist"
    if dist_dir.exists():
        shutil.rmtree(dist_dir)
    jpackage_cmd = str(Path(_java_home()) / "bin" / "jpackage")
    cmd = [
        jpackage_cmd,
        # Somente arquivos da aplicação. JavaFX e runtime ficam fora daqui para
        # não serem duplicados em app/ dentro do pacote final.
        "--input", str(temp_dir / "input"),
        "--name", APP_NAME,
        "--app-version", APP_VERSION,
        "--vendor", VENDOR,
        # Main.APP_VERSION lê essa property em runtime — nada fica hardcoded no Main.java.
        "--java-options", f"-Dgobitech.appVersion={APP_VERSION}",
        # Credenciais são lidas do ambiente/.env durante o build e embutidas no
        # launcher como propriedades da JVM. Base64 evita problemas de escape no cfg.
        "--java-options", _base64_java_option(
            "gobitech.adminLoginBase64", _runtime_credential(ADMIN_LOGIN_KEY)
        ),
        "--java-options", _base64_java_option(
            "gobitech.adminPasswordBase64", _runtime_credential(ADMIN_PASSWORD_KEY)
        ),
        # jSerialComm em Java 24+ exige liberar acesso a código nativo
            # explicitamente — sem isso, a JVM barra o carregamento da lib nativa
            # de portas seriais em runtime.
        "--java-options", "--enable-native-access=ALL-UNNAMED",
        "--main-jar", "app.jar",
        "--main-class", MAIN_CLASS,
        "--dest", "dist",
        "--type", pkg_type,
        "--runtime-image", str(temp_dir / "runtime"),
        "--icon", str(ROOT / ICON_PATH),
    ]
    if extra_args:
        cmd.extend(extra_args)
    subprocess.run(cmd, cwd=ROOT, check=True)


def smoke_test(temp_dir):
    java_exe = temp_dir / "runtime" / "bin" / "java"
    proc = subprocess.Popen(
        [str(java_exe), "-Djava.library.path={}".format(temp_dir / "runtime" / "lib"),
         "-cp", str(temp_dir / "input" / "app.jar"), MAIN_CLASS],
        cwd=ROOT
    )
    try:
        proc.wait(timeout=5)
        if proc.returncode != 0:
            raise RuntimeError(f"Smoke test falhou (exit code {proc.returncode})")
    except subprocess.TimeoutExpired:
        proc.terminate()
        proc.wait()

def open_dist_folder():
    # CI runners (GitHub Actions sets CI=true on all of them) não têm sessão de desktop —
    # xdg-open pode nem existir no runner Linux (FileNotFoundError, não só falhar em
    # silêncio), e os.startfile no Windows não tem Explorer pra abrir. Sem essa guarda, o
    # workflow de release quebraria bem no fim, depois de já ter gerado o pacote.
    if os.environ.get("CI"):
        return
    dist_dir = ROOT / "dist"
    if os.name == "nt":
        os.startfile(dist_dir)
    else:
        subprocess.run(["xdg-open", str(dist_dir)])


def rename_output(pkg_type: str):
    dist_dir = ROOT / "dist"
    ext = f".{pkg_type}"
    files = list(dist_dir.glob(f"*{ext}"))
    if not files:
        raise FileNotFoundError(f"Nenhum pacote {ext} gerado em dist/")
    final_name = f"{APP_NAME}-{APP_VERSION}{ext}"
    files[0].rename(dist_dir / final_name)
    return dist_dir / final_name
