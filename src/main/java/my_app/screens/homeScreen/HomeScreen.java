package my_app.screens.homeScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.*;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.FlowRow;
import megalodonte.ForEachState;
import megalodonte.base.state.State;
import megalodonte.props.*;
import megalodonte.router.v4.ScreenContext;
import my_app.Main;
import my_app.core.AppRoutes;
import my_app.domain.SessaoUsuario;

import java.util.List;

public class HomeScreen implements ScreenComponent {

    private final HomeScreenViewModel viewModel;
    private final ScreenContext ctx;

    public HomeScreen(ScreenContext ctx) {
        this.ctx = ctx;
        this.viewModel = new HomeScreenViewModel(ctx);
    }

    @Override
    public void onMount() {
    }

    @Override
    public void onDestroy() {
        viewModel.onDestroy();
    }

    public Component render() {
        return new Container(new ContainerProps().fillHeight().bgColor("#f5f5f5")).children(
                menuBar(),
                new Container(new ContainerProps().paddingAll(20)).children(
                        new Text("Balanças Gobitech", new TextProps().fontSize(ThemeManager.theme().typography().title()).bold()),
                        new SpacerVertical(4),
                        new Text("Sistema de pesagem", new TextProps().fontSize(ThemeManager.theme().typography().body())),
                        new SpacerVertical(20),
                        centerContent()
                )
        );
    }

    private Column centerContent() {
        var cardsState = State.of(cardItemList);
        ForEachState<CardItem, Component> cardsForEach = ForEachState.of(cardsState, this::CardColumn);

        return new Column(new ColumnProps().spacingOf(10).fillWidth()).children(
                new FlowRow(new FlowRowProps().fillWidth().spacingOf(10))
                        .items(cardsForEach)
        );
    }

    private Component menuBar() {
        var suporteMenu = new Menu("Suporte")
                .item("Relatar erro", () -> ctx.router().spawnWindow(AppRoutes.Screens.RELATAR_ERRO.name(), e -> {}))
                .item("Sugerir melhoria/funcionalidade", () -> ctx.router().spawnWindow(AppRoutes.Screens.SUGERIR_MELHORIA.name(), e -> {}))
                .item("Novidades dessa atualização", () -> ctx.router().spawnWindow(AppRoutes.Screens.INFO_UPDATE.name(), e -> {}));

        var gerencialMenu = new Menu("Gerencial")
                .item("Empresa", () -> ctx.router().spawnWindow(AppRoutes.Screens.EMPRESA.name(), e -> {}))
                .item("Conexão da balança", () -> ctx.router().spawnWindow(AppRoutes.Screens.CONEXAO_BALANCA.name(), e -> {}));

        // Só quem está logado como admin vê a opção de gerar licença — ver
        // SessaoUsuario/DECISIONS.md (André usa seu login de admin em qualquer PC).
        if (SessaoUsuario.isAdmin()) {
            gerencialMenu.item("Gerar licença", () -> ctx.router().spawnWindow(AppRoutes.Screens.LICENSA.name(), e -> {}));
        }

        return new MenuBar()
                .menu(new Menu("Preferências").item("Abrir tela", () -> ctx.router().spawnWindow(AppRoutes.Screens.PREFERENCIAS.name(), e -> {})))
                .menu(new Menu("Cadastros")
                        .item("Usuários", () -> ctx.router().spawnWindow(AppRoutes.Screens.USUARIOS.name(), e -> {}))
                        .item("Clientes", () -> ctx.router().spawnWindow(AppRoutes.Screens.CLIENTES.name(), e -> {}))
                        .item("Produtos", () -> ctx.router().spawnWindow(AppRoutes.Screens.PRODUTOS.name(), e -> {}))
                )
                .menu(gerencialMenu)
                .menu(suporteMenu);
    }

    record CardItem(String img, String title, String desc, String destination) {}

    final List<CardItem> cardItemList = List.of(
            new CardItem("/assets/produtos.png", "Pesagens", "Registrar e consultar pesagens", AppRoutes.Screens.PESAGENS.name()),
            new CardItem("/assets/clientes.png", "Clientes", "Gerencie seus clientes", AppRoutes.Screens.CLIENTES.name()),
            new CardItem("/assets/produtos.png", "Produtos", "Gerencie seus produtos", AppRoutes.Screens.PRODUTOS.name()),
            new CardItem("/assets/clientes.png", "Usuários", "Gerencie usuários do sistema", AppRoutes.Screens.USUARIOS.name())
    );

    Component CardColumn(CardItem cardItem) {
        return new Clickable(
                new Card(
                        new Column(new ColumnProps().centerHorizontally().paddingAll(20))
                                .c_child(new Image(cardItem.img, new ImageProps().size(60)))
                                .c_child(new Text(cardItem.title, new TextProps().fontSize(ThemeManager.theme().typography().body()).bold()))
                                .c_child(new Text(cardItem.desc, new TextProps().fontSize(ThemeManager.theme().typography().small()))),
                        new CardProps().padding(0).height(170).width(230).borderRadius(20)),
                () -> ctx.router().spawnWindow(cardItem.destination, e -> {})
        );
    }
}
