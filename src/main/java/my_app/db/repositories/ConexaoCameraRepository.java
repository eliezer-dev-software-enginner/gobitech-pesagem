package my_app.db.repositories;

import my_app.db.models.ConexaoCameraModel;
import net.sf.persism.Session;

import java.sql.SQLException;

import static net.sf.persism.SQL.sql;

public class ConexaoCameraRepository extends BaseRepository<ConexaoCameraModel> {

    public ConexaoCameraRepository(Session session) {
        super(session);
    }

    @Override
    protected Class<ConexaoCameraModel> modelClass() {
        return ConexaoCameraModel.class;
    }

    public ConexaoCameraModel buscarUnico() throws SQLException {
        return session().fetch(
                ConexaoCameraModel.class,
                sql("SELECT * FROM conexao_camera LIMIT 1")
        );
    }
}
