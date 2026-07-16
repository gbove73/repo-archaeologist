package io.github.gbove73.repoarchaeologist.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configurazione del repository analizzato e dei limiti applicativi. */
@ConfigurationProperties(prefix = "archaeologist")
public record ArchaeologistProperties(Path repository, int commandTimeoutSeconds, int maxOutputCharacters) {

    public ArchaeologistProperties {
        repository = repository == null ? Path.of(".") : repository.toAbsolutePath().normalize();
        commandTimeoutSeconds = commandTimeoutSeconds <= 0 ? 10 : commandTimeoutSeconds;
        maxOutputCharacters = maxOutputCharacters <= 0 ? 30_000 : maxOutputCharacters;
    }
}
