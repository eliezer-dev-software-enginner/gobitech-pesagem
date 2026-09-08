package my_app.domain.components;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.*;
import megalodonte.ComputedState;
import megalodonte.application.ErrorReporter;
import megalodonte.base.Animations;
import megalodonte.base.async.RunnableThrowing;
import megalodonte.base.components.Component;
import megalodonte.base.components.IconInterface;
import megalodonte.base.components.Ref;
import megalodonte.base.state.ReadableState;
import megalodonte.base.state.State;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.*;
import megalodonte.components.Button;
import megalodonte.components.DatePicker;
import megalodonte.components.inputs.OnChangeResult;
import megalodonte.components.inputs.TextAreaInput;
import megalodonte.components.v2.Input;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.FlowRow;
import megalodonte.components.layout_components.Row;
import megalodonte.props.*;
import megalodonte.props.v2.InputProps;
import megalodonte.router.v4.ScreenContext;
import megalodonte.v2.ListState;
import my_app.domain.Data;
import my_app.domain.states.EnderecoState;
import pack.utilities.DatePack;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.antdesignicons.AntDesignIconsOutlined;
import org.kordamp.ikonli.entypo.Entypo;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import static pack.utilities.FormatterPack.formatCep;
import static pack.utilities.FormatterPack.formatCpfCnpj;
import static pack.utilities.FormatterPack.formatPhone;
import static pack.utilities.FormatterPack.formatRgCpf;

public class Components {

    // SimpleTable (megalodonte-components) não tinha teto de altura configurável — cresce até
    // preencher todo o espaço vertical disponível na página por padrão. Cada tela passa
    // "new SimpleTableProps().maxHeight(TABLE_MAX_HEIGHT)" no próprio construtor da tabela
    // (ver SimpleTableProps.maxHeight, megalodonte-components) em vez de mexer no node do
    // JavaFX depois de pronta. Acima do teto a tabela rola por dentro sozinha (comportamento
    // nativo do TableView), então itens extras nunca ficam escondidos.
    public static final double TABLE_MAX_HEIGHT = 300;

    public static IconInterface ikon(Ikon ikon, double size, String color) {
        return IconInterface.of(FontIcon.of(ikon, (int) size, Color.web(color)));
    }

    public record Endereco(String uf, String cep, String cidade, String bairro, String rua, String numero) {
    }

    public static Component ItemDetailEndereco(Endereco endereco) {
        return new Container()
                .c_child(Components.TextWithDetails("UF: ", endereco.uf()))
                .c_child(Components.TextWithDetails("CEP: ", formatCep(endereco.cep())))
                .c_child(Components.TextWithDetails("Cidade: ", endereco.cidade()))
                .c_child(Components.TextWithDetails("Bairro: ", endereco.bairro()))
                .c_child(Components.TextWithDetails("Rua: ", endereco.rua()))
                .c_child(Components.TextWithDetails("Número: ", endereco.numero()));
    }

    public static Component ItemDetailEnderecoState(ReadableState<Endereco> enderecoState) {
        return new Container()
                .c_child(Components.TextWithDetailsState("UF: ", enderecoField(enderecoState, Endereco::uf)))
                .c_child(Components.TextWithDetailsState("CEP: ", enderecoField(enderecoState, e -> formatCep(e.cep()))))
                .c_child(Components.TextWithDetailsState("Cidade: ", enderecoField(enderecoState, Endereco::cidade)))
                .c_child(Components.TextWithDetailsState("Bairro: ", enderecoField(enderecoState, Endereco::bairro)))
                .c_child(Components.TextWithDetailsState("Rua: ", enderecoField(enderecoState, Endereco::rua)))
                .c_child(Components.TextWithDetailsState("Número: ", enderecoField(enderecoState, Endereco::numero)));
    }

    public static Button actionButton(String title, String color, String bgColor, Ikon ikon, RunnableThrowing onclick){
        return new Button(title, new ButtonProps()
                .bgColor(bgColor!=null? bgColor : ThemeManager.theme().colors().primary())
                .paddingTop(ThemeManager.theme().padding().md())
                .paddingDown(ThemeManager.theme().padding().md())
                .paddingLeft(ThemeManager.theme().padding().md())
                .paddingRight(ThemeManager.theme().padding().md())
                .textColor(color))
                .onClick(onclick)
                .icon(Components.ikon(ikon,10, color));
    }

    private static ComputedState<String> enderecoField(ReadableState<Endereco> state, Function<Endereco, String> extract) {
        return ComputedState.of(() -> {
            var endereco = state.get();
            if (endereco == null) return "";
            var value = extract.apply(endereco);
            return value == null ? "" : value;
        }, state);
    }

    public static Component enderecoComponent(EnderecoState enderecoState) {
        return new Container().children(
                Components.FormTitle("Endereço"),
                new FlowRow(new FlowRowProps().spacingOf(10))
                        .children(
                                Components.InputColumnCep("Cep", enderecoState.cep),
                                Components.SelectColumn("UF", Data.ufList, enderecoState.ufSelected, it -> it),
                                Components.InputColumn("Cidade", enderecoState.cidade, "Ex: São Paulo"),
                                Components.InputColumn("Bairro", enderecoState.bairro, "Ex: Centro"),
                                Components.InputColumn("Rua", enderecoState.rua, "Ex: Av. Brasil"),
                                Components.InputColumnNumeric("Número", enderecoState.numero, "Ex: 123")
                        )
        );
    }

    public static Row TextWithDetails(String label, Object value, boolean wrapText) {
        var comp = new Text(value == null ? "" : value.toString(),
                new TextProps().fontSize(ThemeManager.theme().typography().body()));

        var textValueComponent = wrapText ? new TextFlow(comp) : comp;

        return new Row()
                .children(
                        new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().body()).bold()),
                        textValueComponent
                );
    }

    public static Row TextWithDetails(String label, Object value) {
        return TextWithDetails(label, value, false);
    }

    public static Row TextWithDetailsState(String label, ReadableState<String> valueState, boolean wrapText) {
        var comp = new Text(valueState,
                new TextProps().fontSize(ThemeManager.theme().typography().body()));

        var textValueComponent = wrapText ? new TextFlow(comp) : comp;

        return new Row()
                .children(
                        new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().body()).bold()),
                        textValueComponent
                );
    }

    public static Row TextWithDetailsState(String label, ReadableState<String> valueState) {
        return TextWithDetailsState(label, valueState, false);
    }

    public static Component actionButtons(State<String> btnText, RunnableThrowing onClick) {
        return new Button(btnText,
                new ButtonProps()
                        .fillWidth()
                        .fontSize(16)
                        .textColor("black").bgColor(ThemeManager.theme().colors().primary())
        ).onClick(onClick);
    }

    public static Component actionButtons(ComputedState<String> btnText, RunnableThrowing onClick) {
        return new Button(btnText,
                new ButtonProps()
                        .fillWidth()
                        .fontSize(16)
                        .textColor("black").bgColor(ThemeManager.theme().colors().primary())
        ).onClick(onClick);
    }

    public static Component ScrollPaneDefault(Component child) {
        var scroll = new ScrollPane();
        scroll.setContent(child.getJavaFxNode());
        VBox.setVgrow(scroll, Priority.ALWAYS);
        // Sem isso, quando o pai não é uma VBox (ex.: um HBox/Row — vgrow acima só vale pra
        // pai VBox), o ScrollPane fica travado na própria altura preferida em vez de esticar
        // até o espaço que o pai realmente oferece.
        scroll.setMaxHeight(Double.MAX_VALUE);
        // Sem isso, a altura MÍNIMA do ScrollPane é herdada do conteúdo (ex.: SimpleTable tem
        // um piso de 200px de propósito — ver comentário lá — pra ele nunca encolher a ponto de
        // nunca rolar). Esse mínimo, sem ser zerado aqui, sobe pela árvore de layout (contentArea
        // -> Row -> janela) e força a Row inteira a crescer além da altura real da janela sempre
        // que o conteúdo em foco for alto (tabela cheia + formulário), cortando qualquer coisa
        // que esteja do lado — inclusive a Sidebar, mesmo ela não tendo mudado nada. Zerando o
        // mínimo, o ScrollPane nunca dita tamanho pro pai: só aceita o que for dado e rola por
        // dentro quando não é o suficiente — que é o comportamento que se espera de um scroll.
        scroll.setMinHeight(0);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background-color: transparent;-fx-border-color: transparent;");

        return Component.CreateFromJavaFxNode(scroll);
    }

    public static void ShowPopup(ScreenContext context, String message) {
        Popup popup = new Popup();

        Label label = new Label(message);
        label.setStyle("""
                    -fx-background-color: #333;
                    -fx-text-fill: white;
                    -fx-padding: 10 16;
                    -fx-background-radius: 6;
                """);

        popup.getContent().add(label);
        popup.setAutoHide(true);
        popup.show(context.selfStage());

        // Some sozinho depois de 3s (independente de clicar fora) — senão o popup podia ficar
        // na tela até o usuário clicar em outro lugar.
        var timer = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
        timer.setOnFinished(e -> popup.hide());
        timer.play();
    }


    public static Stage ShowModal(Component ui, ScreenContext context, int height) {
        Stage stage = new Stage();

        Scroll scroll = new Scroll(ui);
        stage.setScene(new Scene((Parent) scroll.getJavaFxNode(), 800, height));
        stage.setTitle("Detalhes");
        stage.setResizable(true);

        Stage owner = context.selfStage();
        stage.initOwner(owner);

        stage.setOnHidden(event -> {
            owner.requestFocus();
            owner.toFront();
        });

        stage.show();
        return stage;
    }

    public static void ShowAlertAdvice(String bodyMessage, RunnableThrowing handleSuccessEvent) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmação");
        alert.setHeaderText(bodyMessage);
        alert.setContentText("Essa ação não poderá ser desfeita.");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                handleSuccessEvent.run();
            } catch (Exception e) {
                ErrorReporter.handle(e);
                throw new IllegalStateException(e);
            }
        }
    }

    public static void ShowAlertError(String message) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Erro");
        alert.initModality(Modality.NONE); // não deixa o Glass tocar na janela owner

        ButtonType okButton = new ButtonType("Fechar", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().add(okButton);
        alert.setContentText(message);
        alert.show();
    }

    public static Component DatePickerColumn(State<LocalDate> localDateState, String label) {
        return DatePickerColumn(localDateState, label, null);
    }

    public static Component DatePickerColumn(State<LocalDate> localDateState, String label, IconInterface icon) {
        var datePicker = new DatePicker(localDateState,
                new DatePickerProps().fontSize(ThemeManager.theme().typography().small()).height(31)
                        .placeHolder("dd/mm/yyyy")
                        .locale(new Locale("pt", "BR"))
                        .pattern("dd/MM/yyyy")
                        .width(140)
                        .editable(false)
        );

        if (icon != null) {
            datePicker.icon(icon);
        }

        return new Column()
                .c_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().small())))
                .c_child(datePicker);
    }

    public static Column ImageSelector(String title, State<String> imageState,
                                       ImageProps props,
                                       RunnableThrowing callback) {
        return new Column()
                .c_child(new Image(imageState, props))
                .c_child(new SpacerVertical(10))
                .c_child(ButtonCadastro(title, callback));
    }

    public static Component FormTitle(State<String> titleState) {
        return new Text(titleState, new TextProps().fontSize(ThemeManager.theme().typography().body()).bold());
    }
    public static Component FormTitle(String title) {
        return new Text(title, new TextProps().fontSize(ThemeManager.theme().typography().body()).bold());
    }

    /**
     * Marca um rótulo de campo que é obrigatório: adiciona um {@code *} depois do texto.
     * Usado nos formulários pra indicar visualmente os campos que o sistema valida.
     */
    public static String obrigatorio(String label) {
        return label + " *";
    }

    static final ButtonProps propsBtnCadastro = new ButtonProps().fillWidth().height(31)
            .fontSize(ThemeManager.theme().typography().small()).textColor("black").bgColor(ThemeManager.theme().colors().primary());

    public static Component ButtonCadastro(String textState, RunnableThrowing handleAdd) {
        return new Button(textState, propsBtnCadastro
        ).onClick(handleAdd);
    }

    public static Component ButtonCadastro(ComputedState<String> textState, RunnableThrowing handleAdd) {
        return new Button(textState, propsBtnCadastro
        ).onClick(handleAdd);
    }

    @Deprecated(forRemoval = true)
    public static Component ButtonCadastro(State<String> textState, RunnableThrowing handleAdd) {
        return new Button(textState, propsBtnCadastro).onClick(handleAdd);
    }

    private final static SelectProps selectProps = new SelectProps()
            .minWidth(100)
            .height(31);


    public static <T> Component SelectColumn(String label, List<T> list, State<T> stateSelected, Function<T, String> display) {
        return new Column()
                .c_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().small())))
                .c_child(new Select<T>(selectProps)
                        .items(list)
                        .value(stateSelected)
                        .displayText(display)
                );
    }


    public static <T> Component SelectColumn(String label, ListState<T> list, State<T> stateSelected, Function<T, String> display, boolean compareById) {
        var select = new Select<T>(selectProps)
                .items(list)
                .value(stateSelected)
                .displayText(display);

        if (compareById) {
            select.compareById();
        }

        return new Column()
                .c_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().small())))
                .c_child(select);
    }

    public static Column TextColumn(String label, String value) {
        return new Column(new ColumnProps())
                .c_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().body()).bold()))
                .c_child(new Text(value, new TextProps().fontSize(ThemeManager.theme().typography().body())));
    }

    /**
     * Card de KPI do dashboard: rótulo pequeno em cima, número grande embaixo.
     */
    public static Component StatCard(String label, ReadableState<String> valueState) {
        return new Card(
                new Column(new ColumnProps().spacingOf(8))
                        .c_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().body()).textColor(ThemeManager.theme().colors().textSecondary())))
                        .c_child(new Text(valueState, new TextProps().fontSize(ThemeManager.theme().typography().title()).bold())),
                new CardProps().paddingAll(20).width(220).bgColor("#ffffff")
        );
    }

    public static Component SubtitleWithState(String label, ReadableState<String> valueState) {
        return new Row(new RowProps().centerVertically().hugWidth().
                bgColor(ThemeManager.theme().colors().selection()))
                .r_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().subtitle())))
                .r_child(new Text(valueState, new TextProps().fontSize(ThemeManager.theme().typography().body())));
    }

    public static Component TextWithValue(String label, ReadableState<String> valueState) {
        return new Row()
                .r_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().body()).bold()))
                .r_child(new Text(valueState, new TextProps().fontSize(ThemeManager.theme().typography().body())));
    }

    public static Component InputColumnCep(String label, State<String> inputState) {
        var inputProps = getInputPropsV2("00000-000").width(120);

        var input = new Input(inputState, inputProps)
                .onInitialize(value -> {
                    String formatted = formatCep(value);
                    return OnChangeResult.of(formatted, value);
                })
                .onChange(value -> {
                    String numeric = value.replaceAll("[^0-9]", "");

                    if (numeric.length() > 8) {
                        numeric = numeric.substring(0, 8);
                    }

                    String formatted = formatCep(numeric);
                    return OnChangeResult.of(formatted, numeric);
                })
                .lockCursorToEnd();

        return new Column()
                .c_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().small())))
                .c_child(input);
    }

    public static class InputRef {
        private Input inputRef;

        public void set(Input input) {
            this.inputRef = input;
        }

        public void requestFocus() {
            inputRef.requestFocus();
        }
    }

    public static Component InputColumnDecimal(String label, State<String> inputState, String placeholder, InputRef inputRef) {
        var inputProps = getInputPropsV2(placeholder).width(140);

        var input = new Input(inputState, inputProps)
                .onInitialize(value -> {
                    if (value == null || value.trim().isEmpty()) {
                        return OnChangeResult.of("", "");
                    }
                    return OnChangeResult.of(formatarDecimal(value), value);
                })
                .onChange(value -> {
                    if (value == null) value = "";
                    String cleaned = value.replaceAll("[^0-9,]", "");
                    int commaIdx = cleaned.indexOf(',');
                    if (commaIdx >= 0 && commaIdx != cleaned.lastIndexOf(',')) {
                        cleaned = cleaned.substring(0, cleaned.length() - 1);
                        commaIdx = cleaned.indexOf(',');
                    }
                    String intPart = commaIdx >= 0 ? cleaned.substring(0, commaIdx) : cleaned;
                    String decPart = commaIdx >= 0 ? "," + cleaned.substring(commaIdx + 1) : "";
                    String intTrimmed = intPart.isEmpty() ? "0" : intPart.replaceFirst("^0+(?!$)", "");
                    StringBuilder fmt = new StringBuilder();
                    int len = intTrimmed.length();
                    for (int i = 0; i < len; i++) {
                        if (i > 0 && (len - i) % 3 == 0) fmt.append('.');
                        fmt.append(intTrimmed.charAt(i));
                    }
                    String display = fmt + decPart;
                    String internal = intTrimmed + (commaIdx >= 0 ? "." + cleaned.substring(commaIdx + 1) : "");
                    return OnChangeResult.of(display, internal);
                })
                .lockCursorToEnd();

        if (inputRef != null) inputRef.set((Input) input);

        return new Column()
                .c_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().small())))
                .c_child(input);
    }

    public static Component InputColumnDecimal(String label, State<String> inputState, String placeholder) {
        return InputColumnDecimal(label, inputState, placeholder, null);
    }

    /** Input numérico de inteiro (sem casa decimal), com separador de milhar (ponto). */
    public static Component InputColumnInteger(String label, State<String> inputState, String placeholder) {
        return InputColumnInteger(label, inputState, placeholder, false);
    }

    /**
     * Input numérico de inteiro (sem casa decimal), com separador de milhar (ponto).
     * {@code disableInput} deixa somente-leitura — usado nos pesos que só podem ser capturados
     * da balança (não digitados), com a mesma borda vermelha dos campos não-editáveis.
     */
    public static Component InputColumnInteger(String label, State<String> inputState, String placeholder, boolean disableInput) {
        var inputProps = getInputPropsV2(placeholder).width(140);
        if (disableInput) inputProps.disable();
        String corBorda = disableInput ? "#e74c3c" : ThemeManager.theme().colors().border();

        var input = new Input(inputState, inputProps
                        .borderWidth(ThemeManager.theme().border().width())
                        .borderColor(corBorda)
                        .borderRadius(ThemeManager.theme().border().radiusMd()))
                .onInitialize(value -> {
                    if (value == null || value.trim().isEmpty()) {
                        return OnChangeResult.of("", "");
                    }
                    return OnChangeResult.of(formatarInteiro(value), value);
                })
                .onChange(value -> {
                    if (value == null) value = "";
                    String cleaned = value.replaceAll("[^0-9]", "");
                    if (cleaned.isEmpty()) {
                        return OnChangeResult.of("", "");
                    }
                    String intTrimmed = cleaned.replaceFirst("^0+(?!$)", "");
                    return OnChangeResult.of(formatarInteiro(intTrimmed), intTrimmed);
                })
                .lockCursorToEnd();

        return new Column()
                .c_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().small())))
                .c_child(input);
    }

    /** Igual a {@link #InputWithButtonRow}, mas com o input numérico inteiro (sem decimais). */
    public static Component InputWithButtonRowInteger(String label, String placeholder, String btnTitle, State<String> inputState, RunnableThrowing onClick) {
        return InputWithButtonRowInteger(label, placeholder, btnTitle, inputState, onClick, false);
    }

    /**
     * Input numérico inteiro + botão. {@code disableInput} deixa o campo somente-leitura —
     * o valor só muda ao clicar no botão (capturar da balança), nunca digitando.
     */
    public static Component InputWithButtonRowInteger(String label, String placeholder, String btnTitle, State<String> inputState, RunnableThrowing onClick, boolean disableInput) {
        return new Row(new RowProps().bottomVertically())
                .r_child(Components.InputColumnInteger(label, inputState, placeholder, disableInput))
                .r_child(new Button(btnTitle, new ButtonProps().height(32).textColor("black")
                                .bgColor(ThemeManager.theme().colors().primary())
                                .borderRadius(ThemeManager.theme().border().radiusSm()).borderWidth(ThemeManager.theme().border().width()).borderColor(ThemeManager.theme().colors().primary())
                        )
                                .onClick(onClick)
                );
    }

    private static String formatarInteiro(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        String intPart = value.replaceAll("[^0-9]", "");
        intPart = intPart.isEmpty() ? "0" : intPart.replaceFirst("^0+(?!$)", "");
        StringBuilder fmt = new StringBuilder();
        int len = intPart.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) fmt.append('.');
            fmt.append(intPart.charAt(i));
        }
        return fmt.toString();
    }

    private static String formatarDecimal(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        String normalizado = value.replace(",", ".");
        int dotIdx = normalizado.indexOf('.');
        String intPart = dotIdx >= 0 ? normalizado.substring(0, dotIdx) : normalizado;
        String decPart = dotIdx >= 0 ? normalizado.substring(dotIdx + 1) : "";
        intPart = intPart.replaceAll("[^0-9]", "");
        decPart = decPart.replaceAll("[^0-9]", "");
        intPart = intPart.isEmpty() ? "0" : intPart.replaceFirst("^0+(?!$)", "");
        StringBuilder fmt = new StringBuilder();
        int len = intPart.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) fmt.append('.');
            fmt.append(intPart.charAt(i));
        }
        return decPart.isEmpty() ? fmt.toString() : fmt + "," + decPart;
    }

    /**
     * Campo combinado CPF-ou-CNPJ: formata como CPF (numérico) enquanto o digitado tem 11
     * caracteres ou menos, e como CNPJ (aceita letras — formato alfanumérico mais recente) a
     * partir do 12º — ver {@code FormatterPack.formatCpfCnpj}. Usado em telas onde o mesmo campo aceita
     * tanto pessoa física quanto jurídica (Cliente, Empresa).
     */
    public static Component InputColumnCpfCnpj(String label, State<String> inputState) {
        var inputProps = getInputPropsV2("CPF ou CNPJ").width(190);

        var input = new Input(inputState, inputProps)
                .onInitialize(value -> {
                    String formatted = formatCpfCnpj(value);
                    return OnChangeResult.of(formatted, value);
                })
                .onChange(value -> {
                    String raw = value.toUpperCase().replaceAll("[^0-9A-Z]", "");

                    if (raw.length() > 14) {
                        raw = raw.substring(0, 14);
                    }

                    String formatted = formatCpfCnpj(raw);
                    return OnChangeResult.of(formatted, raw);
                })
                .lockCursorToEnd();

        return new Column()
                .c_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().small())))
                .c_child(input);
    }

    /**
     * Campo combinado RG-ou-CPF com máscara dinâmica: até 9 dígitos assume RG
     * ({@code ##.###.###-#}), a partir do 10º assume CPF ({@code ###.###.###-##}). Devolve no
     * state só os dígitos, sem os separadores — ver {@code FormatterPack.formatRgCpf}.
     */
    public static Component InputRgCpf(String label, State<String> inputState) {
        var inputProps = getInputPropsV2("RG ou CPF").width(170);

        var input = new Input(inputState, inputProps)
                .onInitialize(value -> {
                    String formatted = formatRgCpf(value);
                    return OnChangeResult.of(formatted, value);
                })
                .onChange(value -> {
                    String numeric = value.replaceAll("[^0-9]", "");

                    if (numeric.length() > 11) {
                        numeric = numeric.substring(0, 11);
                    }

                    String formatted = formatRgCpf(numeric);
                    return OnChangeResult.of(formatted, numeric);
                })
                .lockCursorToEnd();

        return new Column()
                .c_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().small())))
                .c_child(input);
    }

    public static Component InputColumnPhone(String label, State<String> inputState) {
        var inputProps = getInputPropsV2("(00) 00000-0000").width(160);

        var input = new Input(inputState, inputProps)
                .onInitialize(value -> {
                    String formatted = formatPhone(value);
                    return OnChangeResult.of(formatted, value);
                })
                .onChange(value -> {
                    String numeric = value.replaceAll("[^0-9]", "");

                    // Limita a 11 dígitos (padrão BR com DDD)
                    if (numeric.length() > 11) {
                        numeric = numeric.substring(0, 11);
                    }

                    String formatted = formatPhone(numeric);
                    return OnChangeResult.of(formatted, numeric);
                })
                .lockCursorToEnd();

        return new Column()
                .c_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().small())))
                .c_child(input);
    }

    public static Component InputColumnNumeric(String label, State<String> inputState, String placeholder) {
        var inputProps = getInputPropsV2(placeholder).width(100);

        var input = new Input(inputState, inputProps)
                .onChange(value -> {
                    String numeric = value.replaceAll("[^0-9]", "");
                    if (numeric.isEmpty()) {
                        return OnChangeResult.of("", "");
                    }
                    return OnChangeResult.of(numeric, numeric);
                })
                .lockCursorToEnd();

        return new Column()
                .c_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().small())))
                .c_child(input);
    }

    static InputProps getInputPropsV2(String placeholder) {
        return getInputPropsV2(placeholder, 31);
    }

    static InputProps getInputPropsV2(String placeholder, int height) {
        return new InputProps().height(height)
                .placeHolder(placeholder).fontSize(ThemeManager.theme().typography().small());
    }

    public static Component InputColumnAuth(String label, ReadableState<String> inputState, String placeholder) {
        var props = getInputPropsV2(placeholder);
        props.height(35);
        props.width(220);

        TextProps labelProps = new TextProps().fontSize(ThemeManager.theme().typography().small());

        return new Column(new ColumnProps().spacingOf(5))
                .children(
                        new Text(label, labelProps),
                        new Input((State<String>) inputState, props)
                );
    }


    public static Component InputColumn(String label, ReadableState<String> inputState, String placeholder, boolean disableInput,
                                        int borderWidth, int borderRadius, String borderColor, String labelColor,
                                        Integer width, Integer height) {
        var props = getInputPropsV2(placeholder);
        if (disableInput) props.disable();
        props.width(width != null ? width : 220);

        props.height(height != null ? height : 35);

        TextProps labelProps = new TextProps().fontSize(ThemeManager.theme().typography().small());
        if (labelColor != null) {
            labelProps.textColor(labelColor);
        }

        // Campo não editável (somente leitura): borda vermelha pra deixar claro que não é
        // editável, em vez da borda padrão que sugere um campo comum.
        String corBorda = disableInput ? "#e74c3c" : borderColor;

        return new Column()
                .c_child(new Text(label, labelProps))
                .c_child(new Input((State<String>) inputState,
                                props.borderWidth(borderWidth).borderColor(corBorda).borderRadius(borderRadius)
                        )
                );
    }

    public static Component InputColumn(String label, ReadableState<String> inputState, String placeholder, boolean disableInput,
                                        String labelColor, Integer width) {
        return InputColumn(label, inputState, placeholder, disableInput, ThemeManager.theme().border().width(),
                ThemeManager.theme().border().radiusMd(),
                ThemeManager.theme().colors().border(), labelColor, width, null);
    }


    public static Component InputColumn(String label, ReadableState<String> inputState, String placeholder, boolean disableInput) {
        return InputColumn(label, inputState, placeholder, disableInput, null, null);
    }

    public static Component InputColumn(String label, ReadableState<String> inputState, String placeholder) {
        return InputColumn(label, inputState, placeholder, false);
    }

    /**
     * Campo de texto com entrada forçada em MAIÚSCULAS — a função upper é aplicada no valor
     * exibido E no state (pra armazenar normalizado). Ex.: Placa do veículo.
     */
    public static Component InputColumnUppercase(String label, ReadableState<String> inputState, String placeholder) {
        State<String> estado = (State<String>) inputState;
        var props = getInputPropsV2(placeholder).width(220).height(35);

        var input = new Input(estado, props)
                .onInitialize(value -> {
                    String up = value == null ? "" : value.toUpperCase();
                    return OnChangeResult.of(up, up);
                })
                .onChange(value -> {
                    if (value == null) value = "";
                    String up = value.toUpperCase();
                    return OnChangeResult.of(up, up);
                })
                .lockCursorToEnd();

        return new Column()
                .c_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().small())))
                .c_child(input);
    }


    // Variante que cresce com o conteúdo em vez de ficar travado numa altura fixa
    // (a outra sobrecarga, com um único "height", trava prefHeight=minHeight=maxHeight).
    // Não reaproveita getInputProps(placeholder) — aquele helper seta height=31 por
    // baixo (feito pra Input de uma linha), e InputProps.applyTextAreaTheme prioriza
    // "height" sobre "maxHeight" quando os dois estão setados, então o campo ficava
    // travado em 31px em vez de crescer.
    public static Component TextAreaColumn(String label, State<String> inputState, String placeholder, int minHeight, int maxHeight) {
        TextAreaInput textAreaInput = new TextAreaInput(inputState,
                new megalodonte.props.InputProps()
                        .placeHolder(placeholder)
                        .fontSize(ThemeManager.theme().typography().small())
                        .minHeight(minHeight)
                        .maxHeight(maxHeight)
                        .width(400)
        );

        return new Column()
                .c_child(new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().small())))
                .c_child(textAreaInput);
    }

    public static Component InputWithButtonRow(String label, String placeholder, String btnTitle, State<String> inputState, RunnableThrowing onClick) {
        return new Row(new RowProps().bottomVertically())
                .r_child(Components.InputColumn(label, inputState, placeholder))
                .r_child(new Button(btnTitle, new ButtonProps().height(32).textColor("black")
                                .bgColor(ThemeManager.theme().colors().primary())
                                .borderRadius(ThemeManager.theme().border().radiusSm()).borderWidth(ThemeManager.theme().border().width()).borderColor(ThemeManager.theme().colors().primary())
                        )
                                .onClick(onClick)
                );
    }

    /** Igual a {@link #InputWithButtonRow}, mas com o input formatado em decimal (vírgula). */
    public static Component InputWithButtonRowDecimal(String label, String placeholder, String btnTitle, State<String> inputState, RunnableThrowing onClick) {
        return new Row(new RowProps().bottomVertically())
                .r_child(Components.InputColumnDecimal(label, inputState, placeholder))
                .r_child(new Button(btnTitle, new ButtonProps().height(32).textColor("black")
                                .bgColor(ThemeManager.theme().colors().primary())
                                .borderRadius(ThemeManager.theme().border().radiusSm()).borderWidth(ThemeManager.theme().border().width()).borderColor(ThemeManager.theme().colors().primary())
                        )
                                .onClick(onClick)
                );
    }

    @Deprecated
    public static Row commonCustomMenus(Runnable onClickNew, Runnable onEdit, Runnable onDelete, Runnable onClone) {
        return new Row(new RowProps().spacingOf(20))
                .r_child(MenuItem("Novo (CTRL + N)", Entypo.ADD_TO_LIST, "green", () -> executar(onClickNew::run)))
                .r_child(MenuItem("Editar", Entypo.EDIT, "blue", () -> executar(onEdit::run)))
                .r_child(MenuItem("Excluir", Entypo.TRASH, "red", () -> executar(onDelete::run)))
                .r_child(MenuItem("Clonar", Entypo.COPY, "black", () -> executar(onClone::run)))
                .r_child(new SpacerHorizontal().fill())
                //.r_child(MenuItem("Sair", Entypo.REPLY, "red", () -> router.closeSpawn("cad-produtos/"+id)));
                ;
    }

    public static Component MenuItem(String title, Ikon ikon, String color, Runnable onClick) {
        var icon = Component.CreateFromJavaFxNode(FontIcon.of(ikon, 25, Color.web(color)));

        return new Clickable(new Card(
                new Column(new ColumnProps().centerHorizontally())
                        .c_child(icon)
                        .c_child(new SpacerVertical(6))
                        .c_child(new Text(title, new TextProps().fontSize(ThemeManager.theme().typography().small())))
        ), onClick);
    }

    public static Component searchInput(State<String> stateInput, String placeholder) {
        var icon = FontIcon.of(AntDesignIconsOutlined.SEARCH, 20, Color.web(ThemeManager.theme().colors().secondary()));
        return new Input(stateInput,
                new InputProps().placeHolder(placeholder)
                        .width(300)
                        .height(31))
                .left(icon);
    }

    private static void executar(Action action) {
        try {
            action.run();
        } catch (Exception e) {
            IO.println("Error: " + e.getMessage());
        }
    }

    @FunctionalInterface
    interface Action {
        void run() throws Exception;
    }
}