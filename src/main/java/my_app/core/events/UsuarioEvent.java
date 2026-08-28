package my_app.core.events;

import my_app.db.models.UsuarioModel;

/** Evento de Usuário — cadastrado, alterado ou inativado. */
public class UsuarioEvent extends EntityEvent<UsuarioModel> {

    public UsuarioEvent(UsuarioModel entity, EventType type) {
        super(entity, type);
    }

    public static UsuarioEvent criado(UsuarioModel entity) {
        return new UsuarioEvent(entity, EventType.CRIADO);
    }

    public static UsuarioEvent editado(UsuarioModel entity) {
        return new UsuarioEvent(entity, EventType.EDITADO);
    }

    public static UsuarioEvent excluido() {
        return new UsuarioEvent(null, EventType.EXCLUIDO);
    }
}
