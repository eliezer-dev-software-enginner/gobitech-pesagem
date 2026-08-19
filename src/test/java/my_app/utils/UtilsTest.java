package my_app.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void isValidCnpj_aceitaNumericoValido() {
        assertTrue(Utils.isValidCnpj("11222333000181"));
    }

    @Test
    void isValidCnpj_aceitaNumericoComMascara() {
        assertTrue(Utils.isValidCnpj("11.222.333/0001-81"));
    }

    @Test
    void isValidCnpj_aceitaAlfanumericoValido() {
        assertTrue(Utils.isValidCnpj("12ABC34501DE35"));
    }

    @Test
    void isValidCnpj_aceitaAlfanumericoComMascara() {
        assertTrue(Utils.isValidCnpj("12.ABC.345/01DE-35"));
    }

    @Test
    void isValidCnpj_rejeitaNull() {
        assertFalse(Utils.isValidCnpj(null));
    }

    @Test
    void isValidCnpj_rejeitaVazio() {
        assertFalse(Utils.isValidCnpj(""));
    }

    @Test
    void isValidCnpj_rejeitaTamanhoIncorreto() {
        assertFalse(Utils.isValidCnpj("123"));
    }

    @Test
    void isValidCnpj_rejeitaUltimosDigitosComLetra() {
        assertFalse(Utils.isValidCnpj("112223330001AB"));
    }

    @Test
    void isValidCnpj_rejeitaLetrasInvalidas() {
        assertFalse(Utils.isValidCnpj("OI__ABC__DE__FG"));
    }

    @Test
    void isValidCnpj_aceitaMistoNumericoAlfanumerico() {
        assertTrue(Utils.isValidCnpj("12AB3456789C11"));
    }

    @Test
    void isValidCnpj_rejeitaUltimosDigitosLetra() {
        assertFalse(Utils.isValidCnpj("12ABC34501DEAB"));
    }

    @Test
    void isValidCnpj_aceitaMistoLetrasDigitos() {
        assertTrue(Utils.isValidCnpj("AB123456789082"));
    }

    @Test
    void formatCpfCnpj_formataComoCpfAte11Caracteres() {
        assertEquals("123", Utils.formatCpfCnpj("123"));
        assertEquals("123.456", Utils.formatCpfCnpj("123456"));
        assertEquals("123.456.789", Utils.formatCpfCnpj("123456789"));
        assertEquals("123.456.789-01", Utils.formatCpfCnpj("12345678901"));
    }

    @Test
    void formatCpfCnpj_formataComoCnpjAPartirDe12Caracteres() {
        assertEquals("12.345.678/9012", Utils.formatCpfCnpj("123456789012"));
        assertEquals("12.345.678/9012-34", Utils.formatCpfCnpj("12345678901234"));
    }

    @Test
    void formatCpfCnpj_aceitaCnpjAlfanumericoAcimaDe11Caracteres() {
        assertEquals("12.ABC.345/01DE-35", Utils.formatCpfCnpj("12ABC34501DE35"));
    }

    @Test
    void formatCpfCnpj_removeLetrasQuandoAindaNaFaixaDeCpf() {
        // enquanto o tamanho ainda sugere CPF, letras digitadas por engano são descartadas —
        // CPF é sempre puramente numérico
        assertEquals("123", Utils.formatCpfCnpj("1A2B3"));
    }

    @Test
    void formatCpfCnpj_vazioOuNuloRetornaVazio() {
        assertEquals("", Utils.formatCpfCnpj(""));
        assertEquals("", Utils.formatCpfCnpj(null));
    }
}
