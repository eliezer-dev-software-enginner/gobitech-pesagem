package my_app.infra;

import my_app.db.models.EmpresaModel;
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

    private EmpresaModel empresaBasica() {
        var empresa = new EmpresaModel();
        empresa.setNome("LUIZ NICOLA SOUZA LIMA");
        empresa.setCpfCnpj("71073280853");
        empresa.setInscricaoEstadual("10.123.456-7");
        empresa.setRua("BR 251 KM 33");
        empresa.setBairro("FAZENDA VEREDA");
        empresa.setCidade("CRISTALINA");
        empresa.setEstado("GO");
        empresa.setTelefone("61-986338894");
        return empresa;
    }

    @Test
    void geraRelatorioNoFormatoDoAndre(@TempDir Path tempDir) throws IOException {
        var headers = List.of("Ticket", "Tara (Kg)", "Entrada", "Horário", "Saída", "Horário",
                "Placa", "Produto", "Cliente", "Peso bruto", "Peso líquido");
        var rows = List.of(
                List.of("1", "19500", "13/08/2026", "09:58:35", "13/08/2026", "09:58:35",
                        "KGY1G25", "SORGO", "COFCO INTERNATIONAL BRASIL", "58500", "39060"),
                List.of("2", "26740", "13/08/2026", "13:28:37", "13/08/2026", "13:28:37",
                        "FUPOD43", "SORGO", "COFCO INTERNATIONAL BRASIL", "74040", "47300")
        );
        var obs = List.of("---", "---");

        var destino = tempDir.resolve("relatorio.pdf").toFile();
        RelatorioPesagemPdfExporter.exportar(destino, empresaBasica(), "Relatório resumo de entradas e saídas",
                headers, rows, obs, 2, "86360");

        assertTrue(destino.exists());
        assertTrue(destino.length() > 0);

        String texto = extrairTexto(destino);
        assertTrue(texto.contains("LUIZ NICOLA SOUZA LIMA"));
        assertTrue(texto.contains("Cpf:") && texto.contains("Insc.e"));
        assertTrue(texto.contains("10.123.456-7"));
        assertTrue(texto.contains("End: BR 251 KM 33"));
        assertTrue(texto.contains("Bairro: FAZENDA VEREDA"));
        assertTrue(texto.contains("Cidade: CRISTALINA - GO"));
        assertTrue(texto.contains("Relatório resumo de entradas e saídas"));
        assertTrue(texto.contains("Ticket"));
        assertTrue(texto.contains("Tara (Kg)"));
        assertTrue(texto.contains("Peso bruto"));
        assertTrue(texto.contains("Peso líquido"));
        assertTrue(texto.contains("KGY1G25"));
        assertTrue(texto.contains("Observação: ---"));
        assertTrue(texto.contains("Quantidade total entradas"));
        assertTrue(texto.contains("Total peso liquido"));
        assertTrue(texto.contains("86360"));
    }

    @Test
    void observacaoDaLinhaEhExibida(@TempDir Path tempDir) throws IOException {
        var headers = List.of("Ticket", "Placa");
        var rows = List.of(List.of("1", "ABC1234"));
        var obs = List.of("Pesou duas vezes");

        var destino = tempDir.resolve("relatorio_obs.pdf").toFile();
        RelatorioPesagemPdfExporter.exportar(destino, null, "Relatório",
                headers, rows, obs, 1, "100");

        String texto = extrairTexto(destino);
        assertTrue(texto.contains("Observação: Pesou duas vezes"));
    }

    @Test
    void quebraPaginaComMuitasLinhas(@TempDir Path tempDir) throws IOException {
        var headers = List.of("Ticket", "Placa");
        var rows = new java.util.ArrayList<List<String>>();
        var obs = new java.util.ArrayList<String>();
        for (int i = 1; i <= 100; i++) {
            rows.add(List.of(String.valueOf(i), "PLACA" + i));
            obs.add("---");
        }

        var destino = tempDir.resolve("relatorio_grande.pdf").toFile();
        assertDoesNotThrow(() -> RelatorioPesagemPdfExporter.exportar(
                destino, null, "Relatório grande", headers, rows, obs, 100, "100"));

        try (var doc = PDDocument.load(destino)) {
            assertTrue(doc.getNumberOfPages() > 1);
        }
    }

    @Test
    void usaLogomarcaPadraoQuandoCadastroExibeFallbackMasBancoEstaNulo(@TempDir Path tempDir) throws IOException {
        var empresa = new EmpresaModel();
        empresa.setNome("BALANÇAS GOBITECH");
        assertNull(empresa.getLogomarca(), "Reproduz o banco recém-criado, sem logo persistida");

        var destino = tempDir.resolve("relatorio-logomarca.pdf").toFile();
        RelatorioPesagemPdfExporter.exportar(destino, empresa, "Relatório",
                List.of("Ticket", "Placa"), List.of(List.of("1", "ABC1234")),
                List.of("---"), 1, "100");

        try (var doc = PDDocument.load(destino)) {
            int imagens = 0;
            for (var nome : doc.getPage(0).getResources().getXObjectNames()) {
                if (doc.getPage(0).getResources().isImageXObject(nome)) imagens++;
            }
            assertEquals(1, imagens, "O relatório deve carregar a mesma logomarca do ticket");

            var render = new org.apache.pdfbox.rendering.PDFRenderer(doc).renderImageWithDPI(0, 72);
            assertTrue(temPixelsColoridos(render, 430, 20, 575, 90), "A logo deve sair no topo à direita");
        }

        assertTrue(extrairTexto(destino).contains("BALANÇAS GOBITECH"), "A logo não pode empurrar o cabeçalho para fora");
    }

    private boolean temPixelsColoridos(java.awt.image.BufferedImage imagem,
                                       int xInicial, int yInicial, int xFinal, int yFinal) {
        int coloridos = 0;
        for (int y = yInicial; y < Math.min(yFinal, imagem.getHeight()); y++) {
            for (int x = xInicial; x < Math.min(xFinal, imagem.getWidth()); x++) {
                int rgb = imagem.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                if (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > 20) coloridos++;
            }
        }
        return coloridos > 200;
    }
}
