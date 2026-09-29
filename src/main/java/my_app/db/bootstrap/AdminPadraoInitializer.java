package my_app.db.bootstrap;

import my_app.config.AdminRuntimeConfig;
import my_app.security.CryptoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;

/** Aplica em runtime as credenciais do administrador gerenciado pelo seed. */
public final class AdminPadraoInitializer {
    private static final Logger log = LoggerFactory.getLogger(AdminPadraoInitializer.class);
    private static final String CONFIG_KEY = "admin_padrao";

    private AdminPadraoInitializer() {}

    public static void apply(String jdbcUrl) throws SQLException {
        try (var connection = DriverManager.getConnection(jdbcUrl);
             var query = connection.prepareStatement(
                     "SELECT usuario_id FROM configuracoes_runtime WHERE chave = ?")) {
            query.setString(1, CONFIG_KEY);
            try (var result = query.executeQuery()) {
                if (!result.next() || result.getObject(1) == null) {
                    log.info("Banco sem administrador padrão gerenciado; credenciais existentes preservadas");
                    return;
                }
                apply(connection, result.getLong(1), AdminRuntimeConfig.loadRequired());
            }
        }
    }

    static void apply(java.sql.Connection connection, long userId, AdminRuntimeConfig config) throws SQLException {
        String login = config.login().trim();
        String password = config.password();
        var crypto = new CryptoManager();
        String encryptedLogin = crypto.encrypt(login);
        String encryptedPassword = crypto.encrypt(password);

        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (var duplicate = connection.prepareStatement(
                    "SELECT id FROM usuarios WHERE login = ? AND id <> ?")) {
                duplicate.setString(1, encryptedLogin);
                duplicate.setLong(2, userId);
                try (var result = duplicate.executeQuery()) {
                    if (result.next()) {
                        throw new IllegalStateException(
                                "O login administrativo configurado já pertence a outro usuário.");
                    }
                }
            }

            try (var update = connection.prepareStatement(
                    "UPDATE usuarios SET login = ?, senha = ?, ativo = 1, admin = 1 WHERE id = ?")) {
                update.setString(1, encryptedLogin);
                update.setString(2, encryptedPassword);
                update.setLong(3, userId);
                if (update.executeUpdate() != 1) {
                    throw new IllegalStateException("Administrador padrão configurado não foi encontrado.");
                }
            }
            connection.commit();
            log.info("Credenciais do administrador padrão carregadas das propriedades runtime");
        } catch (RuntimeException | SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }
}
