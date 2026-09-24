package my_app.domain.pesagem;

import my_app.db.models.PesagemModel;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RelatorioPesagemDadosTest {
    private PesagemModel pesagem(int id, String tipo, int dia, int hora) {
        var p = new PesagemModel();
        p.setId(id);
        p.setTipoPesagem(tipo);
        p.setPlaca("ABC1D23");
        p.setPesoVeiculo(new BigDecimal("8500"));
        p.setPesoTotal(new BigDecimal("32000"));
        p.setPesoFinal(new BigDecimal("23500"));
        p.setDataCriacao(LocalDateTime.of(2026, 9, dia, hora, 30));
        return p;
    }

    @Test
    void parGeraUmaLinhaParaCadaRegistroVisivel() {
        var entrada = pesagem(1, "entrada", 14, 8);
        var saida = pesagem(2, "saida", 15, 17);
        saida.setEntradaId(1);
        var resultado = RelatorioPesagemDados.montar(List.of(entrada, saida));
        assertEquals(2, resultado.linhas().size());
        assertEquals(List.of("14/09/2026", "08:30:00", "", ""), resultado.linhas().getFirst().subList(2, 6));
        assertEquals(List.of("", "", "15/09/2026", "17:30:00"), resultado.linhas().get(1).subList(2, 6));
        assertEquals(new BigDecimal("23500"), resultado.totalLiquido());
    }

    @Test
    void filtroSoSaidaExportaApenasASaidaVisivel() {
        var resultado = RelatorioPesagemDados.montar(List.of(pesagem(2, "saida", 15, 17)));
        assertEquals(1, resultado.linhas().size());
        assertEquals(List.of("", "", "15/09/2026", "17:30:00"), resultado.linhas().getFirst().subList(2, 6));
    }

    @Test
    void saidaSemVinculoPreencheColunaSaida() {
        var resultado = RelatorioPesagemDados.montar(List.of(pesagem(1, "saida", 15, 17)));
        assertEquals(List.of("", "", "15/09/2026", "17:30:00"), resultado.linhas().getFirst().subList(2, 6));
    }

    @Test
    void avulsaEManualTemDataNasDuasColunasEEntradaNaoInventaSaida() {
        for (String tipo : List.of("avulsa", "manual")) {
            var resultado = RelatorioPesagemDados.montar(List.of(pesagem(1, tipo, 15, 17)));
            assertEquals(List.of("15/09/2026", "17:30:00", "15/09/2026", "17:30:00"), resultado.linhas().getFirst().subList(2, 6));
        }
        var resultado = RelatorioPesagemDados.montar(List.of(pesagem(1, "entrada", 15, 17)));
        assertEquals(List.of("15/09/2026", "17:30:00", "", ""), resultado.linhas().getFirst().subList(2, 6));
    }

    @Test
    void listaVaziaTemTotalZero() {
        var resultado = RelatorioPesagemDados.montar(List.of());
        assertTrue(resultado.linhas().isEmpty());
        assertEquals(BigDecimal.ZERO, resultado.totalLiquido());
    }
}
