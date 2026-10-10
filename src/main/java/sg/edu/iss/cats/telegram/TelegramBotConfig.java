package sg.edu.iss.cats.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import jakarta.annotation.PostConstruct;

@Configuration
public class TelegramBotConfig {

    private final String botToken;
    private final CatsTelegramBot catsTelegramBot;

    public TelegramBotConfig(
            @Value("${telegram.bot.token}") String botToken,
            CatsTelegramBot catsTelegramBot) {

        this.botToken = botToken;
        this.catsTelegramBot = catsTelegramBot;
    }

    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            
            botsApplication.registerBot(
                    botToken,
                    catsTelegramBot
            );

            System.out.println("Telegram bot registered!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
