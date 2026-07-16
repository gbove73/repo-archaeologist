# Changelog

Tutte le modifiche rilevanti del progetto sono documentate in questo file.

Il formato segue [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) e il progetto adotta [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Changed

- Riorganizzate esecuzione dei processi Git, validazioni e creazione degli errori HTTP in funzioni più piccole e focalizzate, preservando il comportamento pubblico.
- Ampliata la documentazione italiana dei componenti e dei principali vincoli di sicurezza per rendere comprensibili architettura e motivazioni anche a chi non conosce Java.

## [0.1.0] - 2026-07-16

### Fixed

- Impedita la configurazione accidentale di una sottodirectory di una working tree, che avrebbe esposto a Git file esterni al percorso dichiarato.
- Evitati blocchi e consumi di memoria non limitati consumando l'output Git durante l'esecuzione e conservando solo il numero configurato di caratteri.
- Corretto il limite del tool `blameLines`, che accettava 301 righe anziché le 300 dichiarate.

### Added

- Struttura iniziale dell'applicazione con Java 25, Spring Boot 4.1 e Spring AI 2.0.
- Integrazione locale con Ollama per rispondere a domande sulla storia del codice.
- Tool condivisi Spring AI e MCP per panoramica, file history, blame, ricerca e ispezione dei commit.
- API REST per avviare investigazioni in linguaggio naturale.
- Protezioni per allowlist Git, path traversal, timeout e dimensione dell'output.
- Test unitari, Maven Wrapper, configurazione SDKMAN e workflow CI.
