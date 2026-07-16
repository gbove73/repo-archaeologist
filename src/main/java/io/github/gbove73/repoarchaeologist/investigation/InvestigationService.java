package io.github.gbove73.repoarchaeologist.investigation;

import io.github.gbove73.repoarchaeologist.git.RepositoryTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Coordina il modello locale e i tool Git per produrre una risposta basata su evidenze.
 *
 * <p>Il servizio non decide in anticipo quali comandi eseguire: espone al modello l'insieme
 * controllato di {@link RepositoryTools} e lascia che il tool calling scelga le evidenze utili alla
 * domanda. Ogni comando resta comunque soggetto ai controlli di sicurezza del livello Git.</p>
 */
@Service
public class InvestigationService {

    private final ChatClient chatClient;
    private final RepositoryTools repositoryTools;

    public InvestigationService(ChatClient chatClient, RepositoryTools repositoryTools) {
        this.chatClient = chatClient;
        this.repositoryTools = repositoryTools;
    }

    public String investigate(String question) {
        // La catena costruisce una singola conversazione effimera: l'MVP non conserva memoria fra richieste.
        return chatClient.prompt()
                .user(question)
                .tools(repositoryTools)
                .call()
                .content();
    }
}
