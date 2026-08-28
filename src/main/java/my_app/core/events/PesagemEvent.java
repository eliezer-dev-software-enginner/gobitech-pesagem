package my_app.core.events;

import my_app.db.models.PesagemModel;

/** Evento de Pesagem — cadastrada, alterada ou excluída. */
public class PesagemEvent extends EntityEvent<PesagemModel> {

    public PesagemEvent(PesagemModel entity, EventType type, long entityId) {
        super(entity, type, entityId);
    }

    public static PesagemEvent criado(PesagemModel entity) {
        return new PesagemEvent(entity, EventType.CRIADO, 0);
    }

    public static PesagemEvent editado(PesagemModel entity) {
        return new PesagemEvent(entity, EventType.EDITADO, 0);
    }

    public static PesagemEvent excluido(long id) {
        return new PesagemEvent(null, EventType.EXCLUIDO, id);
    }
}
