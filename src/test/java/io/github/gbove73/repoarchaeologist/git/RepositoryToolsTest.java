package io.github.gbove73.repoarchaeologist.git;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.gbove73.repoarchaeologist.config.ArchaeologistProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositoryToolsTest {

    @TempDir
    Path repository;

    private RepositoryTools tools;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        runGit("init", "-b", "main");
        runGit("config", "user.email", "test@example.com");
        runGit("config", "user.name", "Test Author");
        Files.writeString(repository.resolve("example.txt"), "line\n".repeat(301));
        runGit("add", "example.txt");
        runGit("commit", "-m", "Add example");

        ArchaeologistProperties properties = new ArchaeologistProperties(repository, 5, 10_000);
        tools = new RepositoryTools(
                new GitCommandRunner(properties),
                new RepositoryPathValidator(properties),
                properties);
    }

    @Test
    void acceptsAtMostThreeHundredBlameLines() {
        tools.blameLines("example.txt", 1, 300);

        assertThatThrownBy(() -> tools.blameLines("example.txt", 1, 301))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("300 righe");
    }

    private void runGit(String... arguments) throws IOException, InterruptedException {
        String[] command = new String[arguments.length + 1];
        command[0] = "git";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        Process process = new ProcessBuilder(command).directory(repository.toFile()).start();
        if (process.waitFor() != 0) {
            throw new IllegalStateException("Preparazione del repository Git di test non riuscita");
        }
    }
}
