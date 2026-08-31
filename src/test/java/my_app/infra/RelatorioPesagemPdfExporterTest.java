package my_app.infra;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelatorioPesagemPdfExporterTest {

    private String extrairTexto(File pdf) throws IOException {
        try (var doc = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void geraPdfComCabecalhosLinhasERodape(@TempDir Path tempDir) throws IOException {
        var headers = List.of("Ticket", "Tara (Kg)", "Entrada", "Horário", "Saída", "Horário",
                "Placa", "Produto", "Cliente", "Peso bruto", "Peso líquido");
        var rows = List.of(
                List.of("2", "8500", "27/09/2025", "10:49:31", "27/09/2025", "10:49:31",
                        "RRR", "Soja", "CARGIL", "74000", "64000")
        );
        var rodape = List.of(
                "Observação (todas as linhas): ----",
                "Quantidade total entradas: 1",
                "Total peso líquido: 64000"
        );

        var destino = tempDir.resolve("relatorio.pdf").toFile();
        RelatorioPesagemPdfExporter.exportar(destino, "Relatório resumo de entradas e saídas",
                headers, rows, rodape);

        assertTrue(destino.exists());
        assertTrue(destino.length() > 0);

        String texto = extrairTexto(destino);
        assertTrue(texto.contains("Relatório resumo de entradas e saídas"));
        assertTrue(texto.contains("Tara (Kg)"));
        assertTrue(texto.contains("Peso líquido"));
        assertTrue(texto.contains("Observação (todas as linhas): ----"));
        assertTrue(texto.contains("Quantidade total entradas: 1"));
        assertTrue(texto.contains("Total peso líquido: 64000"));
    }

    @Test
    void funcionaSemRodapeNemLinhas(@TempDir Path tempDir) throws IOException {
        var destino = tempDir.resolve("relatorio_vazio.pdf").toFile();
        assertDoesNotThrow(() -> RelatorioPesagemPdfExporter.exportar(
                destino, "Relatório", List.of("A", "B"), List.of()));
        assertTrue(destino.exists());
    }

    @Test
    void quebraPaginaComMuitasLinhasSemFecharStream(@TempDir Path tempDir) throws IOException {
        var headers = List.of("Ticket", "Placa");
        var rows = new java.util.ArrayList<List<String>>();
        for (int i = 1; i <= 100; i++) {
            rows.add(List.of(String.valueOf(i), "PLACA" + i));
        }

        var destino = tempDir.resolve("relatorio_grande.pdf").toFile();
        assertDoesNotThrow(() -> RelatorioPesagemPdfExporter.exportar(
                destino, "Relatório grande", headers, rows));

        try (var doc = PDDocument.load(destino)) {
            assertTrue(doc.getNumberOfPages() > 1);
        }
    }
}
