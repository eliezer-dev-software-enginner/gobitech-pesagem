package my_app.db.repositories;

import my_app.db.models.ClienteModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.DriverManager;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClienteRepositoryTest extends BaseRepositoryTest {

    private static final Logger log =
            LoggerFactory.getLogger(ClienteRepositoryTest.class);

    ClienteRepository repository;

    @Override
    protected void initRepository() {
        repository = new ClienteRepository(session);
    }

    private ClienteModel novoCliente(String razaoSocial) {
        var cliente = new ClienteModel();
        cliente.setRazaoSocial(razaoSocial);
        cliente.setCpfCnpj("123.456.789-00");
        cliente.setTelefone("(31) 99999-0000");
        cliente.setCep("30140-071");
        cliente.setUf("MG");
        cliente.setCidade("Belo Horizonte");
        cliente.setBairro("Centro");
        cliente.setRua("Rua Principal");
        cliente.setNumero("100");
        cliente.setAtivo(true);
        cliente.setDataCriacao(LocalDateTime.now());
        return cliente;
    }

    @Test
    void salvar() throws SQLException {
        ClienteModel salvo = repository.salvar(novoCliente("Fazenda Santa Rita"));

        log.info("Cliente salvo com id={}", salvo.getId());

        assertNotNull(salvo);
        assertNotNull(salvo.getId());
        assertEquals("Fazenda Santa Rita", salvo.getRazaoSocial());
        assertEquals("123.456.789-00", salvo.getCpfCnpj());
    }

    @Test
    void listar() throws SQLException {
        repository.salvar(novoCliente("Sítio Boa Vista"));

        List<ClienteModel> lista = repository.listar();

        assertNotNull(lista);
        assertFalse(lista.isEmpty());

        boolean encontrou = lista.stream()
                .anyMatch(it -> it.getRazaoSocial().equals("Sítio Boa Vista"));

        assertTrue(encontrou);
    }

    @Test
    void atualizar() throws SQLException {
        ClienteModel salvo = repository.salvar(novoCliente("Original"));

        salvo.setRazaoSocial("Atualizado");
        salvo.setTelefone("(31) 98888-0000");

        repository.atualizar(salvo);

        ClienteModel atualizado = repository.buscarById(salvo.getId());

        assertNotNull(atualizado);
        assertEquals("Atualizado", atualizado.getRazaoSocial());
        assertEquals("(31) 98888-0000", atualizado.getTelefone());
    }

    @Test
    void excluirById() throws SQLException {
        ClienteModel salvo = repository.salvar(novoCliente("Excluir"));

        repository.excluirById(salvo.getId());

        assertNull(repository.buscarById(salvo.getId()));
    }

    @Test
    void buscarById() throws SQLException {
        ClienteModel salvo = repository.salvar(novoCliente("Busca"));

        ClienteModel encontrado = repository.buscarById(salvo.getId());

        assertNotNull(encontrado);
        assertEquals(salvo.getId(), encontrado.getId());
        assertEquals("Busca", encontrado.getRazaoSocial());
    }

    @Test
    void buscarPorCpfCnpj() throws SQLException {
        repository.salvar(novoCliente("Cliente CPF"));

        ClienteModel encontrado = repository.buscarPorCpfCnpj("123.456.789-00");

        assertNotNull(encontrado);
        assertEquals("Cliente CPF", encontrado.getRazaoSocial());
    }

    @Test
    void count() throws SQLException {
        repository.salvar(novoCliente("Um"));
        repository.salvar(novoCliente("Dois"));

        assertEquals(2, repository.count());
    }

    @Test
    void buscarPorIds() throws SQLException {
        var a = repository.salvar(novoCliente("A"));
        var b = repository.salvar(novoCliente("B"));
        repository.salvar(novoCliente("C"));

        var encontrados = repository.buscarPorIds(List.of(a.getId(), b.getId()));

        assertEquals(2, encontrados.size());
        var razoesSociais = encontrados.stream().map(ClienteModel::getRazaoSocial).toList();
        assertTrue(razoesSociais.contains("A"));
        assertTrue(razoesSociais.contains("B"));
        assertFalse(razoesSociais.contains("C"));
    }

    @Test
    void buscarPorIdsVazioRetornaListaVazia() throws SQLException {
        assertTrue(repository.buscarPorIds(List.of()).isEmpty());
        assertTrue(repository.buscarPorIds(null).isEmpty());
    }

    @Test
    void migracaoV23RemoveLojaEPreservaRazaoSocial(@TempDir Path tempDir) throws Exception {
        String url = "jdbc:sqlite:" + tempDir.resolve("clientes-v22.db");
        Flyway.configure().dataSource(url, "", "")
                .locations("classpath:flyway_migrations").target("22").load().migrate();
        try (var connection = DriverManager.getConnection(url);
             var statement = connection.prepareStatement("""
                     INSERT INTO clientes
                     (loja, razao_social, ativo, dataCriacao)
                     VALUES (?, ?, 1, CURRENT_TIMESTAMP)
                     """)) {
            statement.setString(1, "Nome antigo da loja");
            statement.setString(2, "Razão Social Preservada Ltda");
            statement.executeUpdate();
        }

        Flyway.configure().dataSource(url, "", "")
                .locations("classpath:flyway_migrations").load().migrate();

        try (var connection = DriverManager.getConnection(url);
             var columns = connection.createStatement().executeQuery("PRAGMA table_info(clientes)")) {
            var nomes = new java.util.HashSet<String>();
            while (columns.next()) nomes.add(columns.getString("name"));
            assertFalse(nomes.contains("loja"));
            assertTrue(nomes.contains("razao_social"));
        }
        try (var connection = DriverManager.getConnection(url);
             var dados = connection.createStatement()
                     .executeQuery("SELECT razao_social FROM clientes")) {
            assertTrue(dados.next());
            assertEquals("Razão Social Preservada Ltda", dados.getString("razao_social"));
        }
    }
}
