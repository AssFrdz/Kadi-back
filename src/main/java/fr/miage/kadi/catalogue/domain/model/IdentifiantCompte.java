package fr.miage.kadi.catalogue.domain.model;

public record IdentifiantCompte(long valeur) {

    public IdentifiantCompte {
        if (valeur <= 0) {
            throw new IllegalArgumentException(
                    "L'identifiant du compte doit être strictement positif.");
        }
    }
}
