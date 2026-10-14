package my_app.screens.pesagemScreen;

import megalodonte.ComputedState;
import megalodonte.components.Clipboard;
import megalodonte.jsonc.Actions;
import megalodonte.jsonc.UiDefinition;
import megalodonte.jsonc.UiSession;
import megalodonte.application.ErrorReporter;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.base.route.v2.ScreenContextInterface;
import megalodonte.utils.ThrowingSupplier;
import my_app.db.models.PesagemModel;
import my_app.db.services.PesagemService;
import my_app.domain.components.Components;
import my_app.domain.Autorizacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pack.utilities.DatePack;

import java.util.function.Function;

public class DetailsPesagemScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(DetailsPesagemScreen.class, "/views/pesagem-details.view.jsonc");
    private final UiSession session;
    private static final Logger log = LoggerFactory.getLogger(DetailsPesagemScreen.class);
    private final ScreenContextInterface ctx;
    private final PesagemService pesagemService;
    private final PesagemHistoricoViewModel vm;
    private final State<PesagemModel> model = State.of(null);

    private final ComputedState<String> id = campo(PesagemModel::getId);
    private final ComputedState<String> placa = campo(PesagemModel::getPlaca);
    private final ComputedState<String> motorista = campo(PesagemModel::getMotoristaNome);
    private final ComputedState<String> motoristaDocumento = campo(PesagemModel::getMotoristaDocumento);
    private final ComputedState<String> tipo = campo(PesagemModel::getTipoPesagem);
    private final ComputedState<String> cliente = campo(p -> p.getCliente() != null ? p.getCliente().getRazaoSocial() : "-");
    private final ComputedState<String> produto = campo(p -> p.getProduto() != null ? p.getProduto().getNome() : "-");
    private final ComputedState<String> notaFiscal = campo(PesagemModel::getNotaFiscal);
    private final ComputedState<String> tara = peso(PesagemModel::getPesoVeiculo);
    private final ComputedState<String> pesoBruto = peso(PesagemModel::getPesoTotal);
    private final ComputedState<String> pesoLiquido = peso(PesagemModel::getPesoFinal);
    private final ComputedState<String> dataCriacao = campo(p -> DatePack.localDateTimeToBrazilianDateTime(p.getDataCriacao()));
    private final ComputedState<String> observacoes = campo(PesagemModel::getObservacoes);

    public DetailsPesagemScreen(ScreenContextInterface ctx) {
        this.ctx = ctx;
        long id;
        try {
            id = Long.parseLong(ctx.getParams().get("id"));
        } catch (RuntimeException e) {
            log.error("Parâmetro 'id' inválido na rota de detalhes", e);
            UI.runOnUi(() -> Components.ShowAlertError("ID inválido na rota de detalhes."));
            ctx.selfStage().close();
            throw new IllegalStateException(e);
        }
        this.pesagemService = createOrReport(PesagemService::new);
        this.vm = new PesagemHistoricoViewModel(ctx);
        session = VIEW.open(new Actions()
                .register("copyPlate", ignored -> copiarPlaca())
                .register("print", ignored -> {
                    var current = model.get();
                    if (current != null) vm.imprimirTicket(current);
                })
                .register("delete", ignored -> {
                    var current = model.get();
                    if (current == null) return;
                    vm.selected.set(current);
                    ctx.selfStage().close();
                    vm.handleClickMenuDelete();
                }));
        session.display("id", this.id, String.class)
                .display("placa", placa, String.class)
                .display("motorista", motorista, String.class)
                .display("motoristaDocumento", motoristaDocumento, String.class)
                .display("tipo", tipo, String.class)
                .display("cliente", cliente, String.class)
                .display("produto", produto, String.class)
                .display("notaFiscal", notaFiscal, String.class)
                .display("tara", tara, String.class)
                .display("pesoBruto", pesoBruto, String.class)
                .display("pesoLiquido", pesoLiquido, String.class)
                .display("dataCriacao", dataCriacao, String.class)
                .display("observacoes", observacoes, String.class);
        session.states().state("showDelete", Boolean.class).set(Autorizacao.podeAdministrar());

        Async.Run(() -> {
            try {
                var m = pesagemService.buscarComRelacoes(id);
                UI.runOnUi(() -> model.set(m));
            } catch (Exception e) {
                log.error("Erro ao buscar pesagem id={}", id, e);
                UI.runOnUi(() -> Components.ShowAlertError("Não foi possível carregar os dados da pesagem."));
            }
        });
    }

    @Override
    public Component render() { return session.render(); }

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
        Clipboard.copyText(m.getPlaca());
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
        session.close();
        try {
            pesagemService.close();
            vm.onDestroy();
        } catch (Exception e) {
            ErrorReporter.handle(e);
        }
    }
}
