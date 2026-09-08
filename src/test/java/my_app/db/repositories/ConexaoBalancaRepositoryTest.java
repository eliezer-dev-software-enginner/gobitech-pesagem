package my_app.db.repositories;

import my_app.db.models.ConexaoBalancaModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ConexaoBalancaRepositoryTest extends BaseRepositoryTest {

    private static final Logger log =
            LoggerFactory.getLogger(ConexaoBalancaRepositoryTest.class);

    ConexaoBalancaRepository repository;

    @Override
    protected void initRepository() {
        repository = new ConexaoBalancaRepository(session);
    }

    @BeforeEach
    void cleanConexoes() throws Exception {
        try (var conn = DriverManager.getConnection(testUrl());
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM conexao_balanca");
        }
    }

    private ConexaoBalancaModel novaConexaoSerial() {
        var model = new ConexaoBalancaModel();
        model.setTipoConexao("Serial");
        model.setPortaCom("COM4");
        model.setBaudRate(9600);
        model.setDataCriacao(LocalDateTime.now());
        return model;
    }

    @Test
    void salvar() throws SQLException {
        ConexaoBalancaModel salvo = repository.salvar(novaConexaoSerial());

        log.info("Conexão salva com id={}", salvo.getId());

        assertNotNull(salvo);
        assertNotNull(salvo.getId());
        assertEquals("Serial", salvo.getTipoConexao());
        assertEquals("COM4", salvo.getPortaCom());
    }

    @Test
    void buscarUnico() throws SQLException {
        ConexaoBalancaModel salvo = repository.salvar(novaConexaoSerial());

        ConexaoBalancaModel encontrada = repository.buscarUnico();

        assertNotNull(encontrada);
        assertEquals(salvo.getId(), encontrada.getId());
    }

    @Test
    void atualizar() throws SQLException {
        ConexaoBalancaModel salvo = repository.salvar(novaConexaoSerial());

        salvo.setTipoConexao("TCP");
        salvo.setIpAddress("192.168.0.100");
        salvo.setIpPort(9100);
        repository.atualizar(salvo);

        ConexaoBalancaModel atualizada = repository.buscarById(salvo.getId());

        assertEquals("TCP", atualizada.getTipoConexao());
        assertEquals("192.168.0.100", atualizada.getIpAddress());
    }

    @Test
    void excluirById() throws SQLException {
        ConexaoBalancaModel salvo = repository.salvar(novaConexaoSerial());

        repository.excluirById(salvo.getId());

        assertNull(repository.buscarById(salvo.getId()));
    }
}
