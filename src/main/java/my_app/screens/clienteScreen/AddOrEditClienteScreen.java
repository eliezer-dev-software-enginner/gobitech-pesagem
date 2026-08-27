package my_app.screens.clienteScreen;

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
import my_app.db.services.ClienteService;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddOrEditClienteScreen implements ScreenComponent {
    Long id;
    ClienteViewModel viewModel;
    Logger log = LoggerFactory.getLogger(AddOrEditClienteScreen.class);
    ClienteService clienteService;

    State<String> titleState = new State<>("");

    public AddOrEditClienteScreen(ScreenContext screenContext){
        id = Long.parseLong(screenContext.getParams().get("id"));
        String type = screenContext.getParams().get("type");
        viewModel = new ClienteViewModel(screenContext);
        clienteService = createOrReport(ClienteService::new);

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
    public Component render() {
        return Components.ScrollPaneDefault(
                new Card(
                        new Column(new ColumnProps().paddingAll(20))
                                .c_child(Components.FormTitle("Cadastrar cliente"))
                                .c_child(new SpacerVertical(20))
                                .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                                        .children(
                                                Components.InputColumn("Loja", viewModel.loja, "Ex: Fazenda Santa Rita"),
                                                Components.InputColumn("Razão social", viewModel.razaoSocial, "Ex: Santa Rita Agropecuária Ltda"),
                                                Components.InputColumnCpfCnpj("CPF/CNPJ", viewModel.cnpjCpf),
                                                Components.InputColumnPhone("Telefone", viewModel.telefone)
                                        )
                                )
                                .c_child(new SpacerVertical(10))
                                .c_child(Components.enderecoComponent(viewModel.enderecoState.get()))
                                .c_child(Components.InputColumn("Complemento", viewModel.complemento, "Ex: Galpão 2"))
                                .c_child(new SpacerVertical(20))
                                .c_child(new LineHorizontal())
                                .c_child(new SpacerVertical(20))
                                .c_child(Components.actionButtons(viewModel.btnText, this::handleAddOrUpdate))
                )
        ) ;
    }

     void handleAddOrUpdate() {
        try {
            viewModel.handleAddOrUpdate();
            viewModel.modoEdicaoState().set(false);
        } catch (Exception e) {
            log.error("Erro em handleAddOrUpdate", e);
            UI.runOnUi(() -> Components.ShowAlertError(e.getMessage()));
        }
    }
}
