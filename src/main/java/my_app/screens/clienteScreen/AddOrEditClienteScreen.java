package my_app.screens.clienteScreen;

import com.google.gson.Gson;
import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import megalodonte.jsonc.UiSession;
import megalodonte.application.ErrorReporter;
import my_app.domain.Data;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.base.route.v2.ScreenContextInterface;
import megalodonte.utils.ThrowingSupplier;
import my_app.db.services.ClienteService;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddOrEditClienteScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(AddOrEditClienteScreen.class, "/views/cliente-form.view.jsonc");
    private final UiSession session;
    Long id;
    ClienteViewModel viewModel;
    Logger log = LoggerFactory.getLogger(AddOrEditClienteScreen.class);
    ClienteService clienteService;

    State<String> titleState = new State<>("");

    public AddOrEditClienteScreen(ScreenContextInterface screenContext){
        viewModel = new ClienteViewModel(screenContext);
        clienteService = createOrReport(ClienteService::new);
        session = VIEW.open(new Actions().register("save", ignored -> handleAddOrUpdate()));
        var address = viewModel.enderecoState.get();
        session.display("title", titleState, String.class)
                .display("saveText", viewModel.btnText, String.class)
                .bind("razaoSocial", viewModel.razaoSocial, String.class)
                .bind("cnpjCpf", viewModel.cnpjCpf, String.class)
                .bind("telefone", viewModel.telefone, String.class)
                .bind("uf", address.ufSelected, String.class)
                .bind("cep", address.cep, String.class)
                .bind("cidade", address.cidade, String.class)
                .bind("bairro", address.bairro, String.class)
                .bind("rua", address.rua, String.class)
                .bind("numero", address.numero, String.class)
                .bind("complemento", viewModel.complemento, String.class);
        session.states().string("ufs").set(new Gson().toJson(Data.ufList));
        String type = screenContext.getParams().get("type");

        String idParam = screenContext.getParams().get("id");
        try {
            id = Long.parseLong(idParam);
        } catch (RuntimeException e) {
            log.error("Parâmetro 'id' inválido na rota de edição/inclusão: {}", idParam, e);
            try {
                viewModel.onDestroy();
                clienteService.close();
            } catch (Exception cleanup) {
                log.warn("Erro ao limpar recursos após rota inválida", cleanup);
            }
            UI.runOnUi(() -> Components.ShowAlertError("ID inválido na rota de edição/inclusão."));
            return;
        }

        Async.Run(()->{
            var model = clienteService.buscarById(id);
            UI.runOnUi(()-> {
                viewModel.selected.set(model);
                titleState.set("Incluir cliente");
                screenContext.selfStage().setTitle("Inclusão de cliente");

                if(type.equals("edit")){
                    viewModel.modoEdicaoState().set(true);
                    viewModel.populateFieldsFromModel();
                    titleState.set("Editar cliente com Id: " + id);
                    screenContext.selfStage().setTitle("Edição de cliente");
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
            viewModel.modoEdicaoState().set(false);
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
            clienteService.close();
        } catch (Exception e) {
            log.warn("Erro ao destruir AddOrEditClienteScreen", e);
        }
    }
}
