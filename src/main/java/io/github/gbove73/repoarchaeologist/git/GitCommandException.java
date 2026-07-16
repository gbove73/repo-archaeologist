package io.github.gbove73.repoarchaeologist.git;

/** Segnala un errore infrastrutturale durante l'avvio o l'esecuzione di Git. */
public final class GitCommandException extends RuntimeException {

    public GitCommandException(String message) {
        super(message);
    }

    public GitCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
