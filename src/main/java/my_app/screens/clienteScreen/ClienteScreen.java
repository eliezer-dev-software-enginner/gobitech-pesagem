package my_app.screens.clienteScreen;

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
import my_app.db.models.ClienteModel;
import my_app.domain.ContratoTelaCrudV3;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import my_app.utils.DateUtils;

public class ClienteScreen implements ScreenComponent, ContratoTelaCrudV3<ClienteModel> {
    private final ClienteViewModel vm;
    private final ScreenContext screenContext;

    public ClienteScreen(ScreenContext ctx) {
        this.screenContext = ctx;
        this.vm = new ClienteViewModel(ctx);
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
        return mainView(vm.focusState);
    }

    @Override
    public Component form() {
        return new Card(
                new Column(new ColumnProps().paddingAll(20))
                        .c_child(Components.FormTitle("Cadastrar cliente"))
                        .c_child(new SpacerVertical(20))
                        .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                                .children(
                                        Components.InputColumn("Loja", vm.loja, "Ex: Fazenda Santa Rita"),
                                        Components.InputColumn("Razão social", vm.razaoSocial, "Ex: Santa Rita Agropecuária Ltda"),
                                        Components.InputColumnCnpjAlfanumerico("CPF/CNPJ", vm.cnpjCpf),
                                        Components.InputColumnPhone("Telefone", vm.telefone)
                                )
                        )
                        .c_child(new SpacerVertical(10))
                        .c_child(Components.enderecoComponent(vm.enderecoState.get()))
                        .c_child(Components.InputColumn("Complemento", vm.complemento, "Ex: Galpão 2"))
                        .c_child(new SpacerVertical(20))
                        .c_child(new LineHorizontal())
                        .c_child(new SpacerVertical(20))
                        .c_child(Components.actionButtons(vm.btnText, this::handleAddOrUpdate))
        );
    }

    @Override
    public ViewModelScreenContract viewModel() {
        return vm;
    }

    @Override
    public SimpleTable<ClienteModel> table() {
        var simpleTable = new SimpleTable<ClienteModel>();
        simpleTable.fromData(vm.filteredList)
                .header()
                .columns()
                .column("ID", ClienteModel::getId)
                .column("Loja", ClienteModel::getLoja)
                .column("Razão social", ClienteModel::getRazaoSocial)
                .column("CPF/CNPJ", ClienteModel::getCpfCnpj)
                .column("Telefone", ClienteModel::getTelefone)
                .column("Data de criação", it -> DateUtils.localDateTimeToBrazilianDateTime(it.getDataCriacao()))
                .build()
                .onChangeFocus(vm::handleFocusChange)
                .onItemSelectChange(vm.clienteSelecionado::set)
                .onItemDoubleClick(it -> Components.ShowModal(itemDetails(it), this.screenContext, 400));

        return simpleTable;
    }

    public Component itemDetails(ClienteModel model) {
        return new Column(new ColumnProps().paddingAll(20))
                .c_child(new Text("Detalhes do cliente", new TextProps().fontSize(ThemeManager.theme().typography().subtitle())))
                .c_child(new SpacerVertical(20))
                .c_child(Components.TextWithDetails("ID: ", model.getId()))
                .c_child(Components.TextWithDetails("Loja: ", model.getLoja()))
                .c_child(Components.TextWithDetails("Razão social: ", model.getRazaoSocial()))
                .c_child(Components.TextWithDetails("CPF/CNPJ: ", model.getCpfCnpj()))
                .c_child(Components.TextWithDetails("Telefone: ", model.getTelefone()))
                .c_child(Components.ItemDetailEndereco(model.getEndereco()))
                .c_child(Components.TextWithDetails("Complemento: ", model.getComplemento()))
                .c_child(Components.TextWithDetails("Data de criação: ", DateUtils.localDateTimeToBrazilianDateTime(model.getDataCriacao())));
    }
}
