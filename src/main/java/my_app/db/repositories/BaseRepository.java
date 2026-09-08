package my_app.db.repositories;

import my_app.db.DB;
import net.sf.persism.Session;
import net.sf.persism.annotations.Table;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static net.sf.persism.Parameters.params;
import static net.sf.persism.SQL.sql;

public abstract class BaseRepository<M> {

    private Session session;

    public BaseRepository(Session session) {
        this.session = session;
    }

    protected Session session() {
        return session;
    }

    protected abstract Class<M> modelClass();

    public M salvar(M model) throws SQLException {
       var result = session().insert(model);
        return result.dataObject();
    }

    public List<M> listar() throws SQLException {
        return session().query(modelClass());
    }

    /** Total de linhas da tabela — evita trafegar a lista inteira só pra saber o tamanho. */
    public long count() throws SQLException {
        String tableName = modelClass().getAnnotation(Table.class).value();
        // COUNT(*) do SQLite vem como INTEGER → o Persism devolve Integer no scalar;
        // o long é o tipo retornado (o volume nunca chega perto do teto do Integer).
        var resultado = session().query(
                Integer.class,
                sql("SELECT COUNT(*) FROM " + tableName)
        );
        return resultado.isEmpty() ? 0 : resultado.getFirst().longValue();
    }

    /**
     * Busca em lote por {@code WHERE id IN (...)} — substitui N SELECTs individuais (padrão
     * N+1) quando várias entidades de uma mesma tabela vão ser carregadas de uma vez.
     */
    public List<M> buscarPorIds(Collection<Integer> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) return List.of();
        String tableName = modelClass().getAnnotation(Table.class).value();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        return session().query(
                modelClass(),
                sql("SELECT * FROM " + tableName + " WHERE id IN (" + placeholders + ")"),
                params(ids.toArray())
        );
    }

    public void atualizar(M model) throws SQLException {
        session().update(model);
    }

    public void excluirById(long id) throws SQLException {
        M model = buscarById(id);
        if (model != null) session().delete(model);
    }

    public M buscarById(long id) throws SQLException {
        String tableName = modelClass().getAnnotation(Table.class).value();
        return session().fetch(
                modelClass(),
                sql("SELECT * FROM " + tableName + " WHERE id = ?"),
                params(id)
        );
    }

    public void close() {
        if (session != null) {
            DB.unregister(session);
            session.close();
            session = null;
        }
    }
}