package fr.miage.kadi.compte.domain.model;

/**
 * Profil choisi dans la liste déroulante « Utilisateur actuel » du front. Kadi ne gère pas d'authentification :
 * l'utilisateur connecté est simulé, conformément à la consigne du projet.
 */
public record UtilisateurSimule(long id, String nomAffiche, RoleCompte role) {

    public UtilisateurSimule {
        if (id <= 0) {
            throw new IllegalArgumentException("L'identifiant de l'utilisateur doit être strictement positif.");
        }
        if (nomAffiche == null || nomAffiche.isBlank()) {
            throw new IllegalArgumentException("Le nom affiché de l'utilisateur ne peut pas être vide.");
        }
        if (role == null) {
            throw new NullPointerException("Le rôle de l'utilisateur ne peut pas être null.");
        }
    }

    public boolean estAdministrateur() {
        return role == RoleCompte.ADMINISTRATEUR;
    }
}
