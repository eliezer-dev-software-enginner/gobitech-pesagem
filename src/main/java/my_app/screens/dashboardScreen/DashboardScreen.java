package my_app.screens.dashboardScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.FlowRow;
import megalodonte.props.ColumnProps;
import megalodonte.props.FlowRowProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;
import my_app.domain.components.Components;

public class DashboardScreen implements ScreenComponent {
    private final DashboardViewModel vm;

    public DashboardScreen(ScreenContext ctx) {
        this.vm = new DashboardViewModel(ctx);
    }

    @Override
    public void onMount() {
        vm.carregar();
        vm.iniciarLeituraBalanca();
    }

    @Override
    public void onDestroy() {
        vm.onDestroy();
    }

    @Override
    public Component render() {
        return new Column(new ColumnProps().paddingAll(20).spacingOf(20).fillHeight())
                .c_child(new Text("Balanças Gobitech", new TextProps().fontSize(ThemeManager.theme().typography().title()).bold()))
                .c_child(new Text("Sistema de pesagem", new TextProps().fontSize(ThemeManager.theme().typography().body())))
                .c_child(new SpacerVertical(10))
                .c_child(Components.SubtitleWithState("Peso da balança agora (Kg): ", vm.pesoAoVivo))
                .c_child(new SpacerVertical(10))
                .c_child(new FlowRow(new FlowRowProps().fillWidth().spacingOf(16))
                        .children(
                                Components.StatCard("Produtos cadastrados", vm.totalProdutos),
                                Components.StatCard("Clientes cadastrados", vm.totalClientes),
                                Components.StatCard("Pesagens no total", vm.totalPesagens),
                                Components.StatCard("Pesagens neste mês", vm.totalPesagensMes)
                        ));
    }
}
