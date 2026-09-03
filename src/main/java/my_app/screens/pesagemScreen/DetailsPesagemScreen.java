package my_app.screens.pesagemScreen;

import megalodonte.ComputedState;
import megalodonte.application.ErrorReporter;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Button;
import megalodonte.components.Card;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Row;
import megalodonte.props.ButtonProps;
import megalodonte.props.ColumnProps;
import megalodonte.props.RowProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;
import megalodonte.utils.ThrowingSupplier;
import my_app.db.models.PesagemModel;
import my_app.db.services.PesagemService;
import my_app.domain.components.Components;
import pack.utilities.DatePack;

import java.util.function.Function;

public class DetailsPesagemScreen implements ScreenComponent {
    private final ScreenContext ctx;
    private final PesagemService pesagemService;
    private final PesagemHistoricoViewModel vm;
    private final State<PesagemModel> model = State.of(null);

    private final ComputedState<String> id = campo(PesagemModel::getId);
    private final ComputedState<String> placa = campo(PesagemModel::getPlaca);
    private final ComputedState<String> motorista = campo(PesagemModel::getMotoristaNome);
    private final ComputedState<String> motoristaDocumento = campo(PesagemModel::getMotoristaDocumento);
    private final ComputedState<String> tipo = campo(PesagemModel::getTipoPesagem);
    private final ComputedState<String> cliente = campo(p -> p.getCliente() != null ? p.getCliente().getLoja() : "-");
    private final ComputedState<String> produto = campo(p -> p.getProduto() != null ? p.getProduto().getNome() : "-");
    private final ComputedState<String> notaFiscal = campo(PesagemModel::getNotaFiscal);
    private final ComputedState<String> tara = peso(PesagemModel::getPesoVeiculo);
    private final ComputedState<String> pesoBruto = peso(PesagemModel::getPesoTotal);
    private final ComputedState<String> pesoLiquido = peso(PesagemModel::getPesoFinal);
    private final ComputedState<String> dataCriacao = campo(p -> DatePack.localDateTimeToBrazilianDateTime(p.getDataCriacao()));
    private final ComputedState<String> observacoes = campo(PesagemModel::getObservacoes);

    public DetailsPesagemScreen(ScreenContext ctx) {
        this.ctx = ctx;
        long id = Long.parseLong(ctx.getParams().get("id"));
        this.pesagemService = createOrReport(PesagemService::new);
        this.vm = new PesagemHistoricoViewModel(ctx);

        Async.Run(() -> {
            try {
                var m = pesagemService.buscarComRelacoes(id);
                UI.runOnUi(() -> model.set(m));
            } catch (Exception e) {
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao buscar pesagem: " + e.getMessage()));
            }
        });
    }

    @Override
    public Component render() {
        return Components.ScrollPaneDefault(
                new Card(
                        new Column(new ColumnProps().paddingAll(20))
                                .c_child(new Text("Detalhes da pesagem", new TextProps().fontSize(ThemeManager.theme().typography().subtitle())))
                                .c_child(new SpacerVertical(20))
                                .c_child(Components.TextWithDetailsState("ID: ", id))
                                .c_child(new Row(new RowProps().bottomVertically().spacingOf(10))
                                        .r_child(Components.TextWithDetailsState("Placa: ", placa))
                                        .r_child(copiarPlacaButton()))
                                .c_child(Components.TextWithDetailsState("Motorista: ", motorista))
                                .c_child(Components.TextWithDetailsState("Documento do motorista: ", motoristaDocumento))
                                .c_child(Components.TextWithDetailsState("Tipo: ", tipo))
                                .c_child(Components.TextWithDetailsState("Cliente: ", cliente))
                                .c_child(Components.TextWithDetailsState("Produto: ", produto))
                                .c_child(Components.TextWithDetailsState("Nota fiscal: ", notaFiscal))
                                .c_child(Components.TextWithDetailsState("Tara: ", tara))
                                .c_child(Components.TextWithDetailsState("Peso bruto: ", pesoBruto))
                                .c_child(Components.TextWithDetailsState("Peso líquido: ", pesoLiquido))
                                .c_child(Components.TextWithDetailsState("Data de criação: ", dataCriacao))
                                .c_child(Components.TextWithDetailsState("Observações: ", observacoes, true))
                                .c_child(new SpacerVertical(20))
                                .c_child(acoesRow())
                )
        );
    }

    private Button copiarPlacaButton() {
        return new Button("Copiar placa", new ButtonProps().height(32).textColor("black")
                .bgColor(ThemeManager.theme().colors().primary())
                .borderRadius(ThemeManager.theme().border().radiusSm())
                .borderWidth(ThemeManager.theme().border().width())
                .borderColor(ThemeManager.theme().colors().primary()))
                .onClick(() -> copiarPlaca());
    }

    private Row acoesRow() {
        return new Row(new RowProps().fillWidth().spacingOf(10))
                .children(
                        new Button("Baixar ticket", new ButtonProps().bgColor("#16a34a").textColor("white"))
                                .onClick(() -> {
                                    var m = model.get();
                                    if (m != null) vm.imprimirTicket(m);
                                }),
                        new Button("Imprimir nota térmica 80mm", new ButtonProps().bgColor("#16a34a").textColor("white"))
                                .onClick(() -> {
                                    var m = model.get();
                                    if (m != null) vm.imprimirTicketTermica(m);
                                }),
                        new Button("Excluir", new ButtonProps().bgColor("#ef4444").textColor("white"))
                                .onClick(() -> {
                                    var m = model.get();
                                    if (m == null) return;
                                    vm.selected.set(m);
                                    ctx.selfStage().close();
                                    vm.handleClickMenuDelete();
                                })
                );
    }

    private ComputedState<String> campo(Function<PesagemModel, Object> extract) {
        return ComputedState.of(() -> {
            var m = model.get();
            if (m == null) return "";
            var value = extract.apply(m);
            return value == null ? "" : value.toString();
        }, model);
    }

    private ComputedState<String> peso(Function<PesagemModel, Object> extract) {
        return ComputedState.of(() -> {
            var m = model.get();
            if (m == null) return "";
            var value = extract.apply(m);
            if (!(value instanceof Number n)) return "";
            return Math.round(n.doubleValue()) + " Kg";
        }, model);
    }

    private void copiarPlaca() {
        var m = model.get();
        if (m == null) return;
        var content = new javafx.scene.input.ClipboardContent();
        content.putString(m.getPlaca() == null ? "" : m.getPlaca());
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        Components.ShowPopup(ctx, "Placa copiada: " + m.getPlaca());
    }

    protected <T> T createOrReport(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            ErrorReporter.handle(e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onDestroy() {
        try {
            pesagemService.close();
            vm.onDestroy();
        } catch (Exception e) {
            ErrorReporter.handle(e);
        }
    }
}
