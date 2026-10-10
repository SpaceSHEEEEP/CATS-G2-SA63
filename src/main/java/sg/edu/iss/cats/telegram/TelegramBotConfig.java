package sg.edu.iss.cats.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import jakarta.annotation.PostConstruct;

@Configuration
public class TelegramBotConfig {

    private final String botToken;
    private final CatsTelegramBot catsTelegramBot;

    private TelegramBotsLongPollingApplication botsApplication;

    public TelegramBotConfig(
            @Value("${telegram.bot.token:}") String botToken,
            CatsTelegramBot catsTelegramBot) {
        this.botToken = botToken;
        this.catsTelegramBot = catsTelegramBot;
    }

    @PostConstruct
    public void registerBot() {
        if (botToken.isBlank()) return; // Telegram integration is optional.
        try {
            botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, catsTelegramBot);
        } catch (Exception e) {
            throw new IllegalStateException("Telegram bot registration failed", e);
        }
    }

    @jakarta.annotation.PreDestroy
    public void stopBot() throws Exception {
        if (botsApplication != null) botsApplication.close();
    }
}
