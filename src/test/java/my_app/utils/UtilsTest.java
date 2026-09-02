package my_app.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {

    @Test
    void timestampParaArquivo_formatoSeguroParaNomeDeArquivo() {
        String ts = Utils.timestampParaArquivo();
        assertTrue(ts.matches("\\d{2}-\\d{2}-\\d{4}_\\d{4}"),
                "esperado dd-MM-yyyy_HHmm, veio: " + ts);
        assertFalse(ts.contains("/"), "não pode conter barra (quebra caminho do SO)");
        assertFalse(ts.contains(":"), "não pode conter dois-pontos (invalido no Windows)");
    }
}
