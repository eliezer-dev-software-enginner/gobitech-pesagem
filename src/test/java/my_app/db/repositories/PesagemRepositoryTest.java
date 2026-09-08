package my_app.db.repositories;

import my_app.db.models.ClienteModel;
import my_app.db.models.PesagemModel;
import my_app.db.models.ProdutoModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PesagemRepositoryTest extends BaseRepositoryTest {

    private static final Logger log =
            LoggerFactory.getLogger(PesagemRepositoryTest.class);

    PesagemRepository repository;
    ClienteRepository clienteRepository;
    ProdutoRepository produtoRepository;

    Integer clienteId;
    Integer produtoId;

    @Override
    protected void initRepository() {
        repository = new PesagemRepository(session);
        clienteRepository = new ClienteRepository(session);
        produtoRepository = new ProdutoRepository(session);
    }

    @BeforeEach
    void criarClienteEProduto() throws SQLException {
        var cliente = new ClienteModel();
        cliente.setLoja("Fazenda Teste");
        cliente.setRazaoSocial("Fazenda Teste Ltda");
        cliente.setAtivo(true);
        cliente.setDataCriacao(LocalDateTime.now());
        clienteId = clienteRepository.salvar(cliente).getId();

        var produto = new ProdutoModel();
        produto.setNome("Soja");
        produto.setUnidade("SC");
        produto.setAtivo(true);
        produto.setDataCriacao(LocalDateTime.now());
        produtoId = produtoRepository.salvar(produto).getId();
    }

    private PesagemModel novaPesagem(String placa) {
        var model = new PesagemModel();
        model.setMotoristaNome("José da Silva");
        model.setMotoristaDocumento("123.456.789-00");
        model.setPlaca(placa);
        model.setTipoPesagem("entrada");
        model.setPesoVeiculo(BigDecimal.valueOf(8500));
        model.setPesoTotal(BigDecimal.valueOf(32000));
        model.setPesoFinal(BigDecimal.valueOf(23500));
        model.setClienteId(clienteId);
        model.setProdutoId(produtoId);
        model.setDataCriacao(LocalDateTime.now());
        return model;
    }

    @Test
    void salvar() throws SQLException {
        PesagemModel salvo = repository.salvar(novaPesagem("ABC1D23"));

        log.info("Pesagem salva com id={}", salvo.getId());

        assertNotNull(salvo);
        assertNotNull(salvo.getId());
        assertEquals("ABC1D23", salvo.getPlaca());
        assertEquals(0, BigDecimal.valueOf(23500).compareTo(salvo.getPesoFinal()));
    }

    @Test
    void listar() throws SQLException {
        repository.salvar(novaPesagem("XYZ9A87"));

        var lista = repository.listar();

        assertNotNull(lista);
        assertFalse(lista.isEmpty());
    }

    @Test
    void atualizar() throws SQLException {
        PesagemModel salvo = repository.salvar(novaPesagem("ABC1D23"));

        salvo.setObservacoes("Observação atualizada");
        repository.atualizar(salvo);

        PesagemModel atualizado = repository.buscarById(salvo.getId());

        assertEquals("Observação atualizada", atualizado.getObservacoes());
    }

    @Test
    void excluirById() throws SQLException {
        PesagemModel salvo = repository.salvar(novaPesagem("ABC1D23"));

        repository.excluirById(salvo.getId());

        assertNull(repository.buscarById(salvo.getId()));
    }

    @Test
    void buscarPorPlacaRetornaEmOrdemCronologica() throws SQLException {
        var primeira = repository.salvar(novaPesagem("ABC1D23"));
        var segunda = novaPesagem("ABC1D23");
        segunda.setDataCriacao(primeira.getDataCriacao().plusHours(2));
        segunda.setTipoPesagem("saida");
        repository.salvar(segunda);

        var lista = repository.buscarPorPlaca("ABC1D23");

        assertEquals(2, lista.size());
        assertEquals("entrada", lista.get(0).getTipoPesagem());
        assertEquals("saida", lista.get(1).getTipoPesagem());
    }

    @Test
    void filtrarPorPlacaEMotoristaJuntosRestringeAmbos() throws SQLException {
        repository.salvar(novaPesagem("AAA1111"));
        var outraPlaca = novaPesagem("BBB2222");
        outraPlaca.setMotoristaNome("Outro Motorista");
        repository.salvar(outraPlaca);

        // placa E motorista devem combinar (AND) — não bastar um dos dois
        var resultado = repository.filtrar("AAA1111", "Outro Motorista", null, null, null, null, null);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void filtrarPorClienteRespeitaOutrosFiltros() throws SQLException {
        repository.salvar(novaPesagem("AAA1111"));

        var outroCliente = new ClienteModel();
        outroCliente.setLoja("Outra Fazenda");
        outroCliente.setRazaoSocial("Outra Fazenda Ltda");
        outroCliente.setAtivo(true);
        outroCliente.setDataCriacao(LocalDateTime.now());
        var outroClienteId = clienteRepository.salvar(outroCliente).getId();

        var pesagemOutroCliente = novaPesagem("CCC3333");
        pesagemOutroCliente.setClienteId(outroClienteId);
        repository.salvar(pesagemOutroCliente);

        // filtrando pela placa da primeira pesagem, mas pelo cliente da segunda — não
        // deve retornar nada, já que os filtros são combinados por AND (bug do app
        // original: um OR aqui faria essa combinação "vazar" e retornar a primeira)
        var resultado = repository.filtrar("AAA1111", null, outroClienteId, null, null, null, null);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void filtrarSemNenhumCampoRetornaTodas() throws SQLException {
        repository.salvar(novaPesagem("AAA1111"));
        repository.salvar(novaPesagem("BBB2222"));

        var resultado = repository.filtrar(null, null, null, null, null, null, null);

        assertEquals(2, resultado.size());
    }

    @Test
    void filtrarPorPeriodoFiltraPelaDataEmInclusivos() throws SQLException {
        // regressão: o filtro antigo bindava epoch-millis contra o texto que o Persism grava
        // (INTEGER < TEXT no SQLite) — "até" nunca casava e "a partir de" nunca filtrava.
        var hoje = novaPesagem("AAA1000");
        hoje.setDataCriacao(LocalDateTime.now());
        repository.salvar(hoje);

        var ontem = novaPesagem("BBB2000");
        ontem.setDataCriacao(LocalDateTime.now().minusDays(1));
        repository.salvar(ontem);

        var semana = novaPesagem("CCC3000");
        semana.setDataCriacao(LocalDateTime.now().minusDays(7));
        repository.salvar(semana);

        LocalDate hojeDate = LocalDate.now();
        LocalDate ontemDate = hojeDate.minusDays(1);

        // só o dia de hoje
        var soHoje = repository.filtrar(null, null, null, null, hojeDate, hojeDate, null);
        assertEquals(1, soHoje.size());
        assertEquals("AAA1000", soHoje.get(0).getPlaca());

        // só o dia de ontem
        var soOntem = repository.filtrar(null, null, null, null, ontemDate, ontemDate, null);
        assertEquals(1, soOntem.size());
        assertEquals("BBB2000", soOntem.get(0).getPlaca());

        // uma extremidade só: a partir de hoje → só hoje; até 7 dias atrás → só a semana
        var aPartirDeHoje = repository.filtrar(null, null, null, null, hojeDate, null, null);
        assertEquals(1, aPartirDeHoje.size());
        var ateSetima = repository.filtrar(null, null, null, null, null, hojeDate.minusDays(7), null);
        assertEquals(1, ateSetima.size());
        assertEquals("CCC3000", ateSetima.get(0).getPlaca());

        // período amplo cobre as três
        var semanaToda = repository.filtrar(null, null, null, null, hojeDate.minusDays(7), hojeDate, null);
        assertEquals(3, semanaToda.size());
    }

    @Test
    void buscarPorPlacaETipoFiltraPeloTipo() throws SQLException {
        repository.salvar(novaPesagem("ABC1D23")); // entrada
        var saida = novaPesagem("ABC1D23");
        saida.setTipoPesagem("saida");
        repository.salvar(saida);

        var entradas = repository.buscarPorPlacaETipo("ABC1D23", "entrada");
        var saidas = repository.buscarPorPlacaETipo("ABC1D23", "saida");

        assertEquals(1, entradas.size());
        assertEquals("entrada", entradas.get(0).getTipoPesagem());
        assertEquals(1, saidas.size());
        assertEquals("saida", saidas.get(0).getTipoPesagem());
    }

    @Test
    void buscarPorPlacaETipoIgnoraMaiusculaMinuscula() throws SQLException {
        repository.salvar(novaPesagem("abc1d23"));

        var porCaixaDiferente = repository.buscarPorPlacaETipo("ABC1D23", "entrada");
        var porUpper = repository.buscarPorPlacaETipo("ABC1D23", "entrada");

        assertEquals(1, porCaixaDiferente.size());
        assertEquals("abc1d23", porCaixaDiferente.get(0).getPlaca());
        assertEquals(porUpper.size(), porCaixaDiferente.size());
    }

    @Test
    void filtrarPorPlacaIgnoraMaiusculaMinuscula() throws SQLException {
        repository.salvar(novaPesagem("abc1d23"));

        var resultado = repository.filtrar("ABC1D23", null, null, null, null, null, null);

        assertEquals(1, resultado.size());
    }

    @Test
    void filtrarPorTipoRetornaSomenteDesseTipo() throws SQLException {
        repository.salvar(novaPesagem("AAA1111")); // entrada
        var saida = novaPesagem("BBB2222");
        saida.setTipoPesagem("saida");
        repository.salvar(saida);
        var avulsa = novaPesagem("CCC3333");
        avulsa.setTipoPesagem("avulsa");
        repository.salvar(avulsa);

        var entradas = repository.filtrar(null, null, null, null, null, null, "entrada");
        var saidas = repository.filtrar(null, null, null, null, null, null, "saida");

        assertEquals(1, entradas.size());
        assertEquals("entrada", entradas.get(0).getTipoPesagem());
        assertEquals(1, saidas.size());
        assertEquals("saida", saidas.get(0).getTipoPesagem());
    }
}
