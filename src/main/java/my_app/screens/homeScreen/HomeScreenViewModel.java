package my_app.screens.homeScreen;

import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.router.v4.ScreenContext;
import my_app.core.AppRoutes;
import my_app.domain.components.Components;
import my_app.screens.clienteScreen.ClienteScreen;
import my_app.screens.dashboardScreen.DashboardScreen;
import my_app.screens.produtoScreen.ProdutoScreen;
import my_app.screens.pesagemScreen.PesagemScreen;
import my_app.screens.usuarioScreen.UsuarioScreen;

/**
 * A sidebar é fixa e o conteúdo à direita troca dentro da mesma janela — igual o app original
 * (cada tela lá recriava a Sidebar + o conteúdo daquela seção, num JFrame novo; aqui é a mesma
 * ideia sem recriar janela: uma tela embutida por vez, montada/destruída manualmente).
 */
public class HomeScreenViewModel {

    public enum Secao { HOME, PESAGENS, PRODUTOS, CLIENTES, USUARIOS }

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
            case PESAGENS -> new PesagemScreen(ctx);
            case PRODUTOS -> new ProdutoScreen(ctx);
            case CLIENTES -> new ClienteScreen(ctx);
            case USUARIOS -> new UsuarioScreen(ctx);
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
            destruirTelaAtual();
            telaAtiva.set(null);
            screenContext.navigateAndCloseOthers(AppRoutes.Screens.AUTH.name());
        });
    }

    public void onDestroy() {
        destruirTelaAtual();
    }
}
