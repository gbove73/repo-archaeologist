"use client";

import { FormEvent, useState } from "react";

const suggestions = [
  "Perché esiste RepositoryPathValidator?",
  "Quali vincoli di sicurezza emergono dalla storia?",
  "Quando è stato introdotto il supporto MCP?",
];

type InvestigationResponse = { answer?: string; generatedAt?: string; message?: string };

async function readInvestigationResponse(response: Response): Promise<InvestigationResponse> {
  const contentType = response.headers.get("content-type") ?? "";

  // Nginx può restituire una pagina HTML quando il modello supera il timeout:
  // non la esponiamo all'utente e traduciamo lo stato HTTP in un messaggio utile.
  if (!contentType.includes("application/json")) {
    if (response.status === 504) {
      throw new Error("Il modello locale ha impiegato troppo tempo. Riprova con una domanda più specifica.");
    }
    if (response.status === 502 || response.status === 503) {
      throw new Error("Il modello locale non è momentaneamente disponibile. Riprova tra poco.");
    }
    throw new Error(`Il servizio ha restituito una risposta non valida (errore ${response.status}).`);
  }

  try {
    return await response.json() as InvestigationResponse;
  } catch {
    throw new Error("Il servizio ha restituito una risposta incompleta. Riprova tra poco.");
  }
}

export default function Home() {
  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState("");
  const [generatedAt, setGeneratedAt] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isError, setIsError] = useState(false);

  async function investigate(event: FormEvent) {
    event.preventDefault();
    setIsLoading(true);
    setIsError(false);
    setAnswer("Sto attraversando commit, patch e rinomine…");

    try {
      const response = await fetch("api/investigations", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ question: question.trim() }) });
      const body = await readInvestigationResponse(response);
      if (!response.ok) throw new Error(body.message || "Il reperto non è disponibile.");
      setAnswer(body.answer || "Nessuna evidenza trovata.");
      setGeneratedAt(body.generatedAt || "");
    } catch (error) {
      setIsError(true);
      setAnswer(`Indagine non completata. ${error instanceof Error ? error.message : "Verifica il modello locale."}`);
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <main className="site-shell">
      <nav>
        <a className="logo" href="#top"><span>RA</span><b>REPO / ARCHAEOLOGIST</b></a>
        <div className="nav-links"><a href="#investigate">INDAGA</a><a href="#method">METODO</a><a href="https://github.com/gbove73/repo-archaeologist">SOURCE ↗</a></div>
        <span className="system-state"><i /> LOCAL AI / READY</span>
      </nav>
      <a className="home-return" href="https://gianlucabove.it/">← TORNA ALLA HOME</a>

      <section className="hero" id="top">
        <div className="coordinate">41°54&apos;N / 12°29&apos;E <i /> ARCHIVE NODE 01</div>
        <h1>EVERY LINE<br />HAS A <em>PAST.</em></h1>
        <div className="hero-bottom">
          <p>Scava nella cronologia Git. Distingui i fatti dalle inferenze. Ricostruisci il perché del codice, senza inviare nulla fuori dalla tua macchina.</p>
          <a href="#investigate">APRI UN’INDAGINE <span>↓</span></a>
        </div>
        <div className="strata" aria-hidden="true"><i /><i /><i /><i /><i /><i /></div>
        <div className="metrics"><div><strong>05</strong><span>GIT TOOLS</span></div><div><strong>100%</strong><span>READ ONLY</span></div><div><strong>0</strong><span>CLOUD CALLS</span></div></div>
      </section>

      <section className="investigation" id="investigate">
        <header><span>01 / FIELD CONSOLE</span><div><h2>INTERROGA<br /><em>L’ARCHIVIO.</em></h2><p>Fai una domanda in linguaggio naturale. L’agente sceglie gli strumenti Git, raccoglie evidenze e segnala ciò che la storia non può dimostrare.</p></div></header>
        <div className="console">
          <aside>
            <div className="aside-title">AVAILABLE INSTRUMENTS <b>05</b></div>
            {["REPOSITORY OVERVIEW", "FILE HISTORY", "BLAME LINES", "SEARCH HISTORY", "INSPECT COMMIT"].map((tool, index) => <div className="tool" key={tool}><span>0{index + 1}</span><b>{tool}</b><i>READY</i></div>)}
            <div className="security-note"><b>READ-ONLY PERIMETER</b><p>Allowlist dei comandi, path validation, timeout e output limitato.</p></div>
          </aside>
          <form onSubmit={investigate}>
            <div className="form-head"><span>INVESTIGATION QUERY</span><small>MAX 2000 CHAR</small></div>
            <label htmlFor="question">Cosa vuoi portare alla luce?</label>
            <textarea id="question" maxLength={2000} required value={question} onChange={(event) => setQuestion(event.target.value)} placeholder="Perché questa validazione esiste ancora?" />
            <div className="suggestions">{suggestions.map((suggestion) => <button type="button" key={suggestion} onClick={() => setQuestion(suggestion)}>{suggestion}</button>)}</div>
            <div className="actions"><span><i /> OLLAMA · QWEN3:8B</span><button disabled={isLoading}>{isLoading ? "SCAVO IN CORSO…" : "SCAVA NELLA STORIA"}<b>→</b></button></div>
          </form>
        </div>

        {answer && <section className={`evidence ${isError ? "error" : ""}`} aria-live="polite"><header><div><span>02</span><h3>REPERTO / EVIDENCE LOG</h3></div><time>{generatedAt ? new Intl.DateTimeFormat("it-IT", { dateStyle: "medium", timeStyle: "short" }).format(new Date(generatedAt)) : "ANALISI ATTIVA"}</time></header><pre>{answer}</pre></section>}
      </section>

      <section className="method" id="method">
        <span>02 / ARCHAEOLOGICAL METHOD</span>
        <div className="method-grid"><article><b>01</b><h3>OSSERVA.</h3><p>Commit, patch, blame e rinomine diventano reperti verificabili, mai opinioni travestite da fatti.</p></article><article><b>02</b><h3>COLLEGA.</h3><p>Il modello locale attraversa le tracce e ricostruisce le relazioni tra decisioni, test e codice.</p></article><article><b>03</b><h3>DICHIARA.</h3><p>La risposta separa evidenze, inferenze plausibili e limiti espliciti della cronologia.</p></article></div>
      </section>
      <footer><span>REPO ARCHAEOLOGIST / 0.1.1</span><p>SPRING BOOT · MCP · OLLAMA</p><a href="https://gianlucabove.it">GIANLUCABOVE.IT ↗</a></footer>
    </main>
  );
}
