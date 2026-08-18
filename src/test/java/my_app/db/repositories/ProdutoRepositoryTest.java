package my_app.db.repositories;

import my_app.db.models.ProdutoModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;

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
        try (var conn = DriverManager.getConnection("jdbc:sqlite:file:testdb?mode=memory&cache=shared");
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
}
