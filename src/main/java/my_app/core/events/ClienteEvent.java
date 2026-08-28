package my_app.core.events;

import my_app.db.models.ClienteModel;

/** Evento de Cliente — cadastrado, alterado ou excluído. */
public class ClienteEvent extends EntityEvent<ClienteModel> {

    public ClienteEvent(ClienteModel entity, EventType type) {
        super(entity, type);
    }

    public static ClienteEvent criado(ClienteModel entity) {
        return new ClienteEvent(entity, EventType.CRIADO);
    }

    public static ClienteEvent editado(ClienteModel entity) {
        return new ClienteEvent(entity, EventType.EDITADO);
    }

    public static ClienteEvent excluido() {
        return new ClienteEvent(null, EventType.EXCLUIDO);
    }
}
