package my_app.db.services;

import my_app.db.models.PreferenciasModel;
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
}
