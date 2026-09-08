package my_app.domain.pesagem;

import my_app.db.models.ClienteModel;
import my_app.db.models.PesagemModel;
import my_app.db.models.ProdutoModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PesagemRegrasTest {

    private void assertValor(String esperado, BigDecimal valor) {
        assertEquals(0, new BigDecimal(esperado).compareTo(valor),
                "Esperado " + esperado + " mas foi " + valor);
    }

    // ---- soma dos descontos / limite de 100% (H4) ----

    @Test
    void somaDescontosVaziosEhZero() {
        assertValor("0", PesagemRegras.somarDescontos());
        assertValor("0", PesagemRegras.somarDescontos((String) null));
        assertValor("0", PesagemRegras.somarDescontos("", "  ", null));
    }

    @Test
    void somaDescontosComValores() {
        assertValor("20", PesagemRegras.somarDescontos("2", "1", "3", "14"));
    }

    @Test
    void somaDescontosComVirgulaDecimal() {
        assertValor("12.5", PesagemRegras.somarDescontos("5", "7,5"));
    }

    @Test
    void somaDescontosIgnoraTextoInvalido() {
        assertValor("5", PesagemRegras.somarDescontos("5", "abc"));
    }

    @Test
    void descontosExatos100SaoPermitidos() {
        assertFalse(PesagemRegras.descontosUltrapassam100(new BigDecimal("100")));
    }

    @Test
    void descontosAcimaDe100NaoSaoPermitidos() {
        assertTrue(PesagemRegras.descontosUltrapassam100(new BigDecimal("100.01")));
        assertTrue(PesagemRegras.descontosUltrapassam100(new BigDecimal("101")));
    }

    @Test
    void descontosAbaixoDe100SaoPermitidos() {
        assertFalse(PesagemRegras.descontosUltrapassam100(new BigDecimal("99.99")));
        assertFalse(PesagemRegras.descontosUltrapassam100(BigDecimal.ZERO));
        assertFalse(PesagemRegras.descontosUltrapassam100(null));
    }

    // ---- líquido negativo (G4) ----

    @Test
    void liquidoNegativoQuandoBrutoMenorQueTara() {
        assertTrue(PesagemRegras.liquidoNegativo("8500", "9000", BigDecimal.ZERO));
    }

    @Test
    void liquidoNegativoQuandoDescontoZeraOUltrapassaOLiquido() {
        // bruto-tara = 1000 ; 150% de desconto → líquido negativo
        assertTrue(PesagemRegras.liquidoNegativo("11000", "10000", new BigDecimal("150")));
    }

    @Test
    void liquidoPositivoQuandoBrutoMaiorQueTara() {
        assertFalse(PesagemRegras.liquidoNegativo("32000", "8500", BigDecimal.ZERO));
    }

    @Test
    void liquidoComUmPesoVazioNaoENegativo() {
        // fluxo "só Tara" / manual sem bruto continua permitido
        assertFalse(PesagemRegras.liquidoNegativo("", "8500", BigDecimal.ZERO));
        assertFalse(PesagemRegras.liquidoNegativo("32000", "", BigDecimal.ZERO));
        assertFalse(PesagemRegras.liquidoNegativo(null, "8500", BigDecimal.ZERO));
    }

    // ---- nenhum peso informado (F2/G6) ----

    @Test
    void nenhumPesoInformadoQuandoAmbosVazios() {
        assertTrue(PesagemRegras.nenhumPesoInformado("", ""));
        assertTrue(PesagemRegras.nenhumPesoInformado(null, "  "));
    }

    @Test
    void algumPesoInformadoSoMenteComUmCampo() {
        assertFalse(PesagemRegras.nenhumPesoInformado("8500", ""));
        assertFalse(PesagemRegras.nenhumPesoInformado("", "32000"));
    }

    // ---- preenchimento da Entrada na Saída (D2) ----

    private PesagemModel entradaCompleta() {
        var e = new PesagemModel();
        e.setMotoristaNome("João");
        e.setMotoristaDocumento("12345678901");
        e.setNotaFiscal("NF-100");
        e.setPesoVeiculo(new BigDecimal("8500.2"));
        e.setPesoTotal(new BigDecimal("32000.5"));
        e.setClienteId(1);
        e.setProdutoId(2);
        return e;
    }

    private ClienteModel cliente(int id) {
        var c = new ClienteModel();
        c.setId(id);
        c.setLoja("Loja " + id);
        return c;
    }

    private ProdutoModel produto(int id) {
        var p = new ProdutoModel();
        p.setId(id);
        p.setNome("Produto " + id);
        return p;
    }

    @Test
    void preenchimentoCompletoPreencheCamposEPesosArredondados() {
        var dados = PesagemRegras.preencherDaEntrada(entradaCompleta(),
                List.of(cliente(1), cliente(9)), List.of(produto(2)));

        assertEquals("João", dados.motoristaNome());
        assertEquals("12345678901", dados.motoristaDocumento());
        assertEquals("NF-100", dados.notaFiscal());
        assertEquals("8500", dados.pesoVeiculo());
        assertEquals("32001", dados.pesoTotal());
        assertEquals("Loja 1", dados.cliente().getLoja());
        assertEquals("Produto 2", dados.produto().getNome());
    }

    @Test
    void preenchimentoCamposNulosViramVazio() {
        var e = new PesagemModel();
        var dados = PesagemRegras.preencherDaEntrada(e, List.of(), List.of());

        assertEquals("", dados.motoristaNome());
        assertEquals("", dados.motoristaDocumento());
        assertEquals("", dados.notaFiscal());
        assertEquals("", dados.pesoVeiculo());
        assertEquals("", dados.pesoTotal());
        assertNull(dados.cliente());
        assertNull(dados.produto());
    }

    @Test
    void preenchimentoNaoVinculaClienteOuProdutoQueNaoEstaNaLista() {
        var dados = PesagemRegras.preencherDaEntrada(entradaCompleta(),
                List.of(cliente(99)), List.of(produto(99)));

        assertNull(dados.cliente());
        assertNull(dados.produto());
    }

    @Test
    void preenchimentoSemListasNaoEstoura() {
        var dados = PesagemRegras.preencherDaEntrada(entradaCompleta(), null, null);

        assertNull(dados.cliente());
        assertNull(dados.produto());
        assertEquals("8500", dados.pesoVeiculo());
    }

    @Test
    void preenchimentoResolveClienteMesmoSemProduto() {
        var dados = PesagemRegras.preencherDaEntrada(entradaCompleta(),
                List.of(cliente(1)), List.of());

        assertEquals(Integer.valueOf(1), dados.cliente().getId());
        assertNull(dados.produto());
    }

    // ---- fotos (slot + nome) ----

    @Test
    void slot2SoParaSaida() {
        assertTrue(PesagemRegras.usarSlot2("saida"));
        assertFalse(PesagemRegras.usarSlot2("entrada"));
        assertFalse(PesagemRegras.usarSlot2("avulsa"));
        assertFalse(PesagemRegras.usarSlot2("manual"));
        assertFalse(PesagemRegras.usarSlot2(null));
    }

    @Test
    void nomeArquivoFotoSeguePadraoComSlot() {
        assertEquals("pesagem_7_frente_1.jpg", PesagemRegras.nomeArquivoFoto(7, "frente", false));
        assertEquals("pesagem_7_costas_2.jpg", PesagemRegras.nomeArquivoFoto(7, "costas", true));
    }
}