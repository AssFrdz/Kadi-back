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

}
