package my_app.screens.produtoScreen;

import megalodonte.base.state.State;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.router.v4.ScreenContext;
import my_app.db.models.ProdutoModel;
import my_app.db.services.ProdutoService;
import my_app.core.events.EntityEvent;
import my_app.core.events.EventBus;
import my_app.domain.Data;
import my_app.domain.ViewModelScreenContract;
import my_app.domain.components.Components;

public class ProdutoScreenViewModel extends ViewModelScreenContract<ProdutoModel> {
    private final ProdutoService produtoService;

    final State<ProdutoModel> produtoSelecionado = State.of(null);

    final State<String> nome = new State<>("");
    final State<String> unidadeSelected = new State<>(Data.unidadesDeMedidaList.getFirst());
    final State<String> observacoes = new State<>("");

    public ProdutoScreenViewModel(ScreenContext ctx) {
        super(ctx);
        this.produtoService = createOrReport(ProdutoService::new);
    }

    @Override
    protected boolean matchesSearch(ProdutoModel model, String query) {
        return model.getNome() != null && model.getNome().toLowerCase().contains(query);
    }

    @Override
    public void populateFieldsFromModel() {
        final var data = produtoSelecionado.get();
        if (data == null) return;
        nome.set(data.getNome());
        unidadeSelected.set(data.getUnidade() == null ? Data.unidadesDeMedidaList.getFirst() : data.getUnidade());
        observacoes.set(data.getObservacoes() == null ? "" : data.getObservacoes());
    }

    @Override
    public ProdutoModel populateModelFromFields() {
        var model = modoEdicao.get() && produtoSelecionado.get() != null
                ? produtoSelecionado.get()
                : new ProdutoModel();

        model.setNome(nome.get().trim());
        model.setUnidade(unidadeSelected.get());
        model.setObservacoes(observacoes.get());

        return model;
    }

    @Override
    public void fetchListData() {
        Async.Run(() -> {
            try {
                var list = produtoService.listar();
                UI.runOnUi(() -> allDataList.set(list));
            } catch (Exception e) {
                e.printStackTrace();
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao buscar produtos: " + e.getMessage()));
            }
        });
    }

    @Override
    public void handleClickMenuDelete() {
        final var model = produtoSelecionado.get();
        if (model == null) return;

        Components.ShowAlertAdvice("Deseja excluir o produto " + model.getNome(), () -> Async.Run(() -> {
            try {
                produtoService.excluirById(model.getId());
                UI.runOnUi(() -> {
                    allDataList.removeIf(it -> it.getId().equals(model.getId()));
                    Components.ShowPopup(ctx, "Produto excluído com sucesso");
                    EventBus.getInstance().publish(EntityEvent.excluido(model.getId()));
                });
            } catch (Exception e) {
                UI.runOnUi(() -> Components.ShowAlertError("Erro ao tentar excluir: " + e.getMessage()));
            }
        }));
    }

    @Override
    public void handleAddOrUpdate() {
        if (modoEdicao.get() && produtoSelecionado.get() == null) return;

        boolean editando = modoEdicao.get();
        var model = populateModelFromFields();

        Async.Run(() -> {
            try {
                if (editando) {
                    produtoService.atualizar(model);
                    UI.runOnUi(() -> {
                        allDataList.updateIf(it -> it.getId().equals(model.getId()), it -> model);
                        Components.ShowPopup(ctx, "Produto atualizado com sucesso");
                        clearForm();
                        EventBus.getInstance().publish(EntityEvent.editado(model));
                    });
                } else {
                    produtoService.salvar(model);
                    UI.runOnUi(() -> {
                        allDataList.add(model);
                        Components.ShowPopup(ctx, "Produto cadastrado com sucesso");
                        clearForm();
                        EventBus.getInstance().publish(EntityEvent.criado(model));
                    });
                }
            } catch (IllegalArgumentException e) {
                UI.runOnUi(() -> Components.ShowAlertError(e.getMessage()));
            } catch (Exception e) {
                UI.runOnUi(() -> Components.ShowAlertError("Erro inesperado: " + e.getMessage()));
            }
        });
    }

    @Override
    public void clearForm() {
        nome.set("");
        unidadeSelected.set(Data.unidadesDeMedidaList.getFirst());
        observacoes.set("");
    }

    @Override
    public void onDestroy() throws Exception {
        this.produtoService.close();
    }
}
