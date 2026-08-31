package my_app.infra;

import my_app.db.models.EmpresaModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class RelatorioPesagemPdfExporter {

    private static final float MARGEM = 50;
    private static final float LEADING = 16;
    private static final float LARGURA_UTIL = PDRectangle.A4.getWidth() - 2 * MARGEM;

    // padding interno de cada célula (esquerda + direita) usado tanto para
    // desenhar quanto para calcular o limite de truncagem do texto
    private static final float CELL_PADDING = 4;

    public static void exportar(File destino, String titulo,
                                List<String> headers, List<List<String>> rows) throws IOException {
        exportar(destino, titulo, headers, rows, java.util.List.of());
    }

    public static void exportar(File destino, String titulo,
                                List<String> headers, List<List<String>> rows, List<String> rodape) throws IOException {
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

                // --- Titulo do relatorio ---
                y = escreverLinha(cs, fonteTitulo, 14, MARGEM, y, titulo);
                y -= LEADING / 2;

                // --- Tabela ---
                if (headers.isEmpty()) return;

                int totalCols = headers.size();
                float[] colWidths = calcularLarguras(headers, rows, fonteHeaderTabela, fonteCell, 9);

                // Header — texto é truncado para caber na largura final da coluna,
                // evitando que conteúdo muito longo (ex: números não arredondados)
                // invada a coluna vizinha
                float x = MARGEM;
                cs.setNonStrokingColor(50, 50, 50);
                cs.beginText();
                cs.setFont(fonteHeaderTabela, 9);
                cs.newLineAtOffset(x + 2, y);
                cs.showText(truncar(fonteHeaderTabela, 9, headers.get(0), colWidths[0] - CELL_PADDING));
                cs.endText();
                for (int i = 1; i < totalCols; i++) {
                    x += colWidths[i - 1];
                    cs.beginText();
                    cs.setFont(fonteHeaderTabela, 9);
                    cs.newLineAtOffset(x + 2, y);
                    cs.showText(truncar(fonteHeaderTabela, 9, headers.get(i), colWidths[i] - CELL_PADDING));
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
                        cellText = truncar(fonteCell, 9, cellText, colWidths[i] - CELL_PADDING);
                        cs.beginText();
                        cs.setFont(fonteCell, 9);
                        cs.newLineAtOffset(x + 2, y);
                        cs.showText(cellText);
                        cs.endText();
                        x += colWidths[i];
                    }

                    y -= LEADING;
                }

                // --- rodapé personalizado (observações / totais) ---
                if (!rodape.isEmpty()) {
                    y -= LEADING;
                    cs.setNonStrokingColor(0, 0, 0);
                    for (var linha : rodape) {
                        if (y < MARGEM + LEADING) {
                            cs.close();
                            var newPage = new PDPage(PDRectangle.A4);
                            doc.addPage(newPage);
                            cs = new PDPageContentStream(doc, newPage);
                            y = newPage.getMediaBox().getHeight() - MARGEM;
                            cs.setNonStrokingColor(0, 0, 0);
                        }
                        y = escreverLinha(cs, fonteCell, 9, MARGEM, y, linha);
                    }
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

    /**
     * Trunca o texto (adicionando "...") para que ele caiba dentro de larguraMax,
     * usando as métricas reais da fonte. Isso é a rede de segurança final contra
     * sobreposição de colunas: mesmo que calcularLarguras() encolha uma coluna
     * (por normalização) para um valor menor que o conteúdo natural dela, o texto
     * desenhado nunca vai ultrapassar o espaço reservado da coluna.
     */
    private static String truncar(PDFont fonte, float tamanho, String texto, float larguraMax) throws IOException {
        if (texto == null || texto.isEmpty()) return "";
        if (larguraMax <= 0) return "";
        if (fonte.getStringWidth(texto) / 1000 * tamanho <= larguraMax) return texto;

        String reticencias = "...";
        float larguraReticencias = fonte.getStringWidth(reticencias) / 1000 * tamanho;

        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            float w = fonte.getStringWidth(sb.toString() + c) / 1000 * tamanho + larguraReticencias;
            if (w > larguraMax) break;
            sb.append(c);
        }
        return sb + reticencias;
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