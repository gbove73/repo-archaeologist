package io.github.gbove73.repoarchaeologist.git;

import io.github.gbove73.repoarchaeologist.config.ArchaeologistProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * Esegue esclusivamente comandi Git in sola lettura, senza passare attraverso una shell.
 *
 * <p>Questa classe costituisce il confine di sicurezza fra input applicativi e processo esterno:
 * accetta solo sottocomandi esplicitamente autorizzati, impone un timeout e limita la quantità di
 * testo conservata in memoria. Gli argomenti sono passati direttamente a {@link ProcessBuilder},
 * quindi caratteri speciali come spazi o punti e virgola non vengono interpretati da una shell.</p>
 */
@Component
public class GitCommandRunner {

    private static final List<String> ALLOWED_COMMANDS = List.of(
            "blame", "log", "rev-parse", "shortlog", "show", "status");

    private final ArchaeologistProperties properties;

    public GitCommandRunner(ArchaeologistProperties properties) {
        this.properties = properties;
        verifyRepository();
    }

    public String run(String command, String... arguments) {
        validateCommand(command);
        try {
            return execute(createProcess(command, arguments));
        } catch (IOException exception) {
            throw new GitCommandException("Impossibile eseguire Git", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GitCommandException("Esecuzione Git interrotta", exception);
        }
    }

    private void validateCommand(String command) {
        if (!ALLOWED_COMMANDS.contains(command)) {
            throw new IllegalArgumentException("Comando Git non consentito: " + command);
        }
    }

    private Process createProcess(String command, String[] arguments) throws IOException {
        List<String> invocation = new ArrayList<>(arguments.length + 3);
        invocation.add("git");
        invocation.add("--no-pager");
        invocation.add(command);
        invocation.addAll(List.of(arguments));
        return new ProcessBuilder(invocation)
                .directory(properties.repository().toFile())
                .redirectErrorStream(true)
                .start();
    }

    private String execute(Process process) throws InterruptedException, IOException {
        OutputCollector outputCollector = new OutputCollector(process.getInputStream(), properties.maxOutputCharacters());
        Thread outputReader = Thread.ofVirtual().name("git-output-reader").start(outputCollector);

        if (!process.waitFor(properties.commandTimeoutSeconds(), TimeUnit.SECONDS)) {
            terminate(process, outputReader);
            throw new GitCommandException("Comando Git scaduto dopo "
                    + Duration.ofSeconds(properties.commandTimeoutSeconds()));
        }

        outputReader.join();
        String output = outputCollector.result();
        if (process.exitValue() != 0) {
            throw new GitCommandException("Git ha restituito " + process.exitValue() + ": " + output);
        }
        return output;
    }

    private void terminate(Process process, Thread outputReader) throws InterruptedException {
        // La chiusura forzata sblocca anche il lettore collegato alla pipe del processo.
        process.destroyForcibly();
        process.waitFor();
        outputReader.join();
    }

    private void verifyRepository() {
        if (!properties.repository().toFile().isDirectory()) {
            throw new IllegalArgumentException("Repository inesistente: " + properties.repository());
        }
        String workTree = run("rev-parse", "--show-toplevel");
        try {
            Path configuredRepository = properties.repository().toRealPath();
            Path gitWorkTree = Path.of(workTree).toRealPath();
            if (!configuredRepository.equals(gitWorkTree)) {
                throw new IllegalArgumentException(
                        "Il percorso configurato deve coincidere con la root Git: " + gitWorkTree);
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Repository non accessibile: " + properties.repository(), exception);
        }
    }

    /**
     * Consuma l'output mentre Git è attivo per evitare blocchi quando la pipe di sistema si riempie.
     * Il testo oltre il limite viene letto e scartato: smettere di leggere bloccherebbe Git, mentre
     * conservarlo tutto renderebbe inefficace il limite di memoria.
     */
    private static final class OutputCollector implements Runnable {

        private static final int BUFFER_SIZE = 4_096;

        private final InputStream inputStream;
        private final int characterLimit;
        private final StringBuilder output;
        private final AtomicReference<IOException> failure = new AtomicReference<>();
        private boolean truncated;

        private OutputCollector(InputStream inputStream, int characterLimit) {
            this.inputStream = inputStream;
            this.characterLimit = characterLimit;
            this.output = new StringBuilder(Math.min(characterLimit, BUFFER_SIZE));
        }

        @Override
        public void run() {
            char[] buffer = new char[BUFFER_SIZE];
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                int charactersRead;
                while ((charactersRead = reader.read(buffer)) != -1) {
                    int remaining = characterLimit - output.length();
                    if (remaining > 0) {
                        output.append(buffer, 0, Math.min(remaining, charactersRead));
                    }
                    truncated |= charactersRead > remaining;
                }
            } catch (IOException exception) {
                failure.set(exception);
            }
        }

        private String result() throws IOException {
            if (failure.get() != null) {
                throw failure.get();
            }
            String result = output.toString().strip();
            return truncated ? result + "\n...[output troncato dal limite di sicurezza]" : result;
        }
    }
}
