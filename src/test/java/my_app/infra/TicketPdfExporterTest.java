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
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void geraTicketNoFormatoDoAndreComDuasViasNaMesmaFolha(@TempDir Path tempDir) throws IOException {
        var empresa = new EmpresaModel();
        empresa.setNome("BALANÇAS GOBITECH");
        empresa.setCpfCnpj("12345678000199");
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
        assertTrue(texto.contains("Cidade: FORMOSA - GO"));
        assertTrue(texto.contains("Ticket de Pesagem"));
        assertTrue(texto.contains("Nº: 1"));
        assertTrue(texto.contains("Placa:                RED1234"));
        assertTrue(texto.contains("Data Entrada:         17/08/2025"));
        assertTrue(texto.contains("Data saida:           17/08/2025"));
        assertTrue(texto.contains("Operador:             ADMINISTRADOR"));
        assertTrue(texto.contains("Motorista:            ---"));
        assertTrue(texto.contains("Produto:              ---"));
        assertTrue(texto.contains("Peso entrada:         2540 Kg"));
        assertTrue(texto.contains("Peso saida:           9980 Kg"));
        assertTrue(texto.contains("Peso liquido:         7440 Kg"));

        // As duas vias estão na mesma folha: o conteúdo aparece duas vezes, com linha de
        // separação entre elas. Cada nome aparece no campo E na linha de assinatura → 2×via × 2 = 4.
        assertEquals(2, ocorrencias(texto, "Ticket de Pesagem"));
        assertEquals(4, ocorrencias(texto, "ADMINISTRADOR"));
        assertEquals(4, ocorrencias(texto, "Motorista"));

        // Linhas de assinatura acima dos nomes (operador e motorista)
        assertTrue(texto.contains("______________________"));
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
}
