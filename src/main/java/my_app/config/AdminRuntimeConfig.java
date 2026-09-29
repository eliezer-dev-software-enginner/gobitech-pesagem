package my_app.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Credenciais injetadas no launcher pelo build através de opções {@code -D}. */
public record AdminRuntimeConfig(String login, String password) {
    public static final String LOGIN_PROPERTY = "gobitech.adminLoginBase64";
    public static final String PASSWORD_PROPERTY = "gobitech.adminPasswordBase64";

    public static AdminRuntimeConfig loadRequired() {
        return new AdminRuntimeConfig(
                decodeRequired(LOGIN_PROPERTY),
                decodeRequired(PASSWORD_PROPERTY));
    }

    private static String decodeRequired(String property) {
        String encoded = System.getProperty(property);
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("Configuração runtime obrigatória ausente: " + property);
        }
        try {
            String value = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            if (value.isBlank()) {
                throw new IllegalStateException("Configuração runtime vazia: " + property);
            }
            return value;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Configuração runtime inválida: " + property, e);
        }
    }
}
