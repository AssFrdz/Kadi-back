package fr.miage.kadi.prix.domain.model;

import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;
import java.time.Instant;

/**
 * Un prix observé pour un produit, dans un magasin, à une date.
 * L'identifiant est absent tant que le relevé n'est pas enregistré.
 * La date future est refusée par le service, qui connaît l'heure (horloge contrôlable en test).
 */
public record ReleveDePrix(
        Long id,
        long produitId,
        long magasinId,
        Montant prix,
        UniteDePrix unite,
        Instant releveLe,
        TypeDePreuve preuve,
        String contributeur,
        boolean enPromotion) {

    public ReleveDePrix {
        if (produitId <= 0 || magasinId <= 0) {
            throw new DonneeInvalideException("Un relevé désigne un produit et un magasin existants");
        }
        if (prix == null || !prix.estStrictementPositif()) {
            throw new DonneeInvalideException("Un prix relevé est strictement positif");
        }
        if (unite == null || releveLe == null || preuve == null) {
            throw new DonneeInvalideException("Un relevé a une unité, une date et un type de preuve");
        }
        if (contributeur == null || contributeur.isBlank() || contributeur.length() > 64) {
            throw new DonneeInvalideException("Un relevé a un contributeur (64 caractères au plus)");
        }
    }
}
