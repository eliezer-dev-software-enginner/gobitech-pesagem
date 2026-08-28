package my_app.screens.homeScreen;

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

/**
 * A sidebar é fixa e o conteúdo à direita troca dentro da mesma janela — igual o app original
 * (cada tela lá recriava a Sidebar + o conteúdo daquela seção, num JFrame novo; aqui é a mesma
 * ideia sem recriar janela: uma tela embutida por vez, montada/destruída manualmente).
 */
public class HomeScreenViewModel {

    private static final Logger log = LoggerFactory.getLogger(HomeScreenViewModel.class);

    public enum Secao { HOME, PESAGENS_ENTRADA, PESAGENS_SAIDA, PESAGENS_AVULSA, PESAGEM_MANUAL, PESAGEM_HISTORICO }

    private final ScreenContext screenContext;

    public final State<Secao> secaoAtiva = State.of(Secao.HOME);
    public final State<ScreenComponent> telaAtiva = State.of(null);
    public final State<Boolean> sidebarMinimizada = State.of(false);

    private ScreenContext ctxTelaAtiva;

    public HomeScreenViewModel(ScreenContext screenContext) {
        this.screenContext = screenContext;

        // HOME é só mais uma tela embutida (o dashboard) — monta ela de cara, igual
        // navegarPara() faria pras demais seções, pra já abrir com os totais carregando.
        var ctx = new ScreenContext(screenContext.selfStage(), screenContext.router());
        this.ctxTelaAtiva = ctx;
        ScreenComponent telaInicial = new DashboardScreen(ctx);
        telaAtiva.set(telaInicial);
        telaInicial.onMount();
    }

    public void navegarPara(Secao secao) {
        if (secaoAtiva.get() == secao) return;

        destruirTelaAtual();

        var ctx = new ScreenContext(screenContext.selfStage(), screenContext.router());
        ScreenComponent tela = switch (secao) {
            case HOME -> new DashboardScreen(ctx);
            case PESAGENS_ENTRADA -> new PesagemEntradaScreen(ctx);
            case PESAGENS_SAIDA -> new PesagemSaidaScreen(ctx);
            case PESAGENS_AVULSA -> new PesagemAvulsaScreen(ctx);
            case PESAGEM_MANUAL -> new PesagemManualScreen(ctx);
            case PESAGEM_HISTORICO -> new PesagemHistoricoScreen(ctx);
        };

        this.ctxTelaAtiva = ctx;
        secaoAtiva.set(secao);
        telaAtiva.set(tela);
        tela.onMount();
    }

    private void destruirTelaAtual() {
        var telaAnterior = telaAtiva.get();
        if (telaAnterior == null) return;

        if (ctxTelaAtiva != null) ctxTelaAtiva.scope().cancel();
        telaAnterior.onDestroy();
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
