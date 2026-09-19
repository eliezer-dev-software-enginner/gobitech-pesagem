package my_app.domain.pesagem;

import my_app.db.models.DescontoModel;
import my_app.db.models.PesagemModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

public final class TicketPesagemDados {
    private TicketPesagemDados() { }

    public record Desconto(String nome, BigDecimal percentual, BigDecimal quilos, boolean desconta) { }

    public static LocalDateTime dataEntrada(PesagemModel pesagem, PesagemModel entrada) {
        if (entrada != null) return entrada.getDataCriacao();
        return "saida".equals(pesagem.getTipoPesagem()) ? null : pesagem.getDataCriacao();
    }

    public static LocalDateTime dataSaida(PesagemModel pesagem) {
        return "entrada".equals(pesagem.getTipoPesagem()) ? null : pesagem.getDataCriacao();
    }

    public static BigDecimal pesoEntrada(PesagemModel pesagem, PesagemModel entrada) {
        if (entrada != null) return pesoRegistrado(entrada);
        if ("entrada".equals(pesagem.getTipoPesagem())) return pesoRegistrado(pesagem);
        return nz(pesagem.getPesoVeiculo());
    }

    public static BigDecimal pesoSaida(PesagemModel pesagem) {
        return "entrada".equals(pesagem.getTipoPesagem()) ? null : pesagem.getPesoTotal();
    }

    private static BigDecimal pesoRegistrado(PesagemModel pesagem) {
        return nz(pesagem.getPesoTotal()).signum() != 0 ? pesagem.getPesoTotal() : nz(pesagem.getPesoVeiculo());
    }

    public static BigDecimal liquidoAntesDescontos(PesagemModel pesagem) {
        return nz(pesagem.getPesoTotal()).subtract(nz(pesagem.getPesoVeiculo())).max(BigDecimal.ZERO);
    }

    public static List<Desconto> descontos(PesagemModel pesagem) {
        var d = pesagem.getDesconto() == null ? new DescontoModel() : pesagem.getDesconto();
        var base = liquidoAntesDescontos(pesagem);
        return List.of(desconto("Avariados", d.getAvariados(), base, false),
                desconto("Ardidos", d.getArdidos(), base, false),
                desconto("Quebra ardidos", d.getQuebraArdidos(), base, true),
                desconto("Impurezas", d.getImpurezas(), base, false),
                desconto("Quebra impurezas", d.getQuebraImpurezas(), base, true),
                desconto("Umidade", d.getUmidade(), base, false),
                desconto("Quebra umidade", d.getQuebraUmidade(), base, true),
                desconto("Outros", d.getOutros(), base, true));
    }

    private static Desconto desconto(String nome, BigDecimal percentual, BigDecimal base, boolean desconta) {
        var p = nz(percentual);
        var quilos = desconta ? base.multiply(p).movePointLeft(2) : BigDecimal.ZERO;
        return new Desconto(nome, p, quilos, desconta);
    }

    public static BigDecimal totalDescontado(PesagemModel pesagem) {
        return descontos(pesagem).stream()
                .filter(Desconto::desconta)
                .map(Desconto::quilos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static String decimal(BigDecimal valor) {
        return nz(valor).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
