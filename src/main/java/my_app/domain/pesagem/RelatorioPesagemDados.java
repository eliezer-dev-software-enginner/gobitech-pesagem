package my_app.domain.pesagem;

import my_app.db.models.PesagemModel;
import java.math.BigDecimal;
import java.util.List;

public final class RelatorioPesagemDados {
    private RelatorioPesagemDados() { }

    public record Resultado(List<List<String>> linhas, List<String> observacoes, BigDecimal totalLiquido) { }

    public static Resultado montar(List<PesagemModel> snapshotFiltrado, List<PesagemModel> entradasVinculadas) {
        // Uma linha por par Entrada+Saída (mesma placa/visita), igual ao relatório do André.
        // Tara = peso veículo da entrada; bruto/líquido do registro consolidado (a saída, quando
        // há par). Avulsas/manuais e saídas sem a entrada no snapshot entram como linha própria
        // nas colunas do seu tipo — nada some do relatório.
        var porId = new java.util.HashMap<Integer, PesagemModel>();
        for (var p : entradasVinculadas) porId.put(p.getId(), p);
        for (var p : snapshotFiltrado) porId.put(p.getId(), p);

        var consumidas = new java.util.HashSet<Integer>();
        var rows = new java.util.ArrayList<List<String>>();
        var observacoesLinha = new java.util.ArrayList<String>();

        var saidas = snapshotFiltrado.stream()
                .filter(p -> "saida".equals(p.getTipoPesagem()))
                .sorted(java.util.Comparator.comparing(PesagemModel::getDataCriacao, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();

        for (var saida : saidas) {
            var entradaId = saida.getEntradaId();
            var entrada = entradaId != null ? porId.get(entradaId) : null;
            if (entrada != null && "entrada".equals(entrada.getTipoPesagem()) && !consumidas.contains(entradaId)) {
                consumidas.add(entradaId);
                consumidas.add(saida.getId());
                rows.add(linhaPar(entrada, saida));
                observacoesLinha.add(saida.getObservacoes());
            } else {
                consumidas.add(saida.getId());
                rows.add(linhaEventoUnico(saida));
                observacoesLinha.add(saida.getObservacoes());
            }
        }

        for (var p : snapshotFiltrado) {
            if (consumidas.contains(p.getId())) continue;
            rows.add(linhaEventoUnico(p));
            observacoesLinha.add(p.getObservacoes());
        }

        var totalLiquido = rows.isEmpty() ? java.math.BigDecimal.ZERO
                : rows.stream()
                .map(r -> r.get(10))
                .map(s -> s.isBlank() ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(s))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return new Resultado(rows, observacoesLinha, totalLiquido);
    }

    private static List<String> linhaPar(PesagemModel entrada, PesagemModel saida) {
        return List.of(
                String.valueOf(entrada.getId()),
                pesoStr(entrada.getPesoVeiculo()),
                dataHora(entrada.getDataCriacao(), false),
                dataHora(entrada.getDataCriacao(), true),
                dataHora(saida.getDataCriacao(), false),
                dataHora(saida.getDataCriacao(), true),
                entrada.getPlaca() != null ? entrada.getPlaca() : "",
                entrada.getProduto() != null ? entrada.getProduto().getNome() : "---",
                entrada.getCliente() != null ? entrada.getCliente().getLoja() : "",
                pesoStr(saida.getPesoTotal()),
                pesoStr(saida.getPesoFinal())
        );
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
