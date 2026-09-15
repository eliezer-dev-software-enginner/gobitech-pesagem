package my_app.db.repositories;

import my_app.db.models.PreferenciasModel;
import net.sf.persism.Session;
import java.sql.SQLException;
import static net.sf.persism.SQL.sql;

public class PreferenciasRepository extends BaseRepository<PreferenciasModel> {

    public PreferenciasRepository(Session session) {
        super(session);
    }

    @Override
    protected Class<PreferenciasModel> modelClass() {
        return PreferenciasModel.class;
    }

    public PreferenciasModel buscarUnico() throws SQLException {
        return session().fetch(modelClass(), sql("SELECT * FROM preferencias ORDER BY id LIMIT 1"));
    }
}
