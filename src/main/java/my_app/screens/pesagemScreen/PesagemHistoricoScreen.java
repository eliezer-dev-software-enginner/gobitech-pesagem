package my_app.screens.pesagemScreen;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import megalodonte.base.components.Component;
import megalodonte.base.state.State;
import megalodonte.base.state.Subscription;
import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import megalodonte.jsonc.UiSession;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import megalodonte.base.components.ScreenComponent;
import megalodonte.components.SimpleTable;
import megalodonte.props.SimpleTableProps;
import megalodonte.base.route.v2.ScreenContextInterface;
import my_app.core.AppRoutes;
import my_app.db.models.EmpresaModel;
import my_app.db.models.PesagemModel;
import my_app.domain.ContratoTelaCrudV3;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import my_app.domain.Autorizacao;
import my_app.infra.RelatorioPesagemPdfExporter;
import pack.utilities.DatePack;

import java.io.File;
import java.util.List;
import static my_app.domain.pesagem.RelatorioPesagemDados.pesoStr;

/**
 * Histórico de pesagens — a única tela de pesagem que é listagem CRUD
 * ({@code ContratoTelaCrudV3}). Mostra todas as pesagens registradas, com filtro, busca,
 * detalhes (duplo-clique), exclusão e exportação em PDF. Não cria nem edita: as 4 formas
 * de registro são as telas de formulário acessadas pela sidebar.
 */
public class PesagemHistoricoScreen implements ScreenComponent, ContratoTelaCrudV3<PesagemModel> {
    private static final UiDefinition VIEW = UiDefinition.fromResource(PesagemHistoricoScreen.class, "/views/pesagem-historico.view.jsonc");
    private static final Gson JSON = new Gson();
    private final UiSession session;
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final State<String> type = State.of(PesagemHistoricoViewModel.TIPO_TODOS);
    private final State<String> client = State.of("");
    private final State<String> startDate = State.of("");
    private final State<String> endDate = State.of("");

    private final PesagemHistoricoViewModel vm;
    private final ScreenContextInterface screenContext;

    public PesagemHistoricoScreen(ScreenContextInterface ctx) {
        this.screenContext = ctx;
        this.vm = new PesagemHistoricoViewModel(ctx);
        session = VIEW.open(new Actions()
                .register("filter", ignored -> vm.aplicarFiltro())
                .register("select", ignored -> selectRow())
                .register("openDetails", ignored -> openDetails())
                .register("print", ignored -> printSelected())
                .register("export", ignored -> handleClickBaixarLista())
                .register("delete", ignored -> handleClickMenuDelete()));
        session.bind("plate", vm.filtroPlaca, String.class)
                .bind("driver", vm.filtroMotorista, String.class)
                .bind("type", type, String.class)
                .bind("client", client, String.class)
                .bind("startDate", startDate, String.class)
                .bind("endDate", endDate, String.class)
                .bind("search", vm.searchState, String.class);
        session.states().string("types").set(JSON.toJson(PesagemHistoricoViewModel.tiposPesagemOpcoes));
        session.states().state("showDelete", Boolean.class).set(Autorizacao.podeAdministrar());
        subscriptions.add(type.observe(value -> {
            var mapped = value == null || value.equals(PesagemHistoricoViewModel.TIPO_TODOS) ? null : value;
            if (!java.util.Objects.equals(vm.filtroTipo.get(), mapped)) vm.filtroTipo.set(mapped);
        }));
        subscriptions.add(vm.filtroTipo.observe(value -> {
            var mapped = value == null ? PesagemHistoricoViewModel.TIPO_TODOS : value;
            if (!java.util.Objects.equals(type.get(), mapped)) type.set(mapped);
        }));
        subscriptions.add(client.observe(value -> vm.filtroCliente.set(vm.clientesFiltroState.get().stream()
                .filter(item -> item.getId() != null && String.valueOf(item.getId()).equals(value)).findFirst().orElse(null))));
        subscriptions.add(vm.clientesFiltroState.observe(items -> {
            var options = new ArrayList<Map<String, String>>();
            for (var item : items) {
                var option = new LinkedHashMap<String, String>();
                option.put("value", item.getId() == null ? "" : String.valueOf(item.getId()));
                option.put("label", item.getId() == null ? "Todos" : item.getRazaoSocial());
                options.add(option);
            }
            session.states().string("clients").set(JSON.toJson(options));
        }));
        subscriptions.add(vm.filtroDataInicio.observe(value -> startDate.set(value == null ? "" : value.toString())));
        subscriptions.add(vm.filtroDataFim.observe(value -> endDate.set(value == null ? "" : value.toString())));
        subscriptions.add(startDate.observe(value -> updateDate(value, vm.filtroDataInicio)));
        subscriptions.add(endDate.observe(value -> updateDate(value, vm.filtroDataFim)));
        subscriptions.add(vm.filteredList.observe(items -> session.states().string("rows").set(rowsJson(items))));
    }

    @Override
    public void onMount() {
        vm.fetchListData();
    }

    @Override
    public void onDestroy() {
        subscriptions.forEach(Subscription::close);
        session.close();
        ContratoTelaCrudV3.super.onDestroy();
    }

    @Override
    public Component render() {
        return session.render();
    }

    @Override
    public String downloadListaPrefixo() {
        return "relatório";
    }

    @Override
    public ViewModelScreenContract<PesagemModel> viewModel() {
        return vm;
    }

    @Override
    public SimpleTable<PesagemModel> table() {
        var simpleTable = new SimpleTable<PesagemModel>(new SimpleTableProps().maxHeight(Components.TABLE_MAX_HEIGHT));
        simpleTable.fromData(vm.filteredList)
                .header()
                .columns()
                .column("ID", PesagemModel::getId)
                .column("Placa", PesagemModel::getPlaca)
                .column("Motorista", PesagemModel::getMotoristaNome)
                .column("Documento", PesagemModel::getMotoristaDocumento)
                .column("Tipo", PesagemModel::getTipoPesagem)
                .column("Cliente", it -> it.getCliente() != null ? it.getCliente().getRazaoSocial() : "-")
                .column("Produto", it -> it.getProduto() != null ? it.getProduto().getNome() : "-")
                .column("Nota fiscal", PesagemModel::getNotaFiscal)
                .column("Tara (Kg)", it -> pesoStr(it.getPesoVeiculo()))
                .column("Peso bruto (Kg)", it -> pesoStr(it.getPesoTotal()))
                .column("Peso líquido (Kg)", it -> pesoStr(it.getPesoFinal()))
                .column("Data", it -> DatePack.localDateTimeToBrazilianDateTime(it.getDataCriacao()))
                .column("Observações", PesagemModel::getObservacoes)
                .build()
                .horizontalScroll()
                .onItemSelectChange(vm.selected::set)
                //.onItemDoubleClick(it -> showItemDetailsComAcoes(it, this.screenContext, 500));
                .onItemDoubleClick(it -> screenContext.spawnWindow(AppRoutes.Screens.DETAILS_PESAGEM.name() + "/" + it.getId()));

        return simpleTable;
    }


    private void selectRow() {
        var selected = session.states().string("selected").get();
        if (selected.isBlank()) { vm.selected.set(null); return; }
        try {
            long id = JsonParser.parseString(selected).getAsJsonObject().get("id").getAsLong();
            vm.selected.set(vm.filteredList.get().stream().filter(item -> item.getId() == id).findFirst().orElse(null));
        } catch (RuntimeException error) { vm.selected.set(null); }
    }

    private void openDetails() {
        selectRow();
        var selected = vm.selected.get();
        if (selected != null) screenContext.spawnWindow(AppRoutes.Screens.DETAILS_PESAGEM.name() + "/" + selected.getId());
    }

    private void printSelected() {
        selectRow();
        var selected = vm.selected.get();
        if (selected == null) { Components.ShowAlertError("Selecione o item primeiro"); return; }
        vm.imprimirTicket(selected);
    }

    private static void updateDate(String iso, State<LocalDate> target) {
        try {
            var value = iso == null || iso.isBlank() ? null : LocalDate.parse(iso);
            if (!java.util.Objects.equals(target.get(), value)) target.set(value);
        } catch (DateTimeParseException ignored) { /* DatePicker keeps the last valid date. */ }
    }

    private String rowsJson(List<PesagemModel> items) {
        var rows = new ArrayList<Map<String, String>>(items.size());
        for (var item : items) {
            var row = new LinkedHashMap<String, String>();
            row.put("id", String.valueOf(item.getId()));
            row.put("plate", safe(item.getPlaca()));
            row.put("driver", safe(item.getMotoristaNome()));
            row.put("document", safe(item.getMotoristaDocumento()));
            row.put("type", safe(item.getTipoPesagem()));
            row.put("client", item.getCliente() == null ? "-" : safe(item.getCliente().getRazaoSocial()));
            row.put("product", item.getProduto() == null ? "-" : safe(item.getProduto().getNome()));
            row.put("invoice", safe(item.getNotaFiscal()));
            row.put("tare", pesoStr(item.getPesoVeiculo()));
            row.put("gross", pesoStr(item.getPesoTotal()));
            row.put("net", pesoStr(item.getPesoFinal()));
            row.put("created", DatePack.localDateTimeToBrazilianDateTime(item.getDataCriacao()));
            row.put("notes", safe(item.getObservacoes()));
            rows.add(row);
        }
        return JSON.toJson(rows);
    }

    private static String safe(Object value) { return value == null ? "" : value.toString(); }

    @Override
    public void exportPdf(File destino, EmpresaModel empresa, List<PesagemModel> snapshotFiltrado) throws Exception {
        var dados = my_app.domain.pesagem.RelatorioPesagemDados.montar(snapshotFiltrado);
        var headers = List.of("Ticket", "Tara (Kg)", "Entrada", "Horário", "Saída", "Horário",
                "Placa", "Produto", "Cliente", "Peso bruto", "Peso líquido");
        RelatorioPesagemPdfExporter.exportar(destino, empresa, "Relatório resumo de entradas e saídas",
                headers, dados.linhas(), dados.observacoes(), dados.linhas().size(),
                dados.totalLiquido().stripTrailingZeros().toPlainString());
    }
}
