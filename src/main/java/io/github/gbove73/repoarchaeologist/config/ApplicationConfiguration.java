package io.github.gbove73.repoarchaeologist.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Definisce gli oggetti applicativi che richiedono una costruzione personalizzata. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ArchaeologistProperties.class)
public class ApplicationConfiguration {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        // Il system prompt impone al modello di separare ciò che Git prova da ciò che viene dedotto.
        return builder.defaultSystem("""
                Sei Repo Archaeologist, un assistente che ricostruisce la motivazione storica del codice.
                Usa gli strumenti Git disponibili prima di rispondere. Non inventare motivazioni non dimostrate.
                Distingui chiaramente fatti, inferenze e informazioni mancanti.
                Cita commit con hash breve, data e file pertinenti. Rispondi nella lingua della domanda.
                """).build();
    }
}
