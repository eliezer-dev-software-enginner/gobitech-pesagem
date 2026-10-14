package my_app.screens.empresaScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.route.v2.ScreenContextInterface;
import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import megalodonte.jsonc.UiSession;
import com.google.gson.Gson;
import my_app.domain.Data;

/** Company form layout is JSONC; loading and persistence remain in the view model. */
public final class CadastroEmpresaScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(CadastroEmpresaScreen.class, "/views/empresa.view.jsonc");
    private final EmpresaViewModel viewModel;
    private final UiSession session;

    public CadastroEmpresaScreen(ScreenContextInterface context) {
        viewModel = new EmpresaViewModel(context);
        session = VIEW.open(new Actions()
                .register("chooseLogo", ignored -> viewModel.handleUpdateLogoMarca())
                .register("save", ignored -> viewModel.handleSave()));
        session.bind("nome", viewModel.nome, String.class)
                .bind("cpfCnpj", viewModel.cpfCnpj, String.class)
                .bind("inscricaoEstadual", viewModel.inscricaoEstadual, String.class)
                .bind("telefone", viewModel.telefone, String.class)
                .bind("email", viewModel.email, String.class)
                .display("logo", viewModel.logoMarca, String.class)
                .bind("cep", viewModel.cep, String.class)
                .bind("cidade", viewModel.cidade, String.class)
                .bind("uf", viewModel.estadoSelected, String.class)
                .bind("bairro", viewModel.bairro, String.class)
                .bind("rua", viewModel.rua, String.class)
                .bind("numero", viewModel.numero, String.class);
        session.states().string("ufs").set(new Gson().toJson(Data.ufList));
    }

    @Override public Component render() { return session.render(); }
    @Override public void onMount() { viewModel.fetchData(); }
    @Override public void onDestroy() {
        session.close();
        try { viewModel.onDestroy(); }
        catch (Exception error) { throw new IllegalStateException("Could not close company form", error); }
    }
}
