package my_app.domain.pesagem;

import my_app.db.models.ClienteModel;
import my_app.db.models.PesagemModel;
import my_app.db.models.ProdutoModel;
import my_app.infra.balanca.PesagemCalculo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Regras de validação e preenchimento do formulário de pesagem (soma dos descontos ≤ 100%,
 * bruto < tara → líquido negativo, nenhum peso informado, preenchimento a partir da Entrada na
 * Saída e slot/nome das fotos), isoladas da ViewModel pra poderem ser testadas sem depender da
 * thread do JavaFX — a ViewModel dispara {@code Async.Run}/{@code UI.runOnUi} no construtor (ver
 * {@link PesagemCalculo}). Os campos de peso chegam como {@code String} (é assim que os States da
 * tela guardam); String nula/vazia/malformada vale ZERO, mesmo comportamento do
 * {@code parseDecimal} da ViewModel.
 */
public final class PesagemRegras {

    private PesagemRegras() {
        // Classe utilitária: não deve ser instanciada.
    }

    // ---- descontos ----

    /**
     * Soma os percentuais de desconto informados (null/vazio = 0).
     */
    public static BigDecimal somarDescontos(String... percentuais) {
        BigDecimal soma = BigDecimal.ZERO;
        if (percentuais == null) return soma;
        for (var p : percentuais) {
            soma = soma.add(parseDecimal(p));
        }
        return soma;
    }

    /**
     * true quando a soma dos percentuais de desconto ultrapassa 100% (o líquido ficaria
     * negativo qualquer que fosse o bruto/tara).
     */
    public static boolean descontosUltrapassam100(BigDecimal somaDosDescontos) {
        return somaDosDescontos != null && somaDosDescontos.compareTo(new BigDecimal("100")) > 0;
    }

    // ---- pesos ----

    /**
     * true quando bruto e tara estão preenchidos e o peso líquido calculado
     * (bruto − tara − descontos) ficaria negativo. Com algum peso vazio não há o que avaliar
     * (fluxo "só Tara"/pesagem manual sem bruto continua permitido).
     */
    public static boolean liquidoNegativo(String bruto, String tara, BigDecimal percentualDesconto) {
        if (vazio(bruto) || vazio(tara)) return false;
        var liquido = PesagemCalculo.calcularPesoLiquido(
                parseDecimal(bruto), parseDecimal(tara), percentualDesconto == null ? BigDecimal.ZERO : percentualDesconto);
        return liquido != null && liquido.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * true quando nenhum peso foi informado (Tara e Peso bruto vazios) — caso em que a tela
     * pede confirmação antes de salvar.
     */
    public static boolean nenhumPesoInformado(String tara, String bruto) {
        return vazio(tara) && vazio(bruto);
    }

    // ---- preenchimento da Entrada (Saída) ----

    public record PreenchimentoEntrada(String motoristaNome, String motoristaDocumento, String notaFiscal,
                                       String pesoVeiculo, String pesoTotal,
                                       ClienteModel cliente, ProdutoModel produto) {
    }

    /**
     * Extrai da última Entrada da placa os valores do formulário de Saída (motorista, nota,
     * pesos arredondados) e resolve o Cliente/Produto pelo id na lista carregada na tela —
     * sem tocar em State nem em banco. Campos nulos viram vazio; cliente/produto só são
     * vinculados se o id existir na lista (senão null, como na tela).
     */
    public static PreenchimentoEntrada preencherDaEntrada(PesagemModel entrada,
                                                          List<ClienteModel> clientes,
                                                          List<ProdutoModel> produtos) {
        var cliente = entrada.getClienteId() == null ? null
                : procurarCliente(entrada.getClienteId(), clientes);
        var produto = entrada.getProdutoId() == null ? null
                : procurarProduto(entrada.getProdutoId(), produtos);

        return new PreenchimentoEntrada(
                texto(entrada.getMotoristaNome()),
                texto(entrada.getMotoristaDocumento()),
                texto(entrada.getNotaFiscal()),
                pesoInteiro(entrada.getPesoVeiculo()),
                pesoInteiro(entrada.getPesoTotal()),
                cliente,
                produto);
    }

    // ---- fotos ----

    /**
     * Slot da foto capturada pela câmera: entrada/avulsa/manual preenchem o slot 1 e a saída o
     * slot 2 (mesma lógica das duas "visitas" por placa que existia no modelo antigo).
     */
    public static boolean usarSlot2(String tipoPesagem) {
        return "saida".equals(tipoPesagem);
    }

    /**
     * Nome do arquivo da foto capturada: {@code pesagem_<id>_<frente|costas>_<1|2>.jpg}.
     */
    public static String nomeArquivoFoto(int pesagemId, String rotulo, boolean slot2) {
        return "pesagem_" + pesagemId + "_" + rotulo + "_" + (slot2 ? "2" : "1") + ".jpg";
    }

    // ---- helpers ----

    private static ClienteModel procurarCliente(Integer id, List<ClienteModel> clientes) {
        if (clientes == null) return null;
        return clientes.stream().filter(c -> id.equals(c.getId())).findFirst().orElse(null);
    }

    private static ProdutoModel procurarProduto(Integer id, List<ProdutoModel> produtos) {
        if (produtos == null) return null;
        return produtos.stream().filter(p -> id.equals(p.getId())).findFirst().orElse(null);
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private static String pesoInteiro(BigDecimal valor) {
        if (valor == null) return "";
        return PesagemCalculo.arredondarInteiro(valor).toBigInteger().toString();
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static BigDecimal parseDecimal(String valor) {
        try {
            return vazio(valor) ? BigDecimal.ZERO : new BigDecimal(valor.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}