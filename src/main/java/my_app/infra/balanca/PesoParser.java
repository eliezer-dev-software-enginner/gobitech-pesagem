package my_app.infra.balanca;

import java.math.BigDecimal;

/**
 * Extrai um peso numérico do texto bruto que a balança manda pela porta serial/TCP.
 * <p>
 * Diferente do app antigo (evidência 5/6 do projeto original — ver
 * {@code /home/eliezer/Desktop/dev/outros/balanca-gobitech/docs/DECISIONS.md}), preserva o
 * separador decimal em vez de removê-lo: o app antigo tinha um bug onde "1234.5" virava "12345"
 * (10x maior), porque a regex de limpeza mantinha só dígitos/letras, descartando "." e ",".
 */
public class PesoParser {

    private PesoParser() {
    }

    public static BigDecimal parse(String textoBruto) {
        if (textoBruto == null) return null;

        String limpo = textoBruto.replaceAll("[^0-9,.\\-]", "");
        if (limpo.isBlank()) return null;

        int ultimoPonto = limpo.lastIndexOf('.');
        int ultimaVirgula = limpo.lastIndexOf(',');

        String normalizado;
        if (ultimoPonto >= 0 && ultimaVirgula >= 0) {
            // tem os dois separadores: o que aparece por último é o decimal de verdade,
            // o outro é separador de milhar e deve só ser removido.
            normalizado = ultimaVirgula > ultimoPonto
                    ? limpo.replace(".", "").replace(",", ".")
                    : limpo.replace(",", "");
        } else if (ultimaVirgula >= 0) {
            normalizado = limpo.replace(",", ".");
        } else {
            normalizado = limpo;
        }

        try {
            return new BigDecimal(normalizado);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
