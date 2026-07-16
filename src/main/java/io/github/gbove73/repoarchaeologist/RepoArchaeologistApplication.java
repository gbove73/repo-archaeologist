package io.github.gbove73.repoarchaeologist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public final class RepoArchaeologistApplication {

    private RepoArchaeologistApplication() {
        // La classe rappresenta soltanto il punto di avvio e non deve essere istanziata.
    }

    /**
     * Avvia Spring Boot, che crea e collega automaticamente controller, servizi e componenti Git.
     *
     * @param arguments opzioni eventualmente ricevute dalla riga di comando
     */
    public static void main(String[] args) {
        SpringApplication.run(RepoArchaeologistApplication.class, args);
    }
}
