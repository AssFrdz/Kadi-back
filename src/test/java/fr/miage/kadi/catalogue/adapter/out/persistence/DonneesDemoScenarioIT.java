package fr.miage.kadi.catalogue.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.kadi.catalogue.application.service.RechercherProduitsCompatiblesService;
import fr.miage.kadi.catalogue.domain.model.ConfianceRecherche;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.ProduitTrouve;
import fr.miage.kadi.catalogue.domain.model.TypeReference;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

/** REQ-018, REQ-008 : le scénario du dossier se joue avec le catalogue de démonstration, rejouable sans doublon. */
@SpringBootTest
@Transactional
class DonneesDemoScenarioIT {
    private static final IdentifiantCompte INES = new IdentifiantCompte(1);

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private RechercherProduitsCompatiblesService recherche;

    @BeforeEach
    void chargerLaDemoDeuxFois() {
        jdbc.execute("CREATE SCHEMA demo_scenario_test");
        jdbc.execute("SET LOCAL search_path TO demo_scenario_test, public");
        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V1__modele_commun.sql"),
                new ClassPathResource("db/migration/V4__comptes_utilisateur.sql"),
                new ClassPathResource("db/migration/V5__moderation_besoins.sql"),
                new ClassPathResource("db/demo/R__donnees_demo.sql"),
                new ClassPathResource("db/demo/R__images_open_food_facts.sql"),
                new ClassPathResource("db/demo/R__donnees_demo.sql"),
                new ClassPathResource("db/demo/R__images_open_food_facts.sql")).execute(dataSource);
    }

    @Test
    @DisplayName("La démo est idempotente : la rejouer ne crée aucun doublon")
    void laDemoEstRejouable() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM produit WHERE type_reference = 'PRECIS'", Integer.class))
                .isEqualTo(29);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM produit WHERE type_reference = 'GENERIQUE'", Integer.class))
                .isEqualTo(11);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM produit WHERE code_barres LIKE '2001%'", Integer.class))
                .isEqualTo(9);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM produit p JOIN produit_open_food_facts off ON off.produit_id = p.id
                WHERE off.identifiant_externe = p.code_barres AND off.image_url IS NOT NULL AND off.image_auteur IS NOT NULL
                """, Integer.class)).isEqualTo(20);
    }

    @Test
    @DisplayName("« lait » propose le besoin générique et des laits précis ; F2 ne voit que les produits précis")
    void lait() {
        List<ProduitTrouve> liste = recherche.rechercherPourListe("lait", INES);
        assertThat(libelles(liste)).contains("Lait demi-écrémé", "Lait demi-écrémé pack 6 x 1 L");
        assertThat(recherche.rechercherAvecConfiance("lait"))
                .noneMatch(trouve -> trouve.produit().type() == TypeReference.BESOIN_GENERIQUE);
    }

    @Test
    @DisplayName("« pâtes barilla » propose plusieurs pâtes Barilla distinguées par leur format")
    void patesBarilla() {
        List<ProduitTrouve> resultats = recherche.rechercherPourListe("pâtes barilla", INES);
        assertThat(resultats).filteredOn(trouve -> "Barilla".equals(trouve.produit().marque())).hasSizeGreaterThanOrEqualTo(3)
                .extracting(trouve -> trouve.produit().format()).contains("500 g", "1 kg");
    }

    @Test
    @DisplayName("« spaghetti barilla » : un mot peut désigner la marque, la correspondance est sûre")
    void motDeLaMarque() {
        assertThat(recherche.rechercherAvecConfiance("spaghetti barilla")).first()
                .satisfies(trouve -> {
                    assertThat(trouve.produit().libelle()).isEqualTo("Spaghetti n°5");
                    assertThat(trouve.confiance()).isEqualTo(ConfianceRecherche.SUR);
                });
    }

    @Test
    @DisplayName("« coca 6 » trouve les packs 6 x 33 cl sûrs, sans confondre la bouteille de 1,5 L")
    void coca6() {
        List<ProduitTrouve> surs = recherche.rechercherPourListe("coca 6", INES).stream()
                .filter(trouve -> trouve.confiance() == ConfianceRecherche.SUR).toList();
        assertThat(libelles(surs)).contains("Coca-Cola pack 6 x 33 cl").doesNotContain("Coca-Cola bouteille 1,5 L");
    }

    @Test
    @DisplayName("« yaort » retrouve le besoin générique « Yaourt nature » malgré la faute, à confirmer")
    void yaort() {
        List<ProduitTrouve> resultats = recherche.rechercherPourListe("yaort", INES);
        assertThat(resultats).anyMatch(trouve -> trouve.produit().libelle().equals("Yaourt nature")
                && trouve.produit().type() == TypeReference.BESOIN_GENERIQUE
                && trouve.confiance() == ConfianceRecherche.A_CONFIRMER);
    }

    @Test
    @DisplayName("REQ-057, REQ-058 : lessive et gobelets se trouvent en besoin générique ou en produit précis")
    void articlesNonAlimentaires() {
        for (String saisie : new String[] {"lessive", "gobelets"}) {
            List<ProduitTrouve> resultats = recherche.rechercherPourListe(saisie, INES);
            assertThat(resultats).anyMatch(trouve -> trouve.produit().type() == TypeReference.BESOIN_GENERIQUE);
            assertThat(resultats).anyMatch(trouve -> trouve.produit().type() == TypeReference.PRODUIT
                    && trouve.produit().marque() != null && trouve.produit().format() != null);
        }
    }

    @Test
    @DisplayName("Une saisie inconnue ne propose rien : aucun produit n'est attribué au hasard")
    void saisieInconnue() {
        assertThat(recherche.rechercherPourListe("sauce maison spéciale", INES)).isEmpty();
    }

    private static List<String> libelles(List<ProduitTrouve> resultats) {
        return resultats.stream().map(trouve -> trouve.produit().libelle()).toList();
    }
}
