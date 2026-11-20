package fr.miage.kadi.compte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CompteMigrationIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void nouveauCompteEstNonVerifieEtUtilisateurParDefaut() {
        Long id = jdbc.queryForObject(
                "INSERT INTO compte_utilisateur (email, mot_de_passe_hash) VALUES (?, ?) RETURNING id",
                Long.class, "assia@example.test", "hash-de-test");

        assertThat(jdbc.queryForObject(
                "SELECT email_verifie FROM compte_utilisateur WHERE id = ?", Boolean.class, id)).isFalse();
        assertThat(jdbc.queryForObject(
                "SELECT role FROM compte_utilisateur WHERE id = ?", String.class, id)).isEqualTo("UTILISATEUR");
    }

    @Test
    void deuxComptesNePeuventPasAvoirLaMemeAdresseSansTenirCompteDeLaCasse() {
        jdbc.update("INSERT INTO compte_utilisateur (email, mot_de_passe_hash) VALUES (?, ?)",
                "assia@example.test", "hash-de-test");

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO compte_utilisateur (email, mot_de_passe_hash) VALUES (?, ?)",
                "ASSIA@example.test", "autre-hash-de-test"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void roleInconnuEstRefuse() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO compte_utilisateur (email, mot_de_passe_hash, role) VALUES (?, ?, ?)",
                "assia@example.test", "hash-de-test", "SUPERADMIN"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void suppressionDuCompteSupprimeSesJetons() {
        Long id = jdbc.queryForObject(
                "INSERT INTO compte_utilisateur (email, mot_de_passe_hash) VALUES (?, ?) RETURNING id",
                Long.class, "assia@example.test", "hash-de-test");
        jdbc.update("""
                INSERT INTO jeton_compte (compte_id, type, jeton_hash, expire_le)
                VALUES (?, 'VERIFICATION_EMAIL', ?, CURRENT_TIMESTAMP + INTERVAL '1 hour')
                """, id, "empreinte-de-test");

        jdbc.update("DELETE FROM compte_utilisateur WHERE id = ?", id);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM jeton_compte WHERE compte_id = ?",
                Integer.class, id)).isZero();
    }

    @Test
    void jetonDeTypeInconnuEstRefuse() {
        Long id = jdbc.queryForObject(
                "INSERT INTO compte_utilisateur (email, mot_de_passe_hash) VALUES (?, ?) RETURNING id",
                Long.class, "assia@example.test", "hash-de-test");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO jeton_compte (compte_id, type, jeton_hash, expire_le)
                VALUES (?, 'AUTRE', ?, CURRENT_TIMESTAMP + INTERVAL '1 hour')
                """, id, "empreinte-de-test"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
