package my_app.domain;

import megalodonte.base.UI;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.Card;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.props.ColumnProps;
import megalodonte.props.TextProps;
import megalodonte.base.route.v2.ScreenContextInterface;
import my_app.domain.components.Components;

import java.util.function.Function;

/** Impede que uma rota administrativa seja aberta diretamente por um operador. */
public final class AcessoAdminScreen implements ScreenComponent {
    private final ScreenContextInterface ctx;

    private AcessoAdminScreen(ScreenContextInterface ctx) {
        this.ctx = ctx;
    }

    public static ScreenComponent proteger(ScreenContextInterface ctx,
                                            Function<ScreenContextInterface, ScreenComponent> fabrica) {
        return Autorizacao.podeAdministrar() ? fabrica.apply(ctx) : new AcessoAdminScreen(ctx);
    }

    @Override
    public void onMount() {
        UI.runOnUi(() -> {
            Components.ShowAlertError("Acesso restrito a administradores.");
            ctx.selfStage().close();
        });
    }

    @Override
    public Component render() {
        return new Card(new Column(new ColumnProps().paddingAll(20))
                .c_child(new Text("Acesso restrito a administradores.", new TextProps().bold())));
    }
}
