package my_app.screens.homeScreen;

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
import megalodonte.base.route.v2.ScreenContextInterface;
import my_app.core.AppRoutes;
import my_app.domain.SessaoUsuario;

public class HomeScreen implements ScreenComponent {

    private final HomeScreenViewModel viewModel;
    private final ScreenContextInterface ctx;

    public HomeScreen(ScreenContextInterface ctx) {
        this.ctx = ctx;
        this.viewModel = new HomeScreenViewModel(ctx);
    }

    @Override
    public void onMount() {
        viewModel.onMount();
    }

    @Override
    public void onDestroy() {
        viewModel.onDestroy();
    }

    public Component render() {
        var linha = new Row(new RowProps().fillHeight().fillWidth())
                .children(Sidebar.render(viewModel),
                        viewModel.contentArea.current());

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

    private Component menuBar() {
        boolean isAdmin = SessaoUsuario.isAdmin();
               // Pendência M3: reativar item quando a câmera fizer parte do fluxo
                        // .item("Conexão das câmeras", () -> ctx.spawnWindow(AppRoutes.Screens.CONEXAO_CAMERA.name(), e -> {}));

                        return new MenuBar()
                                .bgColor(Sidebar.BG)
                                .menu(new Menu("Gerencial")
                                        .textColor(Sidebar.TEXT_COLOR)
                                        .item("Empresa", () -> ctx.spawnWindow(AppRoutes.Screens.EMPRESA.name(), e -> {}))
                                        .item("Configurações", () -> ctx.spawnWindow(AppRoutes.Screens.CONFIGURACOES.name(), e -> {}))
                                        .item("Conexão da balança", () -> ctx.spawnWindow(AppRoutes.Screens.CONEXAO_BALANCA.name(), e -> {}))
                                        .item("Produtos", () -> ctx.spawnWindow(AppRoutes.Screens.PRODUTOS.name(), e -> {}))
                                        .item("Clientes", () -> ctx.spawnWindow(AppRoutes.Screens.CLIENTES.name(), e -> {}))
                                        .itemIf(isAdmin, "Usuários do sistema", () -> ctx.spawnWindow(AppRoutes.Screens.USUARIOS.name(), e -> {}))
                                        .itemIf(isAdmin, "Gerar licença", () -> ctx.spawnWindow(AppRoutes.Screens.LICENSA.name(), e -> {})))
                                .menu(new Menu("Logs")
                                        .textColor(Sidebar.TEXT_COLOR)
                                        .itemIf(isAdmin, "Ver logs da aplicação", () -> ctx.spawnWindow(AppRoutes.Screens.LOGS.name(), e -> {})));
    }
}
