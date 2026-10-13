package my_app.screens.authScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.route.v2.ScreenContextInterface;
import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import megalodonte.jsonc.UiSession;
import my_app.Main;

/** Keeps authentication in the view model while JSONC owns the layout and bindings. */
public final class AuthScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(AuthScreen.class, "/views/auth.view.jsonc");
    private final ScreenContextInterface context;
    private final AuthScreenViewModel viewModel = new AuthScreenViewModel();
    private final UiSession session;

    public AuthScreen(ScreenContextInterface context) {
        this.context = context;
        session = VIEW.open(new Actions().register("signIn", screen -> {
            viewModel.loginState.set(screen.states().string("login").get());
            viewModel.passwordState.set(screen.states().string("password").get());
            viewModel.entrar(context);
        }));
    }

    @Override public void onMount() {
        context.selfStage().getIcons().add(Main.loadIcon());
    }

    @Override public Component render() { return session.render(); }

    @Override public void onDestroy() {
        session.close();
        try { viewModel.onDestroy(); }
        catch (Exception error) { throw new IllegalStateException("Could not close authentication services", error); }
    }
}
