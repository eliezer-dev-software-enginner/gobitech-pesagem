package my_app.core.events;

import my_app.db.models.UsuarioModel;

/** Evento de Usuário — cadastrado, alterado ou inativado. */
public class UsuarioEvent extends EntityEvent<UsuarioModel> {

    public UsuarioEvent(UsuarioModel entity, EventType type, long entityId) {
        super(entity, type, entityId);
    }

    public static UsuarioEvent criado(UsuarioModel entity) {
        return new UsuarioEvent(entity, EventType.CRIADO, 0);
    }

    public static UsuarioEvent editado(UsuarioModel entity) {
        return new UsuarioEvent(entity, EventType.EDITADO, 0);
    }

    public static UsuarioEvent excluido(long id) {
        return new UsuarioEvent(null, EventType.EXCLUIDO, id);
    }
}
