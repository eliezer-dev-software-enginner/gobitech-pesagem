package my_app.core.events;

import my_app.db.models.UsuarioModel;

/** Evento de Usuário — cadastrado, alterado ou excluído. */
public class UsuarioEvent extends EntityEvent<UsuarioModel> {

    private UsuarioEvent() {}

    public static UsuarioEvent criado() {
        return new UsuarioEvent();
    }

    public static UsuarioEvent editado() {
        return new UsuarioEvent();
    }

    public static UsuarioEvent excluido() {
        return new UsuarioEvent();
    }
}
