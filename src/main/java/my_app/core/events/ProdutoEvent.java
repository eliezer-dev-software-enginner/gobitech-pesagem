package my_app.core.events;

import my_app.db.models.ProdutoModel;

/** Evento de Produto — cadastrado, alterado ou excluído. */
public class ProdutoEvent extends EntityEvent<ProdutoModel> {

    private ProdutoEvent() {}

    public static ProdutoEvent criado() {
        return new ProdutoEvent();
    }

    public static ProdutoEvent editado() {
        return new ProdutoEvent();
    }

    public static ProdutoEvent excluido() {
        return new ProdutoEvent();
    }
}
