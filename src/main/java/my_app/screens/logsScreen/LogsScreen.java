package my_app.screens.logsScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.route.v2.ScreenContextInterface;
import megalodonte.base.state.Subscription;
import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import megalodonte.jsonc.UiSession;

/** Binds the log service to the declarative view. */
public final class LogsScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(LogsScreen.class, "/views/logs.view.jsonc");
    private final LogsScreenViewModel viewModel = new LogsScreenViewModel();
    private final UiSession session = VIEW.open(new Actions()
            .register("refresh", ignored -> viewModel.carregarLogs())
            .register("openFolder", ignored -> viewModel.abrirPastaDeLogs()));
    private final Subscription logUpdates = viewModel.conteudoLogs.observe(
            value -> session.states().string("contents").set(value));

    public LogsScreen(ScreenContextInterface context) {}

    @Override public Component render() { return session.render(); }

    @Override public void onDestroy() {
        logUpdates.close();
        session.close();
    }
}
