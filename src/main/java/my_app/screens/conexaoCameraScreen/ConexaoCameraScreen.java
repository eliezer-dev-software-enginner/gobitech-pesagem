package my_app.screens.conexaoCameraScreen;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Button;
import megalodonte.components.Card;
import megalodonte.components.Image;
import megalodonte.components.LineHorizontal;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.TextFlow;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.FlowRow;
import megalodonte.components.layout_components.Row;
import megalodonte.props.ButtonProps;
import megalodonte.props.ColumnProps;
import megalodonte.props.ContainerProps;
import megalodonte.props.FlowRowProps;
import megalodonte.props.ImageProps;
import megalodonte.props.RowProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;
import my_app.domain.components.Components;

public class ConexaoCameraScreen implements ScreenComponent {
    private final ConexaoCameraViewModel vm;

    public ConexaoCameraScreen(ScreenContext ctx) {
        this.vm = new ConexaoCameraViewModel(ctx);
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
                                .c_child(Components.FormTitle("Conexão das câmeras"))
                                .c_child(new TextFlow(new Text(
                                        "Cada pesagem nova captura uma foto de cada câmera automaticamente — na Entrada preenche a foto 1, na Saída a foto 2. Deixe uma câmera em branco se ainda não tiver sido instalada.",
                                        new TextProps().fontSize(ThemeManager.theme().typography().small()))))
                                .c_child(new SpacerVertical(10))
                                .c_child(secaoCamera("Câmera da frente", vm.frenteIp, vm.frentePorta, vm.frenteCanal,
                                        vm.frenteUsuario, vm.frenteSenha, vm.frentePreview, vm::testarFrente))
                                .c_child(new SpacerVertical(15))
                                .c_child(new LineHorizontal())
                                .c_child(new SpacerVertical(15))
                                .c_child(secaoCamera("Câmera de trás", vm.costasIp, vm.costasPorta, vm.costasCanal,
                                        vm.costasUsuario, vm.costasSenha, vm.costasPreview, vm::testarCostas))
                                .c_child(new SpacerVertical(20))
                                .c_child(Components.ButtonCadastro("Salvar", vm::salvar))
                )
        );
    }

    private Component secaoCamera(String titulo, megalodonte.base.state.State<String> ip, megalodonte.base.state.State<String> porta,
                                   megalodonte.base.state.State<String> canal, megalodonte.base.state.State<String> usuario,
                                   megalodonte.base.state.State<String> senha, megalodonte.base.state.State<String> preview,
                                   Runnable onTestar) {
        return new Row(new RowProps().spacingOf(20))
                .children(
                        new Column(new ColumnProps().spacingOf(10))
                                .c_child(Components.FormTitle(titulo))
                                .c_child(new FlowRow(new FlowRowProps().spacingOf(10))
                                        .children(
                                                Components.InputColumn("Endereço IP", ip, "Ex: 192.168.0.101"),
                                                Components.InputColumnNumeric("Porta", porta, "Ex: 80"),
                                                Components.InputColumnNumeric("Canal", canal, "Ex: 1"),
                                                Components.InputColumn("Usuário", usuario, "Ex: admin"),
                                                Components.InputColumn("Senha", senha, "Senha da câmera")
                                        )
                                )
                                .c_child(new SpacerVertical(10))
                                .c_child(new Button("Testar câmera", new ButtonProps().height(31)
                                        .bgColor("#6b7280").textColor("white"))
                                        .onClick(onTestar)),
                        new Column(new ColumnProps().spacingOf(5))
                                .c_child(Components.FormTitle("Prévia"))
                                .c_child(new Image(preview, new ImageProps().size(140)))
                );
    }
}
