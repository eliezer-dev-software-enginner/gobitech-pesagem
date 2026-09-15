package my_app.infra;

import my_app.db.models.ClienteModel;
import my_app.db.models.EmpresaModel;
import my_app.db.models.PesagemModel;
import my_app.db.models.ProdutoModel;
import my_app.db.models.UsuarioModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TicketPdfExporterTest {

    private final TicketPdfExporter exporter = new TicketPdfExporter();

    private PesagemModel pesagemBasica() {
        var pesagem = new PesagemModel();
        pesagem.setId(1);
        pesagem.setPlaca("RED1234");
        pesagem.setMotoristaNome(null);
        pesagem.setTipoPesagem("saida");
        pesagem.setPesoVeiculo(new BigDecimal("2540.00"));
        pesagem.setPesoTotal(new BigDecimal("9980.00"));
        pesagem.setPesoFinal(new BigDecimal("7440.00"));
        pesagem.setDataCriacao(LocalDateTime.of(2025, 8, 17, 14, 15, 56));

        var usuario = new UsuarioModel();
        usuario.setNome("ADMINISTRADOR");
        pesagem.setUsuario(usuario);

        return pesagem;
    }

    private PesagemModel entradaBasica() {
        var entrada = new PesagemModel();
        entrada.setId(1);
        entrada.setPesoTotal(new BigDecimal("2540.00"));
        entrada.setDataCriacao(LocalDateTime.of(2025, 8, 17, 14, 15, 7));
        return entrada;
    }

    private String extrairTexto(File pdf) throws IOException {
        try (var doc = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(doc).replaceAll("[ \t]+", " ");
        }
    }

    @Test
    void geraTicketNoFormatoDoAndreComDuasViasNaMesmaFolha(@TempDir Path tempDir) throws IOException {
        var empresa = new EmpresaModel();
        empresa.setNome("BALANÇAS GOBITECH");
        empresa.setCpfCnpj("12345678000199");
        empresa.setInscricaoEstadual("10.123.456-7");
        empresa.setCidade("FORMOSA");
        empresa.setEstado("GO");
        empresa.setTelefone("61-99653-2857");

        var destino = tempDir.resolve("ticket.pdf").toFile();
        exporter.gerar(destino, empresa, pesagemBasica(), entradaBasica());

        assertTrue(destino.exists());
        assertTrue(destino.length() > 0);

        // As duas vias ficam na MESMA folha (uma página única)
        try (var doc = PDDocument.load(destino)) {
            assertEquals(1, doc.getNumberOfPages());
        }

        String texto = extrairTexto(destino);
        assertTrue(texto.contains("BALANÇAS GOBITECH"));
        assertTrue(texto.contains("Cnpj:"));
        assertTrue(texto.contains("12.345.678/0001-99"),
                "CNPJ deve sair formatado no ticket (12.345.678/0001-99)");
        assertTrue(texto.contains("Insc.est: 10.123.456-7"));
        assertTrue(texto.contains("(61) 99653-2857"),
                "Fone deve sair formatado no ticket ((61) 99653-2857)");
        assertTrue(texto.contains("Cidade: FORMOSA - GO"));
        assertTrue(texto.contains("Ticket de Pesagem"));
        assertTrue(texto.contains("Nº: 1"));
        assertTrue(texto.contains("Placa: RED1234"));
        assertTrue(texto.contains("Data Entrada: 17/08/2025"));
        assertTrue(texto.contains("Data saida: 17/08/2025"));
        assertTrue(texto.contains("Operador: ADMINISTRADOR"));
        assertTrue(texto.contains("Motorista: ---"));
        assertTrue(texto.contains("Produto: ---"));
        assertTrue(texto.contains("Peso entrada: 2540 Kg"));
        assertTrue(texto.contains("Peso saida: 9980 Kg"));
        assertTrue(texto.contains("Peso liquido inicial: 7440 Kg"));

        // Duas vias, com rótulos fixos Operador/Motorista nas assinaturas.
        assertEquals(2, ocorrencias(texto, "Ticket de Pesagem"));
        assertEquals(2, ocorrencias(texto, "ADMINISTRADOR"));
        assertEquals(4, ocorrencias(texto, "Motorista"));

        // Rótulos das assinaturas como na referência.
        assertEquals(4, ocorrencias(texto, "Operador"));
        assertTrue(texto.contains("Descontos aplicados ao produto"));
        assertTrue(texto.contains("Nenhum desconto aplicado."));
    }

    @Test
    void funcionaSemEmpresaSemEntradaESemRelacoes(@TempDir Path tempDir) throws IOException {
        var destino = tempDir.resolve("ticket_sem_empresa.pdf").toFile();
        assertDoesNotThrow(() -> exporter.gerar(destino, null, pesagemBasica(), null));

        assertTrue(destino.exists());
        String texto = extrairTexto(destino);
        assertTrue(texto.contains("Gobitech"));
        assertTrue(texto.contains("RED1234"));
        assertTrue(texto.contains("---"));
    }

    private int ocorrencias(String texto, String fragmento) {
        int count = 0, idx = 0;
        while ((idx = texto.indexOf(fragmento, idx)) != -1) {
            count++;
            idx += fragmento.length();
        }
        return count;
    }

    @Test
    void entradaComTaraEDescontosCabemNasDuasViasSemFornecedor() throws Exception {
        var pesagem = pesagemBasica();
        pesagem.setPesoVeiculo(new BigDecimal("8500"));
        pesagem.setPesoTotal(new BigDecimal("32000"));
        pesagem.setPesoFinal(new BigDecimal("22325"));
        var desconto = new my_app.db.models.DescontoModel();
        desconto.setArdidos(new BigDecimal("2"));
        desconto.setImpurezas(new BigDecimal("3"));
        pesagem.setDesconto(desconto);
        var entrada = entradaBasica();
        entrada.setPesoTotal(BigDecimal.ZERO);
        entrada.setPesoVeiculo(new BigDecimal("8500"));

        var pasta = Path.of("build/reports/printing");
        java.nio.file.Files.createDirectories(pasta);
        var destino = pasta.resolve("ticket-descontos.pdf").toFile();
        exporter.gerar(destino, null, pesagem, entrada);
        var texto = extrairTexto(destino);
        assertFalse(texto.contains("Fornecedor"));
        assertEquals(2, ocorrencias(texto, "Peso entrada: 8500 Kg"));
        assertEquals(2, ocorrencias(texto, "Total descontado: 1175 Kg"));
        assertEquals(2, ocorrencias(texto, "Peso liquido final: 22325 Kg"));
        assertFalse(texto.contains("QUEBRA IMPUREZAS"));
        assertTrue(texto.contains("% Classificado"));
        assertTrue(texto.contains("% Aplicado"));
        assertTrue(texto.contains("ARDIDOS"));
        assertTrue(texto.contains("IMPUREZAS"));
        assertTrue(texto.contains("470"));
        assertTrue(texto.contains("705"));
        try (var doc = PDDocument.load(destino)) {
            assertEquals(1, doc.getNumberOfPages());
            var stripper = new PDFTextStripper() {
                @Override
                protected void processTextPosition(org.apache.pdfbox.text.TextPosition pos) {
                    assertTrue(pos.getYDirAdj() > 0 && pos.getYDirAdj() < 822, "Texto fora da altura útil");
                    assertTrue(pos.getXDirAdj() >= 23 && pos.getXDirAdj() + pos.getWidthDirAdj() <= 572,
                            "Texto fora da largura útil");
                    super.processTextPosition(pos);
                }
            };
            stripper.getText(doc);
            var imagem = new org.apache.pdfbox.rendering.PDFRenderer(doc).renderImageWithDPI(0, 120);
            javax.imageio.ImageIO.write(imagem, "png", pasta.resolve("ticket-descontos.png").toFile());
        }
    }

    @Test
    void ticketDaPropriaEntradaPreencheEntradaSemInventarSaida(@TempDir Path dir) throws Exception {
        var entrada = entradaBasica();
        entrada.setTipoPesagem("entrada");
        entrada.setPesoTotal(BigDecimal.ZERO);
        entrada.setPesoVeiculo(new BigDecimal("8500"));
        var destino = dir.resolve("entrada.pdf").toFile();
        exporter.gerar(destino, null, entrada, null);
        var texto = extrairTexto(destino);
        assertTrue(texto.contains("Peso entrada: 8500 Kg"));
        assertTrue(texto.contains("Data Entrada: 17/08/2025"));
        assertFalse(texto.contains("Data saida: 17/08/2025"));
    }

    @Test
    void reproduzColunasETipografiaDaReferencia() throws Exception {
        var p = pesagemBasica();
        p.setId(5);
        p.setPlaca("TTT1T11");
        p.setMotoristaNome("LUIZ");
        p.setPesoVeiculo(new BigDecimal("1000"));
        p.setPesoTotal(new BigDecimal("2020"));
        p.setPesoFinal(new BigDecimal("867"));
        p.setDataCriacao(LocalDateTime.of(2026, 9, 15, 10, 17, 16));
        var produto = new ProdutoModel();
        produto.setNome("MILHO");
        p.setProduto(produto);
        var descontos = new my_app.db.models.DescontoModel();
        descontos.setImpurezas(new BigDecimal("10"));
        descontos.setUmidade(new BigDecimal("5"));
        p.setDesconto(descontos);
        var entrada = entradaBasica();
        entrada.setPesoTotal(new BigDecimal("1000"));
        entrada.setDataCriacao(LocalDateTime.of(2026, 9, 15, 10, 16, 7));
        var empresa = new EmpresaModel();
        empresa.setNome("BALANÇAS GOBITECH");
        empresa.setCidade("FORMOSA");
        empresa.setEstado("GO");
        empresa.setTelefone("61996532857");
        var pasta = Path.of("build/reports/printing");
        java.nio.file.Files.createDirectories(pasta);
        var destino = pasta.resolve("ticket-layout-andre.pdf").toFile();
        exporter.gerar(destino, empresa, p, entrada);
        try (var doc = PDDocument.load(destino)) {
            var titulos = new java.util.ArrayList<org.apache.pdfbox.text.TextPosition>();
            var stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, java.util.List<org.apache.pdfbox.text.TextPosition> positions) throws IOException {
                    if (text.contains("Descontos aplicados ao produto")) titulos.add(positions.getFirst());
                    super.writeString(text, positions);
                }
            };
            var texto = stripper.getText(doc);
            assertEquals(2, titulos.size());
            for (var titulo : titulos) {
                assertTrue(titulo.getXDirAdj() > 300, "Tabela deve ficar à direita dos dados");
                assertTrue(titulo.getFont().getName().contains("Bold"));
            }
            assertEquals(2, ocorrencias(texto, "IMPUREZAS"));
            assertEquals(2, ocorrencias(texto, "UMIDADE"));
            assertEquals(2, ocorrencias(texto, "Total descontado: 153 Kg"));
            assertEquals(2, ocorrencias(texto, "Peso liquido inicial: 1020 Kg"));
            assertEquals(2, ocorrencias(texto, "Peso liquido final: 867 Kg"));
            assertFalse(texto.contains("AVARIADOS"));
            assertFalse(texto.contains("Fornecedor"));
            var imagem = new org.apache.pdfbox.rendering.PDFRenderer(doc).renderImageWithDPI(0, 140);
            javax.imageio.ImageIO.write(imagem, "png", pasta.resolve("ticket-layout-andre.png").toFile());
        }
    }
}
