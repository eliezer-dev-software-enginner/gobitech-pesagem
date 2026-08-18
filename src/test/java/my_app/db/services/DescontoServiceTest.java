package my_app.db.services;

import my_app.db.models.DescontoModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DescontoServiceTest extends BaseServiceTest {

    private DescontoService descontoService;

    @Override
    protected void initService() {
        descontoService = new DescontoService(session);
    }

    @Test
    void deveZerarCamposNaoInformados() throws Exception {
        var salvo = descontoService.salvar(new DescontoModel());

        assertNotNull(salvo.getId());
        assertEquals(0, BigDecimal.ZERO.compareTo(salvo.getAvariados()));
        assertEquals(0, BigDecimal.ZERO.compareTo(salvo.getArdidos()));
        assertEquals(0, BigDecimal.ZERO.compareTo(salvo.getQuebraArdidos()));
        assertEquals(0, BigDecimal.ZERO.compareTo(salvo.getImpurezas()));
        assertEquals(0, BigDecimal.ZERO.compareTo(salvo.getQuebraImpurezas()));
        assertEquals(0, BigDecimal.ZERO.compareTo(salvo.getUmidade()));
        assertEquals(0, BigDecimal.ZERO.compareTo(salvo.getQuebraUmidade()));
        assertEquals(0, BigDecimal.ZERO.compareTo(salvo.getOutros()));
    }

    @Test
    void deveManterValoresInformados() throws Exception {
        var model = new DescontoModel();
        model.setUmidade(BigDecimal.valueOf(14.5));
        var salvo = descontoService.salvar(model);

        assertEquals(0, BigDecimal.valueOf(14.5).compareTo(salvo.getUmidade()));
    }

    @Test
    void somaPercentuaisDeveSomarTodosOsCampos() throws Exception {
        var model = new DescontoModel();
        model.setAvariados(BigDecimal.valueOf(2));
        model.setUmidade(BigDecimal.valueOf(3));
        var salvo = descontoService.salvar(model);

        assertEquals(0, BigDecimal.valueOf(5).compareTo(salvo.somaPercentuais()));
    }

    @Test
    void deveZerarCamposNaoInformadosAoAtualizar() throws Exception {
        var salvo = descontoService.salvar(new DescontoModel());

        // atualizar() opera sobre o objeto já existente (com dataCriacao preenchida),
        // igual o resto do app faz — nunca sobre um Model novo em branco.
        salvo.setOutros(BigDecimal.TEN);
        salvo.setAvariados(null);
        descontoService.atualizar(salvo);

        var buscado = descontoService.buscarById(salvo.getId());
        assertEquals(0, BigDecimal.TEN.compareTo(buscado.getOutros()));
        assertEquals(0, BigDecimal.ZERO.compareTo(buscado.getAvariados()));
    }
}
