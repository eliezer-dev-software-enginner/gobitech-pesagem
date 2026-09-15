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
        try (var conn = DriverManager.getConnection(testUrl());
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

    @Test
    void persisteTipoImpressaoAoCriarEAtualizar() throws Exception {
        var model = novaPreferencia(0);
        model.setTipoImpressao("termica");
        repository.salvar(model);
        assertEquals("termica", repository.buscarUnico().getTipoImpressao());
        model.setTipoImpressao("laser");
        repository.atualizar(model);
        assertEquals("laser", repository.buscarById(model.getId()).getTipoImpressao());
    }

    @Test
    void migracaoPreservaPreferenciasExistentesEDefineLaser() throws Exception {
        String url = "jdbc:sqlite:file:testdb-migracao-impressao?mode=memory&cache=shared";
        try (var conn = DriverManager.getConnection(url)) {
            org.flywaydb.core.Flyway.configure().dataSource(url, "", "")
                    .locations("classpath:flyway_migrations").target("19").load().migrate();
            try (var stmt = conn.createStatement()) {
                stmt.executeUpdate("UPDATE preferencias SET primeiro_acesso = 0");
            }
            int id;
            long data;
            try (var stmt = conn.createStatement(); var rs = stmt.executeQuery("SELECT * FROM preferencias")) {
                assertTrue(rs.next());
                id = rs.getInt("id");
                data = rs.getLong("dataCriacao");
            }
            org.flywaydb.core.Flyway.configure().dataSource(url, "", "")
                    .locations("classpath:flyway_migrations").load().migrate();
            try (var sessao = new net.sf.persism.Session(conn)) {
                var repo = new PreferenciasRepository(sessao);
                var preferencias = repo.buscarById(id);
                assertEquals("laser", preferencias.getTipoImpressao());
                assertEquals(0, preferencias.getPrimeiroAcesso());
                assertEquals(1, repo.count());
                try (var stmt = conn.createStatement(); var rs = stmt.executeQuery("SELECT dataCriacao FROM preferencias")) {
                    assertTrue(rs.next());
                    assertEquals(data, rs.getLong(1));
                }
            }
        }
    }
}
