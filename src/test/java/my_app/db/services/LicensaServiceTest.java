package my_app.db.services;

import my_app.db.models.LicensaModel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LicensaServiceTest extends BaseServiceTest {

    private LicensaService licensaService;

    @Override
    protected void initService() {
        licensaService = new LicensaService(session);
    }

    @Test
    void deveLancarExcecaoQuandoValorVazio() {
        var l = new LicensaModel();
        l.setValor("");
        assertThrows(IllegalArgumentException.class, () -> licensaService.salvar(l));
    }

    @Test
    void deveLancarExcecaoQuandoValorDuplicado() throws Exception {
        var l1 = new LicensaModel();
        l1.setValor("MESMO-VALOR");
        licensaService.salvar(l1);

        var l2 = new LicensaModel();
        l2.setValor("MESMO-VALOR");
        assertThrows(IllegalArgumentException.class, () -> licensaService.salvar(l2));
    }

    @Test
    void deveGerarNovaLicensaSemExpiracao() throws Exception {
        var licensa = licensaService.gerarNova(null);
        assertNotNull(licensa.getId());
        assertNotNull(licensa.getValor());
        assertNull(licensa.getExpiraEm());
        assertFalse(licensa.expirada());
    }

    @Test
    void deveGerarNovaLicensaComExpiracaoFutura() throws Exception {
        var licensa = licensaService.gerarNova(LocalDateTime.now().plusDays(30));
        assertFalse(licensa.expirada());
    }

    @Test
    void licensaComExpiracaoNoPassadoDeveEstarExpirada() throws Exception {
        var licensa = licensaService.gerarNova(LocalDateTime.now().minusDays(1));
        assertTrue(licensa.expirada());
    }

    @Test
    void deveBuscarMaisRecente() throws Exception {
        var maisAntiga = licensaService.gerarNova(null);
        // dataCriacao é epoch-ms (inteiro): duas licenças no mesmo milissegundo empatariam
        // na ordenação. Em vez de Thread.sleep (frágil), refaz até ter timestamp maior.
        LicensaModel maisNova = maisAntiga;
        for (int tentativas = 0; tentativas < 1000 && maisNova.getDataCriacao().equals(maisAntiga.getDataCriacao()); tentativas++) {
            maisNova = licensaService.gerarNova(null);
        }
        assertFalse(maisNova.getDataCriacao().equals(maisAntiga.getDataCriacao()), "não conseguiu gerar timestamp distinto");

        var encontrada = licensaService.buscarMaisRecente();
        assertEquals(maisNova.getValor(), encontrada.getValor());
    }

    @Test
    void deveRetornarNullQuandoNaoHaLicensa() throws Exception {
        assertNull(licensaService.buscarMaisRecente());
    }

    @Test
    void deveValidarLicensaAtiva() throws Exception {
        var licensa = licensaService.gerarNova(LocalDateTime.now().plusDays(1));
        assertTrue(licensaService.validar(licensa.getValor()));
    }

    @Test
    void naoDeveValidarLicensaExpirada() throws Exception {
        var licensa = licensaService.gerarNova(LocalDateTime.now().minusDays(1));
        assertFalse(licensaService.validar(licensa.getValor()));
    }

    @Test
    void naoDeveValidarLicensaInexistente() throws Exception {
        assertFalse(licensaService.validar("não existe"));
    }
}
