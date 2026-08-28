package my_app.core.events;

import my_app.db.models.ProdutoModel;

/** Evento de Produto — cadastrado, alterado ou excluído. */
public class ProdutoEvent extends EntityEvent<ProdutoModel> {

    public ProdutoEvent(ProdutoModel entity, EventType type, long entityId) {
        super(entity, type, entityId);
    }

    public static ProdutoEvent criado(ProdutoModel entity) {
        return new ProdutoEvent(entity, EventType.CRIADO, 0);
    }

    public static ProdutoEvent editado(ProdutoModel entity) {
        return new ProdutoEvent(entity, EventType.EDITADO, 0);
    }

    public static ProdutoEvent excluido(long id) {
        return new ProdutoEvent(null, EventType.EXCLUIDO, id);
    }
}
