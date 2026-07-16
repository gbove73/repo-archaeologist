package io.github.gbove73.repoarchaeologist.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurazione immutabile del repository analizzato e dei limiti applicativi.
 *
 * <p>Il costruttore compatto normalizza i valori una sola volta: il resto dell'applicazione può
 * quindi usare un percorso assoluto e limiti positivi senza ripetere controlli difensivi.</p>
 *
 * @param repository directory radice della working tree Git
 * @param commandTimeoutSeconds durata massima di un singolo comando Git
 * @param maxOutputCharacters numero massimo di caratteri restituiti da un comando
 */
@ConfigurationProperties(prefix = "archaeologist")
public record ArchaeologistProperties(Path repository, int commandTimeoutSeconds, int maxOutputCharacters) {

    public ArchaeologistProperties {
        // I fallback rendono sicuro anche l'uso diretto del record nei test, oltre al binding YAML.
        repository = repository == null ? Path.of(".") : repository.toAbsolutePath().normalize();
        commandTimeoutSeconds = commandTimeoutSeconds <= 0 ? 10 : commandTimeoutSeconds;
        maxOutputCharacters = maxOutputCharacters <= 0 ? 30_000 : maxOutputCharacters;
    }
}
