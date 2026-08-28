package my_app.core.events;

import my_app.db.models.ProdutoModel;

/** Evento de Produto — cadastrado, alterado ou excluído. */
public class ProdutoEvent extends EntityEvent<ProdutoModel> {

    public ProdutoEvent(ProdutoModel entity, EventType type) {
        super(entity, type);
    }

    public static ProdutoEvent criado(ProdutoModel entity) {
        return new ProdutoEvent(entity, EventType.CRIADO);
    }

    public static ProdutoEvent editado(ProdutoModel entity) {
        return new ProdutoEvent(entity, EventType.EDITADO);
    }

    public static ProdutoEvent excluido() {
        return new ProdutoEvent(null, EventType.EXCLUIDO);
    }
}
