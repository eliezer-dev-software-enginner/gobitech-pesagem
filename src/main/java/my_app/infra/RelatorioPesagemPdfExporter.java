package my_app.infra;

import my_app.db.models.EmpresaModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gera o relatório resumido de entradas e saídas em PDF como texto monoespaçado (Courier),
 * reproduzindo o visual do relatório do André: cabeçalho da empresa, título centralizado,
 * separadores de '=', colunas, uma linha "Observação:" pra cada ticket e linha de totais.
 * Usa Courier e Courier-Bold — fontes monoespaçadas com a MESMA largura de glifo, então
 * segmentos em negrito (título, nomes das colunas, rótulos "Observação", "Quantidade total
 * entradas" e "Total peso liquido") não desalinham o texto.
 */
public class RelatorioPesagemPdfExporter {

    private static final float MARGEM = 30;
    private static final float LEADING = 15;
    private static final PDFont REGULAR = PDType1Font.COURIER;
    private static final PDFont BOLD = PDType1Font.COURIER_BOLD;

    private record Run(String texto, boolean negrito) {}

    private record Linha(List<Run> runs) {}

    /**
     * @param empresa            cabeçalho (nome/Cpf/Insc.est/End/Bairro/Cidade/Fone)
     * @param headers            nomes das colunas (negrito)
     * @param rows               células de cada linha (mesma ordem das colunas)
     * @param observacoesLinha   observação de cada linha (paralela a {@code rows})
     * @param totalEntradas      número total de entradas
     * @param totalLiquido       soma do peso líquido, já formatada
     */
    public static void exportar(File destino, EmpresaModel empresa, String titulo,
                                List<String> headers, List<List<String>> rows,
                                List<String> observacoesLinha,
                                int totalEntradas, String totalLiquido) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            var page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            // largura da coluna (em caracteres) = maior entre título da coluna e células
            int[] cols = new int[headers.size()];
            for (int i = 0; i < headers.size(); i++) cols[i] = headers.get(i).length();
            for (var row : rows) {
                for (int i = 0; i < row.size() && i < cols.length; i++) {
                    String cell = row.get(i) == null ? "" : row.get(i);
                    if (cell.length() > cols[i]) cols[i] = cell.length();
                }
            }
            int larguraTotal = larguraTotal(cols);
            int OBS_LABEL = "Observação:".length();

            var linhas = new ArrayList<Linha>();
            // cabeçalho da empresa
            String nomeEmpresa = empresa != null && naoVazio(empresa.getNome())
                    ? empresa.getNome() : "Gobitech";
            linha(linhas, nomeEmpresa);
            linha(linhas, "Cpf: " + (empresa != null ? nulo(empresa.getCpfCnpj()) : "")
                    + "    Insc.e ");
            linha(linhas, "End: " + (empresa != null ? nulo(montarEnd(empresa)) : ""));
            linha(linhas, "Bairro: " + (empresa != null ? nulo(empresa.getBairro()) : ""));
            linha(linhas, "Cidade: " + (empresa != null ? nulo(montarCidade(empresa)) : ""));
            linha(linhas, "Fone: " + (empresa != null ? nulo(empresa.getTelefone()) : ""));
            linha(linhas, "_".repeat(Math.max(0, larguraTotal)));

            linhaVazia(linhas);
            linha(linhas, centrar(titulo, larguraTotal), true); // título em negrito
            linha(linhas, "=".repeat(Math.max(0, larguraTotal)));
            linhas.add(formatarColunas(headers, cols)); // nomes das colunas em negrito

            for (int r = 0; r < rows.size(); r++) {
                // linha de dados + observação da linha (rótulo em negrito)
                Linha dados = formatarColunaLinha(rows.get(r), cols);
                linhas.add(dados);
                String obs = observacoesLinha != null && r < observacoesLinha.size()
                        ? observacoesLinha.get(r) : "---";
                if (obs == null || obs.isBlank()) obs = "---";
                var obsRuns = new ArrayList<Run>();
                obsRuns.add(new Run(padDir("Observação:", OBS_LABEL), true));
                obsRuns.add(new Run(" " + obs, false));
                linhas.add(new Linha(obsRuns));
            }
            if (!rows.isEmpty()) linhaVazia(linhas);

            linha(linhas, "=".repeat(Math.max(0, larguraTotal)));
            linhaVazia(linhas);
            // linha de totais — ambos os rótulos em negrito
            var totais = new ArrayList<Run>();
            totais.add(new Run(espacos(16), false));
            totais.add(new Run("Quantidade total entradas", true));
            totais.add(new Run(" : " + totalEntradas, false));
            totais.add(new Run(espacos(4), false));
            totais.add(new Run("Total peso liquido", true));
            totais.add(new Run(": " + totalLiquido + " ", false));
            linhas.add(new Linha(totais));

            renderizar(doc, page, linhas, tamanhoFonte(linhas));
            doc.save(destino);
        }
    }

    /**
     * Tamanho da fonte monoespaçada (Courier) que faz a linha mais larga caber na largura útil
     * da página. Courier tem 0.6 do tamanho por caractere, então
     * {@code tamanho = larguraUtil / (maxChars * 0.6)}. Teto de 8.5pt pra não ficar exagerado.
     */
    private static float tamanhoFonte(List<Linha> linhas) {
        int maxChars = 0;
        for (Linha l : linhas) {
            int chars = 0;
            for (Run run : l.runs()) chars += run.texto().length();
            if (chars > maxChars) maxChars = chars;
        }
        float larguraUtil = PDRectangle.A4.getWidth() - 2 * MARGEM;
        float tamanho = maxChars > 0 ? larguraUtil / (maxChars * 0.6f) : 8.5f;
        return Math.min(8.5f, tamanho);
    }

    private static void renderizar(PDDocument doc, PDPage page, List<Linha> linhas, float tamanho) throws IOException {
        PDPageContentStream cs = new PDPageContentStream(doc, page);
        float y = page.getMediaBox().getHeight() - MARGEM;
        for (int i = 0; i < linhas.size(); i++) {
            if (y < MARGEM + LEADING) {
                cs.close();
                page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                cs = new PDPageContentStream(doc, page);
                y = page.getMediaBox().getHeight() - MARGEM;
            }
            float x = MARGEM;
            for (Run run : linhas.get(i).runs()) {
                PDFont fonte = run.negrito() ? BOLD : REGULAR;
                cs.beginText();
                cs.setFont(fonte, tamanho);
                cs.newLineAtOffset(x, y);
                cs.showText(run.texto());
                cs.endText();
                x += larguraPx(run.texto(), tamanho);
            }
            y -= LEADING;
        }
        cs.close();
    }

    private static float larguraPx(String texto, float tamanho) throws IOException {
        return REGULAR.getStringWidth(texto) / 1000 * tamanho;
    }

    private static void linha(List<Linha> linhas, String texto) {
        linha(linhas, texto, false);
    }

    private static void linha(List<Linha> linhas, String texto, boolean negrito) {
        linhas.add(new Linha(List.of(new Run(texto, negrito))));
    }

    private static void linhaVazia(List<Linha> linhas) {
        linhas.add(new Linha(List.of(new Run("", false))));
    }

    /** Formata todos os valores de uma linha alinhados às colunas (monoespaçado). */
    private static Linha formatarColunaLinha(List<String> row, int[] cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            String cell = i < row.size() && row.get(i) != null ? row.get(i) : "";
            sb.append(padDir(cell, cols[i]));
            if (i < cols.length - 1) sb.append(espacos(2));
        }
        return new Linha(List.of(new Run(sb.toString(), false)));
    }

    /** Formata o cabeçalho (negrito) alinhado às colunas. */
    private static Linha formatarColunas(List<String> headers, int[] cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < headers.size(); i++) {
            sb.append(padDir(headers.get(i), cols[i]));
            if (i < headers.size() - 1) sb.append(espacos(2));
        }
        return new Linha(List.of(new Run(sb.toString(), true)));
    }

    private static int larguraTotal(int[] cols) {
        int total = 0;
        for (int c : cols) total += c;
        total += 2 * (cols.length - 1);
        return total;
    }

    private static String espacos(int n) {
        return n <= 0 ? "" : " ".repeat(n);
    }

    private static String padDir(String texto, int largura) {
        return texto + espacos(Math.max(0, largura - texto.length()));
    }

    private static String centrar(String texto, int largura) {
        if (largura <= 0) return texto;
        int espacos = Math.max(0, (largura - texto.length()) / 2);
        return espacos(espacos) + texto;
    }

    private static boolean naoVazio(String v) {
        return v != null && !v.isBlank();
    }

    private static String nulo(String v) {
        return (v == null || v.isBlank()) ? "" : v;
    }

    private static String montarEnd(EmpresaModel empresa) {
        if (!naoVazio(empresa.getRua())) return "";
        return empresa.getRua() + (naoVazio(empresa.getNumero()) ? ", " + empresa.getNumero() : "");
    }

    private static String montarCidade(EmpresaModel empresa) {
        if (!naoVazio(empresa.getCidade())) return "";
        return empresa.getCidade() + (naoVazio(empresa.getEstado()) ? " - " + empresa.getEstado() : "");
    }
}
