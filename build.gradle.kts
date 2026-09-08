import java.util.Properties


plugins {
    id("java")
    id("application")

    id("org.openjfx.javafxplugin") version "0.1.0"

    // shadow jar pro app empacotado (nativa: MSI/DEB/Flatpak) rodar sem JavaFX no classpath
    id("com.gradleup.shadow") version "8.3.5"
}

val props = Properties()
file("gradle.properties").inputStream().use { props.load(it) }

// appVersion (base, x.x.x) + appPatch (contador, 0 = sem patch) compostos em uma
// única string — mesma lógica de scripts/config.py, pra Gradle e jpackage nunca
// divergirem. Ver gradle.properties para o porquê dos dois campos separados.
val appPatchNumber = props.getProperty("appPatch", "0").toIntOrNull() ?: 0
val fullVersion = if (appPatchNumber > 0) "${props.getProperty("appVersion")}.$appPatchNumber" else props.getProperty("appVersion")

group = "gobitech"
version = fullVersion

repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri("https://jitpack.io") }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

javafx {
    version = "25.0.1"
    modules("javafx.controls", "javafx.graphics")
}

dependencies {

    implementation("com.fazecast:jSerialComm:2.11.0")


    // Dependências de teste
    testImplementation(platform("org.junit:junit-bom:5.13.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("megalodonte:megalodonte-base:1.0.0-beta")
    implementation("megalodonte:megalodonte-components:1.0.0-beta")
    implementation("megalodonte:megalodonte-reactivity:1.0.0-beta")
    implementation("megalodonte:megalodonte-router:1.0.0-beta")
    implementation("megalodonte:megalodonte-theme:1.0.0-beta")

    //Java library for ESC/POS printer commands
    implementation("com.github.anastaciocintra:escpos-coffee:4.1.0")

    implementation("io.github.java-native:jssc:2.10.2")

    implementation("org.kordamp.ikonli:ikonli-core:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-javafx:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-antdesignicons-pack:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-entypo-pack:12.4.0")

    //sqlite
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")

    implementation("io.github.sproket:persism:2.3")
    implementation("org.flywaydb:flyway-core:10.15.0")

    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    testCompileOnly("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")

    //logs
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.18")

    // SQLite/Persism/Flyway/logback também no classpath de teste (herdados do
    // implementation — sem duplicar declaração)

    //geracao de PDF (tela de Relatorios)
    implementation("org.apache.pdfbox:pdfbox:2.0.29")

    //utilitties
    implementation("com.github.eliezer-dev-software-enginner:pack-utilities:v1.0.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    val os = System.getProperty("os.name").lowercase()

    val javafxModulesHome = System.getenv("JAVAFX_MODULES_HOME")
        ?: throw GradleException(
            "Variável de ambiente JAVAFX_MODULES_HOME não definida. " +
                    "Defina-a apontando para a pasta que contém linux-25.0.1/ e windows-25.0.1/."
        )

    val fxPath = if (os.contains("win")) {
        "$javafxModulesHome/windows-25.0.1/lib"
    } else {
        "$javafxModulesHome/linux-25.0.1/lib"
    }

    val jvmArgsList = mutableListOf(
        "--module-path", fxPath,
        "--add-modules", "javafx.controls,javafx.graphics",
        "--enable-native-access=ALL-UNNAMED",
        "-Dgobitech.appVersion=$fullVersion"
    )

    // Prism verbose é debug do JavaFX — só liga quando o próprio DEV_MODE foi
    // setado no ambiente de quem chamou o gradle (senão polui todo log de run).
    if (System.getenv("DEV_MODE") == "true") {
        jvmArgsList.add("-Dprism.verbose=true")
    }

    jvmArgs = jvmArgsList

    environment("DEV_MODE", "true")
}
application {
    mainClass.set(props.getProperty("appMainClass"))
}


tasks.shadowJar {
    dependsOn(tasks.test)
    archiveBaseName.set(props.getProperty("appName"))
    archiveClassifier.set("")
    mergeServiceFiles() // equivalente ao ServicesResourceTransformer

    manifest {
        attributes(
            "Main-Class" to props.getProperty("appMainClass")
        )
    }

    // Exclui JavaFX (como o pom.xml fazia)
    exclude("org/openjfx/**")

    // 🔥 remove assinaturas quebradas (igual no Maven)
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
}

// O jar simples fica desabilitado de propósito: o artefato utilizável é o shadowJar
// (os scripts de MSI/DEB/Flatpak rodam "clean shadowJar").
tasks.jar {
    enabled = false
}