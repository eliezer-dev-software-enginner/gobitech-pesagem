package my_app.db.bootstrap;

import my_app.config.AdminRuntimeConfig;
import my_app.security.CryptoManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminPadraoInitializerTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void clearRuntimeProperties() {
        System.clearProperty(AdminRuntimeConfig.LOGIN_PROPERTY);
        System.clearProperty(AdminRuntimeConfig.PASSWORD_PROPERTY);
    }

    @Test
    void appliesAndRefreshesManagedAdminCredentialsFromJavaOptionsAtRuntime() throws Exception {
        String url = "jdbc:sqlite:" + tempDir.resolve("erp.db");
        Flyway.configure().dataSource(url, "", "")
                .locations("classpath:flyway_migrations").load().migrate();

        setCredentials("primeiro@teste.com", "senha-um");

        AdminPadraoInitializer.apply(url);
        assertCredentials(url, "primeiro@teste.com", "senha-um");

        setCredentials("novo@teste.com", "senha-dois");
        AdminPadraoInitializer.apply(url);
        assertCredentials(url, "novo@teste.com", "senha-dois");
    }

    private static void setCredentials(String login, String password) {
        System.setProperty(AdminRuntimeConfig.LOGIN_PROPERTY, encode(login));
        System.setProperty(AdminRuntimeConfig.PASSWORD_PROPERTY, encode(password));
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertCredentials(String url, String login, String password) throws Exception {
        try (var connection = DriverManager.getConnection(url);
             var statement = connection.prepareStatement("""
                     SELECT u.login, u.senha, u.ativo, u.admin
                     FROM usuarios u
                     JOIN configuracoes_runtime c ON c.usuario_id = u.id
                     WHERE c.chave = 'admin_padrao'
                     """);
             var result = statement.executeQuery()) {
            result.next();
            var crypto = new CryptoManager();
            assertEquals(login, crypto.decrypt(result.getString("login")));
            assertEquals(password, crypto.decrypt(result.getString("senha")));
            assertEquals(1, result.getInt("ativo"));
            assertEquals(1, result.getInt("admin"));
        }
    }
}
