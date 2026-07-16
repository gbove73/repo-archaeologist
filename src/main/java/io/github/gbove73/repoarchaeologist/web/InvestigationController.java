package io.github.gbove73.repoarchaeologist.web;

import io.github.gbove73.repoarchaeologist.investigation.InvestigationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Espone via HTTP il caso d'uso di investigazione, lasciando la logica al livello di servizio. */
@RestController
@RequestMapping("/api/investigations")
public class InvestigationController {

    private final InvestigationService investigationService;

    public InvestigationController(InvestigationService investigationService) {
        this.investigationService = investigationService;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public InvestigationResponse investigate(@Valid @RequestBody InvestigationRequest request) {
        String answer = investigationService.investigate(request.question());
        return new InvestigationResponse(request.question(), answer, Instant.now());
    }

    /**
     * Dati accettati dall'endpoint. Bean Validation rifiuta domande vuote o eccessivamente grandi
     * prima che possano occupare il contesto del modello locale.
     */
    public record InvestigationRequest(
            @NotBlank @Size(max = 2_000) String question) {
    }

    /** Risultato immutabile restituito al client, comprensivo dell'istante di generazione. */
    public record InvestigationResponse(String question, String answer, Instant generatedAt) {
    }
}
