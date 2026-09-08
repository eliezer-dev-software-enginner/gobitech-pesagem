package my_app.screens.produtoScreen;

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
import my_app.db.services.ProdutoService;
import my_app.domain.Data;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddOrEditProdutoScreen implements ScreenComponent {
    Long id;
    ProdutoScreenViewModel viewModel;
    Logger log = LoggerFactory.getLogger(AddOrEditProdutoScreen.class);
    ProdutoService produtoService;

    State<String> titleState = new State<>("");

    public AddOrEditProdutoScreen(ScreenContext screenContext){
        viewModel = new ProdutoScreenViewModel(screenContext);
        produtoService = createOrReport(ProdutoService::new);
        String type = screenContext.getParams().get("type");

        String idParam = screenContext.getParams().get("id");
        try {
            id = Long.parseLong(idParam);
        } catch (RuntimeException e) {
            log.error("Parâmetro 'id' inválido na rota de edição/inclusão: {}", idParam, e);
            try {
                viewModel.onDestroy();
                produtoService.close();
            } catch (Exception cleanup) {
                log.warn("Erro ao limpar recursos após rota inválida", cleanup);
            }
            UI.runOnUi(() -> Components.ShowAlertError("ID inválido na rota de edição/inclusão."));
            return;
        }

        Async.Run(()->{
            var model = produtoService.buscarById(id);
            UI.runOnUi(()-> {
                viewModel.selected.set(model);
                titleState.set("Incluir produto");
                screenContext.selfStage().setTitle("Inclusão de produto");

                if(type.equals("edit")){
                    viewModel.modoEdicaoState().set(true);
                    viewModel.populateFieldsFromModel();
                    titleState.set("Editar produto com Id: " + id);
                    screenContext.selfStage().setTitle("Edição de produto");
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
                new Column(new ColumnProps().paddingAll(10))
                        .c_child(Components.FormTitle(titleState))
                        .c_child(new SpacerVertical(20))
                        .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                                .children(
                                        Components.InputColumn(Components.obrigatorio("Nome do produto"), viewModel.nome, "Ex: Soja"),
                                        Components.SelectColumn("Unidade", Data.unidadesDeMedidaList, viewModel.unidadeSelected, it -> it),
                                        Components.InputColumnDecimal("Desconto padrão (%)", viewModel.desconto, "0")
                                )
                        )
                        .c_child(new SpacerVertical(10))
                        .c_child(new LineHorizontal())
                        .c_child(Components.TextAreaColumn("Observações", viewModel.observacoes, "Alguma observação sobre o produto?", 60, 160))
                        .c_child(new SpacerVertical(20))
                        .c_child(Components.actionButtons(viewModel.btnText, this::handleAddOrUpdate))
        );
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

    // A ViewModel se inscreve no EventBus global e abre a própria sessão; o Service da tela
    // também. Sem o onDestroy, cada abertura do formulário vaza listener + conexão (o padrão
    // do NPE de session nula já corrigido no projeto).
    @Override
    public void onDestroy() {
        try {
            viewModel.onDestroy();
            produtoService.close();
        } catch (Exception e) {
            log.warn("Erro ao destruir AddOrEditProdutoScreen", e);
        }
    }
}
