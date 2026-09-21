package my_app.screens.homeScreen;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.Getter;
import megalodonte.base.components.Ref;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.components.layout_components.Container;
import megalodonte.props.ContainerProps;
import megalodonte.base.route.v2.ScreenContextInterface;
import my_app.core.AppRoutes;
import my_app.domain.components.Components;
import my_app.screens.dashboardScreen.DashboardScreen;
import my_app.screens.pesagemScreen.PesagemAvulsaScreen;
import my_app.screens.pesagemScreen.PesagemEntradaScreen;
import my_app.screens.pesagemScreen.PesagemHistoricoScreen;
import my_app.screens.pesagemScreen.PesagemManualScreen;
import my_app.screens.pesagemScreen.PesagemSaidaScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HomeScreenViewModel {

    private static final Logger log = LoggerFactory.getLogger(HomeScreenViewModel.class);

    private final ScreenContextInterface screenContext;

    public final State<AppRoutes.Screens> screenAtiva = State.of(AppRoutes.Screens.HOME);
    public final State<Boolean> sidebarMinimizada = State.of(false);

    public final Ref<Container> contentArea = new Ref<>();
    public final Ref<ScreenComponent> dashboardRef = new Ref<>();

    public HomeScreenViewModel(ScreenContextInterface screenContext) {
        this.screenContext = screenContext;
        contentArea.setCurrent(new Container(new ContainerProps().fillHeight().bgColor("#f3f4f6")));
        dashboardRef.setCurrent(new DashboardScreen(screenContext));

        contentArea.current().children(dashboardRef.current().render());
        // Sidebar tem largura fixa — a área de conteúdo precisa esticar pra ocupar o resto
        // da Row; Row só faz isso automaticamente pra SpacerHorizontal, não pra qualquer filho.
        HBox.setHgrow(contentArea.current().getJavaFxNode(), Priority.ALWAYS);
    }

    public void spawnWindow(AppRoutes.Screens screen) {
        screenAtiva.set(screen);
        screenContext.spawnWindow(screen.name());
    }

    public void logout() {
        Components.ShowAlertAdvice("Deseja realmente sair?", () -> {
            log.info("Logout realizado");
            screenContext.navigateAndCloseOthers(AppRoutes.Screens.AUTH.name());
        });
    }

    public void onMount(){
        dashboardRef.current().onMount();
    }

    public void onDestroy() {
        dashboardRef.current().onDestroy();
    }
}
