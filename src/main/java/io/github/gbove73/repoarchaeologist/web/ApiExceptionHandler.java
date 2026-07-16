package io.github.gbove73.repoarchaeologist.web;

import io.github.gbove73.repoarchaeologist.git.GitCommandException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce le eccezioni applicative in risposte HTTP uniformi basate sullo standard Problem Details. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ProblemDetail handleBadRequest(Exception exception) {
        return createProblem(HttpStatus.BAD_REQUEST, "Richiesta non valida", exception);
    }

    @ExceptionHandler(GitCommandException.class)
    ProblemDetail handleGitFailure(GitCommandException exception) {
        return createProblem(HttpStatus.UNPROCESSABLE_CONTENT, "Analisi Git non riuscita", exception);
    }

    private ProblemDetail createProblem(HttpStatus status, String title, Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(title);
        // Il timestamp aiuta a correlare la risposta con i log senza esporre dettagli interni.
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
