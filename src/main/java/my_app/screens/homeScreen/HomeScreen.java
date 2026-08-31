package my_app.screens.homeScreen;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.Menu;
import megalodonte.components.MenuBar;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.Row;
import megalodonte.components.layout_components.Stack;
import megalodonte.props.ContainerProps;
import megalodonte.props.RowProps;
import megalodonte.router.v4.ScreenContext;
import my_app.core.AppRoutes;
import my_app.domain.SessaoUsuario;

public class HomeScreen implements ScreenComponent {

    private final HomeScreenViewModel viewModel;
    private final ScreenContext ctx;
    private final Container contentArea = new Container(new ContainerProps().fillHeight().bgColor("#f3f4f6"));

    public HomeScreen(ScreenContext ctx) {
        this.ctx = ctx;
        this.viewModel = new HomeScreenViewModel(ctx);
        viewModel.telaAtiva.subscribe(tela -> renderConteudo());
        // Sidebar tem largura fixa — a área de conteúdo precisa esticar pra ocupar o resto
        // da Row; Row só faz isso automaticamente pra SpacerHorizontal, não pra qualquer filho.
        HBox.setHgrow(contentArea.getJavaFxNode(), Priority.ALWAYS);
    }

    @Override
    public void onMount() {
    }

    @Override
    public void onDestroy() {
        viewModel.onDestroy();
    }

    public Component render() {
        renderConteudo();

        var linha = new Row(new RowProps().fillHeight().fillWidth())
                .children(Sidebar.render(viewModel), contentArea);

        // O botão de toggle precisa ser o último filho de um Stack que envolve Sidebar E
        // contentArea juntos (não só a Sidebar) — ver o porquê no javadoc de
        // Sidebar.toggleButton(): a metade do círculo que "vaza" pra fora da largura da
        // sidebar só pinta por cima do contentArea vizinho se os dois estiverem dentro do
        // mesmo Stack (irmãos numa Row simples pintam na ordem dos filhos, não por
        // sobreposição na tela).
        var corpo = new Stack().children(linha, Sidebar.toggleButton(viewModel));
        var corpoNode = (StackPane) corpo.getJavaFxNode();
        corpoNode.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(corpoNode, Priority.ALWAYS);

        return new Container(new ContainerProps().fillHeight().paddingAll(0)).children(
                menuBar(),
                corpo
        );
    }

    private void renderConteudo() {
        Component conteudo = viewModel.telaAtiva.get().render();
        ((VBox) contentArea.getJavaFxNode()).getChildren().setAll(conteudo.getJavaFxNode());
    }

    private Component menuBar() {
        var suporteMenu = new Menu("Logs")
                .textColor(Sidebar.TEXT_COLOR)
                .item("Ver logs da aplicação", () -> ctx.router().spawnWindow(AppRoutes.Screens.LOGS.name(), e -> {}));
        
        var gerencialMenu = new Menu("Gerencial")
                .textColor(Sidebar.TEXT_COLOR)
                .item("Empresa", () -> ctx.router().spawnWindow(AppRoutes.Screens.EMPRESA.name(), e -> {}))
                .item("Conexão da balança", () -> ctx.router().spawnWindow(AppRoutes.Screens.CONEXAO_BALANCA.name(), e -> {}))
                .item("Produtos", () -> ctx.router().spawnWindow(AppRoutes.Screens.PRODUTOS.name(), e -> {}))
                .item("Clientes", () -> ctx.router().spawnWindow(AppRoutes.Screens.CLIENTES.name(), e -> {}))
                //TODO: Só deve liverar se admin SessaoUsuario.isAdmin()
                .item("Usuários do sistema", () -> ctx.router().spawnWindow(AppRoutes.Screens.USUARIOS.name(), e -> {}));
               // .item("Conexão das câmeras", () -> ctx.router().spawnWindow(AppRoutes.Screens.CONEXAO_CAMERA.name(), e -> {}));

        // Só quem está logado como admin vê a opção de gerar licença — ver
        // SessaoUsuario/DECISIONS.md (André usa seu login de admin em qualquer PC).
        if (SessaoUsuario.isAdmin()) {
            gerencialMenu.item("Gerar licença", () -> ctx.router().spawnWindow(AppRoutes.Screens.LICENSA.name(), e -> {}));
        }

        return new MenuBar()
                .bgColor(Sidebar.BG)
                .menu(gerencialMenu)
                .menu(suporteMenu);
    }
}
