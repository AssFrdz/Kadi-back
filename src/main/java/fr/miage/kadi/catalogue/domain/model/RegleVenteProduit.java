package fr.miage.kadi.catalogue.domain.model;

import java.util.Objects;

public record RegleVenteProduit(UniteQuantite unite, boolean fractionnable) {
    public RegleVenteProduit {
        Objects.requireNonNull(unite, "L'unité de vente ne peut pas être nulle.");
        if (unite == UniteQuantite.ARTICLE && fractionnable) {
            throw new IllegalArgumentException("Une quantité d'articles ne peut pas être fractionnaire.");
        }
    }

    public void verifier(Quantite quantite) {
        Objects.requireNonNull(quantite, "La quantité ne peut pas être nulle.");
        if (quantite.unite() != unite) {
            throw new IllegalArgumentException("La quantité doit utiliser l'unité de vente du produit.");
        }
        if (!fractionnable && quantite.value().stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Ce produit ne peut pas être vendu en fraction d'unité.");
        }
    }
}
