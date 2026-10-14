package my_app.screens.licensaScreen;

import com.google.gson.Gson;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.route.v2.ScreenContextInterface;
import megalodonte.base.state.State;
import megalodonte.base.state.Subscription;
import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import megalodonte.jsonc.UiSession;
import my_app.db.models.LicensaModel;
import my_app.domain.Data;
import pack.utilities.DatePack;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** License generation and history are rendered from the JSONC view. */
public final class LicensaScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(LicensaScreen.class, "/views/licensa.view.jsonc");
    private static final Gson JSON = new Gson();
    private final LicensaViewModel viewModel;
    private final UiSession session;
    private final State<String> date = State.of("");
    private final List<Subscription> subscriptions = new ArrayList<>();

    public LicensaScreen(ScreenContextInterface context) {
        viewModel = new LicensaViewModel(context);
        session = VIEW.open(new Actions().register("generate", ignored -> viewModel.gerar()));
        session.bind("expiryMode", viewModel.definirExpiracaoSelected, String.class)
                .bind("date", date, String.class)
                .display("generatedCode", viewModel.codigoGerado, String.class);
        session.states().string("modes").set(JSON.toJson(Data.simNaoList));
        subscriptions.add(viewModel.dataExpiracao.observe(value -> date.set(value == null ? "" : value.toString())));
        subscriptions.add(date.observe(value -> {
            try {
                var parsed = value == null || value.isBlank() ? null : LocalDate.parse(value);
                if (!java.util.Objects.equals(viewModel.dataExpiracao.get(), parsed)) viewModel.dataExpiracao.set(parsed);
            } catch (DateTimeParseException ignored) { /* DatePicker keeps the last valid date. */ }
        }));
        subscriptions.add(viewModel.definirExpiracao.observe(value -> session.states().state("showDate", Boolean.class).set(value)));
        subscriptions.add(viewModel.codigoGeradoVisible.observe(value -> session.states().state("showCode", Boolean.class).set(value)));
        subscriptions.add(viewModel.licensasState.observe(items -> session.states().string("rows").set(rowsJson(items))));
    }

    @Override public Component render() { return session.render(); }
    @Override public void onMount() {
        if (!viewModel.acessoPermitido()) { viewModel.bloquearAcesso(); return; }
        viewModel.carregar();
    }
    @Override public void onDestroy() {
        subscriptions.forEach(Subscription::close);
        session.close();
        try { viewModel.onDestroy(); }
        catch (Exception error) { throw new IllegalStateException("Could not close license screen", error); }
    }

    private String rowsJson(List<LicensaModel> licenses) {
        var rows = new ArrayList<Map<String, String>>(licenses.size());
        for (var license : licenses) {
            var row = new LinkedHashMap<String, String>();
            row.put("code", license.getValor());
            row.put("expires", license.getExpiraEm() == null ? "Sem expiração" : DatePack.localDateTimeToBrazilianDateTime(license.getExpiraEm()));
            row.put("status", license.expirada() ? "Expirada" : "Válida");
            row.put("created", DatePack.localDateTimeToBrazilianDateTime(license.getDataCriacao()));
            rows.add(row);
        }
        return JSON.toJson(rows);
    }
}
