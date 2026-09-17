package my_app.screens.homeScreen;

import lombok.Getter;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.router.v4.ScreenContext;
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

    @Getter
    private final ScreenContext screenContext;

    public final State<AppRoutes.Screens> screenAtiva = State.of(AppRoutes.Screens.HOME);
    public final State<Boolean> sidebarMinimizada = State.of(false);

    private ScreenContext ctxTelaAtiva;

    public HomeScreenViewModel(ScreenContext screenContext) {
        this.screenContext = screenContext;
    }

    public void navegarPara(AppRoutes.Screens screen) {
        destruirTelaAtual();
    }

    private void destruirTelaAtual() {

        if (ctxTelaAtiva != null) ctxTelaAtiva.scope().cancel();
        ctxTelaAtiva = null;
    }

    public void logout() {
        Components.ShowAlertAdvice("Deseja realmente sair?", () -> {
            // Não zera telaAtiva: renderConteudo() faz telaAtiva.get().render() sem checar
            // null (telaAtiva nunca fica null depois que o construtor passou a montar o
            // Dashboard de cara — ver comentário lá). destruirTelaAtual() já limpa os recursos
            // da tela atual; navegarAndCloseOthers troca a Scene inteira por AUTH logo em
            // seguida, então não sobra nenhum render() acontecendo em cima de um telaAtiva nulo.
            log.info("Logout realizado");
            destruirTelaAtual();
            screenContext.navigateAndCloseOthers(AppRoutes.Screens.AUTH.name());
        });
    }

    public void onDestroy() {
        destruirTelaAtual();
    }
}
