package my_app.screens.licensaScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Card;
import megalodonte.components.LineHorizontal;
import megalodonte.components.SimpleTable;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.props.ColumnProps;
import megalodonte.props.ContainerProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;
import megalodonte.v2.Show;
import my_app.db.models.LicensaModel;
import my_app.domain.components.Components;
import my_app.utils.DateUtils;

public class LicensaScreen implements ScreenComponent {
    private final LicensaViewModel vm;

    public LicensaScreen(ScreenContext ctx) {
        this.vm = new LicensaViewModel(ctx);
    }

    @Override
    public void onMount() {
        if (!vm.acessoPermitido()) {
            vm.bloquearAcesso();
            return;
        }
        vm.carregar();
    }

    @Override
    public void onDestroy() {
        try {
            vm.onDestroy();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Component render() {
        return new Container(new ContainerProps().paddingAll(10)).children(
                new Card(
                        new Column(new ColumnProps().paddingAll(20))
                                .c_child(Components.FormTitle("Gerar licença"))
                                .c_child(new SpacerVertical(10))
                                .c_child(new Text("Data de validade (deixe em branco pra sem expiração):",
                                        new TextProps().fontSize(ThemeManager.theme().typography().small())))
                                .c_child(Components.DatePickerColumn(vm.dataExpiracao, "Validade"))
                                .c_child(new SpacerVertical(10))
                                .c_child(Components.ButtonCadastro("Gerar nova licença", vm::gerar))
                                .c_child(new SpacerVertical(10))
                                .c_child(Show.when(vm.codigoGeradoVisible,
                                        () -> Components.InputColumn("Código gerado", vm.codigoGerado, "", true)))
                                .c_child(new SpacerVertical(20))
                                .c_child(new LineHorizontal())
                                .c_child(new SpacerVertical(10))
                                .c_child(Components.FormTitle("Licenças já geradas"))
                                .c_child(new SpacerVertical(10))
                                .c_child(table())
                )
        );
    }

    private SimpleTable<LicensaModel> table() {
        var simpleTable = new SimpleTable<LicensaModel>();
        simpleTable.fromData(vm.licensasState)
                .header()
                .columns()
                .column("Código", LicensaModel::getValor)
                .column("Validade", it -> it.getExpiraEm() == null ? "Sem expiração"
                        : DateUtils.localDateTimeToBrazilianDateTime(it.getExpiraEm()))
                .column("Gerada em", it -> DateUtils.localDateTimeToBrazilianDateTime(it.getDataCriacao()))
                .build();

        return simpleTable;
    }
}
