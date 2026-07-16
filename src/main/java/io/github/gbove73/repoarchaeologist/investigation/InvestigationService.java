package io.github.gbove73.repoarchaeologist.investigation;

import io.github.gbove73.repoarchaeologist.git.RepositoryTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** Coordina il modello locale e i tool Git per produrre una risposta basata su evidenze. */
@Service
public class InvestigationService {

    private final ChatClient chatClient;
    private final RepositoryTools repositoryTools;

    public InvestigationService(ChatClient chatClient, RepositoryTools repositoryTools) {
        this.chatClient = chatClient;
        this.repositoryTools = repositoryTools;
    }

    public String investigate(String question) {
        return chatClient.prompt()
                .user(question)
                .tools(repositoryTools)
                .call()
                .content();
    }
}
