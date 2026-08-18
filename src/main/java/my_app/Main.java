package my_app;

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

public class Main {
    public static final boolean devMode = "true".equals(System.getenv("DEV_MODE"));

    public static final String APP_NAME = "Gobitech";

    public static final String APP_VERSION = System.getProperty("plics.appVersion", "dev");

    public static final String BASE_TITLE = String.format("%s - %s - Sistemas de balanças",
            APP_NAME,APP_VERSION);

    public static final String ICON_PATH = "/assets/app_ico.png";

    public static Image loadIcon() {
        return new Image(Objects.requireNonNull(Main.class.getResourceAsStream(ICON_PATH)));
    }

    public static class AppHost extends MegalodonteApplication {}

    static void main(String[] args) {
        MegalodonteApp.appName(APP_NAME);
        // Em Linux, garante um .desktop local pra rodar direto de JVM (IDE, gradle
        // run, dev.py) também ter ícone na dock — sem pacote instalado não existe
        // .desktop nenhum pra casar o WM_CLASS. Ver LinuxDesktopEntry.
        MegalodonteApp.appIcon(ICON_PATH);
        MegalodonteApp.run(AppHost.class, args, Main::start, Main::onEvent);
    }

    private static void start(Context context) {
            final var stage = context.javafxStage();

            final String[] images = {"/logo_32x32.png", "/logo_256x256.png"};

            for (String image : images) {
                stage.getIcons().add(new Image(Objects.requireNonNull(Main.class.getResourceAsStream(image))));
            }

            stage.getIcons().add(Main.loadIcon());
        // registra o handler de erro o quanto antes — antes de qualquer Async.Run rodar
        ErrorReporter.register(Main::handleAppError);
        initialize(context);
    }

    private static void onEvent(MegalodonteApp.Event ev) {
        if (ev == MegalodonteApp.Event.CloseRequest) {
            handleClose();
        }
    }

    public static void handleClose(){
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


        Async.Run(() -> {
            // ---- tudo aqui roda fora da FX thread ----
            var flyway = Flyway.configure()
                    .dataSource(DB.production().url(), "", "")
                    .locations("classpath:flyway_migrations")
                    .baselineOnMigrate(true)
                    .load();
            flyway.repair();
            flyway.migrate();

            boolean isFirstAccess = false;

            try (var preferenciasService = new PreferenciasService()) {
                var prefs = preferenciasService.listar();
                if (!prefs.isEmpty()) {
                    isFirstAccess = prefs.getFirst().isFirstAccess();
                }
            }

            String rotaInicial = InitialRouteResolver.resolve(isFirstAccess);

            // ---- volta pra FX thread só pra trocar a splash pela rota real ----
            UI.runOnUi(() -> {
                var result = router.navigateOnStage(rotaInicial, context.javafxStage());
                context.useView(result);
            });
        });
    }

    private static void handleAppError(Throwable t) {
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
