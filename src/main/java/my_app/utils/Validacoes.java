package my_app.utils;

import static pack.utilities.ValidatorPack.isValidCep;
import static pack.utilities.ValidatorPack.isValidCpfOrCnpj;
import static pack.utilities.ValidatorPack.isValidPhone;

/**
 * Validações de formato de campos opcionais (telefone, CEP, CPF/CNPJ) compartilhadas pelos
 * Services da camada de dados. Centraliza o padrão "campo preenchido mas com formato inválido →
 * {@link IllegalArgumentException}", que estava duplicado e com estilos divergentes (static
 * import × {@code ValidatorPack.}) em Cliente/Empresa/Usuário. Campo nulo ou em branco passa
 * sem validar — o campo é opcional.
 */
public final class Validacoes {

    private Validacoes() {
        // Classe utilitária: não deve ser instanciada.
    }

    public static void validarTelefone(String telefone) {
        if (preenchido(telefone) && !isValidPhone(telefone)) {
            throw new IllegalArgumentException("Telefone inválido (informe DDD + Número)");
        }
    }

    public static void validarCep(String cep) {
        if (preenchido(cep) && !isValidCep(cep)) {
            throw new IllegalArgumentException("CEP inválido");
        }
    }

    public static void validarCpfCnpj(String cpfCnpj) {
        if (preenchido(cpfCnpj) && !isValidCpfOrCnpj(cpfCnpj)) {
            throw new IllegalArgumentException("CPF/CNPJ inválido");
        }
    }

    private static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }
}