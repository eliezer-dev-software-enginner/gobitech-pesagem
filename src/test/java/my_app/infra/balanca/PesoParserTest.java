package my_app.infra.balanca;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PesoParserTest {

    @Test
    void deveParsearNumeroInteiro() {
        assertEquals(0, new BigDecimal("1234").compareTo(PesoParser.parse("1234")));
    }

    @Test
    void devePreservarSeparadorDecimalComPonto() {
        // o bug do app antigo: "1234.5" virava "12345" (10x maior) por remover o ponto
        assertEquals(0, new BigDecimal("1234.5").compareTo(PesoParser.parse("1234.5")));
    }

    @Test
    void devePreservarSeparadorDecimalComVirgula() {
        assertEquals(0, new BigDecimal("1234.5").compareTo(PesoParser.parse("1234,5")));
    }

    @Test
    void deveTratarFormatoBrasileiroComMilharEDecimal() {
        // "1.234,5" -> ponto é milhar, vírgula é decimal (formato BR)
        assertEquals(0, new BigDecimal("1234.5").compareTo(PesoParser.parse("1.234,5")));
    }

    @Test
    void deveTratarFormatoAmericanoComMilharEDecimal() {
        // "1,234.5" -> vírgula é milhar, ponto é decimal (formato US)
        assertEquals(0, new BigDecimal("1234.5").compareTo(PesoParser.parse("1,234.5")));
    }

    @Test
    void deveIgnorarLetrasEEspacosAoRedor() {
        assertEquals(0, new BigDecimal("1234.5").compareTo(PesoParser.parse("PESO: 1234.5 kg")));
    }

    @Test
    void deveRetornarNullParaTextoSemNumero() {
        assertNull(PesoParser.parse("sem numero nenhum aqui"));
    }

    @Test
    void deveRetornarNullParaEntradaNula() {
        assertNull(PesoParser.parse(null));
    }

    @Test
    void deveRetornarNullParaTextoVazio() {
        assertNull(PesoParser.parse(""));
    }

    @Test
    void deveParsearNumeroComSinalDeMenos() {
        assertEquals(0, new BigDecimal("-10").compareTo(PesoParser.parse("-10")));
    }
}
