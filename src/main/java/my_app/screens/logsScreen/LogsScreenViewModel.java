package my_app.screens.logsScreen;

import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.state.State;
import my_app.domain.components.Components;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Lê o log configurado em logback.xml (mesmo arquivo pra qualquer plataforma —
 * {@code ~/.gobitech/logs/gobitech.log}) pra exibir na LogsScreen. Só leitura: nunca escreve
 * nesse arquivo, quem faz isso é o próprio Logback em background.
 */
public class LogsScreenViewModel {
    private static final Logger log = LoggerFactory.getLogger(LogsScreenViewModel.class);

    final State<String> conteudoLogs = State.of("Carregando...");

    public LogsScreenViewModel() {
        carregarLogs();
    }

    private Path pastaDeLogs() {
        return Paths.get(System.getProperty("user.home"), ".gobitech", "logs");
    }

    private Path arquivoLogPrincipal() {
        return pastaDeLogs().resolve("gobitech.log");
    }

    public void carregarLogs() {
        conteudoLogs.set("Carregando...");
        Async.Run(() -> {
            try {
                var arquivo = arquivoLogPrincipal();
                String texto = Files.exists(arquivo)
                        ? Files.readString(arquivo, StandardCharsets.UTF_8)
                        : "Nenhum log encontrado ainda em " + arquivo;
                UI.runOnUi(() -> conteudoLogs.set(texto.isBlank() ? "(log vazio)" : texto));
            } catch (IOException e) {
                log.error("Erro ao ler arquivo de log", e);
                UI.runOnUi(() -> conteudoLogs.set("Erro ao ler o log: " + e.getMessage()));
            }
        });
    }

    public void abrirPastaDeLogs() {
        try {
            var pasta = pastaDeLogs();
            if (!Files.exists(pasta)) {
                Components.ShowAlertError("A pasta de logs ainda não existe: " + pasta);
                return;
            }
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(pasta.toFile());
            } else {
                Components.ShowAlertError("Não foi possível abrir a pasta automaticamente. Caminho: " + pasta);
            }
        } catch (Exception e) {
            log.error("Erro ao abrir pasta de logs", e);
            Components.ShowAlertError("Erro ao abrir a pasta de logs: " + e.getMessage());
        }
    }
}
