package my_app.infra;

import my_app.db.models.ClienteModel;
import my_app.db.models.DescontoModel;
import my_app.db.models.EmpresaModel;
import my_app.db.models.PesagemModel;
import my_app.db.models.ProdutoModel;
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
        pesagem.setId(42);
        pesagem.setPlaca("ABC1D23");
        pesagem.setMotoristaNome("José da Silva");
        pesagem.setMotoristaDocumento("123.456.789-00");
        pesagem.setTipoPesagem("saida");
        pesagem.setNotaFiscal("987");
        pesagem.setPesoVeiculo(new BigDecimal("8500.00"));
        pesagem.setPesoTotal(new BigDecimal("32000.00"));
        pesagem.setPesoFinal(new BigDecimal("23500.00"));
        pesagem.setDataCriacao(LocalDateTime.of(2026, 8, 19, 14, 30));

        var cliente = new ClienteModel();
        cliente.setLoja("Fazenda Boa Vista");
        pesagem.setCliente(cliente);

        var produto = new ProdutoModel();
        produto.setNome("Milho");
        pesagem.setProduto(produto);

        return pesagem;
    }

    private String extrairTexto(File pdf) throws IOException {
        try (var doc = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void geraPdfComDadosDaPesagemEDaEmpresa(@TempDir Path tempDir) throws IOException {
        var empresa = new EmpresaModel();
        empresa.setNome("Balanças Gobitech");
        empresa.setCpfCnpj("12345678000199");

        var destino = tempDir.resolve("ticket.pdf").toFile();
        exporter.gerar(destino, empresa, pesagemBasica());

        assertTrue(destino.exists());
        assertTrue(destino.length() > 0);

        String texto = extrairTexto(destino);
        assertTrue(texto.contains("Balanças Gobitech"));
        assertTrue(texto.contains("ABC1D23"));
        assertTrue(texto.contains("José da Silva"));
        assertTrue(texto.contains("Fazenda Boa Vista"));
        assertTrue(texto.contains("Milho"));
        assertTrue(texto.contains("8500.00 Kg"));
        assertTrue(texto.contains("32000.00 Kg"));
        assertTrue(texto.contains("23500.00 Kg"));
        assertTrue(texto.contains("987"));
    }

    @Test
    void funcionaSemEmpresaCadastrada(@TempDir Path tempDir) throws IOException {
        var destino = tempDir.resolve("ticket_sem_empresa.pdf").toFile();
        exporter.gerar(destino, null, pesagemBasica());

        assertTrue(destino.exists());
        String texto = extrairTexto(destino);
        assertTrue(texto.contains("Gobitech"));
        assertTrue(texto.contains("ABC1D23"));
    }

    @Test
    void incluiDescontosSoNaoZerados(@TempDir Path tempDir) throws IOException {
        var pesagem = pesagemBasica();
        var desconto = new DescontoModel();
        desconto.setUmidade(new BigDecimal("2.50"));
        desconto.setAvariados(BigDecimal.ZERO);
        pesagem.setDesconto(desconto);

        var destino = tempDir.resolve("ticket_desconto.pdf").toFile();
        exporter.gerar(destino, null, pesagem);

        String texto = extrairTexto(destino);
        assertTrue(texto.contains("Umidade: 2.50%"));
        assertFalse(texto.contains("Avariados:"));
    }

    @Test
    void naoQuebraQuandoClienteEProdutoSaoNulos(@TempDir Path tempDir) throws IOException {
        var pesagem = pesagemBasica();
        pesagem.setCliente(null);
        pesagem.setProduto(null);

        var destino = tempDir.resolve("ticket_sem_relacoes.pdf").toFile();
        assertDoesNotThrow(() -> exporter.gerar(destino, null, pesagem));
        assertTrue(destino.exists());
    }
}
