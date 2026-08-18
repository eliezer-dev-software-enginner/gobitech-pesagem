package my_app.domain;

import megalodonte.ComputedState;
import megalodonte.application.ErrorReporter;
import megalodonte.base.state.State;
import megalodonte.router.v4.ScreenContext;
import megalodonte.utils.ThrowingSupplier;
import megalodonte.v2.ListState;

public abstract class ViewModelScreenContract<Model> {
    protected final ScreenContext ctx;
    protected final State<Boolean> modoEdicao = State.of(false);

    // Lista e formulário são páginas mutuamente exclusivas (ver ContratoTelaCrudV3) — começa
    // mostrando a lista; "Cadastrar"/"Editar" abre o formulário, "Voltar" fecha.
    public final State<Boolean> formIsVisible = new State<>(false);

    public final ComputedState<String> btnText = ComputedState.of(() -> modoEdicao.get() ? "Atualizar" : "+ Adicionar", modoEdicao);

    public final State<String> searchState = new State<>("");
    public final ListState<Model> allDataList = ListState.ofEmpty();
    public final ListState<Model> filteredList = ListState.ofEmpty();

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

    protected void onInit() {}
    public void onDestroy() throws Exception {
        // no-op por padrão, subclasses sobrescrevem se precisar
    }

    public abstract void populateFieldsFromModel();

    //inverso de populateFieldsFromModel(): monta um Model a partir do estado atual dos campos do formulário
    public abstract Model populateModelFromFields();

    public abstract void clearForm();
    public abstract void handleAddOrUpdate();
    public abstract void handleClickMenuDelete();

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

    /**
     * Sai do formulário e volta pra lista — usado depois de salvar com sucesso e no botão
     * "Voltar" ({@code ContratoTelaCrudV3.handleClickVoltar}).
     */
    public void voltarParaLista() {
        formIsVisible.set(false);
        clearForm();
    }
}
