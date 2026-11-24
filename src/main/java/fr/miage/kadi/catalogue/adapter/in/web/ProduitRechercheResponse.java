package fr.miage.kadi.catalogue.adapter.in.web;

import fr.miage.kadi.catalogue.domain.model.ConfianceRecherche;
import fr.miage.kadi.catalogue.domain.model.ProduitTrouve;
import fr.miage.kadi.catalogue.domain.model.TypeReference;
import fr.miage.kadi.catalogue.domain.model.UniteQuantite;
import java.util.List;

/**
 * Élément de réponse du contrat commun F1/F2.
 * {@code id}, {@code libelle} et {@code codeBarres} sont obligatoires pour F2 ; les autres champs sont des compléments :
 * {@code marque}, {@code quantite} (conditionnement du catalogue, par exemple « 1 L »), {@code confiance},
 * {@code type} et {@code unites} (unités proposées à l'ajout dans une liste).
 * {@code codeBarres}, {@code marque} et {@code quantite} peuvent être nuls.
 */
record ProduitRechercheResponse(long id, String libelle, String codeBarres, String marque, String quantite,
        ConfianceRecherche confiance, TypeReference type, List<UniteQuantite> unites) {

    static ProduitRechercheResponse depuis(ProduitTrouve trouve) {
        var produit = trouve.produit();
        return new ProduitRechercheResponse(produit.id(), produit.libelle(), produit.codeBarres(), produit.marque(),
                produit.format(), trouve.confiance(), produit.type(), produit.unitesAutorisees());
    }
}
