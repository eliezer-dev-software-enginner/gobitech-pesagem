package my_app.core.events;

import my_app.db.models.ClienteModel;

/** Evento de Cliente — cadastrado, alterado ou excluído. */
public class ClienteEvent extends EntityEvent<ClienteModel> {

    public ClienteEvent(ClienteModel entity, EventType type, long entityId) {
        super(entity, type, entityId);
    }

    public static ClienteEvent criado(ClienteModel entity) {
        return new ClienteEvent(entity, EventType.CRIADO, 0);
    }

    public static ClienteEvent editado(ClienteModel entity) {
        return new ClienteEvent(entity, EventType.EDITADO, 0);
    }

    public static ClienteEvent excluido(long id) {
        return new ClienteEvent(null, EventType.EXCLUIDO, id);
    }
}
