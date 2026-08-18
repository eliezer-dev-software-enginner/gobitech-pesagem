package my_app.db.repositories;

import my_app.db.models.PreferenciasModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PreferenciasRepositoryTest extends BaseRepositoryTest {

    private static final Logger log =
            LoggerFactory.getLogger(PreferenciasRepositoryTest.class);

    PreferenciasRepository repository;

    @Override
    protected void initRepository() {
        repository = new PreferenciasRepository(session);
    }

    @BeforeEach
    void cleanPreferencias() throws Exception {
        try (var conn = DriverManager.getConnection("jdbc:sqlite:file:testdb?mode=memory&cache=shared");
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM preferencias");
        }
    }

    private PreferenciasModel novaPreferencia(int primeiroAcesso) {
        var model = new PreferenciasModel();
        model.setPrimeiroAcesso(primeiroAcesso);
        model.setDataCriacao(LocalDateTime.now());
        return model;
    }

    @Test
    void salvar() throws SQLException {
        PreferenciasModel salvo = repository.salvar(novaPreferencia(1));

        log.info("Preferencia salva com id={}", salvo.getId());

        assertNotNull(salvo);
        assertNotNull(salvo.getId());
        assertEquals(1, salvo.getPrimeiroAcesso());
    }

    @Test
    void listar() throws SQLException {
        repository.salvar(novaPreferencia(0));

        var lista = repository.listar();

        assertNotNull(lista);
        assertFalse(lista.isEmpty());
    }

    @Test
    void atualizar() throws SQLException {
        PreferenciasModel salvo = repository.salvar(novaPreferencia(1));

        salvo.setPrimeiroAcesso(0);
        repository.atualizar(salvo);

        PreferenciasModel atualizado = repository.buscarById(salvo.getId());

        assertNotNull(atualizado);
        assertEquals(0, atualizado.getPrimeiroAcesso());
    }

    @Test
    void excluirById() throws SQLException {
        PreferenciasModel salvo = repository.salvar(novaPreferencia(1));

        repository.excluirById(salvo.getId());

        assertNull(repository.buscarById(salvo.getId()));
    }

    @Test
    void buscarById() throws SQLException {
        PreferenciasModel salvo = repository.salvar(novaPreferencia(1));

        PreferenciasModel encontrado = repository.buscarById(salvo.getId());

        assertNotNull(encontrado);
        assertEquals(salvo.getId(), encontrado.getId());
        assertTrue(encontrado.isFirstAccess());
    }
}
