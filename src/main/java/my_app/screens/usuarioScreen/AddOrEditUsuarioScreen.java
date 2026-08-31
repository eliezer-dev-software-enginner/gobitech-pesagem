package my_app.screens.usuarioScreen;

import megalodonte.application.ErrorReporter;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.components.Card;
import megalodonte.components.LineHorizontal;
import megalodonte.components.SpacerVertical;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.FlowRow;
import megalodonte.props.ColumnProps;
import megalodonte.props.FlowRowProps;
import megalodonte.router.v4.ScreenContext;
import megalodonte.utils.ThrowingSupplier;
import my_app.db.services.UsuarioService;
import my_app.domain.Data;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddOrEditUsuarioScreen implements ScreenComponent {
    Long id;
    UsuarioScreenViewModel viewModel;
    Logger log = LoggerFactory.getLogger(AddOrEditUsuarioScreen.class);
    UsuarioService usuarioService;

    State<String> titleState = new State<>("");

    public AddOrEditUsuarioScreen(ScreenContext screenContext){
        id = Long.parseLong(screenContext.getParams().get("id"));
        String type = screenContext.getParams().get("type");
        viewModel = new UsuarioScreenViewModel(screenContext);
        usuarioService = createOrReport(UsuarioService::new);

        Async.Run(()->{
            var model = usuarioService.buscarById(id);
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
    public Component render() {
         return new Card(
                new Column(new ColumnProps().paddingAll(20))
                        .c_child(Components.FormTitle("Cadastrar usuário"))
                        .c_child(new SpacerVertical(20))
                        .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                                .children(
                                        Components.InputColumn(Components.obrigatorio("Nome"), viewModel.nome, "Ex: Maria Silva"),
                                        Components.InputColumn(Components.obrigatorio("Login"), viewModel.login, "Ex: maria"),
                                        Components.InputColumnAuth(Components.obrigatorio("Senha"), viewModel.senha, "Digite a senha"),
                                        Components.InputColumnPhone("Telefone", viewModel.telefone),
                                        Components.SelectColumn("Administrador?", Data.simNaoList, viewModel.ehAdminSelected, it -> it)
                                )
                        )
                        .c_child(new SpacerVertical(10))
                        .c_child(new LineHorizontal())
                        .c_child(new SpacerVertical(20))
                        .c_child(Components.actionButtons(viewModel.btnText, this::handleAddOrUpdate))
        );
    }

     void handleAddOrUpdate() {
        try {
            viewModel.handleAddOrUpdate();
        } catch (Exception e) {
            log.error("Erro em handleAddOrUpdate", e);
            UI.runOnUi(() -> Components.ShowAlertError(e.getMessage()));
        }
    }
}
