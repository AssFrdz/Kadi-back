package fr.miage.kadi.prix.domain.model;

import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;

/** Ce qu'on peut croire d'un prix de référence : un score entre 0 et 1, et son niveau. */
public record Confiance(double score) {

    public Confiance {
        if (Double.isNaN(score) || score < 0 || score > 1) {
            throw new DonneeInvalideException("Un score de confiance est compris entre 0 et 1 : " + score);
        }
    }

    public NiveauDeConfiance niveau() {
        return NiveauDeConfiance.depuis(score);
    }
}
