package my_app.domain;

import megalodonte.ComputedState;
import megalodonte.application.ErrorReporter;
import megalodonte.base.state.State;
import megalodonte.router.v4.ScreenContext;
import megalodonte.utils.ThrowingSupplier;
import megalodonte.v2.ListState;
import my_app.core.Identifier;

import java.util.concurrent.atomic.AtomicBoolean;


public abstract class ViewModelScreenContract<Model extends Identifier> {
    protected final ScreenContext ctx;
    protected final State<Boolean> modoEdicao = State.of(false);
    private final AtomicBoolean salvando = new AtomicBoolean(false);

    public String screenNameSpawn = "";

    public final ComputedState<String> btnText = ComputedState.of(() -> modoEdicao.get() ? "Atualizar" : "+ Adicionar", modoEdicao);

    public final State<String> searchState = new State<>("");
    public final ListState<Model> allDataList = ListState.ofEmpty();
    public final ListState<Model> filteredList = ListState.ofEmpty();

    public final State<Model> selected = State.of(null);

    public ViewModelScreenContract(ScreenContext ctx) {
        this.ctx = ctx;
        searchState.subscribe(_ -> applyFilter());
        allDataList.subscribe(_ -> applyFilter());
    }

    private void applyFilter() {
        var query = searchState.get();
        if (query == null || query.isBlank()) {
            filteredList.set(allDataList.get());
            return;
        }
        filteredList.set(allDataList.get().stream()
                .filter(it -> matchesSearch(it, query.trim().toLowerCase()))
                .toList());
    }

    protected abstract boolean matchesSearch(Model model, String query);

    public void onDestroy() throws Exception {
        // no-op por padrão, subclasses sobrescrevem se precisar
    }

    public abstract void populateFieldsFromModel();

    //inverso de populateFieldsFromModel(): monta um Model a partir do estado atual dos campos do formulário
    public abstract Model populateModelFromFields();

    public abstract void clearForm();
    public abstract void handleAddOrUpdate();
    public abstract void handleClickMenuDelete();

    // Anti duplo-clique no botão Salvar/Adicionar: a gravação roda em Async.Run e o botão
    // continua clicável até o callback chegar; dois cliques rápidos gravariam 2 registros.
    // A trava é liberada só quando o Async.Run termina (success/erro).
    protected boolean tryBeginSalvar() {
        return salvando.compareAndSet(false, true);
    }

    protected void endSalvar() {
        salvando.set(false);
    }

    //deve popular allDataList e filteredList
    //filteredList é o que vai preencher a tabela
    public abstract void fetchListData();

    public State<Boolean> modoEdicaoState(){
        return modoEdicao;
    }

    protected <T> T createOrReport(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            ErrorReporter.handle(e);
            throw new IllegalStateException(e); // interrompe a construção da tela de forma previsível
        }
    }
}
