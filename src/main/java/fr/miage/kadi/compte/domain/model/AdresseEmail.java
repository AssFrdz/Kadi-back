package fr.miage.kadi.compte.domain.model;

import java.util.Locale;

public record AdresseEmail(String valeur) {

    public AdresseEmail {
        if (valeur == null) {
            throw new NullPointerException("L'adresse e-mail ne peut pas être null.");
        }

        valeur = valeur.strip().toLowerCase(Locale.ROOT);
        if (valeur.isEmpty()) {
            throw new IllegalArgumentException("L'adresse e-mail ne peut pas être vide.");
        }
        if (valeur.length() > 320) {
            throw new IllegalArgumentException("L'adresse e-mail ne peut pas dépasser 320 caractères.");
        }
    }
}
