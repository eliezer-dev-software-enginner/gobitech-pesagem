package my_app.db.services;

import my_app.db.models.ClienteModel;
import my_app.db.models.PesagemModel;
import my_app.db.models.ProdutoModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PesagemServiceTest extends BaseServiceTest {

    private PesagemService pesagemService;
    private ClienteService clienteService;
    private ProdutoService produtoService;

    private Integer clienteId;
    private Integer produtoId;

    @Override
    protected void initService() {
        pesagemService = new PesagemService(session);
        clienteService = new ClienteService(session);
        produtoService = new ProdutoService(session);
    }

    @BeforeEach
    void criarClienteEProduto() throws Exception {
        var cliente = new ClienteModel();
        cliente.setLoja("Fazenda Teste");
        cliente.setRazaoSocial("Fazenda Teste Ltda");
        clienteId = clienteService.salvar(cliente).getId();

        var produto = new ProdutoModel();
        produto.setNome("Soja");
        produto.setUnidade("SC");
        produtoId = produtoService.salvar(produto).getId();
    }

    private PesagemModel pesagemValida() {
        var p = new PesagemModel();
        p.setMotoristaNome("José da Silva");
        p.setPlaca("ABC1D23");
        p.setTipoPesagem("entrada");
        p.setClienteId(clienteId);
        p.setProdutoId(produtoId);
        return p;
    }

    @Test
    void deveLancarExcecaoQuandoMotoristaVazio() {
        var p = pesagemValida();
        p.setMotoristaNome("");
        assertThrows(IllegalArgumentException.class, () -> pesagemService.salvar(p));
    }

    @Test
    void deveLancarExcecaoQuandoPlacaVazia() {
        var p = pesagemValida();
        p.setPlaca("");
        assertThrows(IllegalArgumentException.class, () -> pesagemService.salvar(p));
    }

    @Test
    void deveLancarExcecaoQuandoClienteNaoInformado() {
        var p = pesagemValida();
        p.setClienteId(null);
        assertThrows(IllegalArgumentException.class, () -> pesagemService.salvar(p));
    }

    @Test
    void deveLancarExcecaoQuandoProdutoNaoInformado() {
        var p = pesagemValida();
        p.setProdutoId(null);
        assertThrows(IllegalArgumentException.class, () -> pesagemService.salvar(p));
    }

    @Test
    void deveZerarPesosNaoInformados() throws Exception {
        var salvo = pesagemService.salvar(pesagemValida());
        assertNotNull(salvo.getPesoVeiculo());
        assertNotNull(salvo.getPesoTotal());
        assertNotNull(salvo.getPesoFinal());
    }

    @Test
    void deveLancarExcecaoQuandoTipoPesagemVazio() {
        var p = pesagemValida();
        p.setTipoPesagem(null);
        assertThrows(IllegalArgumentException.class, () -> pesagemService.salvar(p));
    }

    @Test
    void tipoPesagemInformadoEhPreservado() throws Exception {
        var p = pesagemValida();
        p.setTipoPesagem("avulsa");
        var salvo = pesagemService.salvar(p);
        assertEquals("avulsa", salvo.getTipoPesagem());
    }

    @Test
    void buscarComRelacoesAnexaClienteEProduto() throws Exception {
        var salvo = pesagemService.salvar(pesagemValida());

        var comRelacoes = pesagemService.buscarComRelacoes(salvo.getId());

        assertNotNull(comRelacoes.getCliente());
        assertEquals("Fazenda Teste", comRelacoes.getCliente().getLoja());
        assertNotNull(comRelacoes.getProduto());
        assertEquals("Soja", comRelacoes.getProduto().getNome());
    }

    @Test
    void listarComRelacoesAnexaClienteEProdutoEmTodasAsPesagens() throws Exception {
        pesagemService.salvar(pesagemValida());

        var lista = pesagemService.listarComRelacoes();

        assertFalse(lista.isEmpty());
        assertTrue(lista.stream().allMatch(it -> it.getCliente() != null && it.getProduto() != null));
    }

    @Test
    void filtrarComRelacoesRetornaClienteEProdutoAnexados() throws Exception {
        pesagemService.salvar(pesagemValida());

        var resultado = pesagemService.filtrar("ABC1D23", null, null, null, null, null);

        assertEquals(1, resultado.size());
        assertNotNull(resultado.getFirst().getCliente());
    }

    @Test
    void buscarUltimaEntradaEhNullQuandoPlacaNuncaFoiPesada() throws Exception {
        assertNull(pesagemService.buscarUltimaEntrada("XYZ9Z99"));
    }

    @Test
    void buscarUltimaEntradaRetornaMesmoComPesagensDeSaidaDepois() throws Exception {
        var entrada = pesagemValida();
        entrada.setTipoPesagem("entrada");
        pesagemService.salvar(entrada);

        var saida = pesagemValida();
        saida.setTipoPesagem("saida");
        pesagemService.salvar(saida);

        var ultima = pesagemService.buscarUltimaEntrada("ABC1D23");

        assertNotNull(ultima);
        assertEquals("entrada", ultima.getTipoPesagem());
    }

    @Test
    void taraSugeridaEhNullQuandoPlacaNuncaFoiPesada() throws Exception {
        assertNull(pesagemService.buscarTaraSugerida("XYZ9Z99"));
    }

    @Test
    void taraSugeridaVemDaUltimaEntradaDaPlaca() throws Exception {
        var entrada = pesagemValida();
        entrada.setTipoPesagem("entrada");
        entrada.setPesoVeiculo(new java.math.BigDecimal("8500"));
        pesagemService.salvar(entrada);

        // uma saída depois não deve tirar a tara sugerida (entrada continua existindo)
        var saida = pesagemValida();
        saida.setTipoPesagem("saida");
        pesagemService.salvar(saida);

        var tara = pesagemService.buscarTaraSugerida("ABC1D23");

        assertEquals(0, new java.math.BigDecimal("8500").compareTo(tara));
    }

    @Test
    void taraSugeridaEhNullQuandoNemTemEntrada() throws Exception {
        var avulsa = pesagemValida();
        avulsa.setTipoPesagem("avulsa");
        pesagemService.salvar(avulsa);

        assertNull(pesagemService.buscarTaraSugerida("ABC1D23"));
    }
}
