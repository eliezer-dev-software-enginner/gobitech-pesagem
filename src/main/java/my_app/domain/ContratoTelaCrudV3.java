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
import my_app.utils.Utils;
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

    String downloadListaPrefixo();

    ViewModelScreenContract<T> viewModel();

    default void handleClickNew() {
        viewModel().ctx.router().spawnWindow(viewModel().screenNameSpawn+"/-1/add/");
    }

    default void handleClickBaixarLista() {
        var fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar lista em PDF");
        //fileChooser.setInitialFileName("relatório - " + Utils.timestampParaArquivo() + ".pdf");
        fileChooser.setInitialFileName(downloadListaPrefixo() + " - " + Utils.timestampParaArquivo() + ".pdf");
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

    void exportPdf(File destino, EmpresaModel empresa, List<T> snapshotFiltrado) throws Exception;

    default void handleClickMenuDelete() {
        if(viewModel().selected.get() == null)throw new IllegalArgumentException("Selecione o item na tabela antes!");

        viewModel().modoEdicaoState().set(false);
        viewModel().handleClickMenuDelete();
    }

    default void handleClickMenuEdit() {
        if(viewModel().selected.get() == null)throw new IllegalArgumentException("Selecione o item na tabela antes!");

        long id = viewModel().selected.get().getId();
        viewModel().ctx.router().spawnWindow(viewModel().screenNameSpawn+"/"+id+"/edit/");
    }

    SimpleTable<T> table();

    /**
     * Conteúdo extra no topo da página de lista, antes da busca/tabela — ex.: o filtro
     * avançado de {@code PesagemScreen}. Vazio por padrão.
     */
    default Component extraListContent() {
        return new Column();
    }

    default Component mainView() {
        return new Container(new ContainerProps().paddingAll(10).bgColor("#f3f4f6").fillHeight())
                .children(this.listPage());
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
                actionButton("Exportar","black","#CDD7D6", Entypo.DOWNLOAD, this::handleClickBaixarLista),
                actionButton("Editar","black","#ADA8BE", Entypo.EDIT, this::handleClickMenuEdit),
                actionButton("Excluir","white","#E55934", Entypo.TRASH, this::handleClickMenuDelete),
                new SpacerVertical(30),
                actionButton("Criar novo","black",null, Entypo.ADD_TO_LIST, this::handleClickNew)
        );
    }

    private Button actionButton(String title, String color, String bgColor, Ikon ikon, RunnableThrowing onclick){
        return new Button(title, new ButtonProps()
                .bgColor(bgColor!=null? bgColor : ThemeManager.theme().colors().primary())
                .paddingTop(ThemeManager.theme().padding().md())
                .paddingDown(ThemeManager.theme().padding().md())
                .paddingLeft(ThemeManager.theme().padding().md())
                .paddingRight(ThemeManager.theme().padding().md())
                .textColor(color))
                .onClick(onclick)
                .icon(Components.ikon(ikon,10, color));
    }

    default void onDestroy() {
        try {
            viewModel().onDestroy();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
