package my_app.infra;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

public class CsvExporter {

    private static final String SEPARATOR = ";";
    private static final String UTF8_BOM = "\uFEFF";

    public static void exportar(File destino, List<String> headers, List<List<String>> rows) throws Exception {
        try (var writer = new PrintWriter(destino, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.print(UTF8_BOM);
            writer.println(String.join(SEPARATOR, headers));
            for (var row : rows) {
                writer.println(String.join(SEPARATOR, row));
            }
        }
    }
}
