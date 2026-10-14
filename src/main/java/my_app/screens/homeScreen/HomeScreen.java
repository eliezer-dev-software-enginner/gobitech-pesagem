package my_app.screens.homeScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.route.v2.ScreenContextInterface;
import megalodonte.base.state.Subscription;
import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import megalodonte.jsonc.UiSession;
import my_app.core.AppRoutes;
import my_app.domain.SessaoUsuario;

/** Native shell and navigation are defined in JSONC; the dashboard occupies its named slot. */
public final class HomeScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(HomeScreen.class, "/views/home.view.jsonc");
    private final HomeScreenViewModel viewModel;
    private final UiSession session;
    private final Subscription minimizedSubscription;

    public HomeScreen(ScreenContextInterface context) {
        viewModel = new HomeScreenViewModel(context);
        session = VIEW.open(new Actions()
                .register("toggle", ignored -> viewModel.sidebarMinimizada.set(!viewModel.sidebarMinimizada.get()))
                .register("home", ignored -> viewModel.navigateFromSidebar(AppRoutes.Screens.HOME))
                .register("entry", ignored -> viewModel.navigateFromSidebar(AppRoutes.Screens.PESAGEM_ENTRADA))
                .register("exit", ignored -> viewModel.navigateFromSidebar(AppRoutes.Screens.PESAGEM_SAIDA))
                .register("adhoc", ignored -> viewModel.navigateFromSidebar(AppRoutes.Screens.PESAGEM_AVULSA))
                .register("manual", ignored -> viewModel.navigateFromSidebar(AppRoutes.Screens.PESAGEM_MANUAL))
                .register("history", ignored -> viewModel.navigateFromSidebar(AppRoutes.Screens.PESAGEM_HISTORICO))
                .register("logout", ignored -> viewModel.logout())
                .register("company", ignored -> context.spawnWindow(AppRoutes.Screens.EMPRESA.name()))
                .register("settings", ignored -> context.spawnWindow(AppRoutes.Screens.CONFIGURACOES.name()))
                .register("scale", ignored -> context.spawnWindow(AppRoutes.Screens.CONEXAO_BALANCA.name()))
                .register("products", ignored -> context.spawnWindow(AppRoutes.Screens.PRODUTOS.name()))
                .register("customers", ignored -> context.spawnWindow(AppRoutes.Screens.CLIENTES.name()))
                .register("users", ignored -> context.spawnWindow(AppRoutes.Screens.USUARIOS.name()))
                .register("licenses", ignored -> context.spawnWindow(AppRoutes.Screens.LICENSA.name()))
                .register("logs", ignored -> context.spawnWindow(AppRoutes.Screens.LOGS.name())))
                .mount("content", viewModel.contentArea.current());
        session.states().state("showAdmin", Boolean.class).set(SessaoUsuario.isAdmin());
        minimizedSubscription = viewModel.sidebarMinimizada.observe(value -> {
            session.states().state("expanded", Boolean.class).set(!value);
            session.states().state("compact", Boolean.class).set(value);
        });
    }

    @Override public Component render() { return session.render(); }
    @Override public void onMount() { viewModel.onMount(); }
    @Override public void onDestroy() {
        minimizedSubscription.close();
        session.close();
        viewModel.onDestroy();
    }
}
