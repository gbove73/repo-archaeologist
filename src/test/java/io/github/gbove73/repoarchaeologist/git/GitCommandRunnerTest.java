package io.github.gbove73.repoarchaeologist.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.gbove73.repoarchaeologist.config.ArchaeologistProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitCommandRunnerTest {

    @TempDir
    Path repository;

    private GitCommandRunner runner;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        runGit("init", "-b", "main");
        runGit("config", "user.email", "test@example.com");
        runGit("config", "user.name", "Test Author");
        Files.writeString(repository.resolve("example.txt"), "first version\n");
        runGit("add", "example.txt");
        runGit("commit", "-m", "Add example");
        runner = new GitCommandRunner(new ArchaeologistProperties(repository, 5, 10_000));
    }

    @Test
    void runsAllowedReadOnlyCommand() {
        assertThat(runner.run("log", "-1", "--pretty=%s")).isEqualTo("Add example");
    }

    @Test
    void rejectsCommandOutsideAllowList() {
        assertThatThrownBy(() -> runner.run("reset", "--hard"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non consentito");
    }

    @Test
    void drainsAndTruncatesOutputWithoutBlocking() throws IOException, InterruptedException {
        String largeContent = "content\n".repeat(20_000);
        Files.writeString(repository.resolve("large.txt"), largeContent);
        runGit("add", "large.txt");
        runGit("commit", "-m", "Add large file");
        GitCommandRunner limitedRunner = new GitCommandRunner(new ArchaeologistProperties(repository, 5, 1_000));

        assertThat(limitedRunner.run("show", "HEAD"))
                .hasSizeLessThan(1_100)
                .endsWith("...[output troncato dal limite di sicurezza]");
    }

    @Test
    void rejectsRepositoryConfiguredAsWorkTreeSubdirectory() throws IOException {
        Path subdirectory = Files.createDirectory(repository.resolve("subdirectory"));

        assertThatThrownBy(() -> new GitCommandRunner(new ArchaeologistProperties(subdirectory, 5, 10_000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("root Git");
    }

    private void runGit(String... arguments) throws IOException, InterruptedException {
        String[] command = new String[arguments.length + 1];
        command[0] = "git";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        Process process = new ProcessBuilder(command).directory(repository.toFile()).start();
        assertThat(process.waitFor()).isZero();
    }
}
