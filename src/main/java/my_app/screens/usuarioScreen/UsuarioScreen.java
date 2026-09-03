package my_app.screens.usuarioScreen;

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
import my_app.db.models.UsuarioModel;
import my_app.domain.ContratoTelaCrudV3;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import my_app.db.models.EmpresaModel;
import my_app.infra.ListaPdfExporter;
import pack.utilities.DatePack;

import java.io.File;
import java.util.List;

public class UsuarioScreen implements ScreenComponent, ContratoTelaCrudV3<UsuarioModel> {
    private final UsuarioScreenViewModel vm;
    private final ScreenContext screenContext;

    public UsuarioScreen(ScreenContext ctx) {
        this.screenContext = ctx;
        this.vm = new UsuarioScreenViewModel(ctx);
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
        return "usuarios";
    }

    @Override
    public ViewModelScreenContract<UsuarioModel> viewModel() {
        return vm;
    }

    @Override
    public SimpleTable<UsuarioModel> table() {
        var simpleTable = new SimpleTable<UsuarioModel>(new SimpleTableProps().maxHeight(Components.TABLE_MAX_HEIGHT));
        simpleTable.fromData(vm.filteredList)
                .header()
                .columns()
                .column("ID", UsuarioModel::getId, 60.0)
                .column("Nome", UsuarioModel::getNome)
                .column("Login", UsuarioModel::getLogin)
                .column("Admin", it -> Boolean.TRUE.equals(it.getAdmin()) ? "Sim" : "Não")
                .column("Data de criação", it -> DatePack.localDateTimeToBrazilianDateTime(it.getDataCriacao()))
                .build()
                .onItemSelectChange(vm.selected::set)
               .onItemDoubleClick(it -> screenContext.router().spawnWindow(AppRoutes.Screens.DETAILS_USUARIO.name() + "/" + it.getId()));

        return simpleTable;
    }

    @Override
    public void exportPdf(File destino, EmpresaModel empresa, List<UsuarioModel> snapshotFiltrado) throws Exception {
        var headers = java.util.List.of("ID", "Nome", "Login", "Admin", "Data de criacao");
        var rows = vm.filteredList.get().stream().map(u -> java.util.List.of(
                String.valueOf(u.getId()),
                u.getNome() != null ? u.getNome() : "",
                u.getLogin() != null ? u.getLogin() : "",
                Boolean.TRUE.equals(u.getAdmin()) ? "Sim" : "Nao",
                DatePack.localDateTimeToBrazilianDateTime(u.getDataCriacao())
        )).toList();
        ListaPdfExporter.exportar(destino, empresa, "Lista de Usuarios", headers, rows);
    }
}
