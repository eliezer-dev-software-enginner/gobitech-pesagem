package my_app.infra;

import my_app.db.models.EmpresaModel;
import my_app.db.models.PesagemModel;
import my_app.domain.pesagem.TicketPesagemDados;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Ticket A4 de duas vias, conforme a referência impressa do André. */
public class TicketPdfExporter {
    private static final PDFont NORMAL = PDType1Font.HELVETICA;
    private static final PDFont NEGRITO = PDType1Font.HELVETICA_BOLD;
    private static final float MARGEM = 24;
    private static final float DIREITA = PDRectangle.A4.getWidth() - MARGEM;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void gerar(File destino, EmpresaModel empresa, PesagemModel pesagem, PesagemModel entrada) throws IOException {
        try (var doc = new PDDocument()) {
            var pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);
            try (var cs = new PDPageContentStream(doc, pagina)) {
                float alturaVia = (PDRectangle.A4.getHeight() - MARGEM * 2) / 2;
                for (int via = 0; via < 2; via++) {
                    desenharVia(cs, PDRectangle.A4.getHeight() - MARGEM - via * alturaVia, empresa, pesagem, entrada);
                }
            }
            doc.save(destino);
        }
    }

    private void desenharVia(PDPageContentStream cs, float topo, EmpresaModel e,
                             PesagemModel p, PesagemModel entrada) throws IOException {
        texto(cs, valor(e == null ? null : e.getNome(), "Gobitech"), MARGEM, topo, 12, true, DIREITA - MARGEM);
        String cnpj = e == null ? "" : nulo(e.getCpfCnpj());
        String telefone = e == null ? "" : nulo(e.getTelefone());
        texto(cs, "Cnpj: " + (cnpj.isBlank() ? "" : FormatterPack.formatCnpj(cnpj)), MARGEM, topo - 14, 10, false, 190);
        texto(cs, "Insc.est: " + (e == null ? "" : nulo(e.getInscricaoEstadual())), 222, topo - 14, 10, false, DIREITA - 222);
        texto(cs, "End: " + (e == null ? "" : endereco(e)), MARGEM, topo - 27, 10, false, DIREITA - MARGEM);
        texto(cs, "Bairro: " + (e == null ? "" : nulo(e.getBairro())), MARGEM, topo - 40, 10, false, DIREITA - MARGEM);
        texto(cs, "Cidade: " + (e == null ? "" : cidade(e)), MARGEM, topo - 53, 10, false, DIREITA - MARGEM);
        texto(cs, "Fone: " + (telefone.isBlank() ? "" : FormatterPack.formatPhone(telefone.replaceAll("[^0-9]", ""))),
                MARGEM, topo - 66, 10, false, DIREITA - MARGEM);
        linha(cs, MARGEM, DIREITA, topo - 71);
        String titulo = "Ticket de Pesagem";
        float larguraTitulo = largura(titulo, NEGRITO, 12);
        float xTitulo = (PDRectangle.A4.getWidth() - larguraTitulo) / 2;
        texto(cs, titulo, xTitulo, topo - 86, 12, true, larguraTitulo + 1);
        linha(cs, xTitulo, xTitulo + larguraTitulo, topo - 88);
        texto(cs, "Nº: " + (p.getId() == null ? "" : p.getId()), 456, topo - 86, 12, true, DIREITA - 456);

        campo(cs, "Placa:", nulo(p.getPlaca()), topo - 104, true);
        texto(cs, "Uf:", 190, topo - 104, 11, false, 30);
        dataHora(cs, "Data Entrada:", "Hora entrada:", TicketPesagemDados.dataEntrada(p, entrada), topo - 118);
        dataHora(cs, "Data saida:", "Hora saida:", TicketPesagemDados.dataSaida(p), topo - 132);
        campo(cs, "Operador:", p.getUsuario() == null ? "---" : valor(p.getUsuario().getNome(), "---"), topo - 146, false);
        campo(cs, "Motorista:", valor(p.getMotoristaNome(), "---"), topo - 160, false);
        campo(cs, "Produto:", p.getProduto() == null ? "---" : valor(p.getProduto().getNome(), "---"), topo - 174, false);
        campo(cs, "Cliente:", p.getCliente() == null ? "" : nulo(p.getCliente().getLoja()), topo - 202, false);
        peso(cs, "Peso entrada:", TicketPesagemDados.pesoEntrada(p, entrada), topo - 216);
        peso(cs, "Peso saida:", TicketPesagemDados.pesoSaida(p), topo - 230);
        peso(cs, "Peso liquido inicial:", TicketPesagemDados.liquidoAntesDescontos(p), topo - 244);
        peso(cs, "Peso liquido final:", p.getPesoFinal(), topo - 258);
        descontos(cs, p, topo - 160);
        texto(cs, "Observação:", MARGEM, topo - 274, 11, false, 150);
        observacoes(cs, p.getObservacoes(), topo - 286);

        linha(cs, 72, 208, topo - 336);
        linha(cs, 388, 524, topo - 336);
        centralizado(cs, "Operador", 140, topo - 350, 11);
        centralizado(cs, "Motorista", 456, topo - 350, 11);
        linha(cs, MARGEM, DIREITA, topo - 360);
    }

    private void descontos(PDPageContentStream cs, PesagemModel p, float y) throws IOException {
        float x = 315;
        String titulo = "Descontos aplicados ao produto";
        texto(cs, titulo, x, y, 10.5f, true, DIREITA - x);
        linha(cs, x, x + largura(titulo, NEGRITO, 10.5f), y - 2);
        texto(cs, "Tipo", x, y - 15, 8, true, 78);
        texto(cs, "% Classificado", 395, y - 15, 7.5f, true, 65);
        texto(cs, "% Aplicado", 466, y - 15, 7.5f, true, 51);
        texto(cs, "Total (Kg)", 524, y - 15, 7.5f, true, DIREITA - 524);
        var aplicados = TicketPesagemDados.descontos(p).stream().filter(d -> d.percentual().signum() != 0).toList();
        float passo = aplicados.size() > 6 ? 10 : 12;
        float linhaY = y - 30;
        for (var d : aplicados) {
            texto(cs, d.nome().toUpperCase(Locale.ROOT), x, linhaY, 8, false, 78);
            texto(cs, "—", 423, linhaY, 9, false, 25);
            texto(cs, TicketPesagemDados.decimal(d.percentual()), 479, linhaY, 9, false, 37);
            texto(cs, d.desconta() ? TicketPesagemDados.decimal(d.quilos()) : "-", 530, linhaY, 9, false, DIREITA - 530);
            linhaY -= passo;
        }
        if (aplicados.isEmpty()) {
            texto(cs, "Nenhum desconto aplicado.", x, linhaY, 9, false, DIREITA - x);
        } else {
            var total = TicketPesagemDados.totalDescontado(p);
            texto(cs, "Total descontado: " + (total.signum() == 0 ? "-" : TicketPesagemDados.decimal(total) + " Kg"),
                    x, linhaY - 2, 9, true, DIREITA - x);
        }
    }

    private void campo(PDPageContentStream cs, String rotulo, String valor, float y, boolean negrito) throws IOException {
        texto(cs, rotulo, MARGEM, y, 11, false, 92);
        texto(cs, valor, 118, y, 11, negrito, 180);
    }

    private void peso(PDPageContentStream cs, String rotulo, BigDecimal valor, float y) throws IOException {
        texto(cs, rotulo, MARGEM, y, 10.5f, false, 91);
        texto(cs, valor == null ? "" : valor.setScale(0, RoundingMode.HALF_UP).toPlainString(), 118, y, 12, true, 64);
        if (valor != null) texto(cs, "Kg", 190, y, 12, true, 30);
    }

    private void dataHora(PDPageContentStream cs, String rotuloData, String rotuloHora, LocalDateTime data, float y) throws IOException {
        texto(cs, rotuloData, MARGEM, y, 11, false, 92);
        texto(cs, data == null ? "" : DATA.format(data), 118, y, 11, true, 69);
        texto(cs, rotuloHora, 194, y, 10.5f, false, 68);
        texto(cs, data == null ? "" : HORA.format(data), 264, y, 11, true, 70);
    }

    private void observacoes(PDPageContentStream cs, String observacoes, float y) throws IOException {
        if (observacoes == null || observacoes.isBlank()) return;
        float tamanho = 9;
        List<String> linhas;
        do {
            linhas = quebrar(observacoes, DIREITA - MARGEM, tamanho);
            if (linhas.size() * tamanho * 1.15f <= 42) break;
            tamanho *= 0.9f;
        } while (tamanho > 1);
        for (String l : linhas) {
            texto(cs, l, MARGEM, y, tamanho, false, DIREITA - MARGEM);
            y -= tamanho * 1.15f;
        }
    }

    private List<String> quebrar(String texto, float limite, float tamanho) throws IOException {
        var linhas = new ArrayList<String>();
        StringBuilder atual = new StringBuilder();
        for (char c : limpar(texto).toCharArray()) {
            if (largura(atual.toString() + c, NORMAL, tamanho) > limite && !atual.isEmpty()) {
                linhas.add(atual.toString());
                atual.setLength(0);
            }
            atual.append(c);
        }
        if (!atual.isEmpty()) linhas.add(atual.toString());
        return linhas;
    }

    private void texto(PDPageContentStream cs, String valor, float x, float y, float tamanho,
                       boolean negrito, float limite) throws IOException {
        String texto = limpar(valor);
        PDFont fonte = negrito ? NEGRITO : NORMAL;
        float medida = largura(texto, fonte, tamanho);
        if (medida > limite) tamanho *= limite / medida;
        cs.beginText();
        cs.setFont(fonte, tamanho);
        cs.newLineAtOffset(x, y);
        cs.showText(texto);
        cs.endText();
    }

    private void centralizado(PDPageContentStream cs, String texto, float centro, float y, float tamanho) throws IOException {
        texto(cs, texto, centro - largura(texto, NEGRITO, tamanho) / 2, y, tamanho, true, 136);
    }

    private float largura(String texto, PDFont fonte, float tamanho) throws IOException {
        return fonte.getStringWidth(limpar(texto)) / 1000 * tamanho;
    }

    private void linha(PDPageContentStream cs, float x1, float x2, float y) throws IOException {
        cs.setLineWidth(0.5f);
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
    }

    private String limpar(String valor) {
        return nulo(valor).replaceAll("[\\r\\n\\t]+", " ");
    }

    private String endereco(EmpresaModel e) {
        return nulo(e.getRua()) + (nulo(e.getNumero()).isBlank() ? "" : ", " + e.getNumero());
    }

    private String cidade(EmpresaModel e) {
        return nulo(e.getCidade()) + (nulo(e.getEstado()).isBlank() ? "" : " - " + e.getEstado());
    }

    private String valor(String texto, String padrao) {
        return texto == null || texto.isBlank() ? padrao : texto;
    }

    private String nulo(String texto) {
        return texto == null ? "" : texto;
    }
}
