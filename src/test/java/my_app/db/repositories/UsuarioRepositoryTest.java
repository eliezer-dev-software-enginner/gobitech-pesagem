package my_app.db.repositories;

import my_app.db.models.UsuarioModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioRepositoryTest extends BaseRepositoryTest {

    private static final Logger log =
            LoggerFactory.getLogger(UsuarioRepositoryTest.class);

    UsuarioRepository repository;

    @Override
    protected void initRepository() {
        repository = new UsuarioRepository(session);
    }

    @BeforeEach
    void cleanUsuarios() throws Exception {
        try (var conn = DriverManager.getConnection("jdbc:sqlite:file:testdb?mode=memory&cache=shared");
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM usuarios");
        }
    }

    private UsuarioModel novoUsuario(String login) {
        var model = new UsuarioModel();
        model.setLogin(login);
        model.setSenha("1234");
        model.setNome("Usuário " + login);
        model.setAtivo(true);
        model.setAdmin(false);
        model.setDataCriacao(LocalDateTime.now());
        return model;
    }

    @Test
    void salvar() throws SQLException {
        UsuarioModel salvo = repository.salvar(novoUsuario("maria"));

        log.info("Usuário salvo com id={}", salvo.getId());

        assertNotNull(salvo);
        assertNotNull(salvo.getId());
        assertEquals("maria", salvo.getLogin());
    }

    @Test
    void listar() throws SQLException {
        repository.salvar(novoUsuario("joao"));

        var lista = repository.listar();

        assertNotNull(lista);
        assertFalse(lista.isEmpty());
    }

    @Test
    void atualizar() throws SQLException {
        UsuarioModel salvo = repository.salvar(novoUsuario("original"));

        salvo.setNome("Nome Atualizado");
        repository.atualizar(salvo);

        UsuarioModel atualizado = repository.buscarById(salvo.getId());

        assertEquals("Nome Atualizado", atualizado.getNome());
    }

    @Test
    void excluirById() throws SQLException {
        UsuarioModel salvo = repository.salvar(novoUsuario("excluir"));

        repository.excluirById(salvo.getId());

        assertNull(repository.buscarById(salvo.getId()));
    }

    @Test
    void buscarById() throws SQLException {
        UsuarioModel salvo = repository.salvar(novoUsuario("busca"));

        UsuarioModel encontrado = repository.buscarById(salvo.getId());

        assertNotNull(encontrado);
        assertEquals("busca", encontrado.getLogin());
    }

    @Test
    void buscarPorLogin() throws SQLException {
        repository.salvar(novoUsuario("gestor2"));

        UsuarioModel encontrado = repository.buscarPorLogin("gestor2");

        assertNotNull(encontrado);
        assertEquals("gestor2", encontrado.getLogin());
    }

    @Test
    void buscarPorLoginInexistente() throws SQLException {
        assertNull(repository.buscarPorLogin("não existe"));
    }

    @Test
    void listarAtivosNaoRetornaInativos() throws SQLException {
        var ativo = repository.salvar(novoUsuario("ativo"));
        var inativo = novoUsuario("inativo");
        inativo.setAtivo(false);
        repository.salvar(inativo);

        var lista = repository.listarAtivos();

        assertTrue(lista.stream().anyMatch(it -> it.getId().equals(ativo.getId())));
        assertTrue(lista.stream().noneMatch(it -> it.getLogin().equals("inativo")));
    }
}
