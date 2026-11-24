package fr.miage.kadi.catalogue.application.service;

import fr.miage.kadi.catalogue.domain.model.Quantite;
import fr.miage.kadi.catalogue.domain.model.RegleVenteProduit;
import java.util.Objects;
import java.util.Optional;

public class VerifierVenteProduitService {
    public void verifier(Quantite quantite, Optional<RegleVenteProduit> regle) {
        Objects.requireNonNull(quantite, "La quantité ne peut pas être nulle.");
        Objects.requireNonNull(regle, "La règle de vente ne peut pas être nulle.");
        RegleVenteProduit regleConnue = regle.orElseThrow(() ->
                new IllegalArgumentException("L'unité de vente du produit n'est pas renseignée."));
        regleConnue.verifier(quantite);
    }
}
