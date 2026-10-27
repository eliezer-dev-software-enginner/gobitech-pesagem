package my_app.domain.telegram;

public class TelegramNotifierFactory {
    public static TelegramNotifier create() {
        var environment=megalodonte.config.RuntimeEnvironment.forAppOrProject("gobitech-pesagem");
        try {
            String token=environment.get("GOBITECH_TELEGRAM_BOT_TOKEN");
            String chat=environment.get("GOBITECH_TELEGRAM_CHAT_ID");
            return new TelegramNotifier(token.isBlank() ? null : token,chat.isBlank() ? null : chat);
        } catch(java.io.IOException e) {
            throw new IllegalStateException("Não foi possível ler a configuração privada do Telegram.",e);
        }
    }
}
