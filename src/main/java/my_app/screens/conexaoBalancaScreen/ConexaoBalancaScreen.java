package my_app.screens.conexaoBalancaScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Card;
import megalodonte.components.SpacerVertical;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.Row;
import megalodonte.props.ColumnProps;
import megalodonte.props.ContainerProps;
import megalodonte.props.RowProps;
import megalodonte.router.v4.ScreenContext;
import megalodonte.v2.Show;
import my_app.domain.components.Components;

public class ConexaoBalancaScreen implements ScreenComponent {
    private final ConexaoBalancaViewModel vm;

    public ConexaoBalancaScreen(ScreenContext ctx) {
        this.vm = new ConexaoBalancaViewModel(ctx);
    }

    @Override
    public void onMount() {
        vm.load();
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
                        new Column(new ColumnProps().paddingAll(20).spacingOf(ThemeManager.theme().spacing().sm()))
                                .c_child(Components.FormTitle("Conexão com a balança"))
                                .c_child(new SpacerVertical(10))
                                .c_child(Components.SelectColumn("Tipo de conexão",
                                        ConexaoBalancaViewModel.tiposConexaoList, vm.tipoConexaoSelected, it -> it))
                                .c_child(new SpacerVertical(10))
                                .c_child(Show.when(vm.ehSerial,
                                        () -> new Row(new RowProps().spacingOf(10)).children(
                                                Components.SelectColumn("Porta COM", vm.portasComState, vm.portaComSelected, it -> it, false),
                                                Components.InputColumnNumeric("Baud rate", vm.baudRate, "Ex: 9600")
                                        ),
                                        () -> new Row(new RowProps().spacingOf(10)).children(
                                                Components.InputColumn("Endereço IP", vm.ipAddress, "Ex: 192.168.0.100"),
                                                Components.InputColumnNumeric("Porta", vm.ipPort, "Ex: 9100")
                                        )
                                ))
                                .c_child(new SpacerVertical(20))
                                .c_child(Components.ButtonCadastro("Salvar", vm::salvar))
                )
        );
    }
}
