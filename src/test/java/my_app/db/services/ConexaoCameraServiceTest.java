package my_app.db.services;

import my_app.db.models.ConexaoCameraModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConexaoCameraServiceTest extends BaseServiceTest {

    private ConexaoCameraService conexaoCameraService;

    @Override
    protected void initService() {
        conexaoCameraService = new ConexaoCameraService(session);
    }

    private ConexaoCameraModel apenasFrente() {
        var m = new ConexaoCameraModel();
        m.setFrenteIp("192.168.0.101");
        m.setFrentePorta(80);
        m.setFrenteCanal(1);
        m.setFrenteUsuario("admin");
        m.setFrenteSenha("senha123");
        return m;
    }

    private ConexaoCameraModel frenteECostas() {
        var m = apenasFrente();
        m.setCostasIp("192.168.0.102");
        m.setCostasPorta(80);
        m.setCostasCanal(1);
        m.setCostasUsuario("admin");
        m.setCostasSenha("senha456");
        return m;
    }

    @Test
    void deveSalvarComApenasUmaCamera() throws Exception {
        var salvo = conexaoCameraService.salvarOuAtualizar(apenasFrente());
        assertNotNull(salvo.getId());
        assertEquals("192.168.0.101", salvo.getFrenteIp());
        assertNull(salvo.getCostasIp());
    }

    @Test
    void deveSalvarSemNenhumaCameraConfigurada() throws Exception {
        var salvo = conexaoCameraService.salvarOuAtualizar(new ConexaoCameraModel());
        assertNotNull(salvo.getId());
        assertNull(salvo.getFrenteIp());
        assertNull(salvo.getCostasIp());
    }

    @Test
    void deveSalvarComAsDuasCameras() throws Exception {
        var salvo = conexaoCameraService.salvarOuAtualizar(frenteECostas());
        assertEquals("192.168.0.101", salvo.getFrenteIp());
        assertEquals("192.168.0.102", salvo.getCostasIp());
    }

    @Test
    void deveLancarExcecaoQuandoFrenteTemIpSemPorta() {
        var m = apenasFrente();
        m.setFrentePorta(null);
        assertThrows(IllegalArgumentException.class, () -> conexaoCameraService.salvarOuAtualizar(m));
    }

    @Test
    void deveLancarExcecaoQuandoCostasTemIpSemPorta() {
        var m = frenteECostas();
        m.setCostasPorta(null);
        assertThrows(IllegalArgumentException.class, () -> conexaoCameraService.salvarOuAtualizar(m));
    }

    @Test
    void deveAtualizarConexaoExistenteEmVezDeCriarNova() throws Exception {
        conexaoCameraService.salvarOuAtualizar(apenasFrente());
        conexaoCameraService.salvarOuAtualizar(frenteECostas());

        var unica = conexaoCameraService.buscarUnico();
        assertEquals("192.168.0.102", unica.getCostasIp());
    }

    @Test
    void deveRetornarNullQuandoNaoHaConexaoConfigurada() throws Exception {
        assertNull(conexaoCameraService.buscarUnico());
    }
}
