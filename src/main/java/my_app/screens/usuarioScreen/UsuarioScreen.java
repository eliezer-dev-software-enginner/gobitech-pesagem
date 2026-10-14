package my_app.screens.usuarioScreen;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import megalodonte.base.components.Component;
import megalodonte.base.state.Subscription;
import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import megalodonte.jsonc.UiSession;
import my_app.domain.Autorizacao;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.SimpleTable;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.props.ColumnProps;
import megalodonte.props.SimpleTableProps;
import megalodonte.props.TextProps;
import megalodonte.base.route.v2.ScreenContextInterface;
import my_app.core.AppRoutes;
import my_app.db.models.UsuarioModel;
import my_app.domain.ContratoTelaCrudV3;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import my_app.db.models.EmpresaModel;
import my_app.infra.ListaPdfExporter;
import pack.utilities.DatePack;
import pack.utilities.FormatterPack;

import java.io.File;
import java.util.List;

public class UsuarioScreen implements ScreenComponent, ContratoTelaCrudV3<UsuarioModel> {
    private static final UiDefinition VIEW = UiDefinition.fromResource(UsuarioScreen.class, "/views/usuario-list.view.jsonc");
    private static final Gson JSON = new Gson();
    private final UiSession session;
    private final Subscription rowsSubscription;
    private final UsuarioScreenViewModel vm;
    private final ScreenContextInterface screenContext;

    public UsuarioScreen(ScreenContextInterface ctx) {
        this.screenContext = ctx;
        this.vm = new UsuarioScreenViewModel(ctx);
        session = VIEW.open(new Actions()
                .register("select", ignored -> selectRow())
                .register("openDetails", ignored -> openDetails())
                .register("export", ignored -> handleClickBaixarLista())
                .register("edit", ignored -> handleClickMenuEdit())
                .register("delete", ignored -> handleClickMenuDelete())
                .register("create", ignored -> handleClickNew()));
        session.bind("search", vm.searchState, String.class);
        session.states().state("showDelete", Boolean.class).set(Autorizacao.podeAdministrar());
        rowsSubscription = vm.filteredList.observe(items -> session.states().string("rows").set(rowsJson(items)));
    }

    @Override
    public void onMount() {
        vm.fetchListData();
    }

    @Override
    public void onDestroy() {
        rowsSubscription.close();
        session.close();
        ContratoTelaCrudV3.super.onDestroy();
    }

    @Override
    public Component render() {
        return session.render();
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
                .column("ID", UsuarioModel::getId)
                .column("Nome", UsuarioModel::getNome)
                .column("Login", UsuarioModel::getLogin)
                .column("Telefone", it -> it.getTelefone() != null ? FormatterPack.formatPhone(it.getTelefone()) : "")
                .column("Perfil", it -> Boolean.TRUE.equals(it.getAdmin()) ? "Admin" : "Operador")
                .column("Ativo", it -> Boolean.TRUE.equals(it.getAtivo()) ? "Sim" : "Não")
                .column("Data de criação", it -> DatePack.localDateTimeToBrazilianDateTime(it.getDataCriacao()))
                .build()
                .onItemSelectChange(vm.selected::set)
               .onItemDoubleClick(it -> screenContext.spawnWindow(AppRoutes.Screens.DETAILS_USUARIO.name() + "/" + it.getId()));

        return simpleTable;
    }

    private void selectRow() {
        var value = session.states().string("selected").get();
        if (value.isBlank()) { vm.selected.set(null); return; }
        try {
            var id = JsonParser.parseString(value).getAsJsonObject().get("id").getAsLong();
            vm.selected.set(vm.filteredList.get().stream().filter(model -> model.getId() == id).findFirst().orElse(null));
        } catch (RuntimeException error) { vm.selected.set(null); }
    }

    private void openDetails() {
        selectRow();
        var selected = vm.selected.get();
        if (selected != null) screenContext.spawnWindow(AppRoutes.Screens.DETAILS_USUARIO.name() + "/" + selected.getId());
    }

    private String rowsJson(List<UsuarioModel> models) {
        var rows = new ArrayList<Map<String, String>>(models.size());
        for (var model : models) {
            var row = new LinkedHashMap<String, String>();
            row.put("id", safe(model.getId()));
            row.put("nome", safe(model.getNome()));
            row.put("login", safe(model.getLogin()));
            row.put("telefone", safe(FormatterPack.formatPhone(model.getTelefone())));
            row.put("perfil", safe(Boolean.TRUE.equals(model.getAdmin()) ? "Admin" : "Operador"));
            row.put("ativo", safe(Boolean.TRUE.equals(model.getAtivo()) ? "Sim" : "Não"));
            row.put("dataCriacao", safe(DatePack.localDateTimeToBrazilianDateTime(model.getDataCriacao())));
            rows.add(row);
        }
        return JSON.toJson(rows);
    }

    private static String safe(Object value) { return value == null ? "" : value.toString(); }

    @Override
    public void exportPdf(File destino, EmpresaModel empresa, List<UsuarioModel> snapshotFiltrado) throws Exception {
        var headers = java.util.List.of("ID", "Nome", "Login", "Perfil", "Data de criacao");
        var rows = snapshotFiltrado.stream().map(u -> java.util.List.of(
                String.valueOf(u.getId()),
                u.getNome() != null ? u.getNome() : "",
                u.getLogin() != null ? u.getLogin() : "",
                Boolean.TRUE.equals(u.getAdmin()) ? "Admin" : "Operador",
                DatePack.localDateTimeToBrazilianDateTime(u.getDataCriacao())
        )).toList();
        ListaPdfExporter.exportar(destino, empresa, "Lista de Usuarios", headers, rows);
    }
}
