package my_app.screens.produtoScreen;

import megalodonte.base.state.State;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.router.v4.ScreenContext;
import my_app.core.AppRoutes;
import my_app.db.models.ProdutoModel;
import my_app.db.services.ProdutoService;
import my_app.core.events.ProdutoEvent;
import my_app.core.events.EventBus;
import my_app.domain.Data;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.function.Consumer;

public class ProdutoScreenViewModel extends ViewModelScreenContract<ProdutoModel> {
    private static final Logger log = LoggerFactory.getLogger(ProdutoScreenViewModel.class);

    private final ProdutoService produtoService;
    private final Consumer<Object> eventListener = this::onEntityEvent;

    final State<String> nome = new State<>("");
    final State<String> unidadeSelected = new State<>(Data.unidadesDeMedidaList.getFirst());
    final State<String> observacoes = new State<>("");

    // Desconto padrão do produto — carregado no campo "Outros" da Pesagem ao selecioná-lo.
    final State<String> desconto = new State<>("");

    public ProdutoScreenViewModel(ScreenContext ctx) {
        super(ctx);
        this.produtoService = createOrReport(ProdutoService::new);
        screenNameSpawn = AppRoutes.Screens.ADD_OR_EDIT_PRODUTO.name();
        EventBus.getInstance().subscribe(eventListener);
    }

    // Mantém a lista sincronizada quando um produto é salvo/alterado — desinscrito no
    // onDestroy, senão uma ViewModel já destruída (com o service fechado) continuaria
    // processando eventos e quebraria com session nula.
    private void onEntityEvent(Object event) {
        if (event instanceof ProdutoEvent) {
            fetchListData();
        }
    }

    @Override
    protected boolean matchesSearch(ProdutoModel model, String query) {
        return model.getNome() != null && model.getNome().toLowerCase().contains(query);
    }

    @Override
    public void populateFieldsFromModel() {
        final var data = selected.get();
        if (data == null) return;
        nome.set(data.getNome());
        unidadeSelected.set(data.getUnidade() == null ? Data.unidadesDeMedidaList.getFirst() : data.getUnidade());
        observacoes.set(data.getObservacoes() == null ? "" : data.getObservacoes());
        desconto.set(data.getDesconto() == null ? "" : data.getDesconto().toPlainString());
    }

    @Override
    public ProdutoModel populateModelFromFields() {
        var model = modoEdicao.get() && selected.get() != null
                ? selected.get()
                : new ProdutoModel();

        model.setNome(nome.get().trim());
        model.setUnidade(unidadeSelected.get());
        model.setObservacoes(observacoes.get());
        model.setDesconto(parseDecimal(desconto.get()));

        return model;
    }

    private BigDecimal parseDecimal(String valor) {
        try {
            return valor == null || valor.isBlank() ? BigDecimal.ZERO : new BigDecimal(valor.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public void fetchListData() {
        Async.Run(() -> {
            try {
                var list = produtoService.listar();
                UI.runOnUi(() -> allDataList.set(list));
            } catch (Exception e) {
                log.error("Erro ao buscar produtos", e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao buscar produtos: " + e.getMessage()));
            }
        });
    }

    @Override
    public void handleClickMenuDelete() {
        final var model = selected.get();
        if (model == null) return;

        Components.ShowAlertAdvice("Deseja excluir o produto " + model.getNome(), () -> Async.Run(() -> {
            try {
                produtoService.excluirById(model.getId());
                UI.runOnUi(() -> {
                    allDataList.removeIf(it -> it.getId().equals(model.getId()));
                    Components.ShowPopup(ctx, "Produto excluído com sucesso");
                    EventBus.getInstance().publish(ProdutoEvent.excluido());
                });
            } catch (Exception e) {
                log.error("Erro ao excluir produto id={}", model.getId(), e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao tentar excluir: " + e.getMessage()));
            }
        }));
    }

    @Override
    public void handleAddOrUpdate() {
        if (modoEdicao.get() && selected.get() == null) return;
        if (!tryBeginSalvar()) return;

        boolean editando = modoEdicao.get();
        var model = populateModelFromFields();

        Async.Run(() -> {
            try {
                if (editando) {
                    produtoService.atualizar(model);
                    UI.runOnUi(() -> {
                        allDataList.updateIf(it -> it.getId().equals(model.getId()), it -> model);
                        Components.ShowPopup(ctx, "Produto atualizado com sucesso");
                        EventBus.getInstance().publish(ProdutoEvent.editado());
                    });
                } else {
                    produtoService.salvar(model);
                    UI.runOnUi(() -> {
                        allDataList.add(model);
                        Components.ShowPopup(ctx, "Produto cadastrado com sucesso");
                        EventBus.getInstance().publish(ProdutoEvent.criado());
                        clearForm();
                    });
                }
            } catch (IllegalArgumentException e) {
                UI.runOnUi(() -> Components.ShowAlertError(e.getMessage()));
            } catch (Exception e) {
                log.error("Erro inesperado ao salvar produto", e);
                UI.runOnUi(() -> Components.ShowAlertError("Erro inesperado: " + e.getMessage()));
            } finally {
                endSalvar();
            }
        });
    }


    @Override
    public void clearForm() {
        nome.set("");
        unidadeSelected.set(Data.unidadesDeMedidaList.getFirst());
        observacoes.set("");
        desconto.set("");
    }

    @Override
    public void onDestroy() throws Exception {
        EventBus.getInstance().unsubscribe(eventListener);
        this.produtoService.close();
    }
}
