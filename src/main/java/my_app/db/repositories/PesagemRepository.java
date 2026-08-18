package my_app.db.repositories;

import my_app.db.models.PesagemModel;
import net.sf.persism.Session;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static net.sf.persism.Parameters.params;
import static net.sf.persism.SQL.sql;

public class PesagemRepository extends BaseRepository<PesagemModel> {

    public PesagemRepository(Session session) {
        super(session);
    }

    @Override
    protected Class<PesagemModel> modelClass() {
        return PesagemModel.class;
    }

    public List<PesagemModel> buscarPorPlaca(String placa) throws SQLException {
        return session().query(
                modelClass(),
                sql("SELECT * FROM pesagens WHERE placa = ? ORDER BY dataCriacao ASC"),
                params(placa)
        );
    }

    /**
     * Todos os campos filtram por AND (cada um preenchido restringe mais o resultado) —
     * corrigindo o bug do app original, que misturava AND/OR sem agrupar e fazia um filtro
     * de cliente/produto ignorar os outros campos preenchidos.
     */
    public List<PesagemModel> filtrar(String placa, String motoristaNome, Integer clienteId,
                                       Integer produtoId, Long dataInicioMillis, Long dataFimMillis) throws SQLException {
        var condicoes = new ArrayList<String>();
        var valores = new ArrayList<Object>();

        if (placa != null && !placa.isBlank()) {
            condicoes.add("placa = ?");
            valores.add(placa);
        }
        if (motoristaNome != null && !motoristaNome.isBlank()) {
            condicoes.add("motorista_nome LIKE ?");
            valores.add("%" + motoristaNome + "%");
        }
        if (clienteId != null) {
            condicoes.add("cliente_id = ?");
            valores.add(clienteId);
        }
        if (produtoId != null) {
            condicoes.add("produto_id = ?");
            valores.add(produtoId);
        }
        if (dataInicioMillis != null) {
            condicoes.add("dataCriacao >= ?");
            valores.add(dataInicioMillis);
        }
        if (dataFimMillis != null) {
            condicoes.add("dataCriacao <= ?");
            valores.add(dataFimMillis);
        }

        String where = condicoes.isEmpty() ? "" : " WHERE " + String.join(" AND ", condicoes);
        return session().query(
                modelClass(),
                sql("SELECT * FROM pesagens" + where + " ORDER BY dataCriacao DESC"),
                params(valores.toArray())
        );
    }
}
