package io.github.gbove73.repoarchaeologist.git;

import io.github.gbove73.repoarchaeologist.config.ArchaeologistProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
            boolean completed = process.waitFor(properties.commandTimeoutSeconds(), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new GitCommandException("Comando Git scaduto dopo "
                        + Duration.ofSeconds(properties.commandTimeoutSeconds()));
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new GitCommandException("Git ha restituito " + process.exitValue() + ": " + truncate(output));
            }
            return truncate(output);
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
        run("rev-parse", "--is-inside-work-tree");
    }

    private String truncate(String output) {
        if (output.length() <= properties.maxOutputCharacters()) {
            return output.strip();
        }
        return output.substring(0, properties.maxOutputCharacters()).strip()
                + "\n...[output troncato dal limite di sicurezza]";
    }
}
