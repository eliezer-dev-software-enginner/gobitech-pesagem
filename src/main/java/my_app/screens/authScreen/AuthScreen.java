package my_app.screens.authScreen;

import megalodonte.base.Redirect;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.*;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.Row;
import megalodonte.props.*;
import megalodonte.router.v4.ScreenContext;
import my_app.Main;
import my_app.domain.Data;
import my_app.domain.components.Components;

public class AuthScreen implements ScreenComponent {
    private final ScreenContext ctx;
    private final AuthScreenViewModel vm;

    public AuthScreen(ScreenContext ctx) {
        this.ctx = ctx;
        this.vm = new AuthScreenViewModel();
    }

    @Override
    public void onMount() {
        ctx.selfStage().getIcons().add(Main.loadIcon());
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
        return new Container(new ContainerProps().paddingAll(20).bgImage("/assets/bgAuth.jpg")).children(
                new Row(new RowProps().fillWidth()).children(
                        new Column(new ColumnProps().spacingOf(ThemeManager.theme().spacing().md()).centerHorizontally()
                                .paddingTop(90)).children(
                                new Image("/assets/app_banner.png", new ImageProps().size(210)),
                                new Text("Seu sistema de pesagem de balança de caminhão completo", new TextProps().color("white").bold())
                        ),
                        new SpacerHorizontal().fill(),
                        new Card(
                                new Column().children(
                                        new Text("Login", new TextProps().fontSize(ThemeManager.theme().typography().subtitle())),
                                        Components.InputColumnAuth("E-mail", vm.loginState, "Ex: gestor@teste.com"),
                                        Components.InputColumnAuth("Senha", vm.passwordState, "Digite sua senha"),
                                        new SpacerVertical(ThemeManager.theme().spacing().sm()),
                                        Components.ButtonCadastro("Entrar", () -> vm.entrar(ctx))
                                )
                        )

                )
        );
    }
}
