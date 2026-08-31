package my_app.infra.balanca;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculo do peso líquido de uma pesagem, isolado da ViewModel pra poder ser testado sem
 * depender da thread do JavaFX (a ViewModel não pode ser instanciada num teste JUnit puro —
 * dispara {@code Async.Run}/{@code UI.runOnUi} no construtor, que exigem o toolkit do JavaFX
 * inicializado).
 */
public class PesagemCalculo {

    private PesagemCalculo() {
    }

    /**
     * peso_liquido = (bruto - tara) - descontos% — mesma fórmula do app original
     * ({@code WeighingForm.sumFinalWheight}), só que reunindo todos os 8 tipos de desconto
     * em vez de um único percentual.
     */
    public static BigDecimal calcularPesoLiquido(BigDecimal bruto, BigDecimal tara, BigDecimal percentualDesconto) {
        var liquidoAntesDoDesconto = bruto.subtract(tara);

        var valorDesconto = liquidoAntesDoDesconto
                .multiply(percentualDesconto)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        return liquidoAntesDoDesconto.subtract(valorDesconto)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Arredonda um valor pra inteiro (sem casa decimal), como o André prefere nos pesos
     * da tela de pesagem (ex.: 70000, 15595). Retorna {@code null} se o valor for {@code null}.
     */
    public static BigDecimal arredondarInteiro(BigDecimal valor) {
        if (valor == null) return null;
        return valor.setScale(0, RoundingMode.HALF_UP);
    }
}
