package my_app.db.repositories;

import my_app.db.models.ConexaoCameraModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ConexaoCameraRepositoryTest extends BaseRepositoryTest {

    private static final Logger log =
            LoggerFactory.getLogger(ConexaoCameraRepositoryTest.class);

    ConexaoCameraRepository repository;

    @Override
    protected void initRepository() {
        repository = new ConexaoCameraRepository(session);
    }

    @BeforeEach
    void cleanConexoes() throws Exception {
        try (var conn = DriverManager.getConnection(testUrl());
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM conexao_camera");
        }
    }

    private ConexaoCameraModel novaConexao() {
        var model = new ConexaoCameraModel();
        model.setFrenteIp("192.168.0.101");
        model.setFrentePorta(80);
        model.setFrenteCanal(1);
        model.setFrenteUsuario("admin");
        model.setFrenteSenha("senha123");
        model.setDataCriacao(LocalDateTime.now());
        return model;
    }

    @Test
    void salvar() throws SQLException {
        ConexaoCameraModel salvo = repository.salvar(novaConexao());

        log.info("Conexão de câmera salva com id={}", salvo.getId());

        assertNotNull(salvo);
        assertNotNull(salvo.getId());
        assertEquals("192.168.0.101", salvo.getFrenteIp());
        assertEquals(80, salvo.getFrentePorta());
    }

    @Test
    void buscarUnico() throws SQLException {
        ConexaoCameraModel salvo = repository.salvar(novaConexao());

        ConexaoCameraModel encontrada = repository.buscarUnico();

        assertNotNull(encontrada);
        assertEquals(salvo.getId(), encontrada.getId());
    }

    @Test
    void atualizar() throws SQLException {
        ConexaoCameraModel salvo = repository.salvar(novaConexao());

        salvo.setCostasIp("192.168.0.102");
        salvo.setCostasPorta(80);
        repository.atualizar(salvo);

        ConexaoCameraModel atualizada = repository.buscarById(salvo.getId());

        assertEquals("192.168.0.102", atualizada.getCostasIp());
    }

    @Test
    void excluirById() throws SQLException {
        ConexaoCameraModel salvo = repository.salvar(novaConexao());

        repository.excluirById(salvo.getId());

        assertNull(repository.buscarById(salvo.getId()));
    }
}
