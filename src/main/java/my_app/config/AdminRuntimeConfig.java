package my_app.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import megalodonte.config.RuntimeEnvironment;

/** Admin credentials loaded at runtime from the user configuration or environment. */
public record AdminRuntimeConfig(String login, String password) {
    public static final String LOGIN_PROPERTY = "gobitech.adminLoginBase64";
    public static final String PASSWORD_PROPERTY = "gobitech.adminPasswordBase64";

    public static AdminRuntimeConfig loadRequired() {
        return loadRequired(RuntimeEnvironment.forAppOrProject("gobitech-pesagem"));
    }

    static AdminRuntimeConfig loadRequired(RuntimeEnvironment environment) {
        return new AdminRuntimeConfig(
                runtimeValue(environment,"GOBITECH_ADMIN_LOGIN",LOGIN_PROPERTY),
                runtimeValue(environment,"GOBITECH_ADMIN_PASSWORD",PASSWORD_PROPERTY));
    }

    private static String runtimeValue(RuntimeEnvironment environment,String key,String legacyProperty) {
        if(System.getProperty(legacyProperty)!=null) return decodeRequired(legacyProperty);
        try {
            return environment.require(key);
        } catch(java.io.IOException e) {
            throw new IllegalStateException("Não foi possível ler a configuração privada de "+key+".",e);
        }
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
