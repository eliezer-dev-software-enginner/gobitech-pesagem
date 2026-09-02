package my_app.utils;

public class Utils {

    /**
     * Data/hora atual em formato seguro pra nome de arquivo (sem {@code /} ou {@code :} que
     * quebrariam no SO): {@code dd-MM-yyyy_HHmm} (dia-mês-ano, pois não deve ser americano).
     * Usado nos nomes iniciais de relatório e ticket ao baixar (ex.: {@code relatório - 02-09-2026_1530.pdf}).
     */
    public static String timestampParaArquivo() {
        return java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy_HHmm"));
    }
}
