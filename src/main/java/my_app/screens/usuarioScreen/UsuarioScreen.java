package my_app.screens.usuarioScreen;

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
import megalodonte.props.SimpleTableProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;
import my_app.db.models.UsuarioModel;
import my_app.domain.ContratoTelaCrudV3;
import my_app.domain.Data;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import my_app.utils.DateUtils;

public class UsuarioScreen implements ScreenComponent, ContratoTelaCrudV3<UsuarioModel> {
    private final UsuarioScreenViewModel vm;
    private final ScreenContext screenContext;

    public UsuarioScreen(ScreenContext ctx) {
        this.screenContext = ctx;
        this.vm = new UsuarioScreenViewModel(ctx);
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
                        .c_child(Components.FormTitle("Cadastrar usuário"))
                        .c_child(new SpacerVertical(20))
                        .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                                .children(
                                        Components.InputColumn("Nome", vm.nome, "Ex: Maria Silva"),
                                        Components.InputColumn("Login", vm.login, "Ex: maria"),
                                        Components.InputColumnAuth("Senha", vm.senha, "Digite a senha"),
                                        Components.InputColumnPhone("Telefone", vm.telefone),
                                        Components.SelectColumn("Administrador?", Data.simNaoList, vm.ehAdminSelected, it -> it)
                                )
                        )
                        .c_child(new SpacerVertical(10))
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
    public SimpleTable<UsuarioModel> table() {
        var simpleTable = new SimpleTable<UsuarioModel>(new SimpleTableProps().maxHeight(Components.TABLE_MAX_HEIGHT));
        simpleTable.fromData(vm.filteredList)
                .header()
                .columns()
                .column("ID", UsuarioModel::getId, 60.0)
                .column("Nome", UsuarioModel::getNome)
                .column("Login", UsuarioModel::getLogin)
                .column("Admin", it -> Boolean.TRUE.equals(it.getAdmin()) ? "Sim" : "Não")
                .column("Data de criação", it -> DateUtils.localDateTimeToBrazilianDateTime(it.getDataCriacao()))
                .build()
                .onItemSelectChange(vm.usuarioSelecionado::set)
                .onItemDoubleClick(it -> showItemDetailsComAcoes(it, this.screenContext, 350));

        return simpleTable;
    }

    public Component itemDetails(UsuarioModel model) {
        return new Column(new ColumnProps().paddingAll(20))
                .c_child(new Text("Detalhes do usuário", new TextProps().fontSize(ThemeManager.theme().typography().subtitle())))
                .c_child(new SpacerVertical(20))
                .c_child(Components.TextWithDetails("ID: ", model.getId()))
                .c_child(Components.TextWithDetails("Nome: ", model.getNome()))
                .c_child(Components.TextWithDetails("Login: ", model.getLogin()))
                .c_child(Components.TextWithDetails("Telefone: ", model.getTelefone()))
                .c_child(Components.TextWithDetails("Administrador: ", Boolean.TRUE.equals(model.getAdmin()) ? "Sim" : "Não"))
                .c_child(Components.TextWithDetails("Data de criação: ", DateUtils.localDateTimeToBrazilianDateTime(model.getDataCriacao())));
    }
}
