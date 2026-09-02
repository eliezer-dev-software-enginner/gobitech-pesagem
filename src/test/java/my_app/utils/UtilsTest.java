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

    @Test
    void formatRgCpf_formataComoRgAte9Caracteres() {
        assertEquals("12", Utils.formatRgCpf("12"));
        assertEquals("12.345", Utils.formatRgCpf("12345"));
        assertEquals("12.345.678", Utils.formatRgCpf("12345678"));
        assertEquals("12.345.678-9", Utils.formatRgCpf("123456789"));
    }

    @Test
    void formatRgCpf_formataComoCpfAPartirDe10Caracteres() {
        assertEquals("123.456.789-0", Utils.formatRgCpf("1234567890"));
        assertEquals("123.456.789-01", Utils.formatRgCpf("12345678901"));
    }

    @Test
    void formatRgCpf_removeLetras() {
        assertEquals("12.345.678", Utils.formatRgCpf("1A2B3C4D5E6F7G8"));
    }

    @Test
    void formatRgCpf_vazioOuNuloRetornaVazio() {
        assertEquals("", Utils.formatRgCpf(""));
        assertEquals("", Utils.formatRgCpf(null));
    }

    @Test
    void isValidDocumento_aceitaCpfVazioOuNulo() {
        assertTrue(Utils.isValidDocumento(""));
        assertTrue(Utils.isValidDocumento(null));
    }

    @Test
    void isValidDocumento_aceitaCpfValido() {
        assertTrue(Utils.isValidDocumento("12345678901"));
        assertTrue(Utils.isValidDocumento("123.456.789-01"));
    }

    @Test
    void isValidDocumento_aceitaRgValido() {
        assertTrue(Utils.isValidDocumento("12345678"));
        assertTrue(Utils.isValidDocumento("12.345.678"));
        assertTrue(Utils.isValidDocumento("123456789"));
        assertTrue(Utils.isValidDocumento("12.345.678-9"));
    }

    @Test
    void isValidDocumento_rejeitaRgCurto() {
        assertFalse(Utils.isValidDocumento("1234567"));
        assertFalse(Utils.isValidDocumento("123"));
    }

    @Test
    void isValidDocumento_rejeitaTamanhoInvalido() {
        assertFalse(Utils.isValidDocumento("123456789012"));
        assertFalse(Utils.isValidDocumento("12345"));
    }

    @Test
    void isValidDocumento_rejeitaLetrasNoCpf() {
        assertFalse(Utils.isValidDocumento("12345A78901"));
    }
}
