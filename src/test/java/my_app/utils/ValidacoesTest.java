package my_app.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidacoesTest {

    @Test
    void aceitaTelefoneValidoComOuSemMascara() {
        assertDoesNotThrow(() -> Validacoes.validarTelefone("11999999999"));
        assertDoesNotThrow(() -> Validacoes.validarTelefone("(31) 99999-0000"));
    }

    @Test
    void rejeitaTelefoneComMenosDe10Digitos() {
        assertThrows(IllegalArgumentException.class, () -> Validacoes.validarTelefone("123"));
    }

    @Test
    void aceitaTelefoneNuloOuVazio() {
        assertDoesNotThrow(() -> Validacoes.validarTelefone(null));
        assertDoesNotThrow(() -> Validacoes.validarTelefone(""));
        assertDoesNotThrow(() -> Validacoes.validarTelefone("   "));
    }

    @Test
    void aceitaCepValidoComOuSemMascara() {
        assertDoesNotThrow(() -> Validacoes.validarCep("30140-071"));
        assertDoesNotThrow(() -> Validacoes.validarCep("30140071"));
    }

    @Test
    void rejeitaCepSem8Digitos() {
        assertThrows(IllegalArgumentException.class, () -> Validacoes.validarCep("abc"));
    }

    @Test
    void aceitaCepNuloOuVazio() {
        assertDoesNotThrow(() -> Validacoes.validarCep(null));
        assertDoesNotThrow(() -> Validacoes.validarCep(""));
    }

    @Test
    void aceitaCpfValidoComMascara() {
        assertDoesNotThrow(() -> Validacoes.validarCpfCnpj("111.444.777-35"));
    }

    @Test
    void aceitaCnpjValidoComMascara() {
        assertDoesNotThrow(() -> Validacoes.validarCpfCnpj("11.222.333/0001-81"));
    }

    @Test
    void rejeitaCpfInvalido() {
        assertThrows(IllegalArgumentException.class, () -> Validacoes.validarCpfCnpj("123.456.789-00"));
    }

    @Test
    void aceitaCpfCnpjNuloOuVazio() {
        assertDoesNotThrow(() -> Validacoes.validarCpfCnpj(null));
        assertDoesNotThrow(() -> Validacoes.validarCpfCnpj(""));
    }
}