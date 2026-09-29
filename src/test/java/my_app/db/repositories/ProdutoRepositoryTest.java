package my_app.db.repositories;

import my_app.db.models.ProdutoModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProdutoRepositoryTest extends BaseRepositoryTest {

    private static final Logger log =
            LoggerFactory.getLogger(ProdutoRepositoryTest.class);

    ProdutoRepository repository;

    @Override
    protected void initRepository() {
        repository = new ProdutoRepository(session);
    }

    @BeforeEach
    void cleanProdutos() throws Exception {
        try (var conn = DriverManager.getConnection(testUrl());
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM produtos");
        }
    }

    private ProdutoModel novoProduto(String nome) {
        var model = new ProdutoModel();
        model.setNome(nome);
        model.setUnidade("SC");
        model.setObservacoes("Observação de teste");
        model.setAtivo(true);
        model.setDataCriacao(LocalDateTime.now());
        return model;
    }

    @Test
    void salvar() throws SQLException {
        ProdutoModel salvo = repository.salvar(novoProduto("Soja"));

        log.info("Produto salvo com id={}", salvo.getId());

        assertNotNull(salvo);
        assertNotNull(salvo.getId());
        assertEquals("Soja", salvo.getNome());
        assertEquals("SC", salvo.getUnidade());
    }

    @Test
    void listar() throws SQLException {
        repository.salvar(novoProduto("Milho"));

        var lista = repository.listar();

        assertNotNull(lista);
        assertFalse(lista.isEmpty());
    }

    @Test
    void atualizar() throws SQLException {
        ProdutoModel salvo = repository.salvar(novoProduto("Original"));

        salvo.setNome("Atualizado");
        salvo.setUnidade("KG");
        repository.atualizar(salvo);

        ProdutoModel atualizado = repository.buscarById(salvo.getId());

        assertNotNull(atualizado);
        assertEquals("Atualizado", atualizado.getNome());
        assertEquals("KG", atualizado.getUnidade());
    }

    @Test
    void excluirById() throws SQLException {
        ProdutoModel salvo = repository.salvar(novoProduto("Excluir"));

        repository.excluirById(salvo.getId());

        assertNull(repository.buscarById(salvo.getId()));
    }

    @Test
    void buscarById() throws SQLException {
        ProdutoModel salvo = repository.salvar(novoProduto("Busca"));

        ProdutoModel encontrado = repository.buscarById(salvo.getId());

        assertNotNull(encontrado);
        assertEquals(salvo.getId(), encontrado.getId());
        assertEquals("Busca", encontrado.getNome());
    }

    @Test
    void buscarPorNome() throws SQLException {
        repository.salvar(novoProduto("Sorgo"));

        ProdutoModel encontrado = repository.buscarPorNome("Sorgo");

        assertNotNull(encontrado);
        assertEquals("Sorgo", encontrado.getNome());
    }

    @Test
    void buscarPorNomeInexistente() throws SQLException {
        assertNull(repository.buscarPorNome("Não existe"));
    }

    @Test
    void migracaoV24RemoveDescontoEConverteQuilosParaKg(@TempDir Path tempDir) throws Exception {
        String url = "jdbc:sqlite:" + tempDir.resolve("produtos-v23.db");
        Flyway.configure().dataSource(url, "", "")
                .locations("classpath:flyway_migrations").target("23").load().migrate();
        try (var connection = DriverManager.getConnection(url);
             var statement = connection.prepareStatement("""
                     INSERT INTO produtos
                     (nome, unidade, observacoes, ativo, dataCriacao, desconto)
                     VALUES (?, 'Quilos', '', 1, CURRENT_TIMESTAMP, 7.5)
                     """)) {
            statement.setString(1, "Milho legado");
            statement.executeUpdate();
        }

        Flyway.configure().dataSource(url, "", "")
                .locations("classpath:flyway_migrations").load().migrate();

        try (var connection = DriverManager.getConnection(url);
             var columns = connection.createStatement().executeQuery("PRAGMA table_info(produtos)")) {
            var nomes = new java.util.HashSet<String>();
            while (columns.next()) nomes.add(columns.getString("name"));
            assertFalse(nomes.contains("desconto"));
        }
        try (var connection = DriverManager.getConnection(url);
             var dados = connection.createStatement()
                     .executeQuery("SELECT unidade FROM produtos WHERE nome = 'Milho legado'")) {
            assertTrue(dados.next());
            assertEquals("Kg", dados.getString("unidade"));
        }
    }
}
