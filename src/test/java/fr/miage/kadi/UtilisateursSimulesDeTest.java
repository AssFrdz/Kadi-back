package fr.miage.kadi;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Crée des utilisateurs simulés et les sélectionne dans les requêtes, comme la liste déroulante du front. */
public final class UtilisateursSimulesDeTest {

    public static final String EN_TETE = "X-Utilisateur-Id";

    private UtilisateursSimulesDeTest() {
    }

    /** Insère un profil simulé (rôle UTILISATEUR) et renvoie son identifiant. */
    public static long creer(JdbcTemplate jdbc, String email) {
        return jdbc.queryForObject("""
                INSERT INTO compte_utilisateur(email, mot_de_passe_hash, email_verifie, nom_affiche)
                VALUES (?, 'SIMULE', true, ?) RETURNING id""", Long.class, email, email);
    }

    public static long idDe(JdbcTemplate jdbc, String email) {
        return jdbc.queryForObject("SELECT id FROM compte_utilisateur WHERE lower(email) = lower(?)", Long.class, email);
    }

    /** Ajoute l'en-tête X-Utilisateur-Id à la requête. */
    public static RequestPostProcessor en(long id) {
        return requete -> {
            requete.addHeader(EN_TETE, String.valueOf(id));
            return requete;
        };
    }
}
