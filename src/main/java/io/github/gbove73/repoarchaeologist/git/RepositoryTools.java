package io.github.gbove73.repoarchaeologist.git;

import io.github.gbove73.repoarchaeologist.config.ArchaeologistProperties;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * Raccolta delle operazioni di lettura offerte sia al modello locale sia ai client MCP esterni.
 *
 * <p>I metodi descrivono intenzioni di dominio (per esempio «storia di un file») e delegano i due
 * aspetti infrastrutturali ai componenti dedicati: {@link GitCommandRunner} esegue Git in sicurezza,
 * mentre {@link RepositoryPathValidator} controlla i percorsi ricevuti dall'esterno.</p>
 */
@Service
public class RepositoryTools {

    private static final String LOG_FORMAT = "%h%x09%ad%x09%an%x09%s";

    private final GitCommandRunner git;
    private final RepositoryPathValidator pathValidator;
    private final ArchaeologistProperties properties;

    public RepositoryTools(
            GitCommandRunner git,
            RepositoryPathValidator pathValidator,
            ArchaeologistProperties properties) {
        this.git = git;
        this.pathValidator = pathValidator;
        this.properties = properties;
    }

    @Tool(description = "Restituisce identità, branch, stato e statistiche recenti del repository Git")
    @McpTool(description = "Restituisce identità, branch, stato e statistiche recenti del repository Git")
    public String repositoryOverview() {
        String root = git.run("rev-parse", "--show-toplevel");
        String branch = git.run("rev-parse", "--abbrev-ref", "HEAD");
        String status = git.run("status", "--short");
        String recent = git.run("log", "-10", "--date=short", "--pretty=format:" + LOG_FORMAT);
        return """
                Repository configurato: %s
                Root Git: %s
                Branch corrente: %s
                Stato: %s

                Ultimi commit:
                %s
                """.formatted(properties.repository(), root, branch, status.isBlank() ? "pulito" : status, recent);
    }

    @Tool(description = "Mostra la storia di un file, incluse rinomine e patch rilevanti")
    @McpTool(description = "Mostra la storia di un file, incluse rinomine e patch rilevanti")
    public String fileHistory(
            @ToolParam(description = "Percorso relativo del file nel repository")
            @McpToolParam(description = "Percorso relativo del file nel repository", required = true)
            String filePath) {
        String validPath = pathValidator.validateFile(filePath);
        return git.run("log", "--follow", "-p", "-12", "--date=short",
                "--pretty=format:COMMIT %h | %ad | %an | %s", "--", validPath);
    }

    @Tool(description = "Attribuisce le righe di un file ai commit che le hanno introdotte")
    @McpTool(description = "Attribuisce le righe di un file ai commit che le hanno introdotte")
    public String blameLines(
            @ToolParam(description = "Percorso relativo del file nel repository")
            @McpToolParam(description = "Percorso relativo del file nel repository", required = true)
            String filePath,
            @ToolParam(description = "Prima riga inclusa, numerata da 1")
            @McpToolParam(description = "Prima riga inclusa, required = true")
            int startLine,
            @ToolParam(description = "Ultima riga inclusa")
            @McpToolParam(description = "Ultima riga inclusa", required = true)
            int endLine) {
        validateLineRange(startLine, endLine);
        String validPath = pathValidator.validateFile(filePath);
        return git.run("blame", "--date=short", "-L", startLine + "," + endLine, "--", validPath);
    }

    @Tool(description = "Cerca commit il cui messaggio o contenuto modificato contiene un termine")
    @McpTool(description = "Cerca commit il cui messaggio o contenuto modificato contiene un termine")
    public String searchHistory(
            @ToolParam(description = "Testo letterale da cercare nella cronologia")
            @McpToolParam(description = "Testo letterale da cercare nella cronologia", required = true)
            String query) {
        validateSearchQuery(query);
        String byMessage = git.run("log", "-20", "--date=short", "--regexp-ignore-case",
                "--grep=" + query, "--pretty=format:" + LOG_FORMAT);
        String byPatch = git.run("log", "-20", "--date=short", "-S" + query,
                "--pretty=format:" + LOG_FORMAT, "--name-status");
        return "Commit trovati nel messaggio:\n" + emptyLabel(byMessage)
                + "\n\nCommit che aggiungono o rimuovono il testo:\n" + emptyLabel(byPatch);
    }

    @Tool(description = "Mostra metadati e patch completa di uno specifico commit")
    @McpTool(description = "Mostra metadati e patch completa di uno specifico commit")
    public String inspectCommit(
            @ToolParam(description = "Hash Git completo o abbreviato")
            @McpToolParam(description = "Hash Git completo o abbreviato", required = true)
            String commit) {
        if (commit == null || !commit.matches("[0-9a-fA-F]{4,40}")) {
            throw new IllegalArgumentException("Hash commit non valido");
        }
        return git.run("show", "--date=iso-strict", "--stat", "--patch", commit);
    }

    private String emptyLabel(String value) {
        return value.isBlank() ? "Nessun risultato" : value;
    }

    private void validateLineRange(int startLine, int endLine) {
        int requestedLines = endLine - startLine + 1;
        if (startLine < 1 || endLine < startLine || requestedLines > 300) {
            throw new IllegalArgumentException("Intervallo righe non valido o superiore a 300 righe");
        }
    }

    private void validateSearchQuery(String query) {
        if (query == null || query.isBlank() || query.length() > 120) {
            throw new IllegalArgumentException("La ricerca deve contenere da 1 a 120 caratteri");
        }
    }
}
