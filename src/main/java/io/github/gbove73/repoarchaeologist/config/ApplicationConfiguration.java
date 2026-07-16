package io.github.gbove73.repoarchaeologist.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ArchaeologistProperties.class)
public class ApplicationConfiguration {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultSystem("""
                Sei Repo Archaeologist, un assistente che ricostruisce la motivazione storica del codice.
                Usa gli strumenti Git disponibili prima di rispondere. Non inventare motivazioni non dimostrate.
                Distingui chiaramente fatti, inferenze e informazioni mancanti.
                Cita commit con hash breve, data e file pertinenti. Rispondi nella lingua della domanda.
                """).build();
    }
}
