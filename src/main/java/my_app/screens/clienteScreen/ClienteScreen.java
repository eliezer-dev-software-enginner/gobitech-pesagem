package my_app.screens.clienteScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.SimpleTable;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.props.ColumnProps;
import megalodonte.props.SimpleTableProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;
import my_app.core.AppRoutes;
import my_app.db.models.ClienteModel;
import my_app.domain.ContratoTelaCrudV3;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import my_app.db.models.EmpresaModel;
import my_app.infra.ListaPdfExporter;
import pack.utilities.DatePack;
import pack.utilities.FormatterPack;

import java.io.File;
import java.util.List;

public class ClienteScreen implements ScreenComponent, ContratoTelaCrudV3<ClienteModel> {
    private final ClienteViewModel vm;
    private final ScreenContext screenContext;

    public ClienteScreen(ScreenContext ctx) {
        this.screenContext = ctx;
        this.vm = new ClienteViewModel(ctx);
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
        return "clientes";
    }

    @Override
    public ViewModelScreenContract<ClienteModel> viewModel() {
        return vm;
    }

    @Override
    public SimpleTable<ClienteModel> table() {
        var simpleTable = new SimpleTable<ClienteModel>(new SimpleTableProps().maxHeight(Components.TABLE_MAX_HEIGHT));
        simpleTable.fromData(vm.filteredList)
                .header()
                .columns()
                .column("ID", ClienteModel::getId, 60.0)
                .column("Loja", ClienteModel::getLoja)
                .column("Razão social", ClienteModel::getRazaoSocial)
                .column("CPF/CNPJ", it->FormatterPack.formatCpfCnpj(it.getCpfCnpj()))
                .column("Telefone", it->FormatterPack.formatPhone(it.getTelefone()))
                .column("Data de criação", it -> DatePack.localDateTimeToBrazilianDateTime(it.getDataCriacao()))
                .build()
                .onItemSelectChange(vm.selected::set)
                //.onItemDoubleClick(it -> showItemDetails(it, this.screenContext, 350));
                .onItemDoubleClick(it -> screenContext.router().spawnWindow(AppRoutes.Screens.DETAILS_CLIENTE.name() + "/" + it.getId()));

        return simpleTable;
    }

    @Override
    public void exportPdf(File destino, EmpresaModel empresa, List<ClienteModel> snapshotFiltrado) throws Exception {
        var headers = java.util.List.of("ID", "Loja", "Razao social", "CPF/CNPJ", "Telefone", "Data de criacao");
        var rows = vm.filteredList.get().stream().map(c -> java.util.List.of(
                String.valueOf(c.getId()),
                c.getLoja() != null ? c.getLoja() : "",
                c.getRazaoSocial() != null ? c.getRazaoSocial() : "",
                c.getCpfCnpj() != null ? FormatterPack.formatCpfCnpj(c.getCpfCnpj()) : "",
                c.getTelefone() != null ? FormatterPack.formatPhone(c.getTelefone()) : "",
                DatePack.localDateTimeToBrazilianDateTime(c.getDataCriacao())
        )).toList();
        ListaPdfExporter.exportar(destino, empresa, "Lista de Clientes", headers, rows);
    }
}
