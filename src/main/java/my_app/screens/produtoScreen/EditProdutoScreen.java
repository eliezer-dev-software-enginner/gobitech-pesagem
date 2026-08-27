package my_app.screens.produtoScreen;

import megalodonte.base.UI;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.Card;
import megalodonte.components.LineHorizontal;
import megalodonte.components.SpacerVertical;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.FlowRow;
import megalodonte.props.ColumnProps;
import megalodonte.props.FlowRowProps;
import megalodonte.router.v4.ScreenContext;
import my_app.domain.Data;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProdutoDetails implements ScreenComponent {
    Long id;
    ProdutoScreenViewModel viewModel;
    Logger log = LoggerFactory.getLogger(ProdutoDetails.class);

    public ProdutoDetails(ScreenContext screenContext){
        id = Long.parseLong(screenContext.getParams().get("id"));
        viewModel = new ProdutoScreenViewModel(screenContext);
    }

    @Override
    public Component render() {
         return new Card(
                new Column(new ColumnProps().paddingAll(10))
                        .c_child(Components.FormTitle("Cadastrar produto"))
                        .c_child(new SpacerVertical(20))
                        .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                                .children(
                                        Components.InputColumn("Nome do produto", viewModel.nome, "Ex: Soja"),
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
}
