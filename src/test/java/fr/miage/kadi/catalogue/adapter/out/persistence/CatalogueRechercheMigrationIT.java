package fr.miage.kadi.catalogue.adapter.out.persistence;

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
class CatalogueRechercheMigrationIT {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void distingueLesBesoinsGeneriquesDesProduitsPrecis() {
        Long precis = jdbc.queryForObject("INSERT INTO produit(libelle, code_barres) VALUES (?, ?) RETURNING id",
                Long.class, "Lactel Demi-écrémé 1 L", "2001000000011");
        Long generique = jdbc.queryForObject("""
                INSERT INTO produit(libelle, type_reference, nature_besoin) VALUES (?, 'GENERIQUE', 'LIQUIDE')
                RETURNING id
                """, Long.class, "Lait demi-écrémé");

        assertThat(jdbc.queryForObject("SELECT type_reference FROM produit WHERE id = ?", String.class, precis))
                .isEqualTo("PRECIS");
        assertThat(jdbc.queryForObject("SELECT nature_besoin FROM produit WHERE id = ?", String.class, generique))
                .isEqualTo("LIQUIDE");
    }

    @Test
    void refuseUnBesoinGeneriqueAvecCodeBarres() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO produit(libelle, code_barres, type_reference, nature_besoin)
                VALUES (?, ?, 'GENERIQUE', 'SOLIDE')
                """, "Générique avec code", "2001000000028")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refuseDeuxBesoinsGeneriquesDeMemeLibelleSansDistinctionDeCasse() {
        jdbc.update("INSERT INTO produit(libelle, type_reference, nature_besoin) VALUES (?, 'GENERIQUE', 'LIQUIDE')",
                "Lait demi-écrémé");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO produit(libelle, type_reference, nature_besoin) VALUES (?, 'GENERIQUE', 'SOLIDE')
                """, "LAIT DEMI-ÉCRÉMÉ")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refuseUnIdentifiantExterneDejaAssocie() {
        Long premier = jdbc.queryForObject("INSERT INTO produit(libelle) VALUES (?) RETURNING id",
                Long.class, "Premier");
        Long second = jdbc.queryForObject("INSERT INTO produit(libelle) VALUES (?) RETURNING id",
                Long.class, "Second");
        jdbc.update("INSERT INTO produit_open_food_facts(produit_id, identifiant_externe) VALUES (?, ?)",
                premier, "1234567890123");

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO produit_open_food_facts(produit_id, identifiant_externe) VALUES (?, ?)",
                second, "1234567890123")).isInstanceOf(DataIntegrityViolationException.class);
    }
}
