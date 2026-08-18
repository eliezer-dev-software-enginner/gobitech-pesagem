package my_app.screens.produtoScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Card;
import megalodonte.components.LineHorizontal;
import megalodonte.components.SimpleTable;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.FlowRow;
import megalodonte.props.ColumnProps;
import megalodonte.props.FlowRowProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;
import my_app.db.models.ProdutoModel;
import my_app.domain.ContratoTelaCrudV3;
import my_app.domain.Data;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import my_app.utils.DateUtils;

public class ProdutoScreen implements ScreenComponent, ContratoTelaCrudV3<ProdutoModel> {
    private final ProdutoScreenViewModel vm;
    private final ScreenContext screenContext;

    public ProdutoScreen(ScreenContext ctx) {
        this.screenContext = ctx;
        this.vm = new ProdutoScreenViewModel(ctx);
    }

    @Override
    public void onMount() {
        vm.fetchListData();
    }

    @Override
    public void onDestroy() {
        ContratoTelaCrudV3.super.onDestroy();
    }

    @Override
    public Component render() {
        return mainView();
    }

    @Override
    public Component form() {
        return new Card(
                new Column(new ColumnProps().paddingAll(20))
                        .c_child(Components.FormTitle("Cadastrar produto"))
                        .c_child(new SpacerVertical(20))
                        .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                                .children(
                                        Components.InputColumn("Nome do produto", vm.nome, "Ex: Soja"),
                                        Components.SelectColumn("Unidade", Data.unidadesDeMedidaList, vm.unidadeSelected, it -> it),
                                        Components.InputColumnDecimal("Desconto padrão (%)", vm.desconto, "0")
                                )
                        )
                        .c_child(new SpacerVertical(10))
                        .c_child(new LineHorizontal())
                        .c_child(Components.TextAreaColumn("Observações", vm.observacoes, "Alguma observação sobre o produto?", 60, 160))
                        .c_child(new SpacerVertical(20))
                        .c_child(Components.actionButtons(vm.btnText, this::handleAddOrUpdate))
        );
    }

    @Override
    public ViewModelScreenContract viewModel() {
        return vm;
    }

    @Override
    public SimpleTable<ProdutoModel> table() {
        var simpleTable = new SimpleTable<ProdutoModel>();
        simpleTable.fromData(vm.filteredList)
                .header()
                .columns()
                .column("ID", ProdutoModel::getId, 60.0)
                .column("Nome", ProdutoModel::getNome)
                .column("Unidade", ProdutoModel::getUnidade)
                .column("Desconto (%)", it -> it.getDesconto() == null ? "0" : it.getDesconto().toPlainString())
                .column("Data de criação", it -> DateUtils.localDateTimeToBrazilianDateTime(it.getDataCriacao()))
                .build()
                .onItemSelectChange(vm.produtoSelecionado::set)
                .onItemDoubleClick(it -> showItemDetailsComAcoes(it, this.screenContext, 350));

        return simpleTable;
    }

    public Component itemDetails(ProdutoModel model) {
        return new Column(new ColumnProps().paddingAll(20))
                .c_child(new Text("Detalhes do produto", new TextProps().fontSize(ThemeManager.theme().typography().subtitle())))
                .c_child(new SpacerVertical(20))
                .c_child(Components.TextWithDetails("ID: ", model.getId()))
                .c_child(Components.TextWithDetails("Nome: ", model.getNome()))
                .c_child(Components.TextWithDetails("Unidade: ", model.getUnidade()))
                .c_child(Components.TextWithDetails("Desconto padrão (%): ", model.getDesconto() == null ? "0" : model.getDesconto().toPlainString()))
                .c_child(Components.TextWithDetails("Data de criação: ", DateUtils.localDateTimeToBrazilianDateTime(model.getDataCriacao())))
                .c_child(Components.TextWithDetails("Observações: ", model.getObservacoes(), true));
    }
}
