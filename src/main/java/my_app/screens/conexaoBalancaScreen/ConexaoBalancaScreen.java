package my_app.screens.conexaoBalancaScreen;

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
import java.util.List;

/** Serial and TCP settings are data-bound to a JSONC view. */
public final class ConexaoBalancaScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(ConexaoBalancaScreen.class, "/views/conexao-balanca.view.jsonc");
    private static final Gson JSON = new Gson();
    private final ConexaoBalancaViewModel viewModel;
    private final UiSession session;
    private final List<Subscription> subscriptions = new ArrayList<>();

    public ConexaoBalancaScreen(ScreenContextInterface context) {
        viewModel = new ConexaoBalancaViewModel(context);
        session = VIEW.open(new Actions().register("save", ignored -> viewModel.salvar()));
        bind("connectionType", viewModel.tipoConexaoSelected);
        bind("serialPort", viewModel.portaComSelected);
        bind("baudRate", viewModel.baudRate);
        bind("ipAddress", viewModel.ipAddress);
        bind("ipPort", viewModel.ipPort);
        subscriptions.add(viewModel.portasComState.observe(values ->
                session.states().string("ports").set(JSON.toJson(values))));
        subscriptions.add(viewModel.ehSerial.observe(serial -> {
            session.states().state("showSerial", Boolean.class).set(serial);
            session.states().state("showTcp", Boolean.class).set(!serial);
        }));
    }

    private void bind(String name, State<String> model) {
        var view = session.states().string(name);
        subscriptions.add(model.observe(view::set));
        subscriptions.add(view.observe(model::set));
    }

    @Override public Component render() { return session.render(); }
    @Override public void onMount() { viewModel.load(); }

    @Override public void onDestroy() {
        subscriptions.forEach(Subscription::close);
        session.close();
        try { viewModel.onDestroy(); }
        catch (Exception error) { throw new IllegalStateException("Could not close scale settings", error); }
    }
}
