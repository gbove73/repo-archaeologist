const form = document.querySelector("#investigation-form");
const questionInput = document.querySelector("#question");
const submitButton = document.querySelector("#submit-button");
const resultPanel = document.querySelector("#result-panel");
const resultContent = document.querySelector("#result-content");
const generatedAt = document.querySelector("#generated-at");

document.querySelectorAll(".suggestion").forEach((button) => {
    button.addEventListener("click", () => {
        questionInput.value = button.textContent;
        questionInput.focus();
    });
});

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    setLoadingState(true);
    showResult("Sto consultando la cronologia Git…");

    try {
        const response = await fetch("/api/investigations", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({question: questionInput.value.trim()})
        });
        const responseBody = await response.json();

        if (!response.ok) {
            throw new Error(extractErrorMessage(responseBody));
        }

        showResult(responseBody.answer, responseBody.generatedAt);
    } catch (error) {
        // Gli errori restano leggibili nella demo senza esporre stack trace o dettagli interni.
        showResult(
            `Indagine non completata. ${error.message}`,
            null,
            true
        );
    } finally {
        setLoadingState(false);
    }
});

function setLoadingState(isLoading) {
    submitButton.disabled = isLoading;
    submitButton.firstElementChild.textContent = isLoading
        ? "Analisi in corso"
        : "Scava nella storia";
}

function showResult(message, timestamp = null, isError = false) {
    resultPanel.hidden = false;
    resultContent.textContent = message;
    resultContent.classList.toggle("error", isError);
    generatedAt.textContent = timestamp
        ? new Intl.DateTimeFormat("it-IT", {
            dateStyle: "medium",
            timeStyle: "short"
        }).format(new Date(timestamp))
        : "";
    resultPanel.scrollIntoView({behavior: "smooth", block: "start"});
}

function extractErrorMessage(responseBody) {
    if (typeof responseBody?.message === "string") {
        return responseBody.message;
    }
    return "Verifica che Ollama sia avviato e che il modello configurato sia disponibile.";
}
