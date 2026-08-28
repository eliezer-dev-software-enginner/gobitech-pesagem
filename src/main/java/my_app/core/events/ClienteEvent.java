package my_app.core.events;

import my_app.db.models.ClienteModel;

/** Evento de Cliente — cadastrado, alterado ou excluído. */
public class ClienteEvent extends EntityEvent<ClienteModel> {

    private ClienteEvent() {}

    public static ClienteEvent criado() {
        return new ClienteEvent();
    }

    public static ClienteEvent editado() {
        return new ClienteEvent();
    }

    public static ClienteEvent excluido() {
        return new ClienteEvent();
    }
}
