package my_app.screens.produtoScreen;

import megalodonte.ComputedState;
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
import my_app.db.models.ProdutoModel;
import my_app.db.services.ProdutoService;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pack.utilities.DatePack;

import java.util.function.Function;

public class DetailsProdutoScreen implements ScreenComponent {
    private static final UiDefinition VIEW = UiDefinition.fromResource(DetailsProdutoScreen.class, "/views/produto-details.view.jsonc");
    private final UiSession session;
    private static final Logger log = LoggerFactory.getLogger(DetailsProdutoScreen.class);
    private final ProdutoService produtoService;
    private final State<ProdutoModel> model = State.of(null);

    private final ComputedState<String> id = campo(ProdutoModel::getId);
    private final ComputedState<String> nome = campo(ProdutoModel::getNome);
    private final ComputedState<String> unidade = campo(ProdutoModel::getUnidade);
    private final ComputedState<String> dataCriacao = campo(p -> DatePack.localDateTimeToBrazilianDateTime(p.getDataCriacao()));
    private final ComputedState<String> observacoes = campo(ProdutoModel::getObservacoes);

    public DetailsProdutoScreen(ScreenContextInterface ctx) {
        long id;
        try {
            id = Long.parseLong(ctx.getParams().get("id"));
        } catch (RuntimeException e) {
            log.error("Parâmetro 'id' inválido na rota de detalhes", e);
            UI.runOnUi(() -> Components.ShowAlertError("ID inválido na rota de detalhes."));
            ctx.selfStage().close();
            throw new IllegalStateException(e);
        }
        this.produtoService = createOrReport(ProdutoService::new);
        session = VIEW.open(new Actions());
        session.display("id", this.id, String.class)
                .display("nome", nome, String.class)
                .display("unidade", unidade, String.class)
                .display("dataCriacao", dataCriacao, String.class)
                .display("observacoes", observacoes, String.class);

        Async.Run(() -> {
            try {
                var m = produtoService.buscarById(id);
                UI.runOnUi(() -> model.set(m));
            } catch (Exception e) {
                log.error("Erro ao buscar produto id={}", id, e);
                UI.runOnUi(() -> Components.ShowAlertError("Não foi possível carregar os dados do produto."));
            }
        });
    }

    @Override
    public Component render() { return session.render(); }

    private ComputedState<String> campo(Function<ProdutoModel, Object> extract) {
        return ComputedState.of(() -> {
            var m = model.get();
            if (m == null) return "";
            var value = extract.apply(m);
            return value == null ? "" : value.toString();
        }, model);
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
            produtoService.close();
        } catch (Exception e) {
            ErrorReporter.handle(e);
        }
    }
}
