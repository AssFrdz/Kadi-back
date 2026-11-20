package fr.miage.kadi.compte.domain.model;

public record CompteUtilisateur(
        long id,
        AdresseEmail adresseEmail,
        String motDePasseHash,
        boolean emailVerifie,
        RoleCompte role) {

    public CompteUtilisateur {
        if (id <= 0) {
            throw new IllegalArgumentException("L'identifiant du compte doit être strictement positif.");
        }
        if (adresseEmail == null) {
            throw new NullPointerException("L'adresse e-mail du compte ne peut pas être null.");
        }
        if (motDePasseHash == null || motDePasseHash.isBlank()) {
            throw new IllegalArgumentException("Le hash du mot de passe ne peut pas être absent.");
        }
        if (role == null) {
            throw new NullPointerException("Le rôle du compte ne peut pas être null.");
        }
    }
}
