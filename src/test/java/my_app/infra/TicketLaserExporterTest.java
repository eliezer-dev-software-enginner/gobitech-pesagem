package my_app.infra;

import my_app.db.models.PesagemModel;
import org.junit.jupiter.api.Test;

import javax.print.PrintService;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class TicketLaserExporterTest {
    private PesagemModel pesagem() {
        var model = new PesagemModel();
        model.setId(42);
        model.setPlaca("ABC1D23");
        model.setTipoPesagem("manual");
        return model;
    }

    @Test
    void enviaUmaPaginaA4RenderizavelParaImpressoraPadraoSemDialogo() {
        PrintService impressora = (PrintService) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{PrintService.class},
                (proxy, method, args) -> { throw new AssertionError("Consulta inesperada: " + method); });
        var job = new JobTeste();
        var exporter = new TicketLaserExporter(() -> impressora, () -> job);

        assertTrue(exporter.imprimir(null, pesagem(), null));
        assertSame(impressora, job.impressora);
        assertEquals("Ticket de pesagem 42", job.nome);
        assertEquals(1, job.envios);
    }

    @Test
    void semImpressoraNaoCriaJob() {
        var exporter = new TicketLaserExporter(() -> null, () -> {
            throw new AssertionError("Não deve criar job sem impressora");
        });
        assertFalse(exporter.imprimir(null, pesagem(), null));
    }

    @Test
    void erroNoSpoolerNaoRetornaSucesso() {
        PrintService impressora = (PrintService) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{PrintService.class}, (p, m, args) -> null);
        var job = new JobTeste() {
            @Override
            public void print() throws PrinterException {
                throw new PrinterException("Spooler indisponível");
            }
        };
        assertFalse(new TicketLaserExporter(() -> impressora, () -> job).imprimir(null, pesagem(), null));
    }

    private static class JobTeste extends PrinterJob {
        PrintService impressora;
        Pageable documento;
        String nome;
        int envios;

        @Override public void setPrintService(PrintService service) { impressora = service; }
        @Override public void setPageable(Pageable document) { documento = document; }
        @Override public void setJobName(String jobName) { nome = jobName; }
        @Override public String getJobName() { return nome; }

        @Override
        public void print() throws PrinterException {
            assertEquals(1, documento.getNumberOfPages());
            var formato = documento.getPageFormat(0);
            assertEquals(595, formato.getWidth(), 1);
            assertEquals(842, formato.getHeight(), 1);
            var imagem = new BufferedImage(600, 850, BufferedImage.TYPE_INT_RGB);
            var graphics = imagem.createGraphics();
            try {
                assertEquals(Printable.PAGE_EXISTS, documento.getPrintable(0).print(graphics, formato, 0));
            } finally {
                graphics.dispose();
            }
            envios++;
        }

        @Override public boolean printDialog() { throw new AssertionError("Não deve abrir diálogo"); }
        @Override public PageFormat pageDialog(PageFormat page) { throw new AssertionError("Não deve abrir diálogo"); }
        @Override public PageFormat defaultPage(PageFormat page) { return page; }
        @Override public PageFormat validatePage(PageFormat page) { return page; }
        @Override public void setPrintable(Printable painter) { throw new AssertionError(); }
        @Override public void setPrintable(Printable painter, PageFormat format) { throw new AssertionError(); }
        @Override public void setCopies(int copies) { }
        @Override public int getCopies() { return 1; }
        @Override public String getUserName() { return "teste"; }
        @Override public void cancel() { }
        @Override public boolean isCancelled() { return false; }
    }
}
