package my_app.screens.preferenciasScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Button;
import megalodonte.components.LineHorizontal;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.props.ButtonProps;
import megalodonte.props.ColumnProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;
import my_app.domain.components.Components;

public class PreferenciasScreen implements ScreenComponent {

    private final PreferenciasViewModel vm;

    public PreferenciasScreen(ScreenContext ctx) {
        this.vm = new PreferenciasViewModel(ctx);
    }

    public Component render() {
        return new Column(new ColumnProps().paddingAll(20).spacingOf(ThemeManager.theme().spacing().sm())).children(
                new Text("Preferências", new TextProps().bold().fontSize(ThemeManager.theme().typography().subtitle())),
                new LineHorizontal(),
                new Button("Encerrar sessão",
                        new ButtonProps().fillWidth().height(31)
                                .fontSize(14).textColor("white").bgColor("#dc2626"))
                        .onClick(() -> Components.ShowAlertAdvice(
                                "Tem certeza que deseja sair?",
                                vm::signOut
                        ))
        );
    }
}
