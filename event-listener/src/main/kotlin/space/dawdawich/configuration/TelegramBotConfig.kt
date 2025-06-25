package space.dawdawich.configuration

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean

import org.springframework.context.annotation.Configuration

@Configuration
class TelegramBotConfig {

    @Bean
    fun telegramBot(@Value("\${app.api-token}") apiToken: String): Bot = bot {
        token = apiToken
    }
}
