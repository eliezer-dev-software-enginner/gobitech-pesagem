package my_app.screens.dashboardScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.route.v2.ScreenContextInterface;
import megalodonte.base.state.Subscription;
import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import megalodonte.jsonc.UiSession;

/** Keeps live scale and branding data in the view model while JSONC owns the layout. */
public final class DashboardScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(DashboardScreen.class, "/views/dashboard.view.jsonc");
    private final DashboardViewModel viewModel;
    private final UiSession session = VIEW.open(new Actions());
    private final Subscription logoUpdates;
    private final Subscription logoVisibilityUpdates;
    private final Subscription weightUpdates;

    public DashboardScreen(ScreenContextInterface context) {
        viewModel = new DashboardViewModel(context);
        logoUpdates = viewModel.logoHorizontal.observe(value -> session.states().string("logo").set(value));
        logoVisibilityUpdates = viewModel.logoNaoVazia.observe(value -> session.states().state("showLogo", Boolean.class).set(value));
        weightUpdates = viewModel.pesoAoVivo.observe(value -> session.states().string("weight").set(value));
    }

    @Override public Component render() { return session.render(); }

    @Override public void onMount() {
        viewModel.carregar();
        viewModel.iniciarLeituraBalanca();
    }

    @Override public void onDestroy() {
        logoUpdates.close();
        logoVisibilityUpdates.close();
        weightUpdates.close();
        session.close();
        viewModel.onDestroy();
    }
}
