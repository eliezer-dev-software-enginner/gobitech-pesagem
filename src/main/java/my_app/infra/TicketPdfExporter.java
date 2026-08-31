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
 */
public class TicketPdfExporter {

    private static final DateTimeFormatter DATA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final float MARGEM = 50;
    private static final float LEADING = 16;

    /**
     * @param entrada pesagem de entrada vinculada à saída (pode ser {@code null} para
     *                entrada/avulsa/manual sem par) — fornece data/hora e peso de entrada.
     */
    public void gerar(File destino, EmpresaModel empresa, PesagemModel pesagem, PesagemModel entrada) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            var fonte = PDType1Font.COURIER;
            float tamanho = 11;

            var linhas = montarLinhas(empresa, pesagem, entrada);
            var pagina = montarPagina(linhas);

            var page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (var cs = new PDPageContentStream(doc, page)) {
                float y = page.getMediaBox().getHeight() - MARGEM;
                for (String linha : pagina) {
                    y = escreverLinha(cs, fonte, tamanho, MARGEM, y, linha);
                }
            }

            doc.save(destino);
        }
    }

    /**
     * As duas vias na MESMA folha, separadas por uma linha longa — essa linha é o corte que
     * indica o fim do primeiro ticket e o começo do segundo (por isso não pertence a nenhuma via).
     */
    List<String> montarPagina(List<String> via) {
        var pagina = new ArrayList<String>();
        pagina.addAll(via);
        pagina.add("----------------------------------------------------------------------");
        pagina.addAll(via);
        return pagina;
    }

    /**
     * Monta o ticket como lista de linhas monoespaçadas, reproduzindo o layout do André.
     */
    List<String> montarLinhas(EmpresaModel empresa, PesagemModel pesagem, PesagemModel entrada) {
        var linhas = new ArrayList<String>();

        // Cabeçalho da empresa
        String nomeEmpresa = empresa != null && valor(empresa.getNome()) != null ? empresa.getNome() : "Gobitech";
        linhas.add(nomeEmpresa);
        linhas.add("Cnpj:              " + (empresa != null ? nulo(empresa.getCpfCnpj()) : "") + "          Insc.est:");
        linhas.add("End:             " + (empresa != null ? nulo(montarEnd(empresa)) : ""));
        linhas.add("Bairro:          " + (empresa != null ? nulo(empresa.getBairro()) : ""));
        linhas.add("Cidade: " + (empresa != null ? nulo(montarCidade(empresa)) : ""));
        linhas.add("Fone:    " + (empresa != null ? nulo(empresa.getTelefone()) : ""));
        linhas.add("");

        // Título + número
        linhas.add(String.format("                         Ticket de Pesagem                         Nº: %d",
                pesagem.getId() == null ? 0 : pesagem.getId()));
        linhas.add("");

        boolean temEntrada = entrada != null && entrada.getDataCriacao() != null;

        // Placa / Uf
        linhas.add(String.format("Placa:                %-16s Uf:", nulo(pesagem.getPlaca())));

        // Data/Hora entrada
        if (temEntrada) {
            linhas.add("Data Entrada:         " + DATA_FMT.format(entrada.getDataCriacao())
                    + "     Hora entrada: " + HORA_FMT.format(entrada.getDataCriacao()));
        } else {
            linhas.add("Data Entrada:         " + "     Hora entrada: ");
        }

        // Data/Hora saida (a própria pesagem)
        if (pesagem.getDataCriacao() != null) {
            linhas.add("Data saida:           " + DATA_FMT.format(pesagem.getDataCriacao())
                    + "     Hora saida:    " + HORA_FMT.format(pesagem.getDataCriacao()));
        } else {
            linhas.add("Data saida:           " + "     Hora saida:    ");
        }

        // Operador / Motorista / Produto / Fornecedor / Cliente
        linhas.add("Operador:             " + nomeOperador(pesagem));
        linhas.add("Motorista:            " + valorOu(pesagem.getMotoristaNome(), "---"));
        linhas.add("Produto:              " + (pesagem.getProduto() != null ? valorOu(pesagem.getProduto().getNome(), "---") : "---"));
        linhas.add("Fornecedor:");
        linhas.add("Cliente:              " + (pesagem.getCliente() != null ? valorOu(pesagem.getCliente().getLoja(), "") : ""));

        // Pesos
        linhas.add("Peso entrada:         " + pesoSemLegenda(entrada != null ? entrada.getPesoTotal() : null) + " Kg");
        linhas.add("Peso saida:           " + pesoSemLegenda(pesagem.getPesoTotal()) + " Kg");
        linhas.add("Peso liquido:         " + pesoSemLegenda(pesagem.getPesoFinal()) + " Kg");
        linhas.add("Observacao:");

        // Observação (quebrada em linhas)
        if (pesagem.getObservacoes() != null && !pesagem.getObservacoes().isBlank()) {
            for (String linha : quebrar(pesagem.getObservacoes())) {
                linhas.add("  " + linha);
            }
        }
        linhas.add("");

        // Assinaturas à mão: uma linha de assinatura ACIMA do nome do operador (à esquerda) e
        // do motorista (à direita) — depois de imprimir, quem faz a pesagem assina sobre a linha.
        String operador = nomeOperador(pesagem);
        linhas.add(String.format("%-30s %50s", "______________________", "______________________"));
        linhas.add(String.format("%-30s %50s", operador, "Motorista"));

        return linhas;
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

    private String valorOu(String valor, String fallback) {
        return naoVazio(valor) ? valor : fallback;
    }

    /** Retorna o próprio valor se não vazio, senão {@code null} (pra ajudar a decidir vazio). */
    private String valor(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    private float escreverLinha(PDPageContentStream cs, PDFont fonte, float tamanho, float x, float y, String texto) throws IOException {
        cs.beginText();
        cs.setFont(fonte, tamanho);
        cs.newLineAtOffset(x, y);
        cs.showText(texto != null ? texto : "");
        cs.endText();
        return y - LEADING;
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
