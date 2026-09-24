package my_app.domain.pesagem;

import my_app.db.models.PesagemModel;
import java.math.BigDecimal;
import java.util.List;

public final class RelatorioPesagemDados {
    private RelatorioPesagemDados() { }

    public record Resultado(List<List<String>> linhas, List<String> observacoes, BigDecimal totalLiquido) { }

    public static Resultado montar(List<PesagemModel> snapshotFiltrado) {
        // O PDF espelha a lista filtrada: cada pesagem visível gera uma linha, inclusive uma
        // Entrada e a Saída vinculada. Agrupar o par ocultava um registro da exportação.
        var rows = new java.util.ArrayList<List<String>>();
        var observacoesLinha = new java.util.ArrayList<String>();
        for (var p : snapshotFiltrado) {
            rows.add(linhaEventoUnico(p));
            observacoesLinha.add(p.getObservacoes());
        }

        // As duas linhas do par devem aparecer, mas o total líquido não pode contar a mesma
        // operação duas vezes: quando a Saída está no filtro, ela representa o peso do par.
        var entradasComSaidaNoFiltro = snapshotFiltrado.stream()
                .filter(p -> "saida".equals(p.getTipoPesagem()))
                .map(PesagemModel::getEntradaId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        var totalLiquido = snapshotFiltrado.stream()
                .filter(p -> !"entrada".equals(p.getTipoPesagem()) || !entradasComSaidaNoFiltro.contains(p.getId()))
                .map(PesagemModel::getPesoFinal)
                .map(valor -> valor == null ? java.math.BigDecimal.ZERO : valor)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return new Resultado(rows, observacoesLinha, totalLiquido);
    }

    private static List<String> linhaEventoUnico(PesagemModel p) {
        return List.of(
                String.valueOf(p.getId()),
                pesoStr(p.getPesoVeiculo()),
                dataHora(TicketPesagemDados.dataEntrada(p, null), false),
                dataHora(TicketPesagemDados.dataEntrada(p, null), true),
                dataHora(TicketPesagemDados.dataSaida(p), false),
                dataHora(TicketPesagemDados.dataSaida(p), true),
                p.getPlaca() != null ? p.getPlaca() : "",
                p.getProduto() != null ? p.getProduto().getNome() : "---",
                p.getCliente() != null ? p.getCliente().getLoja() : "",
                pesoStr(p.getPesoTotal()),
                pesoStr(p.getPesoFinal())
        );
    }

    /**
     * Formata um valor de peso (Tara/Peso bruto/Peso líquido) como número inteiro, sem casas
     * decimais — o relatório não deve exibir frações de Kg, e valores de ponto flutuante
     * (ex: 31999.900390625, resultado de imprecisão de double) nunca devem ir pro PDF como
     * estão, porque inflam a largura das colunas e derrubam o layout da tabela.
     */
    public static String pesoStr(Number valor) {
        if (valor == null) return "0";
        return String.valueOf(Math.round(valor.doubleValue()));
    }

    /** data (dd/MM/yyyy) ou hora (HH:mm:ss) de um LocalDateTime. */
    private static String dataHora(java.time.LocalDateTime dataHora, boolean soHora) {
        if (dataHora == null) return "";
        if (soHora) return dataHora.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        return dataHora.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
