package my_app.screens.clienteScreen;

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
import megalodonte.components.layout_components.Container;
import megalodonte.props.ColumnProps;
import megalodonte.props.SimpleTableProps;
import megalodonte.props.TextProps;
import megalodonte.base.route.v2.ScreenContextInterface;
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
    private static final UiDefinition VIEW = UiDefinition.fromResource(ClienteScreen.class, "/views/cliente-list.view.jsonc");
    private static final Gson JSON = new Gson();
    private final UiSession session;
    private final Subscription rowsSubscription;
    private final ClienteViewModel vm;
    private final ScreenContextInterface screenContext;

    public ClienteScreen(ScreenContextInterface ctx) {
        this.screenContext = ctx;
        this.vm = new ClienteViewModel(ctx);
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
                .column("ID", ClienteModel::getId)
                .column("Razão social", ClienteModel::getRazaoSocial)
                .column("CPF/CNPJ", it->FormatterPack.formatCpfCnpj(it.getCpfCnpj()))
                .column("Telefone", it->FormatterPack.formatPhone(it.getTelefone()))
                .column("UF", it -> it.getUf() != null ? it.getUf() : "")
                .column("CEP", it -> it.getCep() != null ? FormatterPack.formatCep(it.getCep()) : "")
                .column("Cidade", it -> it.getCidade() != null ? it.getCidade() : "")
                .column("Bairro", it -> it.getBairro() != null ? it.getBairro() : "")
                .column("Rua", it -> it.getRua() != null ? it.getRua() : "")
                .column("Número", it -> it.getNumero() != null ? it.getNumero() : "")
                .column("Complemento", ClienteModel::getComplemento)
                .column("Ativo", it -> Boolean.TRUE.equals(it.getAtivo()) ? "Sim" : "Não")
                .column("Data de criação", it -> DatePack.localDateTimeToBrazilianDateTime(it.getDataCriacao()))
                .build()
                .onItemSelectChange(vm.selected::set)
                //.onItemDoubleClick(it -> showItemDetails(it, this.screenContext, 350));
                .onItemDoubleClick(it -> screenContext.spawnWindow(AppRoutes.Screens.DETAILS_CLIENTE.name() + "/" + it.getId()));

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
        if (selected != null) screenContext.spawnWindow(AppRoutes.Screens.DETAILS_CLIENTE.name() + "/" + selected.getId());
    }

    private String rowsJson(List<ClienteModel> models) {
        var rows = new ArrayList<Map<String, String>>(models.size());
        for (var model : models) {
            var row = new LinkedHashMap<String, String>();
            row.put("id", safe(model.getId()));
            row.put("razaoSocial", safe(model.getRazaoSocial()));
            row.put("cpfCnpj", safe(FormatterPack.formatCpfCnpj(model.getCpfCnpj())));
            row.put("telefone", safe(FormatterPack.formatPhone(model.getTelefone())));
            row.put("uf", safe(model.getUf()));
            row.put("cep", safe(FormatterPack.formatCep(model.getCep())));
            row.put("cidade", safe(model.getCidade()));
            row.put("bairro", safe(model.getBairro()));
            row.put("rua", safe(model.getRua()));
            row.put("numero", safe(model.getNumero()));
            row.put("complemento", safe(model.getComplemento()));
            row.put("ativo", safe(Boolean.TRUE.equals(model.getAtivo()) ? "Sim" : "Não"));
            row.put("dataCriacao", safe(DatePack.localDateTimeToBrazilianDateTime(model.getDataCriacao())));
            rows.add(row);
        }
        return JSON.toJson(rows);
    }

    private static String safe(Object value) { return value == null ? "" : value.toString(); }

    @Override
    public void exportPdf(File destino, EmpresaModel empresa, List<ClienteModel> snapshotFiltrado) throws Exception {
        var headers = java.util.List.of("ID", "Razao social", "CPF/CNPJ", "Telefone", "Data de criacao");
        var rows = snapshotFiltrado.stream().map(c -> java.util.List.of(
                String.valueOf(c.getId()),
                c.getRazaoSocial() != null ? c.getRazaoSocial() : "",
                c.getCpfCnpj() != null ? FormatterPack.formatCpfCnpj(c.getCpfCnpj()) : "",
                c.getTelefone() != null ? FormatterPack.formatPhone(c.getTelefone()) : "",
                DatePack.localDateTimeToBrazilianDateTime(c.getDataCriacao())
        )).toList();
        ListaPdfExporter.exportar(destino, empresa, "Lista de Clientes", headers, rows);
    }
}
