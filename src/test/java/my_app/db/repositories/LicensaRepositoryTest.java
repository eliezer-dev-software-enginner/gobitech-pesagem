package my_app.db.repositories;

import my_app.db.models.LicensaModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LicensaRepositoryTest extends BaseRepositoryTest {

    private static final Logger log =
            LoggerFactory.getLogger(LicensaRepositoryTest.class);

    LicensaRepository repository;

    @Override
    protected void initRepository() {
        repository = new LicensaRepository(session);
    }

    @BeforeEach
    void cleanLicensas() throws Exception {
        try (var conn = DriverManager.getConnection(testUrl());
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM licensas");
        }
    }

    private LicensaModel novaLicensa(String valor) {
        var model = new LicensaModel();
        model.setValor(valor);
        model.setDataCriacao(LocalDateTime.now());
        return model;
    }

    @Test
    void salvar() throws SQLException {
        LicensaModel salvo = repository.salvar(novaLicensa("ABC-123"));

        log.info("Licença salva com id={}", salvo.getId());

        assertNotNull(salvo);
        assertNotNull(salvo.getId());
        assertEquals("ABC-123", salvo.getValor());
    }

    @Test
    void buscarPorValor() throws SQLException {
        repository.salvar(novaLicensa("XYZ-789"));

        LicensaModel encontrada = repository.buscarPorValor("XYZ-789");

        assertNotNull(encontrada);
        assertEquals("XYZ-789", encontrada.getValor());
    }

    @Test
    void buscarPorValorInexistente() throws SQLException {
        assertNull(repository.buscarPorValor("não existe"));
    }

    @Test
    void listarMaisRecentePrimeiro() throws SQLException {
        var primeira = repository.salvar(novaLicensa("PRIMEIRA"));
        var segunda = novaLicensa("SEGUNDA");
        segunda.setDataCriacao(primeira.getDataCriacao().plusMinutes(5));
        repository.salvar(segunda);

        var lista = repository.listarMaisRecentePrimeiro();

        assertEquals("SEGUNDA", lista.getFirst().getValor());
    }

    @Test
    void excluirById() throws SQLException {
        LicensaModel salvo = repository.salvar(novaLicensa("EXCLUIR"));

        repository.excluirById(salvo.getId());

        assertNull(repository.buscarById(salvo.getId()));
    }
}
