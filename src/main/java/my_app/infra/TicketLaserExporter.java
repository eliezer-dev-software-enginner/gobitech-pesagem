package my_app.infra;

import my_app.db.models.EmpresaModel;
import my_app.db.models.PesagemModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.PrintServiceLookup;
import javax.print.PrintService;
import java.awt.print.PrinterJob;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class TicketLaserExporter {
    private static final Logger log = LoggerFactory.getLogger(TicketLaserExporter.class);
    private final Supplier<PrintService> impressoraPadrao;
    private final Supplier<PrinterJob> criarJob;

    public TicketLaserExporter() {
        this(PrintServiceLookup::lookupDefaultPrintService, PrinterJob::getPrinterJob);
    }

    TicketLaserExporter(Supplier<PrintService> impressoraPadrao, Supplier<PrinterJob> criarJob) {
        this.impressoraPadrao = impressoraPadrao;
        this.criarJob = criarJob;
    }

    public boolean imprimir(EmpresaModel empresa, PesagemModel pesagem, PesagemModel entrada) {
        Path arquivo = null;
        try {
            var impressora = impressoraPadrao.get();
            if (impressora == null) {
                log.warn("Nenhuma impressora padrão configurada para impressão a laser");
                return false;
            }
            arquivo = Files.createTempFile("gobitech-ticket-", ".pdf");
            new TicketPdfExporter().gerar(arquivo.toFile(), empresa, pesagem, entrada);
            try (var documento = PDDocument.load(arquivo.toFile())) {
                var job = criarJob.get();
                job.setPrintService(impressora);
                job.setJobName("Ticket de pesagem " + pesagem.getId());
                job.setPageable(new PDFPageable(documento));
                job.print();
            }
            return true;
        } catch (Exception e) {
            log.error("Erro ao imprimir ticket a laser: pesagemId={}", pesagem.getId(), e);
            return false;
        } finally {
            if (arquivo != null) {
                try {
                    Files.deleteIfExists(arquivo);
                } catch (Exception e) {
                    log.warn("Não foi possível remover o ticket temporário: {}", arquivo, e);
                }
            }
        }
    }
}
