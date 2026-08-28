package my_app.core.events;

import my_app.db.models.PesagemModel;

/** Evento de Pesagem — cadastrada, alterada ou excluída. */
public class PesagemEvent extends EntityEvent<PesagemModel> {

    private PesagemEvent() {}

    public static PesagemEvent criado() {
        return new PesagemEvent();
    }

    public static PesagemEvent editado() {
        return new PesagemEvent();
    }

    public static PesagemEvent excluido() {
        return new PesagemEvent();
    }
}
