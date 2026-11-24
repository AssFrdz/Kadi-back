package fr.miage.kadi.catalogue.domain.model;

import java.util.Objects;

public record ProduitTrouve(ProduitRechercheCompatible produit, ConfianceRecherche confiance) {

    public ProduitTrouve {
        Objects.requireNonNull(produit, "Le produit est obligatoire.");
        Objects.requireNonNull(confiance, "La confiance est obligatoire.");
    }
}
