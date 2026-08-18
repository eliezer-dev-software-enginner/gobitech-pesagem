package my_app.db.repositories;

import my_app.db.models.LicensaModel;
import net.sf.persism.Session;

import java.sql.SQLException;
import java.util.List;

import static net.sf.persism.Parameters.params;
import static net.sf.persism.SQL.sql;

public class LicensaRepository extends BaseRepository<LicensaModel> {

    public LicensaRepository(Session session) {
        super(session);
    }

    @Override
    protected Class<LicensaModel> modelClass() {
        return LicensaModel.class;
    }

    public LicensaModel buscarPorValor(String valor) throws SQLException {
        return session().fetch(
                modelClass(),
                sql("SELECT * FROM licensas WHERE valor = ?"),
                params(valor)
        );
    }

    public List<LicensaModel> listarMaisRecentePrimeiro() throws SQLException {
        return session().query(
                modelClass(),
                sql("SELECT * FROM licensas ORDER BY dataCriacao DESC")
        );
    }
}
