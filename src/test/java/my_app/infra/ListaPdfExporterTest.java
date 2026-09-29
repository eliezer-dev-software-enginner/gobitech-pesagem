package my_app.infra;

import my_app.db.models.EmpresaModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListaPdfExporterTest {

    @Test
    void exportacaoDeProdutosCarregaLogomarcaDaEmpresaDoClasspath() throws Exception {
        var empresa = new EmpresaModel();
        empresa.setNome("BALANÇAS GOBITECH");
        empresa.setLogomarca("assets/app_banner_square.png");
        empresa.setCpfCnpj("27337390000146");
        empresa.setRua("R. 10 QD 36");
        empresa.setNumero("14");
        empresa.setBairro("VILA VERDE");
        empresa.setCidade("FORMOSA");
        empresa.setEstado("GO");
        empresa.setTelefone("61996532857");

        var pasta = Path.of("build", "reports", "printing");
        Files.createDirectories(pasta);
        var destino = pasta.resolve("produtos-com-logomarca.pdf").toFile();

        ListaPdfExporter.exportar(
                destino,
                empresa,
                "Lista de Produtos",
                List.of("ID", "Nome", "Unidade", "Data de criacao"),
                List.of(List.of("1", "SOJA", "Kg", "29/09/2026 10:00"))
        );

        assertTrue(destino.isFile());
        assertTrue(destino.length() > 0);
        try (var documento = PDDocument.load(destino)) {
            assertEquals(1, documento.getNumberOfPages());
            int imagens = 0;
            for (var nome : documento.getPage(0).getResources().getXObjectNames()) {
                if (documento.getPage(0).getResources().isImageXObject(nome)) imagens++;
            }
            assertEquals(1, imagens, "A exportação deve incorporar a logomarca da empresa");

            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("BALANÇAS GOBITECH"));
            assertTrue(texto.contains("Tel: (61) 99653-2857"));
            assertTrue(texto.contains("Lista de Produtos"));
            assertTrue(texto.contains("SOJA"));
        }
    }
}
