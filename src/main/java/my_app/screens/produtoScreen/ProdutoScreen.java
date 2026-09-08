package my_app.screens.produtoScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.SimpleTable;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.props.ColumnProps;
import megalodonte.props.SimpleTableProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;
import my_app.core.AppRoutes;
import my_app.db.models.ProdutoModel;
import my_app.domain.ContratoTelaCrudV3;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import my_app.db.models.EmpresaModel;
import my_app.infra.ListaPdfExporter;
import pack.utilities.DatePack;

import java.io.File;
import java.util.List;

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
    public String downloadListaPrefixo() {
        return "produtos";
    }

    @Override
    public ViewModelScreenContract<ProdutoModel> viewModel() {
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
                .column("Data de criação", it -> DatePack.localDateTimeToBrazilianDateTime(it.getDataCriacao()))
                .build()
                .onItemSelectChange(vm.selected::set)
                //.onItemDoubleClick(it -> showItemDetails(it, this.screenContext, 350));
                .onItemDoubleClick(it -> screenContext.router().spawnWindow(AppRoutes.Screens.DETAILS_PRODUTO.name() + "/" + it.getId()));

        return simpleTable;
    }

    @Override
    public void exportPdf(File destino, EmpresaModel empresa, List<ProdutoModel> snapshotFiltrado) throws Exception {
        var headers = java.util.List.of("ID", "Nome", "Unidade", "Desconto (%)", "Data de criacao");
        var rows = snapshotFiltrado.stream().map(p -> java.util.List.of(
                String.valueOf(p.getId()),
                p.getNome() != null ? p.getNome() : "",
                p.getUnidade() != null ? p.getUnidade() : "",
                p.getDesconto() != null ? p.getDesconto().toPlainString() : "0",
                DatePack.localDateTimeToBrazilianDateTime(p.getDataCriacao())
        )).toList();
        ListaPdfExporter.exportar(destino, empresa, "Lista de Produtos", headers, rows);
    }
}
