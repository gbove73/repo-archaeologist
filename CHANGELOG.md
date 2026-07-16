# Changelog

Tutte le modifiche rilevanti del progetto sono documentate in questo file.

Il formato segue [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) e il progetto adotta [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Struttura iniziale dell'applicazione con Java 25, Spring Boot 4.1 e Spring AI 2.0.
- Integrazione locale con Ollama per rispondere a domande sulla storia del codice.
- Tool condivisi Spring AI e MCP per panoramica, file history, blame, ricerca e ispezione dei commit.
- API REST per avviare investigazioni in linguaggio naturale.
- Protezioni per allowlist Git, path traversal, timeout e dimensione dell'output.
- Test unitari, Maven Wrapper, configurazione SDKMAN e workflow CI.
