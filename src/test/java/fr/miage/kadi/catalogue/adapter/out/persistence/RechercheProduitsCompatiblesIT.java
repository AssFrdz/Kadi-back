package fr.miage.kadi.catalogue.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.kadi.catalogue.application.service.RechercherProduitsCompatiblesService;
import fr.miage.kadi.catalogue.domain.model.ProduitRechercheCompatible;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RechercheProduitsCompatiblesIT {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private RechercherProduitsCompatiblesService recherche;

    @BeforeEach
    void preparerCatalogueIsole() {
        jdbc.execute("CREATE SCHEMA recherche_compatible_test");
        jdbc.execute("SET LOCAL search_path TO recherche_compatible_test, public");
        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V1__modele_commun.sql"),
                new ClassPathResource("db/migration/V4__comptes_utilisateur.sql")).execute(dataSource);
    }

    @Test
    void lesCaracteresSpeciauxRestentDesDonneesEtNonDuSql() {
        long id = ajouter("Prix 100% naturel", null);
        assertThat(recherche.rechercher("100%")).extracting(ProduitRechercheCompatible::id).contains(id);
        assertThat(recherche.rechercher("' OR 1=1 --")).isEmpty();
    }

    private long ajouter(String libelle, String codeBarres) {
        return jdbc.queryForObject("INSERT INTO produit(libelle, code_barres) VALUES (?, ?) RETURNING id",
                Long.class, libelle, codeBarres);
    }
}
