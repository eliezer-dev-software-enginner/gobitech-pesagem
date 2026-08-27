package my_app.domain;

import java.io.File;
import java.util.List;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.async.RunnableThrowing;
import megalodonte.base.components.Component;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Button;
import megalodonte.components.Card;
import megalodonte.components.SimpleTable;
import megalodonte.components.SpacerVertical;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.Row;
import megalodonte.components.layout_components.Stack;
import megalodonte.props.ButtonProps;
import megalodonte.props.CardProps;
import megalodonte.props.ColumnProps;
import megalodonte.props.ContainerProps;
import megalodonte.props.RowProps;
import megalodonte.router.v4.ScreenContext;
import megalodonte.v2.Show;
import my_app.core.Identifier;
import my_app.db.models.EmpresaModel;
import my_app.domain.components.Components;
import my_app.db.services.EmpresaService;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.antdesignicons.AntDesignIconsOutlined;
import org.kordamp.ikonli.entypo.Entypo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lista e formulário são páginas mutuamente exclusivas — "Criar novo" navega pra dentro do
 * formulário, "Voltar" (ou salvar com sucesso) navega de volta pra lista. Igual o app antigo
 * (Table -> Form -> Table), em vez do formulário só recolher/expandir em cima da lista.
 * <p>
 * Editar/Excluir/Clonar não ficam mais numa barra fixa no topo da lista — moraram pro modal de
 * detalhes (duplo-clique numa linha), também igual o app antigo (clique na linha abre o
 * "Profile", que é de lá que se edita/exclui/clona).
 */
public interface ContratoTelaCrudV3<T extends Identifier> {

    Logger log = LoggerFactory.getLogger(ContratoTelaCrudV3.class);

    ViewModelScreenContract<T> viewModel();

    default void handleClickNew() {
        viewModel().ctx.router().spawnWindow(viewModel().screenNameSpawn+"/-1/add/");
    }

    default void handleClickBaixarLista() {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar lista em PDF");
        fileChooser.setInitialFileName("lista.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File destino = fileChooser.showSaveDialog(viewModel().ctx.selfStage());
        if (destino == null) return;

        // captura ainda na FX thread — mesma thread que escreve filteredList
        var snapshotFiltrado = viewModel().filteredList.get();

        Async.Run(() -> {
            try {
                var empresaService = new EmpresaService();
                var empresa = empresaService.buscarUnico();
                empresaService.close();
                exportPdf(destino, empresa, snapshotFiltrado); // passa o snapshot, não lê vm de novo
                UI.runOnUi(() -> Components.ShowPopup(viewModel().ctx, "PDF salvo em: " + destino.getAbsolutePath()));
            } catch (Exception e) {
                log.error("Erro ao exportar PDF", e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao exportar: " + e.getMessage()));
            }
        });
    }

//    default void handleClickBaixarLista() {
//        var fileChooser = new FileChooser();
//        fileChooser.setTitle("Salvar lista em PDF");
//        fileChooser.setInitialFileName("lista.pdf");
//        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
//        File destino = fileChooser.showSaveDialog(viewModel().ctx.selfStage());
//        if (destino == null) return;
//
//        megalodonte.base.async.Async.Run(() -> {
//            try {
//                var empresaService = new EmpresaService();
//                var empresa = empresaService.buscarUnico();
//                empresaService.close();
//                exportPdf(destino, empresa);
//                UI.runOnUi(() -> Components.ShowPopup(viewModel().ctx, "PDF salvo em: " + destino.getAbsolutePath()));
//            } catch (Exception e) {
//                log.error("Erro ao exportar PDF", e);
//                UI.runOnUi(() -> Components.ShowAlertError("Erro ao exportar: " + e.getMessage()));
//            }
//        });
//    }

    //void exportPdf(java.io.File destino, EmpresaModel empresa) throws Exception;
    void exportPdf(File destino, EmpresaModel empresa, List<T> snapshotFiltrado) throws Exception;

    default void handleClickMenuDelete() {
        if(viewModel().selected.get() == null)throw new IllegalArgumentException("Selecione o item na tabela antes!");

        viewModel().modoEdicaoState().set(false);
        viewModel().handleClickMenuDelete();
    }

    default void handleClickMenuClone() {
        viewModel().formIsVisible.set(true);
        populateFieldsFromModel();
        viewModel().modoEdicaoState().set(false);
    }

    default void handleClickMenuEdit() {
//        viewModel().formIsVisible.set(true);
//        populateFieldsFromModel();
//        viewModel().modoEdicaoState().set(true);

        if(viewModel().selected.get() == null)throw new IllegalArgumentException("Selecione o item na tabela antes!");

        long id = viewModel().selected.get().getId();
        viewModel().ctx.router().spawnWindow(viewModel().screenNameSpawn+"/"+id+"/edit/");
    }

    default void handleClickVoltar() {
        viewModel().modoEdicaoState().set(false);
        viewModel().voltarParaLista();
    }

    SimpleTable<T> table();

    /**
     * Mantido por enquanto apenas porque PesagemScreen o utiliza
     * @return
     */
    @Deprecated(forRemoval = true)
    Component form();
    /**
     * Mantido por enquanto apenas porque PesagemScreen o utiliza
     * @return
     */
    @Deprecated(forRemoval = true)
    Component itemDetails(T model);

    /**
     * Conteúdo extra no topo da página de lista, antes da busca/tabela — ex.: o filtro
     * avançado de {@code PesagemScreen}. Vazio por padrão.
     */
    default Component extraListContent() {
        return new Column();
    }

    default Component mainView() {
        // .fillHeight() no Show é essencial: sem ele, o Show trava a própria altura em
        // USE_PREF_SIZE (não estica, não encolhe) — a página de lista/formulário nunca é
        // forçada a caber no espaço real disponível, então o ScrollPane lá dentro nunca é
        // forçado a rolar de verdade; ele só cresce, e o conteúdo que não cabe na janela some
        // sem jeito de rolar até ele (reportado: tabela cheia empurrando "Criar novo" pra fora).
        return new Container(new ContainerProps().paddingAll(10).bgColor("#f3f4f6").fillHeight())
                .children(
                        Show.when(viewModel().formIsVisible, this::formPage, this::listPage).fillHeight()
                );
    }

    /**
     * "Criar novo" flutua em posição absoluta (canto inferior direito, ~20px de margem) por
     * cima do conteúdo scrollável, em vez de ficar no fluxo normal — assim continua visível e
     * clicável mesmo com a tabela cheia, sem precisar rolar até o fim da página pra achá-lo.
     */
    private Component listPage() {
        Component conteudo = Components.ScrollPaneDefault(
                new Column(new ColumnProps().fillWidth().spacingOf(15))
                        .children(
                                extraListContent(),
                                new Card(
                                        new Column(new ColumnProps().fillWidth().spacingOf(15))
                                                .children(
                                                        Components.searchInput(viewModel().searchState, "Pesquisar"),
                                                        table()
                                                ),
                                        new CardProps().fillWidth().paddingAll(20).bgColor("#ffffff")
                                )
                        )
        );

        return new Stack()
                .children(conteudo)
                .childInCorner(actionButtonsRow(), Stack.Corner.BOTTOM_RIGHT, 20)
                .fillHeight();
    }

    private Row actionButtonsRow(){
        return new Row(new RowProps().spacingOf(10).hugWidth()).children(
                actionButton("Baixar lista","black","#CDD7D6", Entypo.DOWNLOAD, this::handleClickBaixarLista),
                actionButton("Editar","black","#ADA8BE", Entypo.EDIT, this::handleClickMenuEdit),
                actionButton("Excluir","white","#E55934", Entypo.TRASH, this::handleClickMenuDelete),
                new SpacerVertical(30),
                actionButton("Criar novo","black",null, Entypo.ADD_TO_LIST, this::handleClickNew)
        );
    }

    private Button actionButton(String title, String color, String bgColor, Ikon ikon, RunnableThrowing onclick){
        return new Button(title, new ButtonProps()
                .bgColor(bgColor!=null? bgColor : ThemeManager.theme().colors().primary())
                .textColor(color))
                .onClick(onclick)
                .icon(Components.ikon(ikon,10, color));
    }

    private Component formPage() {
        return Components.ScrollPaneDefault(
                new Column(new ColumnProps().fillWidth().spacingOf(15))
                        .children(
                                new Button("Voltar", new ButtonProps().bgColor("#e5e7eb")
                                        .textColor("#111"))
                                        .onClick(this::handleClickVoltar)
                                        .icon(Components.ikon(AntDesignIconsOutlined.LEFT, 12, "black")),
                                new Card(form(), new CardProps().fillWidth().paddingAll(20).bgColor("#ffffff"))
                        )
        );
    }

    /**
     * Duplo-clique numa linha abre isso: os detalhes de {@code model} + Editar/Excluir/Clonar
     * embaixo. Cada ação fecha o modal (é uma janela própria, ver {@code Components.ShowModal})
     * antes de disparar — a edição de fato acontece na tela principal, atrás do modal.
     */
    default void showItemDetailsComAcoes(T model, ScreenContext ctx, int height) {
        Stage[] modalStage = new Stage[1];
        Runnable fechar = () -> {
            if (modalStage[0] != null) modalStage[0].close();
        };

        Component conteudo = new Column(new ColumnProps().fillWidth().spacingOf(15))
                .children(
                        itemDetails(model),
                        new Row(new RowProps().fillWidth().spacingOf(10))
                                .children(
                                        new Button("Editar", new ButtonProps().bgColor("#2563eb").textColor("white"))
                                                .onClick(() -> {
                                                    fechar.run();
                                                    handleClickMenuEdit();
                                                }),
                                        new Button("Clonar", new ButtonProps().bgColor("#6b7280").textColor("white"))
                                                .onClick(() -> {
                                                    fechar.run();
                                                    handleClickMenuClone();
                                                }),
                                        new Button("Excluir", new ButtonProps().bgColor("#ef4444").textColor("white"))
                                                .onClick(() -> {
                                                    fechar.run();
                                                    handleClickMenuDelete();
                                                })
                                )
                );

        modalStage[0] = Components.ShowModal(conteudo, ctx, height);
    }

    default void populateFieldsFromModel() {
        viewModel().populateFieldsFromModel();
    }

    default void clearForm() {
        viewModel().clearForm();
    }

    default void handleAddOrUpdate() {
        try {
            viewModel().handleAddOrUpdate();
            viewModel().modoEdicaoState().set(false);
        } catch (Exception e) {
            log.error("Erro em handleAddOrUpdate", e);
            UI.runOnUi(() -> Components.ShowAlertError(e.getMessage()));
        }

    }

    default void onDestroy() {
        try {
            viewModel().onDestroy();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
