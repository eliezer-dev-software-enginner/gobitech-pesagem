package my_app;

import java.nio.file.Path;
import java.util.Objects;

import javafx.application.Platform;
import javafx.scene.image.Image;
import megalodonte.ListenerManager;
import megalodonte.application.Context;
import megalodonte.application.ErrorReporter;
import megalodonte.application.MegalodonteApp;
import megalodonte.application.MegalodonteApplication;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.theme.ThemeManager;
import megalodonte.router.v4.Router;
import my_app.core.InitialRouteResolver;
import my_app.core.Themes;
import my_app.db.DB;
import my_app.db.services.PreferenciasService;
import my_app.domain.components.Components;
import my_app.core.AppRoutes;
import my_app.domain.telegram.TelegramNotifierFactory;
import my_app.infra.ProcessKiller;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static final boolean devMode = "true".equals(System.getenv("DEV_MODE"));

    public static final String APP_NAME = "Gobitech";

    public static final String APP_VERSION = System.getProperty("gobitech.appVersion", "dev");

    public static final String BASE_TITLE = String.format("%s - %s - Sistema de pesagem",
            APP_NAME,APP_VERSION);

    //public static final String ICON_PATH = "/assets/app_ico.png";
    public static final String ICON_PATH = "/assets/app_banner_square.png";

    public static Image loadIcon() {
        return new Image(Objects.requireNonNull(Main.class.getResourceAsStream(ICON_PATH)));
    }

    public static class AppHost extends MegalodonteApplication {}

    static void main(String[] args) {
        log.info("Iniciando {} versão {}", APP_NAME, APP_VERSION);
        corrigirArquiteturaNativa();
        configurarDiretorioNativoJSerialComm();
        MegalodonteApp.appName(APP_NAME);
        // Em Linux, garante um .desktop local pra rodar direto de JVM (IDE, gradle
        // run, dev.py) também ter ícone na dock — sem pacote instalado não existe
        // .desktop nenhum pra casar o WM_CLASS. Ver LinuxDesktopEntry.
        MegalodonteApp.appIcon(ICON_PATH);
        MegalodonteApp.run(AppHost.class, args, context->{
            final var stage = context.javafxStage();
            stage.getIcons().add(loadIcon());

            // registra o handler de erro o quanto antes — antes de qualquer Async.Run rodar
            ErrorReporter.register(Main::handleAppError);
            initialize(context);
        }, event -> {
            if (event == MegalodonteApp.Event.CloseRequest) {
                handleClose();
            }
        });
    }

    private static void corrigirArquiteturaNativa() {
        var arch = System.getProperty("os.arch");
        if (arch != null && arch.toLowerCase().contains("aarch64")) {
            var procArch = System.getenv("PROCESSOR_ARCHITECTURE");
            var procArchW6432 = System.getenv("PROCESSOR_ARCHITEW6432");
            if ((procArch != null && procArch.contains("AMD64")) ||
                    (procArchW6432 != null && procArchW6432.contains("AMD64"))) {
                System.setProperty("os.arch", "amd64");
            }
        }
    }

    // jSerialComm por padrão extrai sua DLL nativa em %TEMP% ou ~/.jSerialComm — se
// esse cache ficar corrompido/travado (ex: DLL de arquitetura errada presa por
// processo anterior), a extração falha com "Acesso negado" e a lib não sobe.
// Aponta pra um diretório próprio dentro de ~/.gobitech, sob controle exclusivo
// do app, evitando conflito com cache global do usuário/sistema.
    private static void configurarDiretorioNativoJSerialComm() {
        var nativeDir = Path.of(System.getProperty("user.home"), ".gobitech", "native");
        System.setProperty("jSerialComm.tmpdir", nativeDir.toString());
    }

    public static void handleClose(){
            log.info("Encerrando {}", APP_NAME);
            ListenerManager.disposeAll();
            DB.closeAllSessions();

        // força o encerramento de verdade, não espera thread nenhuma.
        ProcessKiller.killCurrentProcessAsync();
        Platform.exit();
        System.exit(0);
    }

    public static void initialize(Context context) {
        // Fontes de assets/fonts/ (Roboto incluso) já foram carregadas automaticamente
        // pelo Bootstrap antes disso rodar — ver megalodonte.base.theme.FontLoader.
        ThemeManager.setTheme(Themes.LIGHT); // mexe em Scene/Stylesheets -> FX thread, fica fora do Async.Run

        var routes = new AppRoutes().routes();
        Router router = new Router(routes, AppRoutes.Screens.SPLASH.name());
        context.useRouter(router).start(); // mostra a splash via fluxo normal do Router

        // Manda o log acumulado até agora pro Telegram a cada abertura do app — dá
        // visibilidade de suporte sem depender do cliente mandar o arquivo manualmente.
        // Caminho igual ao configurado em logback.xml. Propositalmente sem nenhum log
        // sobre esse envio (ver comentário em TelegramNotifier.enviarArquivo).
        Async.Run(() -> TelegramNotifierFactory.create().enviarArquivo(
                Path.of(System.getProperty("user.home"), ".gobitech", "logs", "gobitech.log"),
                "Log automático — " + APP_NAME + " " + APP_VERSION));

        Async.Run(() -> {
            // ---- tudo aqui roda fora da FX thread ----
            var flyway = Flyway.configure()
                    .dataSource(DB.production().url(), "", "")
                    .locations("classpath:flyway_migrations")
                    .baselineOnMigrate(true)
                    .load();
            flyway.repair();
            flyway.migrate();
            log.info("Migrations do banco aplicadas com sucesso");

            String rotaInicial = InitialRouteResolver.resolve();

            // ---- volta pra FX thread só pra trocar a splash pela rota real ----
            UI.runOnUi(() -> {
                var result = router.navigateOnStage(rotaInicial, context.javafxStage());
                context.useView(result);
            });
        });
    }

    private static void handleAppError(Throwable t) {
        log.error("Erro não tratado na aplicação", t);
        TelegramNotifierFactory.create().enviarMensagem("ERRO NA APLICAÇÃO: " + descreverErro(t));

        Platform.runLater(() -> {
            if (t instanceof IllegalArgumentException) {
                Components.ShowAlertError(t.getMessage());
            } else {
                Components.ShowAlertError("Ocorreu um erro inesperado. Detalhes foram registrados.");
            }
        });
    }

    // t.getMessage() é frequentemente null (ex: NullPointerException sem mensagem) —
    // sem tipo da exceção nem origem, a notificação chegava só como "null", sem
    // nenhuma pista de causa. Inclui o tipo e as primeiras linhas do stack trace.
    private static String descreverErro(Throwable t) {
        var sb = new StringBuilder(t.getClass().getName())
                .append(": ")
                .append(t.getMessage() != null ? t.getMessage() : "(sem mensagem)");
        var stack = t.getStackTrace();
        for (int i = 0; i < Math.min(3, stack.length); i++) {
            sb.append("\n  em ").append(stack[i]);
        }
        return sb.toString();
    }
}
