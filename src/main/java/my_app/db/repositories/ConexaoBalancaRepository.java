package my_app.db.repositories;

import my_app.db.models.ConexaoBalancaModel;
import net.sf.persism.Session;

import java.sql.SQLException;

import static net.sf.persism.SQL.sql;

public class ConexaoBalancaRepository extends BaseRepository<ConexaoBalancaModel> {

    public ConexaoBalancaRepository(Session session) {
        super(session);
    }

    @Override
    protected Class<ConexaoBalancaModel> modelClass() {
        return ConexaoBalancaModel.class;
    }

    public ConexaoBalancaModel buscarUnico() throws SQLException {
        return session().fetch(
                ConexaoBalancaModel.class,
                sql("SELECT * FROM conexao_balanca LIMIT 1")
        );
    }
}
