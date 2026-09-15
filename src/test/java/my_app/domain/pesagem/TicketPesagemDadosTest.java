package my_app.domain.pesagem;

import my_app.db.models.DescontoModel;
import my_app.db.models.PesagemModel;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class TicketPesagemDadosTest {
    private PesagemModel pesagem(String tipo, int tara, int bruto) {
        var p = new PesagemModel();
        p.setTipoPesagem(tipo);
        p.setPesoVeiculo(BigDecimal.valueOf(tara));
        p.setPesoTotal(BigDecimal.valueOf(bruto));
        p.setDataCriacao(LocalDateTime.of(2026, 9, 15, 10, 20));
        return p;
    }

    @Test
    void entradaSoComTaraNaoImprimeZeroNemSaidaFicticia() {
        var p = pesagem("entrada", 8500, 0);
        assertEquals(new BigDecimal("8500"), TicketPesagemDados.pesoEntrada(p, null));
        assertEquals(p.getDataCriacao(), TicketPesagemDados.dataEntrada(p, null));
        assertNull(TicketPesagemDados.dataSaida(p));
        assertNull(TicketPesagemDados.pesoSaida(p));
    }

    @Test
    void saidaUsaPesoRegistradoNaEntradaVinculada() {
        var entrada = pesagem("entrada", 8500, 0);
        var saida = pesagem("saida", 9000, 32000);
        assertEquals(new BigDecimal("8500"), TicketPesagemDados.pesoEntrada(saida, entrada));
        entrada.setPesoTotal(new BigDecimal("31000"));
        assertEquals(new BigDecimal("31000"), TicketPesagemDados.pesoEntrada(saida, entrada));
    }

    @Test
    void avulsaManualESaidaSemVinculoUsamTaraLocal() {
        for (String tipo : new String[]{"avulsa", "manual", "saida"}) {
            var p = pesagem(tipo, 8500, 32000);
            assertEquals(new BigDecimal("8500"), TicketPesagemDados.pesoEntrada(p, null));
            assertEquals(new BigDecimal("32000"), TicketPesagemDados.pesoSaida(p));
            assertEquals(p.getDataCriacao(), TicketPesagemDados.dataSaida(p));
            if ("saida".equals(tipo)) assertNull(TicketPesagemDados.dataEntrada(p, null));
            else assertEquals(p.getDataCriacao(), TicketPesagemDados.dataEntrada(p, null));
        }
    }

    @Test
    void descontosIncidemSobreBrutoMenosTaraSemAplicacaoSequencial() {
        var p = pesagem("manual", 8500, 32000);
        var d = new DescontoModel();
        d.setArdidos(new BigDecimal("2"));
        d.setImpurezas(new BigDecimal("3"));
        p.setDesconto(d);
        var linhas = TicketPesagemDados.descontos(p);
        assertEquals(8, linhas.size());
        assertEquals(0, new BigDecimal("470").compareTo(linhas.get(1).quilos()));
        assertEquals(0, new BigDecimal("705").compareTo(linhas.get(3).quilos()));
        assertEquals(0, new BigDecimal("1175").compareTo(TicketPesagemDados.totalDescontado(p)));
        assertEquals(new BigDecimal("23500"), TicketPesagemDados.liquidoAntesDescontos(p));
    }

    @Test
    void semDescontoOuSemBrutoNaoProduzDescontoNegativo() {
        var p = pesagem("entrada", 8500, 0);
        assertTrue(TicketPesagemDados.descontos(p).stream().allMatch(d -> d.quilos().signum() == 0));
        var d = new DescontoModel();
        d.setOutros(new BigDecimal("100"));
        p.setDesconto(d);
        assertEquals(0, TicketPesagemDados.totalDescontado(p).signum());
    }
}
