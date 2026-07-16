package io.github.gbove73.repoarchaeologist.web;

import io.github.gbove73.repoarchaeologist.git.GitCommandException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ProblemDetail handleBadRequest(Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Richiesta non valida");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(GitCommandException.class)
    ProblemDetail handleGitFailure(GitCommandException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
        problem.setTitle("Analisi Git non riuscita");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
