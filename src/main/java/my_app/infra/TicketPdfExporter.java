package my_app.infra;

import my_app.db.models.EmpresaModel;
import my_app.db.models.PesagemModel;
import my_app.db.models.UsuarioModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import pack.utilities.FormatterPack;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Gera o ticket de pesagem em PDF no layout exato do ticket do André (app antigo): texto
 * monoespaçado com cabeçalho da empresa, "Ticket de Pesagem Nº", placa, data/hora de entrada
 * e saída, operador/motorista/produto/fornecedor/cliente, pesos de entrada/saída/líquido,
 * observação e assinaturas. Cada PDF traz o ticket impresso 2 vezes (duas vias) na MESMA
 * folha, separadas por uma linha longa. Usa o mesmo Apache PDFBox do restante do app.
 *
 * <p>O nome da empresa (topo) e o título "Ticket de Pesagem" vêm em negrito (Courier-Bold),
 * com uma linha grossa (de sublinhados) abaixo do título — no mesmo estilo da linha de
 * assinatura que fica acima de "Motorista". Usa Courier e Courier-Bold — fontes monoespaçadas
 * com a MESMA largura de glifo, então trechos em negrito não desalinham o layout.
 */
public class TicketPdfExporter {

    private static final DateTimeFormatter DATA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final float MARGEM = 50;
    private static final float LEADING = 16;
    private static final PDFont REGULAR = PDType1Font.COURIER;
    private static final PDFont BOLD = PDType1Font.COURIER_BOLD;

    /** Um trecho de texto e se ele deve sair em negrito (Courier-Bold). */
    private record Run(String texto, boolean negrito) {}

    private record Linha(List<Run> runs) {}

    /**
     * @param entrada pesagem de entrada vinculada à saída (pode ser {@code null} para
     *                entrada/avulsa/manual sem par) — fornece data/hora e peso de entrada.
     */
    public void gerar(File destino, EmpresaModel empresa, PesagemModel pesagem, PesagemModel entrada) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            var linhas = montarPagina(montarLinhas(empresa, pesagem, entrada));

            var page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            renderizar(doc, page, linhas);

            doc.save(destino);
        }
    }

    /**
     * As duas vias na MESMA folha, separadas por uma linha longa — essa linha é o corte que
     * indica o fim do primeiro ticket e o começo do segundo (por isso não pertence a nenhuma via).
     */
    List<Linha> montarPagina(List<Linha> via) {
        var pagina = new ArrayList<Linha>();
        pagina.addAll(via);
        pagina.add(linha("----------------------------------------------------------------------"));
        pagina.addAll(via);
        return pagina;
    }

    /**
     * Monta o ticket como lista de linhas monoespaçadas, reproduzindo o layout do André.
     * O nome da empresa e o título "Ticket de Pesagem" saem em negrito, com uma linha grossa
     * de sublinhados abaixo do título.
     */
    List<Linha> montarLinhas(EmpresaModel empresa, PesagemModel pesagem, PesagemModel entrada) {
        var linhas = new ArrayList<Linha>();

        // Cabeçalho da empresa (nome em negrito)
        String nomeEmpresa = empresa != null && valor(empresa.getNome()) != null ? empresa.getNome() : "Gobitech";
        linhas.add(linha(nomeEmpresa, true));
        linhas.add(linha("Cnpj:              " + (empresa != null ? cnpjFormatado(empresa.getCpfCnpj()) : "")
                + "          Insc.est: " + (empresa != null ? nulo(empresa.getInscricaoEstadual()) : "")));
        linhas.add(linha("End:             " + (empresa != null ? nulo(montarEnd(empresa)) : "")));
        linhas.add(linha("Bairro:          " + (empresa != null ? nulo(empresa.getBairro()) : "")));
        linhas.add(linha("Cidade: " + (empresa != null ? nulo(montarCidade(empresa)) : "")));
        linhas.add(linha("Fone:    " + (empresa != null ? telefoneFormatado(empresa.getTelefone()) : "")));
        linhas.add(linhaVazia());

        // Título + número ("Ticket de Pesagem" em negrito)
        String id = String.valueOf(pesagem.getId() == null ? 0 : pesagem.getId());
        linhas.add(new Linha(List.of(
                new Run(" ".repeat(25), false),
                new Run("Ticket de Pesagem", true),
                new Run(" ".repeat(25) + "Nº: " + id, false))));
        // Linha grossa (mesmo estilo da linha de assinatura acima de "Motorista"), na mesma
        // coluna/posição do título e com o mesmo comprimento de "Ticket de Pesagem".
        linhas.add(linha(" ".repeat(25) + "_".repeat("Ticket de Pesagem".length())));
        linhas.add(linhaVazia());

        boolean temEntrada = entrada != null && entrada.getDataCriacao() != null;

        // Placa / Uf
        linhas.add(linha(String.format("Placa:                %-16s Uf:", nulo(pesagem.getPlaca()))));

        // Data/Hora entrada
        if (temEntrada) {
            linhas.add(linha("Data Entrada:         " + DATA_FMT.format(entrada.getDataCriacao())
                    + "     Hora entrada: " + HORA_FMT.format(entrada.getDataCriacao())));
        } else {
            linhas.add(linha("Data Entrada:         " + "     Hora entrada: "));
        }

        // Data/Hora saida (a própria pesagem)
        if (pesagem.getDataCriacao() != null) {
            linhas.add(linha("Data saida:           " + DATA_FMT.format(pesagem.getDataCriacao())
                    + "     Hora saida:    " + HORA_FMT.format(pesagem.getDataCriacao())));
        } else {
            linhas.add(linha("Data saida:           " + "     Hora saida:    "));
        }

        // Operador / Motorista / Produto / Fornecedor / Cliente
        linhas.add(linha("Operador:             " + nomeOperador(pesagem)));
        linhas.add(linha("Motorista:            " + valorOu(pesagem.getMotoristaNome(), "---")));
        linhas.add(linha("Produto:              " + (pesagem.getProduto() != null ? valorOu(pesagem.getProduto().getNome(), "---") : "---")));
        linhas.add(linha("Fornecedor:"));
        linhas.add(linha("Cliente:              " + (pesagem.getCliente() != null ? valorOu(pesagem.getCliente().getLoja(), "") : "")));

        // Pesos
        linhas.add(linha("Peso entrada:         " + pesoSemLegenda(entrada != null ? entrada.getPesoTotal() : null) + " Kg"));
        linhas.add(linha("Peso saida:           " + pesoSemLegenda(pesagem.getPesoTotal()) + " Kg"));
        linhas.add(linha("Peso liquido:         " + pesoSemLegenda(pesagem.getPesoFinal()) + " Kg"));
        linhas.add(linha("Observacao:"));

        // Observação (quebrada em linhas)
        if (pesagem.getObservacoes() != null && !pesagem.getObservacoes().isBlank()) {
            for (String l : quebrar(pesagem.getObservacoes())) {
                linhas.add(linha("  " + l));
            }
        }
        linhas.add(linhaVazia());

        // Assinaturas à mão: uma linha de assinatura ACIMA do nome do operador (à esquerda) e
        // do motorista (à direita) — depois de imprimir, quem faz a pesagem assina sobre a linha.
        String operador = nomeOperador(pesagem);
        linhas.add(linha(String.format("%-30s %50s", "______________________", "______________________")));
        linhas.add(linha(String.format("%-30s %50s", operador, "Motorista")));

        return linhas;
    }

    private Linha linha(String texto) {
        return linha(texto, false);
    }

    private Linha linha(String texto, boolean negrito) {
        return new Linha(List.of(new Run(texto, negrito)));
    }

    private Linha linhaVazia() {
        return linha("");
    }

    private String nomeOperador(PesagemModel pesagem) {
        UsuarioModel usuario = pesagem.getUsuario();
        if (usuario != null && valor(usuario.getNome()) != null) return usuario.getNome();
        return "---";
    }

    private String pesoSemLegenda(BigDecimal valor) {
        return valor == null ? "0" : valor.setScale(0, java.math.RoundingMode.HALF_UP).toBigInteger().toString();
    }

    private String montarEnd(EmpresaModel empresa) {
        if (!naoVazio(empresa.getRua())) return null;
        return empresa.getRua() + (naoVazio(empresa.getNumero()) ? ", " + empresa.getNumero() : "");
    }

    private String montarCidade(EmpresaModel empresa) {
        if (!naoVazio(empresa.getCidade())) return null;
        return empresa.getCidade() + (naoVazio(empresa.getEstado()) ? " - " + empresa.getEstado() : "");
    }

    /**
     * Preenche um valor ajustado à largura de formato do ticket (com espaços à direita),
     * para manter o alinhamento monoespaçado; {@code null} vira espaços.
     */
    private boolean naoVazio(String valor) {
        return valor != null && !valor.isBlank();
    }

    /** Valor cru, ou string vazia se {@code null}/em branco (mantém o alinhamento do layout). */
    private String nulo(String v) {
        return (v == null || v.isBlank()) ? "" : v;
    }

    /** CNPJ formatado ex.: {@code 12.345.678/0001-99} (vide {@code FormatterPack.formatCnpj}). */
    private String cnpjFormatado(String v) {
        if (v == null || v.isBlank()) return "";
        String soDigitos = v.replaceAll("\\D", "");
        if (soDigitos.isEmpty()) return v;
        return FormatterPack.formatCnpj(soDigitos);
    }

    /** Telefone formatado ex.: {@code (61) 99653-2857} (vide {@code FormatterPack.formatPhone}). */
    private String telefoneFormatado(String v) {
        if (v == null || v.isBlank()) return "";
        String soDigitos = v.replaceAll("\\D", "");
        if (soDigitos.isEmpty()) return v;
        return FormatterPack.formatPhone(soDigitos);
    }

    private String valorOu(String valor, String fallback) {
        return naoVazio(valor) ? valor : fallback;
    }

    /** Retorna o próprio valor se não vazio, senão {@code null} (pra ajudar a decidir vazio). */
    private String valor(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    /** Rende as linhas (runs) no PDF; cada run sai em regular ou negrito conforme {@code negrito}. */
    private void renderizar(PDDocument doc, PDPage page, List<Linha> linhas) throws IOException {
        float tamanho = 11;
        try (var cs = new PDPageContentStream(doc, page)) {
            // Sem margin do topo no cabeçalho: começa rente à borda superior da página (um
            // pequeno deslocamento do tamanho da fonte pra não cortar o topo dos glifos).
            float y = page.getMediaBox().getHeight() - tamanho;
            for (Linha linha : linhas) {
                float x = MARGEM;
                for (Run run : linha.runs()) {
                    PDFont fonte = run.negrito() ? BOLD : REGULAR;
                    cs.beginText();
                    cs.setFont(fonte, tamanho);
                    cs.newLineAtOffset(x, y);
                    cs.showText(run.texto());
                    cs.endText();
                    x += REGULAR.getStringWidth(run.texto()) / 1000 * tamanho;
                }
                y -= LEADING;
            }
        }
    }

    private List<String> quebrar(String texto) {
        var linhas = new ArrayList<String>();
        var atual = new StringBuilder();
        for (String palavra : texto.split("\\s+")) {
            String candidata = atual.isEmpty() ? palavra : atual + " " + palavra;
            if (candidata.length() > 66 && !atual.isEmpty()) {
                linhas.add(atual.toString());
                atual = new StringBuilder(palavra);
            } else {
                atual = new StringBuilder(candidata);
            }
        }
        if (!atual.isEmpty()) linhas.add(atual.toString());
        return linhas;
    }
}
