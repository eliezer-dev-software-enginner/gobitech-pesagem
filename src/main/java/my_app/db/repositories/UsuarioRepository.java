package my_app.db.repositories;

import my_app.db.models.UsuarioModel;
import net.sf.persism.Session;

import java.sql.SQLException;
import java.util.List;

import static net.sf.persism.Parameters.params;
import static net.sf.persism.SQL.sql;

public class UsuarioRepository extends BaseRepository<UsuarioModel> {

    public UsuarioRepository(Session session) {
        super(session);
    }

    @Override
    protected Class<UsuarioModel> modelClass() {
        return UsuarioModel.class;
    }

    public UsuarioModel buscarPorLogin(String login) throws SQLException {
        return session().fetch(
                modelClass(),
                sql("SELECT * FROM usuarios WHERE login = ?"),
                params(login)
        );
    }

    public List<UsuarioModel> listarAtivos() throws SQLException {
        return session().query(
                modelClass(),
                sql("SELECT * FROM usuarios WHERE ativo = 1 ORDER BY nome")
        );
    }
}
