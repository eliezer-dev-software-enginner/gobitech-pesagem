package my_app.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/** Define o destino dos tickets de pré-visualização gerados em modo de desenvolvimento. */
public final class TicketDevStorage {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private TicketDevStorage() { }

    public static Path novoDestino(int pesagemId) throws IOException {
        var areaDeTrabalho = localizarAreaDeTrabalho();
        Files.createDirectories(areaDeTrabalho);
        return montarDestino(areaDeTrabalho, pesagemId, LocalDateTime.now());
    }

    static Path montarDestino(Path areaDeTrabalho, int pesagemId, LocalDateTime agora) {
        return areaDeTrabalho.resolve("ticket-pesagem-%d-%s.pdf".formatted(
                pesagemId, agora.format(TIMESTAMP)));
    }

    static Path localizarAreaDeTrabalho() {
        var candidatos = new ArrayList<Path>();
        adicionarDesktop(candidatos, System.getenv("USERPROFILE"));
        adicionarDesktop(candidatos, System.getenv("OneDrive"));
        adicionarDesktop(candidatos, System.getenv("OneDriveConsumer"));
        adicionarDesktop(candidatos, System.getProperty("user.home"));

        return candidatos.stream()
                .filter(Files::isDirectory)
                .findFirst()
                .orElseGet(() -> Path.of(System.getProperty("user.home"), "Desktop"));
    }

    private static void adicionarDesktop(ArrayList<Path> candidatos, String raiz) {
        if (raiz != null && !raiz.isBlank()) candidatos.add(Path.of(raiz, "Desktop"));
    }
}
