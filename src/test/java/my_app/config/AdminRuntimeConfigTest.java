package my_app.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminRuntimeConfigTest {
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
        var error = assertThrows(IllegalStateException.class, AdminRuntimeConfig::loadRequired);
        assertEquals("Configuração runtime obrigatória ausente: "
                + AdminRuntimeConfig.LOGIN_PROPERTY, error.getMessage());
    }

    private static void set(String property, String value) {
        System.setProperty(property, Base64.getEncoder()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8)));
    }
}
