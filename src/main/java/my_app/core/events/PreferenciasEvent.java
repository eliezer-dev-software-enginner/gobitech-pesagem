package my_app.core.events;

import my_app.db.models.PreferenciasModel;

/** Evento de Preferências — configurações salvas (ex.: logomarca horizontal). */
public class PreferenciasEvent extends EntityEvent<PreferenciasModel> {

    private PreferenciasEvent() {}

    public static PreferenciasEvent salvas() {
        return new PreferenciasEvent();
    }
}