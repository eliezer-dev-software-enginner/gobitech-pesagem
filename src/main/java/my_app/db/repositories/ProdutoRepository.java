package my_app.db.repositories;

import my_app.db.models.ProdutoModel;
import net.sf.persism.Session;

import java.sql.SQLException;

import static net.sf.persism.Parameters.params;
import static net.sf.persism.SQL.sql;

public class ProdutoRepository extends BaseRepository<ProdutoModel> {

    public ProdutoRepository(Session session) {
        super(session);
    }

    @Override
    protected Class<ProdutoModel> modelClass() {
        return ProdutoModel.class;
    }

    public ProdutoModel buscarPorNome(String nome) throws SQLException {
        return session().fetch(
                modelClass(),
                sql("SELECT * FROM produtos WHERE nome = ?"),
                params(nome)
        );
    }
}
