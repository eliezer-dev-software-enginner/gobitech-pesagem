package my_app.infra;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TicketDevStorageTest {

    @Test
    void destinoUsaAreaDeTrabalhoENomeIdentificavel() {
        var desktop = Path.of("perfil", "Desktop");
        var destino = TicketDevStorage.montarDestino(
                desktop, 42, LocalDateTime.of(2026, 9, 29, 14, 30, 45));

        assertEquals(desktop, destino.getParent());
        assertEquals("ticket-pesagem-42-20260929-143045.pdf", destino.getFileName().toString());
    }
}
