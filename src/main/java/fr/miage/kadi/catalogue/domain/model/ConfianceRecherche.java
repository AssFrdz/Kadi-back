package fr.miage.kadi.catalogue.domain.model;

/**
 * Zone de confiance d'un résultat de recherche. Elle informe l'utilisateur ; elle ne choisit jamais à sa place.
 */
public enum ConfianceRecherche {
    /** Code-barres identique, ou tous les mots saisis présents dans le libellé (casse, accents et espaces de quantité ignorés). */
    SUR,
    /** Résultat approché, par exemple malgré une faute de frappe : à confirmer par l'utilisateur. */
    A_CONFIRMER
}
