package io.github.gbove73.repoarchaeologist.git;

import io.github.gbove73.repoarchaeologist.config.ArchaeologistProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * Impedisce ai tool di accedere a file esterni al repository configurato.
 *
 * <p>La verifica avviene sia sul percorso normalizzato sia sul percorso reale. Il primo controllo
 * blocca sequenze come {@code ../}; il secondo risolve i collegamenti simbolici e impedisce che un
 * file apparentemente interno punti in realtà a dati esterni alla working tree.</p>
 */
@Component
public class RepositoryPathValidator {

    private final Path repositoryRoot;

    public RepositoryPathValidator(ArchaeologistProperties properties) {
        repositoryRoot = resolveRealPath(properties.repository(), "Repository non accessibile");
    }

    public String validateFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Il percorso del file è obbligatorio");
        }
        Path normalizedCandidate = repositoryRoot.resolve(relativePath).normalize();
        if (!normalizedCandidate.startsWith(repositoryRoot)) {
            throw new IllegalArgumentException("File non valido o esterno al repository: " + relativePath);
        }
        if (!Files.isRegularFile(normalizedCandidate)) {
            throw new IllegalArgumentException("Il percorso non identifica un file regolare: " + relativePath);
        }
        Path realCandidate = resolveRealPath(normalizedCandidate, "File non accessibile");
        if (!realCandidate.startsWith(repositoryRoot)) {
            throw new IllegalArgumentException("Il collegamento simbolico esce dal repository: " + relativePath);
        }
        return repositoryRoot.relativize(realCandidate).toString();
    }

    private Path resolveRealPath(Path path, String message) {
        try {
            return path.toRealPath();
        } catch (IOException exception) {
            throw new IllegalArgumentException(message + ": " + path, exception);
        }
    }
}
