package fr.miage.kadi.prix.domain.model;

/**
 * Ce qui accompagne un relevé, et le poids qu'on lui accorde.
 * Un ticket est le prix réellement payé ; une étiquette peut être périmée ; une saisie n'a aucune preuve.
 */
public enum TypeDePreuve {
    TICKET(1.0),
    EXPORT_FIDELITE(1.0),
    IMPORT_ENSEIGNE(1.0),
    ETIQUETTE(0.8),
    SAISIE(0.5);

    private final double poids;

    TypeDePreuve(double poids) {
        this.poids = poids;
    }

    public double poids() {
        return poids;
    }
}
