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
    void refuseUnBesoinGeneriqueSansNature() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO produit(libelle, type_reference) VALUES (?, 'GENERIQUE')",
                "Générique sans nature")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refuseUnBesoinGeneriqueAvecCodeBarres() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO produit(libelle, code_barres, type_reference, nature_besoin)
                VALUES (?, ?, 'GENERIQUE', 'SOLIDE')
                """, "Générique avec code", "2001000000028")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refuseUneNatureSurUnProduitPrecis() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO produit(libelle, nature_besoin) VALUES (?, 'SOLIDE')",
                "Précis avec nature")).isInstanceOf(DataIntegrityViolationException.class);
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
    void lesInformationsManquantesLaissentLeProduitAccessible() {
        Long id = jdbc.queryForObject("INSERT INTO produit(libelle) VALUES (?) RETURNING id",
                Long.class, "Produit sans informations");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM produit WHERE id = ? AND marque IS NULL "
                        + "AND format IS NULL AND categorie_id IS NULL AND NOT categorie_corrigee",
                Integer.class, id)).isEqualTo(1);
    }

    @Test
    void conserveLaCorrectionDeCategorieEtLesInformationsSource() {
        Long categorie = jdbc.queryForObject(
                "INSERT INTO categorie_catalogue(libelle) VALUES (?) RETURNING id",
                Long.class, "Epicerie");
        Long id = jdbc.queryForObject("""
                INSERT INTO produit(libelle, code_barres, marque, format, categorie_id, categorie_corrigee)
                VALUES (?, ?, ?, ?, ?, true) RETURNING id
                """, Long.class, "Produit de test", "1234567890123", "Marque test", "500 g", categorie);
        jdbc.update("""
                INSERT INTO produit_open_food_facts(produit_id, identifiant_externe, categorie_source, importe_le)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                """, id, "1234567890123", "en:test-category");

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM produit p JOIN produit_open_food_facts off ON off.produit_id = p.id
                WHERE p.id = ? AND p.categorie_id = ? AND p.categorie_corrigee
                  AND off.categorie_source = ? AND off.importe_le IS NOT NULL
                """, Integer.class, id, categorie, "en:test-category")).isEqualTo(1);
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
