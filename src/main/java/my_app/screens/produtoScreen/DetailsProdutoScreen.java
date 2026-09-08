package my_app.screens.produtoScreen;

import megalodonte.ComputedState;
import megalodonte.application.ErrorReporter;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Card;
import megalodonte.components.SpacerVertical;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.props.ColumnProps;
import megalodonte.props.TextProps;
import megalodonte.router.v4.ScreenContext;
import megalodonte.utils.ThrowingSupplier;
import my_app.db.models.ProdutoModel;
import my_app.db.services.ProdutoService;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pack.utilities.DatePack;

import java.util.function.Function;

public class DetailsProdutoScreen implements ScreenComponent {
    private static final Logger log = LoggerFactory.getLogger(DetailsProdutoScreen.class);
    private final ProdutoService produtoService;
    private final State<ProdutoModel> model = State.of(null);

    private final ComputedState<String> id = campo(ProdutoModel::getId);
    private final ComputedState<String> nome = campo(ProdutoModel::getNome);
    private final ComputedState<String> unidade = campo(ProdutoModel::getUnidade);
    private final ComputedState<String> desconto = campo(p -> p.getDesconto() == null ? "0" : p.getDesconto().toPlainString());
    private final ComputedState<String> dataCriacao = campo(p -> DatePack.localDateTimeToBrazilianDateTime(p.getDataCriacao()));
    private final ComputedState<String> observacoes = campo(ProdutoModel::getObservacoes);

    public DetailsProdutoScreen(ScreenContext ctx) {
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
    public Component render() {
        return Components.ScrollPaneDefault(
                new Card(
                        new Column(new ColumnProps().paddingAll(20))
                                .c_child(new Text("Detalhes do produto", new TextProps().fontSize(ThemeManager.theme().typography().subtitle())))
                                .c_child(new SpacerVertical(20))
                                .c_child(Components.TextWithDetailsState("ID: ", id))
                                .c_child(Components.TextWithDetailsState("Nome: ", nome))
                                .c_child(Components.TextWithDetailsState("Unidade: ", unidade))
                                .c_child(Components.TextWithDetailsState("Desconto padrão (%): ", desconto))
                                .c_child(Components.TextWithDetailsState("Data de criação: ", dataCriacao))
                                .c_child(Components.TextWithDetailsState("Observações: ", observacoes, true))
                )
        );
    }

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
        try {
            produtoService.close();
        } catch (Exception e) {
            ErrorReporter.handle(e);
        }
    }
}
