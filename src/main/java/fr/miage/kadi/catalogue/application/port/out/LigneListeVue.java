package fr.miage.kadi.catalogue.application.port.out;

import fr.miage.kadi.catalogue.domain.model.TypeReference;
import fr.miage.kadi.catalogue.domain.model.UniteQuantite;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Ligne de liste prête à afficher. {@code libelle} est la saisie ou le libellé connu au moment de l'ajout ;
 * {@code marque} et {@code formatProduit} viennent du catalogue pour un produit précis ;
 * {@code statutBesoin} n'existe que pour un besoin personnel.
 */
public record LigneListeVue(UUID id, TypeReference type, long referenceId, String libelle, String marque,
        String formatProduit, BigDecimal quantite, UniteQuantite unite, String formatSouhaite, String statutBesoin,
        List<UniteQuantite> unitesAutorisees) {
}
