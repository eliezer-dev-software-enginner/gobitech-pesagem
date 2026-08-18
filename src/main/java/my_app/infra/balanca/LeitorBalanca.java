package my_app.infra.balanca;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Lê o peso da balança continuamente (serial ou TCP) e entrega cada leitura via callback —
 * não é um "ler uma vez e retornar", é um listener que fica ativo até {@link #parar()}.
 */
public interface LeitorBalanca {
    void iniciar(Consumer<BigDecimal> onPeso, Consumer<String> onErro);

    void parar();
}
