package my_app.screens.homeScreen;

import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import megalodonte.ComputedState;
import megalodonte.base.Animations;
import megalodonte.base.async.RunnableThrowing;
import megalodonte.base.components.Component;
import megalodonte.base.state.State;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Button;
import megalodonte.components.Image;
import megalodonte.components.SpacerVertical;
import megalodonte.components.layout_components.Column;
import megalodonte.props.ButtonProps;
import megalodonte.props.ColumnProps;
import megalodonte.props.ImageProps;
import megalodonte.v2.Show;
import my_app.domain.SessaoUsuario;
import my_app.domain.components.Components;
import my_app.screens.homeScreen.HomeScreenViewModel.Secao;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.antdesignicons.AntDesignIconsOutlined;
import org.kordamp.ikonli.entypo.Entypo;

import java.util.ArrayList;

/**
 * Menu fixo à esquerda com os mesmos itens do app original ({@code UI.Sidebar} do
 * pesagemFinal): Pesagem, Produto, Cliente, Admin (aqui "Usuários", só pra quem é admin — ver
 * SessaoUsuario) e Logout. Recolhível: minimizada mostra só os ícones + a logo quadrada.
 */
public class Sidebar {

    public static final String BG = "#2f3030";
    public static final String TEXT_COLOR = "#ffffff";
    private static final String ICON_COLOR = "#ffffff";

    private static final double LARGURA_EXPANDIDA = 190;
    private static final double LARGURA_MINIMIZADA = 64;
    private static final double DIAMETRO_TOGGLE = 28;

    public static Component render(HomeScreenViewModel viewModel) {
        var minimizada = viewModel.sidebarMinimizada;

        var filhos = new ArrayList<Component>();
        filhos.add(logo(minimizada));
        filhos.add(new SpacerVertical(10));
        filhos.add(botaoNav("Início", Entypo.HOME, Secao.HOME, viewModel));
        filhos.add(botaoNav("Pesagem entrada", Entypo.HOME, Secao.HOME, viewModel));
        filhos.add(botaoNav("Pesagem de saida", Entypo.HOME, Secao.HOME, viewModel));
        filhos.add(botaoNav("Pesagem avulsa", Entypo.HOME, Secao.HOME, viewModel));
        filhos.add(botaoNav("Pesagem manual", Entypo.HOME, Secao.HOME, viewModel));
        //filhos.add(botaoNav("Pesagem", AntDesignIconsOutlined.CAR, Secao.PESAGENS, viewModel));
        // Gerenciar usuários é ato administrativo — quem não é admin não deve manipular
        // outros usuários, então o item nem aparece (mesmo padrão de "Gerar licença").
        if (SessaoUsuario.isAdmin()) {
            filhos.add(botaoNav("Usuários", Entypo.USERS, Secao.USUARIOS, viewModel));
        }
        filhos.add(new SpacerVertical().fill());
        filhos.add(botaoLogout(minimizada, viewModel::logout));

        var coluna = new Column(new ColumnProps().paddingAll(12).spacingOf(14).fillHeight().bgColor(BG))
                .children(filhos.toArray(new Component[0]));

        // ScaleProvider (megalodonte-base) detecta o fator de escala UMA VEZ, olhando
        // Screen.getPrimary() — e nunca mais reavalia, mesmo que a janela seja aberta/movida
        // pra um monitor diferente com DPI diferente do que foi detectado no boot. Numa tela
        // onde esse fator "errado" deixa menos altura de verdade disponível que o esperado, o
        // conteúdo da sidebar (que cresce com mais itens de nav, ex.: quando "Usuários"
        // aparece pra admin) pode passar da altura da janela. Envolver num ScrollPane garante
        // que o Logout sempre fica alcançável rolando, em vez de ficar cortado sem jeito de
        // clicar — independente de qual monitor a janela está.
        var scroll = (ScrollPane) Components.ScrollPaneDefault(coluna).getJavaFxNode();
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        aplicarLargura(scroll, minimizada.get());
        minimizada.subscribe(valor -> aplicarLargura(scroll, valor));

        return Component.CreateFromJavaFxNode(scroll);
    }

    private static void aplicarLargura(Region node, boolean minimizada) {
        double largura = minimizada ? LARGURA_MINIMIZADA : LARGURA_EXPANDIDA;
        node.setPrefWidth(largura);
        node.setMinWidth(largura);
        node.setMaxWidth(largura);
    }

    private static Component logo(State<Boolean> minimizada) {
        return Show.when(minimizada,
                () -> new Image("/assets/app_banner_square.png", new ImageProps().width(40).height(40).preserveRatio(true)),
                () -> new Image("/assets/app_banner.png", new ImageProps().width(130).height(48).preserveRatio(true))
        );
    }

    /**
     * Botão circular que flutua sobre a borda direita da sidebar (metade dentro, metade fora).
     * Não faz parte de {@link #render(HomeScreenViewModel)} — precisa ser o último filho de um
     * {@code Stack} que envolva a sidebar E a área de conteúdo ao lado (ver {@code HomeScreen}),
     * senão a metade que "vaza" pra fora da sidebar fica atrás do conteúdo vizinho e só a metade
     * de dentro aparece (ordem de pintura de irmãos numa Row segue a ordem dos filhos, não
     * sobreposição na tela). O ícone é um único {@code CHEVRON_LEFT} que gira até a posição
     * final em vez de trocar de ícone na marra — 0° mostra "<" (expandida), e girado 180° o
     * mesmo desenho vira ">" (minimizada), então a rotação já entrega o ícone certo pro estado
     * novo, animada. Posição: y ancorado no topo (30px do início da sidebar, perto da logo —
     * longe do Logout lá embaixo), x acompanha a largura atual (64/160) pra sempre ficar em cima
     * da borda, expandida ou não.
     */
    public static Component toggleButton(HomeScreenViewModel viewModel) {
        var minimizada = viewModel.sidebarMinimizada;

        var icone = Components.ikon(Entypo.CHEVRON_LEFT, 16, "#000000");
        var iconeNode = icone.getJavaFxNode();
        iconeNode.setRotate(minimizada.get() ? 180 : 0);
        minimizada.subscribe(valor ->
                Animations.rotate(iconeNode, iconeNode.getRotate(), valor ? 180 : 0, Duration.millis(250)).play()
        );

        var botao = new Button("", new ButtonProps()
                .bgColor("#ffffff")
                .borderColor(ThemeManager.theme().colors().primary())
                .borderWidth(2)
                .borderRadius((int) (DIAMETRO_TOGGLE / 2)))
                .icon(icone)
                .onClick(() -> minimizada.set(!minimizada.get()));

        var node = (javafx.scene.control.Button) botao.getJavaFxNode();
        node.setPrefSize(DIAMETRO_TOGGLE, DIAMETRO_TOGGLE);
        node.setMinSize(DIAMETRO_TOGGLE, DIAMETRO_TOGGLE);
        node.setMaxSize(DIAMETRO_TOGGLE, DIAMETRO_TOGGLE);

        StackPane.setAlignment(node, Pos.TOP_LEFT);
        node.setTranslateY(30);
        Runnable atualizarX = () -> node.setTranslateX(
                (minimizada.get() ? LARGURA_MINIMIZADA : LARGURA_EXPANDIDA) - DIAMETRO_TOGGLE / 2
        );
        atualizarX.run();
        minimizada.subscribe(valor -> atualizarX.run());

        return botao;
    }

    /**
     * Item de navegação: só a seção ativa fica com fundo amarelo (cor primária do tema) e
     * ícone/texto pretos (contraste melhor que branco sobre o amarelo); os demais ficam com
     * ícone e texto brancos sobre o fundo escuro da sidebar.
     */
    private static Component botaoNav(String texto, Ikon icone, Secao secao, HomeScreenViewModel viewModel) {
        var minimizada = viewModel.sidebarMinimizada;
        var textoComputado = ComputedState.of(() -> minimizada.get() ? "" : texto, minimizada);
        var selecionado = ComputedState.of(() -> viewModel.secaoAtiva.get() == secao, viewModel.secaoAtiva);
        var bgComputado = ComputedState.of(
                () -> selecionado.get() ? ThemeManager.theme().colors().primary() : "transparent",
                selecionado
        );
        var corComputada = ComputedState.of(
                () -> selecionado.get() ? "#000000" : TEXT_COLOR,
                selecionado
        );
        var iconeComputado = ComputedState.of(
                () -> Components.ikon(icone, 18, selecionado.get() ? "#000000" : ICON_COLOR),
                selecionado
        );

        var botao = new Button(textoComputado, new ButtonProps().fillWidth().bgColor(bgComputado).textColor(corComputada))
                .icon(iconeComputado)
                .onClick(() -> viewModel.navegarPara(secao));
        alinharEsquerda(botao);
        return botao;
    }

    private static Component botaoLogout(State<Boolean> minimizada, RunnableThrowing onClick) {
        var textoComputado = ComputedState.of(() -> minimizada.get() ? "" : "Logout", minimizada);

        var botao = new Button(textoComputado, new ButtonProps().fillWidth().bgColor("transparent").textColor(TEXT_COLOR))
                .icon(Components.ikon(Entypo.LOG_OUT, 18, ICON_COLOR))
                .onClick(onClick);
        alinharEsquerda(botao);
        return botao;
    }

    /**
     * Ícone+texto alinhados à esquerda em vez de centralizados — sem isso, cada botão centraliza
     * o grupo ícone+texto de acordo com o próprio comprimento do texto, e como cada item tem um
     * texto de tamanho diferente ("Início" vs. "Usuários"), os ícones acabavam em colunas X
     * diferentes. Alinhado à esquerda, o ícone sempre começa no mesmo x — mesma reta vertical
     * pra todos, texto mais longo ou mais curto só muda o quanto ele se estende à direita.
     */
    private static void alinharEsquerda(Component botao) {
        ((javafx.scene.control.Button) botao.getJavaFxNode()).setAlignment(Pos.CENTER_LEFT);
    }
}
