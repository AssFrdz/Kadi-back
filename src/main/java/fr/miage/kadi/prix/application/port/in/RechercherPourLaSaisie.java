package fr.miage.kadi.prix.application.port.in;

import java.util.List;

/**
 * Trouver le produit et le magasin d'une saisie ou d'une consultation de prix.
 * Recherche PROVISOIRE (le nom contient les mots tapés) : la recherche du catalogue (F1) la remplacera
 * derrière les mêmes adresses d'API.
 */
public interface RechercherPourLaSaisie {

    record ProduitTrouve(long id, String libelle, String codeBarres) {
    }

    record MagasinTrouve(long id, String nom, String enseigne) {
    }

    /** Par mots du libellé, ou par code-barres exact ; vide en dessous de 2 caractères. */
    List<ProduitTrouve> produits(String recherche);

    /** Par mots du nom du magasin ou de son enseigne ; vide en dessous de 2 caractères. */
    List<MagasinTrouve> magasins(String recherche);
}
