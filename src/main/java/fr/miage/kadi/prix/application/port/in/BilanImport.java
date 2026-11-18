package fr.miage.kadi.prix.application.port.in;

/** Ce qu'un import a fait : relevés reçus, ajoutés, et déjà présents (import relancé). */
public record BilanImport(int recus, int ajoutes, int dejaPresents) {
}
