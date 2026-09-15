package my_app.db.services;

import my_app.db.models.PreferenciasModel;
import my_app.domain.pesagem.TipoImpressao;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PreferenciasServiceTest extends BaseServiceTest {

    private PreferenciasService preferenciasService;

    @Override
    protected void initService() {
        preferenciasService = new PreferenciasService(session);
    }

    private PreferenciasModel modelBase() {
        var model = new PreferenciasModel();
        model.setPrimeiroAcesso(1);
        model.setDataCriacao(LocalDateTime.now());
        return model;
    }

    @Test
    void deveSalvar() throws Exception {
        var salvo = preferenciasService.salvar(modelBase());
        assertNotNull(salvo.getId());
        assertTrue(salvo.isFirstAccess());
    }

    @Test
    void deveAtualizarMarcandoPrimeiroAcessoConcluido() throws Exception {
        var salvo = preferenciasService.salvar(modelBase());
        salvo.setPrimeiroAcesso(0);
        preferenciasService.atualizar(salvo);

        var buscado = preferenciasService.buscarById(salvo.getId());
        assertFalse(buscado.isFirstAccess());
    }

    @Test
    void usaLaserSemConfiguracaoSalva() throws Exception {
        assertEquals(TipoImpressao.LASER, preferenciasService.buscarTipoImpressao());
        assertEquals(0, preferenciasService.count());
    }

    @Test
    void criaConfiguracaoERelêEmOutraSessao() throws Exception {
        preferenciasService.salvarTipoImpressao(TipoImpressao.TERMICA);
        try (var conn = java.sql.DriverManager.getConnection(rawConnection.getMetaData().getURL());
             var service = new PreferenciasService(new net.sf.persism.Session(conn))) {
            assertEquals(TipoImpressao.TERMICA, service.buscarTipoImpressao());
        }
    }

    @Test
    void trocaTipoSemDuplicarOuAlterarPrimeiroAcesso() throws Exception {
        var model = modelBase();
        model.setPrimeiroAcesso(0);
        preferenciasService.salvar(model);
        var antes = preferenciasService.buscarById(model.getId());

        preferenciasService.salvarTipoImpressao(TipoImpressao.TERMICA);
        assertEquals(TipoImpressao.TERMICA, preferenciasService.buscarTipoImpressao());
        preferenciasService.salvarTipoImpressao(TipoImpressao.LASER);

        var depois = preferenciasService.buscarById(model.getId());
        assertEquals("laser", depois.getTipoImpressao());
        assertEquals(antes.getDataCriacao(), depois.getDataCriacao());
        assertEquals(0, depois.getPrimeiroAcesso());
        assertEquals(1, preferenciasService.count());
    }

    @Test
    void rejeitaTipoAusenteOuInvalidoSemAlterarConfiguracao() throws Exception {
        preferenciasService.salvarTipoImpressao(TipoImpressao.TERMICA);
        assertThrows(IllegalArgumentException.class, () -> preferenciasService.salvarTipoImpressao(null));
        var model = preferenciasService.listar().getFirst();
        model.setTipoImpressao("invalido");
        assertThrows(IllegalArgumentException.class, () -> preferenciasService.atualizar(model));
        assertEquals(TipoImpressao.TERMICA, preferenciasService.buscarTipoImpressao());
    }
}
