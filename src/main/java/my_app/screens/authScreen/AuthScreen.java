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
        return new Container(new ContainerProps().paddingAll(20).bgImage("/assets/wallpapers/bgAuth.jpg")).children(
                new Row(new RowProps().fillWidth().fillHeight()).children(
                        new Column(new ColumnProps().fillHeight().spacingOf(ThemeManager.theme().spacing().md())
                                .centerHorizontally()
                                .centerVertically()
                                .maxWidth(400)
                                //.paddingTop(90)).children(
                                ).children(
                                new Image("/assets/app_banner.png", new ImageProps().size(300)),
                                new Row().children(
                                        new SpacerHorizontal(50),
                                        new TextFlow(
                                                new Text("Seu sistema completo de pesagem de balança de caminhão: leitura automática da balança, controle de tara, peso bruto e líquido, cadastro de clientes e produtos — tudo em um só lugar.",
                                                        new TextProps().color("white").fontSize(ThemeManager.theme().typography().small()))
                                        )
                                )
                        ),
                        new SpacerHorizontal().fill(),
                        new Column(new ColumnProps().fillHeight().centerVertically().paddingRight(50)).children(
                                new Card(
                                        new Column(new ColumnProps().paddingAll(10)).children(
                                                new Text("Login", new TextProps().fontSize(ThemeManager.theme().typography().subtitle())),
                                                new SpacerVertical(ThemeManager.theme().spacing().lg()),
                                                Components.InputColumnAuth(Components.obrigatorio("E-mail"), vm.loginState, "Ex: gestor@teste.com"),
                                                new SpacerVertical(ThemeManager.theme().spacing().lg()),
                                                Components.InputColumnAuth(Components.obrigatorio("Senha"), vm.passwordState, "Digite sua senha"),
                                                new SpacerVertical(ThemeManager.theme().spacing().xl()),
                                                Components.ButtonCadastro("Entrar", () -> vm.entrar(ctx))
                                        )
                                )
                        )


                )
        );
    }
}
