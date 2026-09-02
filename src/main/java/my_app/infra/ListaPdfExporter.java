package my_app.infra;

import my_app.db.models.EmpresaModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import pack.utilities.FormatterPack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ListaPdfExporter {

    private static final float MARGEM = 50;
    private static final float LEADING = 16;
    private static final float LARGURA_UTIL = PDRectangle.A4.getWidth() - 2 * MARGEM;

    public static void exportar(File destino, EmpresaModel empresa, String titulo,
                                List<String> headers, List<List<String>> rows) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            var page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            var fonteTitulo = PDType1Font.HELVETICA_BOLD;
            var fonteTexto = PDType1Font.HELVETICA;
            var fonteHeaderTabela = PDType1Font.HELVETICA_BOLD;
            var fonteCell = PDType1Font.HELVETICA;

            // cs não é mais final/try-with-resources: precisa ser fechado e reaberto de
            // verdade a cada quebra de página (antes era fechado e nunca reaberto, o que
            // lançaria "stream closed" assim que a lista estourasse a primeira página).
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            try {
                float y = page.getMediaBox().getHeight() - MARGEM;

                // --- Cabeçalho da empresa ---
                if (empresa != null) {
                    float logoHeight = 0;
                    if (empresa.getLogomarca() != null && !empresa.getLogomarca().isBlank()) {
                        try {
                            var img = carregarImagem(empresa.getLogomarca());
                            if (img != null) {
                                float maxHeight = 50;
                                float ratio = (float) img.getWidth() / img.getHeight();
                                float width = maxHeight * ratio;
                                if (width > 100) { width = 100; maxHeight = width / ratio; }

                                var pdImage = PDImageXObject.createFromByteArray(doc, imgBytes(empresa.getLogomarca()), "logo");
                                cs.drawImage(pdImage, MARGEM, y - maxHeight, width, maxHeight);
                                logoHeight = maxHeight;
                            }
                        } catch (Exception ignored) {}
                    }

                    float textX = MARGEM + (logoHeight > 0 ? 110 : 0);
                    float textY = logoHeight > 0 ? y - 15 : y;

                    if (empresa.getNome() != null) {
                        textY = escreverLinha(cs, fonteTitulo, 14, textX, textY, empresa.getNome());
                    }
                    if (empresa.getCpfCnpj() != null && !empresa.getCpfCnpj().isBlank()) {
                        textY = escreverLinha(cs, fonteTexto, 9, textX, textY,
                                "CNPJ/CPF: " + FormatterPack.formatCpfCnpj(empresa.getCpfCnpj()));
                    }
                    String endereco = formatarEndereco(empresa);
                    if (!endereco.isBlank()) {
                        textY = escreverLinha(cs, fonteTexto, 9, textX, textY, endereco);
                    }
                    String contato = formatarContato(empresa);
                    if (!contato.isBlank()) {
                        textY = escreverLinha(cs, fonteTexto, 9, textX, textY, contato);
                    }

                    y = logoHeight > 0 ? y - logoHeight - 10 : textY - 10;

                    // linha separadora
                    cs.setStrokingColor(200, 200, 200);
                    cs.moveTo(MARGEM, y);
                    cs.lineTo(MARGEM + LARGURA_UTIL, y);
                    cs.stroke();
                    y -= LEADING;
                }

                // --- Titulo do relatorio ---
                y = escreverLinha(cs, fonteTitulo, 14, MARGEM, y, titulo);
                y -= LEADING / 2;

                // --- Tabela ---
                if (headers.isEmpty()) return;

                int totalCols = headers.size();
                float[] colWidths = calcularLarguras(headers, rows, fonteHeaderTabela, fonteCell, 9);

                // Header
                float x = MARGEM;
                cs.setNonStrokingColor(50, 50, 50);
                cs.beginText();
                cs.setFont(fonteHeaderTabela, 9);
                cs.newLineAtOffset(x + 2, y);
                cs.showText(headers.get(0));
                cs.endText();
                for (int i = 1; i < totalCols; i++) {
                    x += colWidths[i - 1];
                    cs.beginText();
                    cs.setFont(fonteHeaderTabela, 9);
                    cs.newLineAtOffset(x + 2, y);
                    cs.showText(headers.get(i));
                    cs.endText();
                }
                y -= 2;
                cs.setStrokingColor(50, 50, 50);
                cs.moveTo(MARGEM, y);
                cs.lineTo(MARGEM + LARGURA_UTIL, y);
                cs.stroke();
                y -= LEADING - 2;

                cs.setNonStrokingColor(0, 0, 0);

                // Rows
                for (int r = 0; r < rows.size(); r++) {
                    var row = rows.get(r);

                    if (y < MARGEM + LEADING) {
                        // nova pagina — fecha o stream atual de verdade e abre um novo pra
                        // página nova, em vez de continuar escrevendo num stream fechado
                        cs.close();
                        var newPage = new PDPage(PDRectangle.A4);
                        doc.addPage(newPage);
                        cs = new PDPageContentStream(doc, newPage);
                        y = newPage.getMediaBox().getHeight() - MARGEM;
                        cs.setNonStrokingColor(0, 0, 0);
                    }

                    // zebra stripe é desenhado ANTES do texto da linha — antes ficava depois
                    // do showText() e, como quem desenha por último fica em cima em PDF, o
                    // fill() cobria o texto de toda linha par (metade da lista "sumia")
                    if (r % 2 == 0) {
                        cs.setNonStrokingColor(245, 245, 245);
                        cs.addRect(MARGEM, y - 2, LARGURA_UTIL, LEADING);
                        cs.fill();
                        cs.setNonStrokingColor(0, 0, 0);
                    }

                    x = MARGEM;
                    for (int i = 0; i < totalCols; i++) {
                        String cellText = i < row.size() ? row.get(i) : "";
                        cs.beginText();
                        cs.setFont(fonteCell, 9);
                        cs.newLineAtOffset(x + 2, y);
                        cs.showText(cellText);
                        cs.endText();
                        x += colWidths[i];
                    }

                    y -= LEADING;
                }

                // rodape
                y -= LEADING;
                cs.setNonStrokingColor(150, 150, 150);
                escreverLinha(cs, fonteTexto, 8, MARGEM, y,
                        "Gerado em " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                + " - " + rows.size() + " registro(s)");
            } finally {
                cs.close();
            }

            doc.save(destino);
        }
    }

    private static BufferedImage carregarImagem(String path) {
        try {
            if (path.startsWith("file:")) {
                return ImageIO.read(new File(java.net.URI.create(path)));
            }
            return ImageIO.read(new File(path));
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] imgBytes(String path) throws IOException {
        var img = carregarImagem(path);
        if (img == null) throw new IOException("Logo not found");
        var baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private static float[] calcularLarguras(List<String> headers, List<List<String>> rows,
                                            PDFont fonteH, PDFont fonteC, float fontSize) throws IOException {
        int totalCols = headers.size();
        float[] widths = new float[totalCols];

        for (int i = 0; i < totalCols; i++) {
            float maxW = fonteH.getStringWidth(headers.get(i)) / 1000 * fontSize;
            for (var row : rows) {
                String cell = i < row.size() ? row.get(i) : "";
                float w = fonteC.getStringWidth(cell) / 1000 * fontSize;
                if (w > maxW) maxW = w;
            }
            widths[i] = maxW + 10; // padding
        }

        // normalizar pra caber na largura util
        float total = 0;
        for (float w : widths) total += w;
        if (total > LARGURA_UTIL) {
            float ratio = LARGURA_UTIL / total;
            for (int i = 0; i < totalCols; i++) widths[i] *= ratio;
        }

        return widths;
    }

    private static String formatarEndereco(EmpresaModel empresa) {
        var partes = new java.util.ArrayList<String>();
        if (naoVazio(empresa.getRua())) partes.add(empresa.getRua() + (naoVazio(empresa.getNumero()) ? ", " + empresa.getNumero() : ""));
        if (naoVazio(empresa.getBairro())) partes.add(empresa.getBairro());
        if (naoVazio(empresa.getCidade())) partes.add(empresa.getCidade() + (naoVazio(empresa.getEstado()) ? "/" + empresa.getEstado() : ""));
        return String.join(" - ", partes);
    }

    private static String formatarContato(EmpresaModel empresa) {
        var partes = new java.util.ArrayList<String>();
        if (naoVazio(empresa.getTelefone())) partes.add("Tel: " + FormatterPack.formatPhone(empresa.getTelefone()));
        if (naoVazio(empresa.getEmail())) partes.add(empresa.getEmail());
        return String.join("    ", partes);
    }

    private static boolean naoVazio(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static float escreverLinha(PDPageContentStream cs, PDFont fonte, float tamanho,
                                       float x, float y, String texto) throws IOException {
        cs.beginText();
        cs.setFont(fonte, tamanho);
        cs.newLineAtOffset(x, y);
        cs.showText(texto != null ? texto : "");
        cs.endText();
        return y - LEADING;
    }
}