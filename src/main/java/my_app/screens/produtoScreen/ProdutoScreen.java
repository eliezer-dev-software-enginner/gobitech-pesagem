package my_app.screens.produtoScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Card;
import megalodonte.components.LineHorizontal;
import megalodonte.components.SimpleTable;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.FlowRow;
import megalodonte.props.ColumnProps;
import megalodonte.props.FlowRowProps;
import megalodonte.props.SimpleTableProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;
import my_app.db.models.ProdutoModel;
import my_app.domain.ContratoTelaCrudV3;
import my_app.domain.Data;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import my_app.infra.CsvExporter;
import my_app.utils.DateUtils;

public class ProdutoScreen implements ScreenComponent, ContratoTelaCrudV3<ProdutoModel> {
    private final ProdutoScreenViewModel vm;
    private final ScreenContext screenContext;

    public ProdutoScreen(ScreenContext ctx) {
        this.screenContext = ctx;
        this.vm = new ProdutoScreenViewModel(ctx);
    }

    @Override
    public void onMount() {
        vm.fetchListData();
    }

    @Override
    public void onDestroy() {
        ContratoTelaCrudV3.super.onDestroy();
    }

    @Override
    public Component render() {
        return mainView();
    }

    @Override
    public Component form() {
        return new Container();
    }

    @Override
    public ViewModelScreenContract viewModel() {
        return vm;
    }

    @Override
    public SimpleTable<ProdutoModel> table() {
        var simpleTable = new SimpleTable<ProdutoModel>(new SimpleTableProps().maxHeight(Components.TABLE_MAX_HEIGHT));
        simpleTable.fromData(vm.filteredList)
                .header()
                .columns()
                .column("ID", ProdutoModel::getId, 60.0)
                .column("Nome", ProdutoModel::getNome)
                .column("Unidade", ProdutoModel::getUnidade)
                .column("Desconto (%)", it -> it.getDesconto() == null ? "0" : it.getDesconto().toPlainString())
                .column("Data de criação", it -> DateUtils.localDateTimeToBrazilianDateTime(it.getDataCriacao()))
                .build()
                .onItemSelectChange(vm.selected::set);
                //.onItemDoubleClick(it -> showItemDetailsComAcoes(it, this.screenContext, 350));

        return simpleTable;
    }

    @Override
    public void exportCsv(java.io.File destino) throws Exception {
        var headers = java.util.List.of("ID", "Nome", "Unidade", "Desconto (%)", "Data de criacao");
        var rows = vm.filteredList.get().stream().map(p -> java.util.List.of(
                String.valueOf(p.getId()),
                p.getNome() != null ? p.getNome() : "",
                p.getUnidade() != null ? p.getUnidade() : "",
                p.getDesconto() != null ? p.getDesconto().toPlainString() : "0",
                DateUtils.localDateTimeToBrazilianDateTime(p.getDataCriacao())
        )).toList();
        CsvExporter.exportar(destino, headers, rows);
    }

    public Component itemDetails(ProdutoModel model) {
        return new Column(new ColumnProps().paddingAll(20))
                .c_child(new Text("Detalhes do produto", new TextProps().fontSize(ThemeManager.theme().typography().subtitle())))
                .c_child(new SpacerVertical(20))
                .c_child(Components.TextWithDetails("ID: ", model.getId()))
                .c_child(Components.TextWithDetails("Nome: ", model.getNome()))
                .c_child(Components.TextWithDetails("Unidade: ", model.getUnidade()))
                .c_child(Components.TextWithDetails("Desconto padrão (%): ", model.getDesconto() == null ? "0" : model.getDesconto().toPlainString()))
                .c_child(Components.TextWithDetails("Data de criação: ", DateUtils.localDateTimeToBrazilianDateTime(model.getDataCriacao())))
                .c_child(Components.TextWithDetails("Observações: ", model.getObservacoes(), true));
    }
}
