package io.github.gbove73.repoarchaeologist.git;

import io.github.gbove73.repoarchaeologist.config.ArchaeologistProperties;
import java.io.IOException;
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

/** Esegue esclusivamente comandi Git in sola lettura, senza passare attraverso una shell. */
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
        if (!ALLOWED_COMMANDS.contains(command)) {
            throw new IllegalArgumentException("Comando Git non consentito: " + command);
        }

        List<String> invocation = new ArrayList<>();
        invocation.add("git");
        invocation.add("--no-pager");
        invocation.add(command);
        invocation.addAll(List.of(arguments));

        ProcessBuilder processBuilder = new ProcessBuilder(invocation)
                .directory(properties.repository().toFile())
                .redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            OutputCollector outputCollector = new OutputCollector(process, properties.maxOutputCharacters());
            Thread outputReader = Thread.ofVirtual().name("git-output-reader").start(outputCollector);
            boolean completed = process.waitFor(properties.commandTimeoutSeconds(), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                process.waitFor();
                outputReader.join();
                throw new GitCommandException("Comando Git scaduto dopo "
                        + Duration.ofSeconds(properties.commandTimeoutSeconds()));
            }
            outputReader.join();
            String output = outputCollector.result();
            if (process.exitValue() != 0) {
                throw new GitCommandException("Git ha restituito " + process.exitValue() + ": " + output);
            }
            return output;
        } catch (IOException exception) {
            throw new GitCommandException("Impossibile eseguire Git", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GitCommandException("Esecuzione Git interrotta", exception);
        }
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

    /** Consuma l'output mentre Git è attivo per evitare blocchi quando la pipe di sistema si riempie. */
    private static final class OutputCollector implements Runnable {

        private static final int BUFFER_SIZE = 4_096;

        private final Process process;
        private final int characterLimit;
        private final StringBuilder output;
        private final AtomicReference<IOException> failure = new AtomicReference<>();
        private boolean truncated;

        private OutputCollector(Process process, int characterLimit) {
            this.process = process;
            this.characterLimit = characterLimit;
            this.output = new StringBuilder(Math.min(characterLimit, BUFFER_SIZE));
        }

        @Override
        public void run() {
            char[] buffer = new char[BUFFER_SIZE];
            try (Reader reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
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
