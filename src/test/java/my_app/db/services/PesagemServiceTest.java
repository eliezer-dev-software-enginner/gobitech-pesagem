package my_app.db.services;

import my_app.db.models.ClienteModel;
import my_app.db.models.PesagemModel;
import my_app.db.models.ProdutoModel;
import my_app.db.models.UsuarioModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PesagemServiceTest extends BaseServiceTest {

    private PesagemService pesagemService;
    private ClienteService clienteService;
    private ProdutoService produtoService;
    private UsuarioService usuarioService;

    private Integer clienteId;
    private Integer produtoId;

    @Override
    protected void initService() {
        pesagemService = new PesagemService(session);
        clienteService = new ClienteService(session);
        produtoService = new ProdutoService(session);
        usuarioService = new UsuarioService(session);
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
    void motoristaEhOpcional() throws Exception {
        var p = pesagemValida();
        p.setMotoristaNome(null);
        var salvo = pesagemService.salvar(p);
        assertNull(salvo.getMotoristaNome());
    }

    @Test
    void deveLancarExcecaoQuandoPlacaVazia() {
        var p = pesagemValida();
        p.setPlaca("");
        assertThrows(IllegalArgumentException.class, () -> pesagemService.salvar(p));
    }

    @Test
    void clienteEhOpcional() throws Exception {
        var p = pesagemValida();
        p.setClienteId(null);
        var salvo = pesagemService.salvar(p);
        assertNull(salvo.getClienteId());
    }

    @Test
    void produtoEhOpcional() throws Exception {
        var p = pesagemValida();
        p.setProdutoId(null);
        var salvo = pesagemService.salvar(p);
        assertNull(salvo.getProdutoId());
    }

    @Test
    void deveZerarPesosNaoInformados() throws Exception {
        var salvo = pesagemService.salvar(pesagemValida());
        assertNotNull(salvo.getPesoVeiculo());
        assertNotNull(salvo.getPesoTotal());
        assertNotNull(salvo.getPesoFinal());
    }

    @Test
    void deveLancarExcecaoQuandoBrutoMenorQueTara() {
        var p = pesagemValida();
        p.setPesoVeiculo(new java.math.BigDecimal("10000"));
        p.setPesoTotal(new java.math.BigDecimal("8000"));
        assertThrows(IllegalArgumentException.class, () -> pesagemService.salvar(p));
    }

    @Test
    void aceitaBrutoIgualOuMaiorQueTara() throws Exception {
        var p = pesagemValida();
        p.setPesoVeiculo(new java.math.BigDecimal("8500"));
        p.setPesoTotal(new java.math.BigDecimal("32000"));
        assertNotNull(pesagemService.salvar(p).getId());

        var igual = pesagemValida();
        igual.setPesoVeiculo(new java.math.BigDecimal("8500"));
        igual.setPesoTotal(new java.math.BigDecimal("8500"));
        assertNotNull(pesagemService.salvar(igual).getId());
    }

    @Test
    void aceitaSomenteTaraSemBruto() throws Exception {
        // fluxo C1: Entrada registrada só com o caminhão vazio (tara) não pode ser barrada
        var p = pesagemValida();
        p.setPesoVeiculo(new java.math.BigDecimal("8500"));
        assertNotNull(pesagemService.salvar(p).getId());
    }

    @Test
    void atualizarTambemValidaBrutoMenorQueTara() throws Exception {
        var p = pesagemValida();
        var salvo = pesagemService.salvar(p); // sem pesos

        salvo.setPesoVeiculo(new java.math.BigDecimal("10000"));
        salvo.setPesoTotal(new java.math.BigDecimal("8000"));
        assertThrows(IllegalArgumentException.class, () -> pesagemService.atualizar(salvo));
    }

    @Test
    void deveLancarExcecaoQuandoDocumentoMotoristaInvalido() {
        var p = pesagemValida();
        p.setMotoristaDocumento("123");
        assertThrows(IllegalArgumentException.class, () -> pesagemService.salvar(p));
    }

    @Test
    void aceitaDocumentoMotoristaVazioOuNulo() throws Exception {
        var p1 = pesagemValida();
        p1.setMotoristaDocumento("");
        assertNotNull(pesagemService.salvar(p1).getId());

        var p2 = pesagemValida();
        p2.setMotoristaDocumento(null);
        assertNotNull(pesagemService.salvar(p2).getId());
    }

    @Test
    void aceitaDocumentoMotoristaRgOuCpfValido() throws Exception {
        var p1 = pesagemValida();
        p1.setMotoristaDocumento("12345678");
        assertNotNull(pesagemService.salvar(p1).getId());

        var p2 = pesagemValida();
        p2.setMotoristaDocumento("12345678901");
        assertNotNull(pesagemService.salvar(p2).getId());
    }

    @Test
    void deveLancarExcecaoQuandoNomeMotoristaExcede100Caracteres() {
        var p = pesagemValida();
        p.setMotoristaNome("X".repeat(101));
        assertThrows(IllegalArgumentException.class, () -> pesagemService.salvar(p));
    }

    @Test
    void aceitaNomeMotoristaComAte100Caracteres() throws Exception {
        var p = pesagemValida();
        p.setMotoristaNome("X".repeat(100));
        assertNotNull(pesagemService.salvar(p).getId());
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

        var resultado = pesagemService.filtrar("ABC1D23", null, null, null, null, null, null);

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

    @Test
    void entradaIdDaSaidaEhPersistidoERelido() throws Exception {
        var entrada = pesagemValida();
        entrada.setTipoPesagem("entrada");
        var entradaSalva = pesagemService.salvar(entrada);

        var saida = pesagemValida();
        saida.setTipoPesagem("saida");
        saida.setEntradaId(entradaSalva.getId());
        var saidaSalva = pesagemService.salvar(saida);

        var relida = pesagemService.buscarById(saidaSalva.getId());

        assertNotNull(relida);
        assertEquals(entradaSalva.getId(), relida.getEntradaId());
    }

    @Test
    void usuarioIdDaPesagemEhPersistido() throws Exception {
        var usuario = new UsuarioModel();
        usuario.setNome("Maria");
        usuario.setLogin("maria");
        usuario.setSenha("x1234567!");
        var usuarioSalvo = usuarioService.salvar(usuario);

        var p = pesagemValida();
        p.setUsuarioId(usuarioSalvo.getId());
        var salvo = pesagemService.salvar(p);

        var relida = pesagemService.buscarById(salvo.getId());
        assertEquals(usuarioSalvo.getId(), relida.getUsuarioId());
    }

    @Test
    void buscarComRelacoesAnexaOperador() throws Exception {
        var usuario = new UsuarioModel();
        usuario.setNome("Maria");
        usuario.setLogin("maria");
        usuario.setSenha("x1234567!");
        var usuarioSalvo = usuarioService.salvar(usuario);

        var p = pesagemValida();
        p.setUsuarioId(usuarioSalvo.getId());
        var salvo = pesagemService.salvar(p);

        var comRelacoes = pesagemService.buscarComRelacoes(salvo.getId());
        assertNotNull(comRelacoes.getUsuario());
        assertEquals("Maria", comRelacoes.getUsuario().getNome());
    }

    @Test
    void buscarEntradaVinculadaRetornaAEntradaDaSaida() throws Exception {
        var entrada = pesagemValida();
        entrada.setTipoPesagem("entrada");
        var entradaSalva = pesagemService.salvar(entrada);

        var saida = pesagemValida();
        saida.setTipoPesagem("saida");
        saida.setEntradaId(entradaSalva.getId());
        var saidaSalva = pesagemService.salvar(saida);

        var vinculada = pesagemService.buscarEntradaVinculada(saidaSalva);
        assertNotNull(vinculada);
        assertEquals(entradaSalva.getId(), vinculada.getId());
    }

    @Test
    void buscarEntradaVinculadaEhNullQuandoNaoHaVinculo() throws Exception {
        var avulsa = pesagemValida();
        avulsa.setTipoPesagem("avulsa");
        var salva = pesagemService.salvar(avulsa);

        assertNull(pesagemService.buscarEntradaVinculada(salva));
    }

    @Test
    void countRetornaOTotalDePesagens() throws Exception {
        assertEquals(0, pesagemService.count());
        pesagemService.salvar(pesagemValida());
        pesagemService.salvar(pesagemValida());
        assertEquals(2, pesagemService.count());
    }

    @Test
    void contarPorPeriodoContaSoAsPesagensDoPeriodo() throws Exception {
        pesagemComDataCriacao(LocalDateTime.of(2020, 1, 15, 10, 0));

        assertEquals(1, pesagemService.contarPorPeriodo(LocalDate.of(2020, 1, 15), LocalDate.of(2020, 1, 16)));
        // dias diferentes, fora do período
        assertEquals(0, pesagemService.contarPorPeriodo(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 28)));
    }

    @Test
    void contarPorPeriodoComDataIgualECoberta() throws Exception {
        pesagemComDataCriacao(LocalDateTime.of(2021, 6, 10, 8, 0));

        assertEquals(1, pesagemService.contarPorPeriodo(LocalDate.of(2021, 6, 10), LocalDate.of(2021, 6, 10)));
    }

    /**
     * Salva uma pesagem com dataCriacao própria direto pelo repositório (o {@code salvar()} do
     * service sobrescreve a data com "agora"). Os pesos vão zerados explicitamente porque o
     * {@code validarCampos()} do service (que os zera) também é contornado aqui.
     */
    private PesagemModel pesagemComDataCriacao(LocalDateTime quando) throws Exception {
        var p = pesagemValida();
        p.setPesoVeiculo(java.math.BigDecimal.ZERO);
        p.setPesoTotal(java.math.BigDecimal.ZERO);
        p.setPesoFinal(java.math.BigDecimal.ZERO);
        p.setDataCriacao(quando);
        return pesagemService.repository.salvar(p);
    }

    @Test
    void listarComRelacoesAnexaClientesDistintosEmLote() throws Exception {
        var segundoCliente = new ClienteModel();
        segundoCliente.setLoja("Usina B");
        segundoCliente.setRazaoSocial("Usina B Ltda");
        var segundoClienteId = clienteService.salvar(segundoCliente).getId();

        var segundaPlaca = pesagemValida();
        segundaPlaca.setPlaca("XYZ9X99");
        segundaPlaca.setClienteId(segundoClienteId);
        pesagemService.salvar(pesagemValida());
        pesagemService.salvar(segundaPlaca);

        var lista = pesagemService.listarComRelacoes();

        assertEquals(2, lista.size());
        var comClienteA = lista.stream().filter(it -> "ABC1D23".equals(it.getPlaca())).findFirst().orElseThrow();
        var comClienteB = lista.stream().filter(it -> "XYZ9X99".equals(it.getPlaca())).findFirst().orElseThrow();
        assertEquals("Fazenda Teste", comClienteA.getCliente().getLoja());
        assertEquals("Usina B", comClienteB.getCliente().getLoja());
    }
}
