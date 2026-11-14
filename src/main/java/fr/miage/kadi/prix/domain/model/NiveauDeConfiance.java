package fr.miage.kadi.prix.domain.model;

/** Le niveau affiché au ménage, déduit du score de confiance. */
public enum NiveauDeConfiance {
    ELEVEE,
    MOYENNE,
    FAIBLE;

    static NiveauDeConfiance depuis(double score) {
        if (score >= 0.60) {
            return ELEVEE;
        }
        return score >= 0.35 ? MOYENNE : FAIBLE;
    }
}
