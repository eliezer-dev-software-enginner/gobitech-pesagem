package my_app.db.services;

import my_app.db.models.ConexaoBalancaModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConexaoBalancaServiceTest extends BaseServiceTest {

    private ConexaoBalancaService conexaoBalancaService;

    @Override
    protected void initService() {
        conexaoBalancaService = new ConexaoBalancaService(session);
    }

    private ConexaoBalancaModel conexaoSerial() {
        var m = new ConexaoBalancaModel();
        m.setTipoConexao("Serial");
        m.setPortaCom("COM4");
        m.setBaudRate(9600);
        return m;
    }

    private ConexaoBalancaModel conexaoTcp() {
        var m = new ConexaoBalancaModel();
        m.setTipoConexao("TCP");
        m.setIpAddress("192.168.0.100");
        m.setIpPort(9100);
        return m;
    }

    @Test
    void deveLancarExcecaoQuandoTipoConexaoVazio() {
        var m = new ConexaoBalancaModel();
        m.setTipoConexao("");
        assertThrows(IllegalArgumentException.class, () -> conexaoBalancaService.salvarOuAtualizar(m));
    }

    @Test
    void deveLancarExcecaoQuandoTipoInvalido() {
        var m = new ConexaoBalancaModel();
        m.setTipoConexao("Bluetooth");
        assertThrows(IllegalArgumentException.class, () -> conexaoBalancaService.salvarOuAtualizar(m));
    }

    @Test
    void deveLancarExcecaoQuandoSerialSemPortaCom() {
        var m = conexaoSerial();
        m.setPortaCom(null);
        assertThrows(IllegalArgumentException.class, () -> conexaoBalancaService.salvarOuAtualizar(m));
    }

    @Test
    void deveLancarExcecaoQuandoSerialSemBaudRate() {
        var m = conexaoSerial();
        m.setBaudRate(null);
        assertThrows(IllegalArgumentException.class, () -> conexaoBalancaService.salvarOuAtualizar(m));
    }

    @Test
    void deveLancarExcecaoQuandoTcpSemIpAddress() {
        var m = conexaoTcp();
        m.setIpAddress(null);
        assertThrows(IllegalArgumentException.class, () -> conexaoBalancaService.salvarOuAtualizar(m));
    }

    @Test
    void deveLancarExcecaoQuandoTcpSemPorta() {
        var m = conexaoTcp();
        m.setIpPort(null);
        assertThrows(IllegalArgumentException.class, () -> conexaoBalancaService.salvarOuAtualizar(m));
    }

    @Test
    void deveSalvarPrimeiraConexaoSerial() throws Exception {
        var salvo = conexaoBalancaService.salvarOuAtualizar(conexaoSerial());
        assertNotNull(salvo.getId());
        assertEquals("COM4", salvo.getPortaCom());
    }

    @Test
    void deveSalvarPrimeiraConexaoTcp() throws Exception {
        var salvo = conexaoBalancaService.salvarOuAtualizar(conexaoTcp());
        assertNotNull(salvo.getId());
        assertEquals("192.168.0.100", salvo.getIpAddress());
    }

    @Test
    void deveAtualizarConexaoExistenteEmVezDeCriarNova() throws Exception {
        conexaoBalancaService.salvarOuAtualizar(conexaoSerial());
        conexaoBalancaService.salvarOuAtualizar(conexaoTcp());

        var unica = conexaoBalancaService.buscarUnico();
        assertEquals("TCP", unica.getTipoConexao());
    }

    @Test
    void deveRetornarNullQuandoNaoHaConexaoConfigurada() throws Exception {
        assertNull(conexaoBalancaService.buscarUnico());
    }
}
