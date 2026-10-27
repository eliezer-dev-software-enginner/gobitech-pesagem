package my_app.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;
import org.junit.jupiter.api.io.TempDir;
import megalodonte.config.RuntimeEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminRuntimeConfigTest {
    @TempDir Path folder;
    @AfterEach
    void clearProperties() {
        System.clearProperty(AdminRuntimeConfig.LOGIN_PROPERTY);
        System.clearProperty(AdminRuntimeConfig.PASSWORD_PROPERTY);
    }

    @Test
    void decodesCredentialsInjectedAsJavaOptions() {
        set(AdminRuntimeConfig.LOGIN_PROPERTY, "admin@teste.com");
        set(AdminRuntimeConfig.PASSWORD_PROPERTY, "senha com = e #");

        AdminRuntimeConfig config = AdminRuntimeConfig.loadRequired();

        assertEquals("admin@teste.com", config.login());
        assertEquals("senha com = e #", config.password());
    }

    @Test
    void rejectsMissingPropertyWithoutExposingCredentials() {
        var error = assertThrows(IllegalStateException.class,
                () -> AdminRuntimeConfig.loadRequired(new RuntimeEnvironment(folder.resolve(".env"),Map.of())));
        assertEquals("Não foi possível ler a configuração privada de GOBITECH_ADMIN_LOGIN.", error.getMessage());
    }

    @Test
    void readsCredentialsFromExternalDotenv() throws Exception {
        Path file=folder.resolve(".env");
        Files.writeString(file,"GOBITECH_ADMIN_LOGIN=admin@teste.com\nGOBITECH_ADMIN_PASSWORD='senha com = e #'\n");
        var config=AdminRuntimeConfig.loadRequired(new RuntimeEnvironment(file,Map.of()));
        assertEquals("admin@teste.com",config.login());
        assertEquals("senha com = e #",config.password());
    }

    private static void set(String property, String value) {
        System.setProperty(property, Base64.getEncoder()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8)));
    }
}
