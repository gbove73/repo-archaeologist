# Repo Archaeologist

Repo Archaeologist è un assistente locale e open source che aiuta a rispondere a una domanda spesso difficile nei progetti software:

> Perché questo codice esiste e quali evidenze storiche lo dimostrano?

L'applicazione analizza in sola lettura la cronologia di un repository Git, affida il ragionamento a un modello eseguito localmente con Ollama e rende gli stessi strumenti disponibili tramite REST e Model Context Protocol (MCP).

Il progetto non si limita a riassumere il codice corrente. Interroga commit, patch, rinomine e `git blame`, separando nella risposta:

- fatti direttamente verificabili;
- inferenze plausibili;
- informazioni che la cronologia non permette di stabilire.

## Stato del progetto

La versione `0.1.0` è un MVP funzionante. Può:

- descrivere repository, branch, stato e commit recenti;
- ricostruire la storia di un file seguendone le rinomine;
- attribuire intervalli di righe ai commit di origine;
- cercare un termine nei messaggi e nelle modifiche storiche;
- mostrare metadati e patch di un commit;
- combinare autonomamente questi strumenti tramite Spring AI e Ollama;
- esporre gli strumenti a client MCP tramite Streamable HTTP.

## Esempi di domande

- Perché è stata introdotta questa validazione?
- Quale commit ha aggiunto questa dipendenza e con quale motivazione?
- Questo workaround sembra ancora necessario?
- Quando è cambiato il comportamento di questa classe?
- Quali test documentano questa scelta?
- La documentazione attuale è coerente con l'evoluzione del codice?

## Architettura

```text
Utente o client REST
        |
        v
InvestigationService -----> Ollama locale
        |                       |
        +---- tool calling <----+
        |
        v
RepositoryTools <-------- Client MCP esterno
        |
        v
GitCommandRunner -----> repository Git locale (sola lettura)
```

`RepositoryTools` è il nucleo dell'applicazione. Ogni operazione è annotata sia come tool Spring AI sia come tool MCP, evitando di duplicare logica e regole di sicurezza.

### Componenti principali

| Componente | Responsabilità |
|---|---|
| `InvestigationController` | Espone l'API REST per le domande in linguaggio naturale |
| `InvestigationService` | Coordina modello locale e tool calling |
| `RepositoryTools` | Definisce le operazioni archeologiche disponibili |
| `GitCommandRunner` | Esegue comandi Git consentiti senza utilizzare una shell |
| `RepositoryPathValidator` | Blocca path traversal e file esterni al repository |

## Stack tecnologico

- Eclipse Temurin OpenJDK 25 LTS;
- Spring Boot 4.1.0;
- Spring AI 2.0.0;
- Spring AI MCP Server;
- Ollama;
- Maven Wrapper 3.9.11;
- JUnit 5 e AssertJ.

## Requisiti

- macOS, Linux o Windows con Git disponibile nel `PATH`;
- SDKMAN consigliato su macOS/Linux;
- Ollama avviato localmente;
- almeno un modello Ollama con supporto adeguato al tool calling;
- il repository da analizzare già clonato sul computer.

Il progetto non richiede IntelliJ IDEA Ultimate: IntelliJ IDEA Community Edition è sufficiente.

## Installazione di Java con SDKMAN

```bash
sdk install java 25.0.3-tem
```

Entrando nella directory del progetto:

```bash
sdk env
java -version
```

La configurazione è dichiarata in `.sdkmanrc` e non dipende dal JDK selezionato globalmente.

## Preparazione di Ollama

Il modello predefinito è `qwen3:8b`:

```bash
ollama pull qwen3:8b
ollama serve
```

È possibile usare un altro modello tramite `OLLAMA_MODEL`, purché supporti correttamente il tool calling.

## Avvio

Indicare sempre il repository che si vuole analizzare:

```bash
export ARCHAEOLOGIST_REPOSITORY=/percorso/assoluto/al/repository
./mvnw spring-boot:run
```

L'applicazione ascolta per impostazione predefinita su `http://localhost:8080`.

### Avvio da IntelliJ IDEA Community Edition

1. Aprire la directory del progetto.
2. Importare il progetto Maven quando richiesto.
3. Selezionare Temurin 25 come Project SDK.
4. Creare una configurazione per `RepoArchaeologistApplication`.
5. Aggiungere `ARCHAEOLOGIST_REPOSITORY` e, se necessario, `OLLAMA_MODEL` alle variabili d'ambiente.
6. Avviare la configurazione.

## Uso tramite API REST

```bash
curl --request POST http://localhost:8080/api/investigations \
  --header 'Content-Type: application/json' \
  --data '{"question":"Perché è stato modificato il controllo degli input?"}'
```

Risposta:

```json
{
  "question": "Perché è stato modificato il controllo degli input?",
  "answer": "...risposta con commit ed evidenze...",
  "generatedAt": "2026-07-16T08:00:00Z"
}
```

## Uso come server MCP

Il server MCP usa Streamable HTTP ed è disponibile all'endpoint:

```text
http://localhost:8080/mcp
```

Esempio concettuale di configurazione di un client MCP:

```json
{
  "mcpServers": {
    "repo-archaeologist": {
      "type": "streamable-http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

I tool pubblicati sono:

| Tool | Descrizione |
|---|---|
| `repositoryOverview` | Panoramica e commit recenti |
| `fileHistory` | Storia e patch di un file |
| `blameLines` | Origine di un intervallo di righe |
| `searchHistory` | Ricerca nei messaggi e nel contenuto delle modifiche |
| `inspectCommit` | Analisi completa di un commit |

Il formato esatto della configurazione dipende dal client MCP utilizzato.

## Configurazione

| Variabile | Predefinito | Significato |
|---|---|---|
| `ARCHAEOLOGIST_REPOSITORY` | `.` | Repository Git analizzato |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Endpoint Ollama |
| `OLLAMA_MODEL` | `qwen3:8b` | Modello locale |
| `SERVER_PORT` | `8080` | Porta HTTP |
| `GIT_COMMAND_TIMEOUT_SECONDS` | `10` | Timeout di ogni comando Git |
| `GIT_MAX_OUTPUT_CHARACTERS` | `30000` | Dimensione massima restituita da un tool |

## Sicurezza e privacy

Repo Archaeologist è progettato per lavorare localmente:

- non invia intenzionalmente codice a provider cloud;
- usa solo il repository indicato esplicitamente;
- non esegue comandi Git di scrittura;
- non passa gli argomenti attraverso `sh`, `bash` o altre shell;
- applica un'allowlist ai sottocomandi Git;
- impedisce l'accesso a file esterni al repository;
- limita durata e dimensione dell'output dei processi.

Il modello riceve comunque porzioni della cronologia e delle patch necessarie a rispondere. Prima di collegare un modello diverso da Ollama, verificare attentamente dove vengono elaborati i dati.

L'MVP non implementa autenticazione HTTP: esporlo solo su `localhost` o dietro un livello di sicurezza appropriato.

## Compilazione e test

```bash
./mvnw clean verify
```

I test unitari creano repository Git temporanei e verificano sia l'allowlist dei comandi sia la protezione dal path traversal. Non richiedono Ollama.

## Limiti attuali

- Analizza un repository alla volta.
- La qualità delle risposte dipende dal modello e dalla qualità dei messaggi di commit.
- Non indicizza ancora ADR, issue o pull request remote.
- Non conserva una knowledge base persistente.
- Non offre ancora streaming delle risposte REST.
- Non autentica i client MCP.

## Roadmap

- importazione e collegamento degli Architecture Decision Record;
- rilevamento automatico di test e documentazione correlati;
- timeline strutturata delle decisioni;
- supporto opzionale a issue e pull request GitHub;
- cache locale delle evidenze già analizzate;
- output strutturato con livello di confidenza;
- UI web minimale;
- autenticazione e policy per il server MCP.

## Contribuire

Issue, proposte e pull request sono benvenute. Prima di inviare una modifica:

```bash
./mvnw clean verify
```

Mantenere identificatori e nomi tecnici in inglese; documentazione e commenti possono essere in italiano. Le nuove operazioni Git devono essere strettamente in sola lettura, avere limiti espliciti e includere test.

## Licenza

Distribuito con licenza [Apache License 2.0](LICENSE).

## Autore

Gianluca Bove.
