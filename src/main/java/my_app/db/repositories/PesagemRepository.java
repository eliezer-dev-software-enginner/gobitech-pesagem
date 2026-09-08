package my_app.db.repositories;

import my_app.db.models.PesagemModel;
import net.sf.persism.Session;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
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
                sql("SELECT * FROM pesagens WHERE UPPER(placa) = UPPER(?) ORDER BY dataCriacao ASC"),
                params(placa)
        );
    }

    public List<PesagemModel> buscarPorPlacaETipo(String placa, String tipoPesagem) throws SQLException {
        return session().query(
                modelClass(),
                sql("SELECT * FROM pesagens WHERE UPPER(placa) = UPPER(?) AND tipo_pesagem = ? ORDER BY dataCriacao ASC"),
                params(placa, tipoPesagem)
        );
    }

    /**
     * Todos os campos filtram por AND (cada um preenchido restringe mais o resultado) —
     * corrigindo o bug do app original, que misturava AND/OR sem agrupar e fazia um filtro
     * de cliente/produto ignorar os outros campos preenchidos.
     *
     * <p>As datas entram como {@link LocalDate} (dia inicial e dia final, inclusivos) e são
     * convertidas pra epoch-millis no fuso do sistema antes do bind. A coluna {@code dataCriacao}
     * é TIMESTAMP, e o Persism/sqlite-jdbc grava o valor como INTEGER epoch-millis (ver
     * {@code DECISIONS.md} 2026-09-07) — por isso a comparação é numérica e cobre o dia inteiro
     * (início do dia inicial até o fim do dia final).
     */
    public List<PesagemModel> filtrar(String placa, String motoristaNome, Integer clienteId,
                                       Integer produtoId, LocalDate dataInicio, LocalDate dataFim,
                                       String tipoPesagem) throws SQLException {
        var condicoes = new ArrayList<String>();
        var valores = new ArrayList<Object>();

        if (placa != null && !placa.isBlank()) {
            condicoes.add("UPPER(placa) = UPPER(?)");
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
        if (tipoPesagem != null && !tipoPesagem.isBlank()) {
            condicoes.add("tipo_pesagem = ?");
            valores.add(tipoPesagem);
        }
        if (dataInicio != null) {
            condicoes.add("dataCriacao >= ?");
            valores.add(millisDoInicioDoDia(dataInicio));
        }
        if (dataFim != null) {
            condicoes.add("dataCriacao <= ?");
            valores.add(millisDoFimDoDia(dataFim));
        }

        String where = condicoes.isEmpty() ? "" : " WHERE " + String.join(" AND ", condicoes);
        return session().query(
                modelClass(),
                sql("SELECT * FROM pesagens" + where + " ORDER BY dataCriacao DESC"),
                params(valores.toArray())
        );
    }

    /** Início (00:00:00) de um dia em epoch-millis no fuso do sistema. */
    private static long millisDoInicioDoDia(LocalDate dia) {
        return dia.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** Fim (23:59:59.999) de um dia em epoch-millis no fuso do sistema. */
    private static long millisDoFimDoDia(LocalDate dia) {
        return dia.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1;
    }
}
