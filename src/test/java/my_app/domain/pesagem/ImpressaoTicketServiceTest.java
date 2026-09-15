package my_app.domain.pesagem;

import my_app.db.models.EmpresaModel;
import my_app.db.models.PesagemModel;
import my_app.db.services.BaseServiceTest;
import my_app.db.services.EmpresaService;
import my_app.db.services.PesagemService;
import my_app.db.services.PreferenciasService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImpressaoTicketServiceTest extends BaseServiceTest {
    private PreferenciasService preferencias;
    private PesagemService pesagens;
    private EmpresaService empresas;
    private ImpressaoTicketService impressao;
    private final List<String> destinos = new ArrayList<>();
    private PesagemModel impresso;
    private PesagemModel entradaImpressa;
    private EmpresaModel empresaImpressa;

    @Override
    protected void initService() {
        preferencias = new PreferenciasService(session);
        pesagens = new PesagemService(session);
        empresas = new EmpresaService(session);
        impressao = new ImpressaoTicketService(preferencias, pesagens, empresas,
                (empresa, pesagem, entrada) -> registrar("termica", empresa, pesagem, entrada),
                (empresa, pesagem, entrada) -> registrar("laser", empresa, pesagem, entrada));
    }

    private boolean registrar(String destino, EmpresaModel empresa, PesagemModel pesagem, PesagemModel entrada) {
        destinos.add(destino);
        impresso = pesagem;
        entradaImpressa = entrada;
        empresaImpressa = empresa;
        return true;
    }

    private PesagemModel novaPesagem(String tipo) throws Exception {
        var pesagem = new PesagemModel();
        pesagem.setPlaca("ABC1D23");
        pesagem.setTipoPesagem(tipo);
        return pesagens.salvar(pesagem);
    }

    @Test
    void imprimeLaserPorPadrao() throws Exception {
        var pesagem = novaPesagem("manual");
        impressao.imprimir(pesagem.getId());
        assertEquals(List.of("laser"), destinos);
        assertEquals(pesagem.getId(), impresso.getId());
        assertNull(entradaImpressa);
    }

    @Test
    void consultaConfiguracaoAtualACadaImpressao() throws Exception {
        var pesagem = novaPesagem("avulsa");
        preferencias.salvarTipoImpressao(TipoImpressao.TERMICA);
        impressao.imprimir(pesagem.getId());
        preferencias.salvarTipoImpressao(TipoImpressao.LASER);
        impressao.imprimir(pesagem.getId());
        assertEquals(List.of("termica", "laser"), destinos);
    }

    @Test
    void enviaEmpresaEEntradaVinculadaComDadosRecarregados() throws Exception {
        var empresa = new EmpresaModel();
        empresa.setNome("Balança de teste");
        empresas.salvarOuAtualizar(empresa);
        var entrada = novaPesagem("entrada");
        var saida = novaPesagem("saida");
        saida.setEntradaId(entrada.getId());
        saida.setMotoristaNome("Motorista atualizado");
        pesagens.atualizar(saida);
        preferencias.salvarTipoImpressao(TipoImpressao.TERMICA);

        impressao.imprimir(saida.getId());

        assertEquals(List.of("termica"), destinos);
        assertEquals("Motorista atualizado", impresso.getMotoristaNome());
        assertEquals(entrada.getId(), entradaImpressa.getId());
        assertEquals("Balança de teste", empresaImpressa.getNome());
    }

    @Test
    void naoImprimePesagemExcluida() {
        assertThrows(IllegalArgumentException.class, () -> impressao.imprimir(9999));
        assertTrue(destinos.isEmpty());
    }

    @Test
    void falhaNaTermicaNaoEnviaParaLaser() throws Exception {
        var pesagem = novaPesagem("entrada");
        preferencias.salvarTipoImpressao(TipoImpressao.TERMICA);
        var comFalha = new ImpressaoTicketService(preferencias, pesagens, empresas,
                (e, p, entrada) -> false,
                (e, p, entrada) -> registrar("laser", e, p, entrada));
        assertThrows(IllegalArgumentException.class, () -> comFalha.imprimir(pesagem.getId()));
        assertTrue(destinos.isEmpty());
    }
}
