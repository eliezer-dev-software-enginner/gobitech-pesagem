package my_app.screens.usuarioScreen;

import com.google.gson.Gson;
import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import megalodonte.jsonc.UiSession;
import megalodonte.application.ErrorReporter;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.base.route.v2.ScreenContextInterface;
import megalodonte.utils.ThrowingSupplier;
import my_app.db.services.UsuarioService;
import my_app.domain.Data;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddOrEditUsuarioScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(AddOrEditUsuarioScreen.class, "/views/usuario-form.view.jsonc");
    private final UiSession session;
    Long id;
    UsuarioScreenViewModel viewModel;
    Logger log = LoggerFactory.getLogger(AddOrEditUsuarioScreen.class);
    UsuarioService usuarioService;

    State<String> titleState = new State<>("");

    public AddOrEditUsuarioScreen(ScreenContextInterface screenContext){
        viewModel = new UsuarioScreenViewModel(screenContext);
        usuarioService = createOrReport(UsuarioService::new);
        session = VIEW.open(new Actions().register("save", ignored -> handleAddOrUpdate()));
        session.display("title", titleState, String.class)
                .display("saveText", viewModel.btnText, String.class)
                .bind("nome", viewModel.nome, String.class)
                .bind("login", viewModel.login, String.class)
                .bind("senha", viewModel.senha, String.class)
                .bind("telefone", viewModel.telefone, String.class)
                .bind("perfil", viewModel.perfilSelected, String.class);
        session.states().string("profiles").set(new Gson().toJson(Data.perfisUsuarioList));
        String type = screenContext.getParams().get("type");

        String idParam = screenContext.getParams().get("id");
        try {
            id = Long.parseLong(idParam);
        } catch (RuntimeException e) {
            log.error("Parâmetro 'id' inválido na rota de edição/inclusão: {}", idParam, e);
            try {
                viewModel.onDestroy();
                usuarioService.close();
            } catch (Exception cleanup) {
                log.warn("Erro ao limpar recursos após rota inválida", cleanup);
            }
            UI.runOnUi(() -> Components.ShowAlertError("ID inválido na rota de edição/inclusão."));
            return;
        }

        Async.Run(()->{
            var model = usuarioService.buscarById(id);
            UI.runOnUi(()-> {
                viewModel.selected.set(model);
                titleState.set("Incluir usuário");
                screenContext.selfStage().setTitle("Inclusão de usuário");

                if(type.equals("edit")){
                    viewModel.modoEdicaoState().set(true);
                    viewModel.populateFieldsFromModel();
                    titleState.set("Editar usuário com Id: " + id);
                    screenContext.selfStage().setTitle("Edição de usuário");
                }
            });
        });
    }

    protected <T> T createOrReport(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            ErrorReporter.handle(e);
            throw new IllegalStateException(e); // interrompe a construção da tela de forma previsível
        }
    }

    @Override
    public Component render() { return session.render(); }

     void handleAddOrUpdate() {
        try {
            viewModel.handleAddOrUpdate();
        } catch (Exception e) {
            log.error("Erro em handleAddOrUpdate", e);
            UI.runOnUi(() -> Components.ShowAlertError("Não foi possível salvar. Tente novamente."));
        }
    }

    // A ViewModel se inscreve no EventBus global e abre a própria sessão; o Service da tela
    // também. Sem o onDestroy, cada abertura do formulário vaza listener + conexão (o padrão
    // do NPE de session nula já corrigido no projeto).
    @Override
    public void onDestroy() {
        session.close();
        try {
            viewModel.onDestroy();
            usuarioService.close();
        } catch (Exception e) {
            log.warn("Erro ao destruir AddOrEditUsuarioScreen", e);
        }
    }
}
