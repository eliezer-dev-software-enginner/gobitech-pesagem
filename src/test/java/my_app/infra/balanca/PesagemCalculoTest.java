package my_app.infra.balanca;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PesagemCalculoTest {

    private void assertPeso(String esperado, BigDecimal calculado) {
        assertEquals(0, new BigDecimal(esperado).compareTo(calculado),
                "Esperado " + esperado + " mas foi " + calculado);
    }

    @Test
    void semDescontoLiquidoEhSoBrutoMenosTara() {
        var liquido = PesagemCalculo.calcularPesoLiquido(
                BigDecimal.valueOf(32000), BigDecimal.valueOf(8500), BigDecimal.ZERO);
        assertPeso("23500.00", liquido);
    }

    @Test
    void comUmDescontoDeCincoPorCentoSubtraiCincoPorCentoDoLiquido() {
        // bruto - tara = 20000 ; 5% de 20000 = 1000 ; líquido final = 19000
        var liquido = PesagemCalculo.calcularPesoLiquido(
                BigDecimal.valueOf(30000), BigDecimal.valueOf(10000), BigDecimal.valueOf(5));
        assertPeso("19000.00", liquido);
    }

    @Test
    void somaVariosTiposDeDescontoComoUmSoPercentual() {
        // simula o que a ViewModel faz: soma avariados+ardidos+...+outros antes de chamar aqui.
        // 2% + 1% + 3% + 14% = 20% de desconto sobre 10000 (bruto-tara) = 2000
        var percentualTotal = BigDecimal.valueOf(2)
                .add(BigDecimal.valueOf(1))
                .add(BigDecimal.valueOf(3))
                .add(BigDecimal.valueOf(14));
        var liquido = PesagemCalculo.calcularPesoLiquido(
                BigDecimal.valueOf(18000), BigDecimal.valueOf(8000), percentualTotal);
        assertPeso("8000.00", liquido);
    }

    @Test
    void taraIgualAoBrutoDaLiquidoZero() {
        var liquido = PesagemCalculo.calcularPesoLiquido(
                BigDecimal.valueOf(8500), BigDecimal.valueOf(8500), BigDecimal.ZERO);
        assertPeso("0.00", liquido);
    }

    @Test
    void arredondaParaDuasCasasDecimais() {
        // 10000 - 3333 = 6667 ; 3% de 6667 = 200.01 ; líquido = 6466.99
        var liquido = PesagemCalculo.calcularPesoLiquido(
                BigDecimal.valueOf(10000), BigDecimal.valueOf(3333), BigDecimal.valueOf(3));
        assertPeso("6466.99", liquido);
    }

    @Test
    void cemPorCentoDeDescontoZeraOLiquido() {
        var liquido = PesagemCalculo.calcularPesoLiquido(
                BigDecimal.valueOf(10000), BigDecimal.valueOf(2000), BigDecimal.valueOf(100));
        assertPeso("0.00", liquido);
    }
}
