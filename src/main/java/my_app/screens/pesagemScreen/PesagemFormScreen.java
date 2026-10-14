package my_app.screens.pesagemScreen;

import com.google.gson.Gson;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.route.v2.ScreenContextInterface;
import megalodonte.base.state.State;
import megalodonte.base.state.Subscription;
import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import megalodonte.jsonc.UiSession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared declarative form for incoming, outgoing, ad hoc and manual weighings. */
public abstract class PesagemFormScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(PesagemFormScreen.class, "/views/pesagem-form.view.jsonc");
    private static final Gson JSON = new Gson();
    protected final ScreenContextInterface screenContext;
    protected final PesagemFormViewModel vm;
    private final UiSession session;
    private final State<String> clientId = State.of("");
    private final State<String> productId = State.of("");
    private final List<Subscription> subscriptions = new ArrayList<>();

    protected PesagemFormScreen(ScreenContextInterface context) {
        screenContext = context;
        vm = criarViewModel(context);
        session = VIEW.open(new Actions()
                .register("captureTare", ignored -> vm.capturarTara())
                .register("captureGross", ignored -> vm.capturarPesoBruto())
                .register("save", ignored -> vm.salvar())
                .register("saveAndPrint", ignored -> vm.salvarEImprimir()));
        session.bind("plate", vm.placa, String.class)
                .bind("driver", vm.motoristaNome, String.class)
                .bind("document", vm.motoristaDocumento, String.class)
                .bind("invoice", vm.notaFiscal, String.class)
                .bind("notes", vm.observacoes, String.class)
                .bind("tare", vm.pesoVeiculo, String.class)
                .bind("gross", vm.pesoTotal, String.class)
                .display("net", vm.pesoFinal, String.class)
                .bind("avariados", vm.avariados, String.class)
                .bind("ardidos", vm.ardidos, String.class)
                .bind("quebraArdidos", vm.quebraArdidos, String.class)
                .bind("impurezas", vm.impurezas, String.class)
                .bind("quebraImpurezas", vm.quebraImpurezas, String.class)
                .bind("umidade", vm.umidade, String.class)
                .bind("quebraUmidade", vm.quebraUmidade, String.class)
                .bind("outros", vm.outros, String.class)
                .bind("client", clientId, String.class)
                .bind("product", productId, String.class);
        session.states().string("title").set(vm.tituloFormulario());
        session.states().string("saveText").set(vm.textoBotaoSalvar());
        session.states().state("showExitHint", Boolean.class).set(mostrarAvisoSaida());
        session.states().state("showCaptureTare", Boolean.class).set(permitirCapturarTara());
        session.states().state("showCaptureGross", Boolean.class).set(permitirCapturarBruto());
        session.states().state("tareDisabled", Boolean.class).set(permitirCapturarTara() || !taraEditavel());
        session.states().state("grossDisabled", Boolean.class).set(permitirCapturarBruto() || !brutoEditavel());
        subscriptions.add(vm.clientesState.observe(items -> session.states().string("clients").set(clientOptions(items))));
        subscriptions.add(vm.produtosState.observe(items -> session.states().string("products").set(productOptions(items))));
        subscriptions.add(clientId.observe(value -> vm.clienteSelected.set(vm.clientesState.get().stream()
                .filter(item -> item.getId() != null && String.valueOf(item.getId()).equals(value)).findFirst().orElse(null))));
        subscriptions.add(productId.observe(value -> vm.produtoSelected.set(vm.produtosState.get().stream()
                .filter(item -> item.getId() != null && String.valueOf(item.getId()).equals(value)).findFirst().orElse(null))));
        subscriptions.add(vm.clienteSelected.observe(value -> {
            var id = value == null || value.getId() == null ? "" : String.valueOf(value.getId());
            if (!clientId.get().equals(id)) clientId.set(id);
        }));
        subscriptions.add(vm.produtoSelected.observe(value -> {
            var id = value == null || value.getId() == null ? "" : String.valueOf(value.getId());
            if (!productId.get().equals(id)) productId.set(id);
        }));
    }

    protected abstract PesagemFormViewModel criarViewModel(ScreenContextInterface context);
    protected boolean usarBalanca() { return true; }
    protected boolean permitirCapturarBruto() { return true; }
    protected boolean brutoEditavel() { return true; }
    protected boolean permitirCapturarTara() { return true; }
    protected boolean taraEditavel() { return true; }
    protected boolean mostrarAvisoSaida() { return false; }

    @Override public Component render() { return session.render(); }
    @Override public void onMount() { if (usarBalanca()) vm.iniciarLeituraBalanca(); }
    @Override public void onDestroy() {
        subscriptions.forEach(Subscription::close);
        session.close();
        try { vm.onDestroy(); }
        catch (Exception error) { throw new IllegalStateException("Could not close weighing form", error); }
    }

    private static String clientOptions(List<my_app.db.models.ClienteModel> items) {
        var options = new ArrayList<Map<String, String>>();
        options.add(Map.of("value", "", "label", "Nenhum"));
        for (var item : items) {
            var option = new LinkedHashMap<String, String>();
            option.put("value", String.valueOf(item.getId()));
            option.put("label", item.getRazaoSocial());
            options.add(option);
        }
        return JSON.toJson(options);
    }

    private static String productOptions(List<my_app.db.models.ProdutoModel> items) {
        var options = new ArrayList<Map<String, String>>();
        options.add(Map.of("value", "", "label", "Nenhum"));
        for (var item : items) {
            var option = new LinkedHashMap<String, String>();
            option.put("value", String.valueOf(item.getId()));
            option.put("label", item.getNome());
            options.add(option);
        }
        return JSON.toJson(options);
    }
}
