package my_app.core.events;

import my_app.db.models.PesagemModel;

/** Evento de Pesagem — cadastrada, alterada ou excluída. */
public class PesagemEvent extends EntityEvent<PesagemModel> {

    public PesagemEvent(PesagemModel entity, EventType type) {
        super(entity, type);
    }

    public static PesagemEvent criado(PesagemModel entity) {
        return new PesagemEvent(entity, EventType.CRIADO);
    }

    public static PesagemEvent editado(PesagemModel entity) {
        return new PesagemEvent(entity, EventType.EDITADO);
    }

    public static PesagemEvent excluido() {
        return new PesagemEvent(null, EventType.EXCLUIDO);
    }
}
