package my_app.infra;

import my_app.db.models.DescontoModel;
import my_app.db.models.EmpresaModel;
import my_app.db.models.PesagemModel;
import my_app.utils.DateUtils;
import my_app.utils.Utils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Gera o ticket de pesagem em PDF (mesma família de libs do RelatorioPdfExporter do
 * plics-sw — Apache PDFBox). O app original imprimia um ticket parecido, mas direto pra
 * impressora térmica via ESC/POS; aqui, como em plics-sw, o caminho é exportar um PDF que
 * o operador abre e imprime pelo visualizador padrão — sem depender de porta/spooler de
 * impressora (ver docs/DECISIONS.md, "Recursos específicos de varejo... removidos").
 */
public class TicketPdfExporter {

    private static final DateTimeFormatter DATA_HORA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final float MARGEM = 50;
    private static final float LEADING = 18;
    private static final float LARGURA_UTIL = PDRectangle.A4.getWidth() - 2 * MARGEM;

    public void gerar(File destino, EmpresaModel empresa, PesagemModel pesagem) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            var page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            var fonteTitulo = PDType1Font.HELVETICA_BOLD;
            var fonteSecao = PDType1Font.HELVETICA_BOLD;
            var fonteTexto = PDType1Font.HELVETICA;

            try (var cs = new PDPageContentStream(doc, page)) {
                float y = page.getMediaBox().getHeight() - MARGEM;

                y = escreverLinha(cs, fonteTitulo, 16, MARGEM, y, empresa != null && empresa.getNome() != null ? empresa.getNome() : "Gobitech");
                if (empresa != null) {
                    if (empresa.getCpfCnpj() != null && !empresa.getCpfCnpj().isBlank()) {
                        y = escreverLinha(cs, fonteTexto, 10, MARGEM, y, "CNPJ/CPF: " + Utils.formatCpfCnpj(empresa.getCpfCnpj()));
                    }
                    String endereco = formatarEndereco(empresa);
                    if (!endereco.isBlank()) {
                        y = escreverLinha(cs, fonteTexto, 10, MARGEM, y, endereco);
                    }
                    String contato = formatarContato(empresa);
                    if (!contato.isBlank()) {
                        y = escreverLinha(cs, fonteTexto, 10, MARGEM, y, contato);
                    }
                }
                y -= LEADING / 2;

                y = escreverLinha(cs, fonteTitulo, 14, MARGEM, y, "TICKET DE PESAGEM Nº " + zeroPad(pesagem.getId()));
                y = escreverLinha(cs, fonteTexto, 11, MARGEM, y,
                        "Operação: " + valorOu(pesagem.getOperacao(), "-") + "    Data: " + DateUtils.localDateTimeToBrazilianDateTime(pesagem.getDataCriacao()));
                y -= LEADING / 2;

                y = escreverLinha(cs, fonteSecao, 12, MARGEM, y, "VEÍCULO E MOTORISTA");
                y = escreverLinha(cs, fonteTexto, 11, MARGEM, y, "Placa: " + valorOu(pesagem.getPlaca(), "-"));
                y = escreverLinha(cs, fonteTexto, 11, MARGEM, y, "Motorista: " + valorOu(pesagem.getMotoristaNome(), "-")
                        + (pesagem.getMotoristaDocumento() != null && !pesagem.getMotoristaDocumento().isBlank()
                        ? " (Doc: " + pesagem.getMotoristaDocumento() + ")" : ""));
                y -= LEADING / 2;

                y = escreverLinha(cs, fonteSecao, 12, MARGEM, y, "CLIENTE E PRODUTO");
                y = escreverLinha(cs, fonteTexto, 11, MARGEM, y, "Cliente: " + (pesagem.getCliente() != null ? valorOu(pesagem.getCliente().getLoja(), "-") : "-"));
                y = escreverLinha(cs, fonteTexto, 11, MARGEM, y, "Produto: " + (pesagem.getProduto() != null ? valorOu(pesagem.getProduto().getNome(), "-") : "-"));
                if (pesagem.getNotaFiscal() != null && !pesagem.getNotaFiscal().isBlank()) {
                    y = escreverLinha(cs, fonteTexto, 11, MARGEM, y, "Nota fiscal: " + pesagem.getNotaFiscal());
                }
                y -= LEADING / 2;

                y = escreverLinha(cs, fonteSecao, 12, MARGEM, y, "PESAGEM");
                y = escreverLinha(cs, fonteTexto, 11, MARGEM, y, "Tara: " + formatarKg(pesagem.getPesoVeiculo()));
                y = escreverLinha(cs, fonteTexto, 11, MARGEM, y, "Peso bruto: " + formatarKg(pesagem.getPesoTotal()));
                y = escreverLinha(cs, fonteTitulo, 12, MARGEM, y, "Peso líquido: " + formatarKg(pesagem.getPesoFinal()));
                y -= LEADING / 2;

                var desconto = pesagem.getDesconto();
                if (desconto != null && desconto.somaPercentuais().compareTo(BigDecimal.ZERO) > 0) {
                    y = escreverLinha(cs, fonteSecao, 12, MARGEM, y, "DESCONTOS APLICADOS");
                    y = escreverPercentualSeExistir(cs, fonteTexto, MARGEM, y, "Avariados", desconto.getAvariados());
                    y = escreverPercentualSeExistir(cs, fonteTexto, MARGEM, y, "Ardidos", desconto.getArdidos());
                    y = escreverPercentualSeExistir(cs, fonteTexto, MARGEM, y, "Quebra (ardidos)", desconto.getQuebraArdidos());
                    y = escreverPercentualSeExistir(cs, fonteTexto, MARGEM, y, "Impurezas", desconto.getImpurezas());
                    y = escreverPercentualSeExistir(cs, fonteTexto, MARGEM, y, "Quebra (impurezas)", desconto.getQuebraImpurezas());
                    y = escreverPercentualSeExistir(cs, fonteTexto, MARGEM, y, "Umidade", desconto.getUmidade());
                    y = escreverPercentualSeExistir(cs, fonteTexto, MARGEM, y, "Quebra (umidade)", desconto.getQuebraUmidade());
                    y = escreverPercentualSeExistir(cs, fonteTexto, MARGEM, y, "Outros", desconto.getOutros());
                    y = escreverLinha(cs, fonteSecao, 11, MARGEM, y, "Total de desconto: " + formatarPercentual(desconto.somaPercentuais()));
                    y -= LEADING / 2;
                }

                if (pesagem.getObservacoes() != null && !pesagem.getObservacoes().isBlank()) {
                    y = escreverLinha(cs, fonteSecao, 12, MARGEM, y, "OBSERVAÇÕES");
                    for (String linha : quebrarLinhas(fonteTexto, 10, pesagem.getObservacoes(), LARGURA_UTIL)) {
                        y = escreverLinha(cs, fonteTexto, 10, MARGEM, y, linha);
                    }
                    y -= LEADING / 2;
                }

                y -= LEADING * 2;
                float xAssinaturaDireita = MARGEM + LARGURA_UTIL / 2 + 20;
                escreverLinha(cs, fonteTexto, 10, MARGEM, y, "____________________________");
                escreverLinha(cs, fonteTexto, 10, xAssinaturaDireita, y, "____________________________");
                y -= LEADING;
                escreverLinha(cs, fonteTexto, 9, MARGEM, y, "Assinatura do Balanceiro");
                escreverLinha(cs, fonteTexto, 9, xAssinaturaDireita, y, "Assinatura do Motorista");
                y -= LEADING * 2;

                escreverLinha(cs, fonteTexto, 8, MARGEM, y,
                        "Gerado em " + java.time.LocalDateTime.now().format(DATA_HORA_FMT) + " pelo Gobitech");
            }

            doc.save(destino);
        }
    }

    private String formatarEndereco(EmpresaModel empresa) {
        var partes = new ArrayList<String>();
        if (naoVazio(empresa.getRua())) partes.add(empresa.getRua() + (naoVazio(empresa.getNumero()) ? ", " + empresa.getNumero() : ""));
        if (naoVazio(empresa.getBairro())) partes.add(empresa.getBairro());
        if (naoVazio(empresa.getCidade())) partes.add(empresa.getCidade() + (naoVazio(empresa.getEstado()) ? "/" + empresa.getEstado() : ""));
        return String.join(" - ", partes);
    }

    private String formatarContato(EmpresaModel empresa) {
        var partes = new ArrayList<String>();
        if (naoVazio(empresa.getTelefone())) partes.add("Tel: " + Utils.formatPhone(empresa.getTelefone()));
        if (naoVazio(empresa.getEmail())) partes.add(empresa.getEmail());
        return String.join("    ", partes);
    }

    private boolean naoVazio(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String valorOu(String valor, String fallback) {
        return naoVazio(valor) ? valor : fallback;
    }

    private String formatarKg(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, java.math.RoundingMode.HALF_UP) + " Kg";
    }

    private String formatarPercentual(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, java.math.RoundingMode.HALF_UP) + "%";
    }

    private String zeroPad(Integer id) {
        return id == null ? "-" : String.format("%06d", id);
    }

    private float escreverPercentualSeExistir(PDPageContentStream cs, PDFont fonte, float x, float y, String label, BigDecimal valor) throws IOException {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) == 0) return y;
        return escreverLinha(cs, fonte, 11, x, y, label + ": " + formatarPercentual(valor));
    }

    private float escreverLinha(PDPageContentStream cs, PDFont fonte, float tamanho, float x, float y, String texto) throws IOException {
        cs.beginText();
        cs.setFont(fonte, tamanho);
        cs.newLineAtOffset(x, y);
        cs.showText(texto != null ? texto : "");
        cs.endText();
        return y - LEADING;
    }

    private List<String> quebrarLinhas(PDFont fonte, float tamanho, String texto, float larguraMax) throws IOException {
        var linhas = new ArrayList<String>();
        var linhaAtual = new StringBuilder();
        for (String palavra : texto.split("\\s+")) {
            String candidata = linhaAtual.isEmpty() ? palavra : linhaAtual + " " + palavra;
            if (fonte.getStringWidth(candidata) / 1000 * tamanho > larguraMax && !linhaAtual.isEmpty()) {
                linhas.add(linhaAtual.toString());
                linhaAtual = new StringBuilder(palavra);
            } else {
                linhaAtual = new StringBuilder(candidata);
            }
        }
        if (!linhaAtual.isEmpty()) linhas.add(linhaAtual.toString());
        return linhas;
    }
}
