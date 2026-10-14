package my_app.screens.conexaoCameraScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.route.v2.ScreenContextInterface;
import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import megalodonte.jsonc.UiSession;

/** Camera form structure and copy are defined in JSONC; camera access stays in the view model. */
public final class ConexaoCameraScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(ConexaoCameraScreen.class, "/views/conexao-camera.view.jsonc");
    private final ConexaoCameraViewModel viewModel;
    private final UiSession session;

    public ConexaoCameraScreen(ScreenContextInterface context) {
        viewModel = new ConexaoCameraViewModel(context);
        session = VIEW.open(new Actions()
                .register("testFront", ignored -> viewModel.testarFrente())
                .register("testBack", ignored -> viewModel.testarCostas())
                .register("save", ignored -> viewModel.salvar()));
        session.bind("frontIp", viewModel.frenteIp, String.class)
                .bind("frontPort", viewModel.frentePorta, String.class)
                .bind("frontChannel", viewModel.frenteCanal, String.class)
                .bind("frontUser", viewModel.frenteUsuario, String.class)
                .bind("frontPassword", viewModel.frenteSenha, String.class)
                .display("frontPreview", viewModel.frentePreview, String.class)
                .bind("backIp", viewModel.costasIp, String.class)
                .bind("backPort", viewModel.costasPorta, String.class)
                .bind("backChannel", viewModel.costasCanal, String.class)
                .bind("backUser", viewModel.costasUsuario, String.class)
                .bind("backPassword", viewModel.costasSenha, String.class)
                .display("backPreview", viewModel.costasPreview, String.class);
    }

    @Override public Component render() { return session.render(); }
    @Override public void onMount() { viewModel.load(); }
    @Override public void onDestroy() {
        session.close();
        try { viewModel.onDestroy(); }
        catch (Exception error) { throw new IllegalStateException("Could not close camera settings", error); }
    }
}
