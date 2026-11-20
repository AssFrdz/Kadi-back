package fr.miage.kadi.catalogue.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;

@SpringBootTest
@Transactional
class CatalogueRechercheMigrationIT {
    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    void metANiveauLesProduitsOffDeLaVersionQuatreSansLesModifier() {
        jdbc.execute("CREATE SCHEMA migration_f1_test");
        jdbc.execute("SET LOCAL search_path TO migration_f1_test, public");
        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V1__modele_commun.sql"),
                new ClassPathResource("db/migration/V4__comptes_utilisateur.sql")).execute(dataSource);
        Long id = jdbc.queryForObject("INSERT INTO produit(libelle, code_barres) VALUES (?, ?) RETURNING id",
                Long.class, "Ancien produit", "1234567890123");
        jdbc.update("INSERT INTO produit_open_food_facts(produit_id, image_url, image_auteur) VALUES (?, ?, ?)",
                id, "https://images.openfoodfacts.org/test.jpg", "auteur-test");

        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V5__catalogue_recherche_temporaire.sql")).execute(dataSource);

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM produit p JOIN produit_open_food_facts off ON off.produit_id = p.id
                WHERE p.id = ? AND p.libelle = ? AND off.identifiant_externe = ?
                  AND off.image_auteur = ? AND p.marque IS NULL AND p.categorie_id IS NULL
                """, Integer.class, id, "Ancien produit", "1234567890123", "auteur-test")).isEqualTo(1);
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
