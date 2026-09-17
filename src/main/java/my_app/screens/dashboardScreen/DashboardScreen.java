package my_app.screens.dashboardScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.ReadableState;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Image;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.FlowRow;
import megalodonte.components.layout_components.Row;
import megalodonte.props.*;
import megalodonte.router.v4.ScreenContext;
import megalodonte.v2.Show;
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
        return new Column(new ColumnProps().paddingAll(20).fillHeight())
                .c_child(renderTop())
                .c_child(new SpacerVertical().fill())
                .c_child(renderBalancaLive());
    }

    Column renderTop(){
        return new Column(new ColumnProps().paddingAll(20).spacingOf(10)).children(
                new Text("Balanças Gobitech", new TextProps().fontSize(ThemeManager.theme().typography().title()).bold()),
                new Text("Sistema de pesagem", new TextProps().fontSize(ThemeManager.theme().typography().body())),
                Show.when(vm.logoVazia, ()->  new Image(vm.logoHorizontal, new ImageProps().width(700).height(200)))
        );
    }

     Component renderBalancaLive() {
        return new Row(new RowProps().fillWidth().rightHorizontally()).children(
                new Column(new ColumnProps().paddingAll(20).spacingOf(5)).children(
                        new Text("Balança", new TextProps().fontSize(ThemeManager.theme().typography().title()).bold()),
                        new Text(vm.pesoAoVivo, new TextProps().fontSize(ThemeManager.theme().typography().title()).bold())
                )
        );
    }
}
