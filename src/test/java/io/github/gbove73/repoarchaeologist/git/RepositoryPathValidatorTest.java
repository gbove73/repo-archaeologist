package io.github.gbove73.repoarchaeologist.git;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.gbove73.repoarchaeologist.config.ArchaeologistProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositoryPathValidatorTest {

    @TempDir
    Path repository;

    @Test
    void acceptsExistingFileInsideRepository() throws IOException {
        Files.createDirectories(repository.resolve("src"));
        Files.writeString(repository.resolve("src/example.txt"), "content");
        RepositoryPathValidator validator = validator();

        assertThat(validator.validateFile("src/example.txt")).isEqualTo("src/example.txt");
    }

    @Test
    void rejectsPathTraversal() {
        RepositoryPathValidator validator = validator();

        assertThatThrownBy(() -> validator.validateFile("../secret.txt"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSymbolicLinkOutsideRepository() throws IOException {
        Path externalFile = Files.createTempFile("repo-archaeologist-secret", ".txt");
        Files.createSymbolicLink(repository.resolve("linked-secret.txt"), externalFile);

        assertThatThrownBy(() -> validator().validateFile("linked-secret.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("simbolico");
    }

    private RepositoryPathValidator validator() {
        return new RepositoryPathValidator(new ArchaeologistProperties(repository, 5, 10_000));
    }
}
