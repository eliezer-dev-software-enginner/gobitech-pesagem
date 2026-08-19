package my_app.screens.logsScreen;

import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Button;
import megalodonte.components.SpacerHorizontal;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.inputs.TextAreaInput;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.Row;
import megalodonte.props.ButtonProps;
import megalodonte.props.ColumnProps;
import megalodonte.props.ContainerProps;
import megalodonte.props.InputProps;
import megalodonte.props.RowProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;

public class LogsScreen implements ScreenComponent {
    private final LogsScreenViewModel vm;

    public LogsScreen(ScreenContext ctx) {
        this.vm = new LogsScreenViewModel();
    }

    @Override
    public Component render() {
        var textoLogs = new TextAreaInput(new State<>(vm.conteudoLogs.get()),
                new InputProps().fontSize(ThemeManager.theme().typography().small()));
        var textArea = (TextArea) textoLogs.getNode();
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setStyle("-fx-font-family: 'Courier New', monospace;");
        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(textArea, Priority.ALWAYS);
        vm.conteudoLogs.subscribe(textArea::setText);

        return new Container(new ContainerProps().paddingAll(20).fillHeight())
                .children(
                        new Column(new ColumnProps().fillWidth().fillHeight().spacingOf(10))
                                .children(
                                        new Text("Logs da aplicação", new TextProps().fontSize(ThemeManager.theme().typography().subtitle())),
                                        new Row(new RowProps().fillWidth().spacingOf(10))
                                                .children(
                                                        new Button("Atualizar", new ButtonProps().height(31)
                                                                .bgColor(ThemeManager.theme().colors().primary()).textColor("black"))
                                                                .onClick(vm::carregarLogs),
                                                        new Button("Abrir pasta de logs", new ButtonProps().height(31)
                                                                .bgColor("#6b7280").textColor("white"))
                                                                .onClick(vm::abrirPastaDeLogs),
                                                        new SpacerHorizontal().fill()
                                                ),
                                        new SpacerVertical(5),
                                        textoLogs
                                )
                );
    }
}
