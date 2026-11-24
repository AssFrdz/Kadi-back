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
    void accepteDeuxCaracteresEtRenvoieUneListeVidePourUneSaisieTropCourteOuSansResultat() {
        long id = ajouter("Lait demi-écrémé", null);
        assertThat(recherche.rechercher("")).isEmpty();
        assertThat(recherche.rechercher("l")).isEmpty();
        assertThat(recherche.rechercher("LA")).extracting(ProduitRechercheCompatible::id).contains(id);
        assertThat(recherche.rechercher("xyzzy123z")).isEmpty();
    }

    @Test
    void rechercheUnCodeBarresExactSansRetournerLesLibellesContenantLesChiffres() {
        long id = ajouter("Produit avec code", "3428272960015");
        ajouter("Produit 3428272960015 autre", "1234567890123");
        assertThat(recherche.rechercher("3428272960015"))
                .containsExactly(new ProduitRechercheCompatible(id, "Produit avec code", "3428272960015"));
        assertThat(recherche.rechercher("999999")).isEmpty();
        long court = ajouter("Produit 12345", null);
        assertThat(recherche.rechercher("12345")).extracting(ProduitRechercheCompatible::id).contains(court);
    }

    @Test
    void conserveLesCorrespondancesParMotsDansNimporteQuelOrdreEtIgnoreLesAccents() {
        long premier = ajouter("Lactel Demi-écrémé 1 L", "3428272960015");
        long second = ajouter("Demi-écrémé Lait", null);
        assertThat(recherche.rechercher("DEMI lactel")).extracting(ProduitRechercheCompatible::id).contains(premier);
        assertThat(recherche.rechercher("ecreme demi")).extracting(ProduitRechercheCompatible::id)
                .contains(premier, second);
        assertThat(recherche.rechercher("écrémé demi")).extracting(ProduitRechercheCompatible::id)
                .contains(premier, second);
    }

    @Test
    void proposeDesResultatsProchesApresLesCorrespondancesParMots() {
        long exact = ajouter("Yaort boisson", null);
        long proche = ajouter("Yaourt nature", null);
        assertThat(recherche.rechercher("yaort")).extracting(ProduitRechercheCompatible::id)
                .contains(exact, proche).startsWith(exact);
    }

    @Test
    void retrouveUneQuantiteAvecOuSansEspaceDevantLUnite() {
        long espace = ajouter("Lactel Demi-écrémé 1 L", null);
        long colle = ajouter("Lait 1l", null);
        assertThat(recherche.rechercher("lactel 1l")).extracting(ProduitRechercheCompatible::id).contains(espace);
        assertThat(recherche.rechercher("1l")).extracting(ProduitRechercheCompatible::id).contains(espace, colle);
    }

    @Test
    void limiteAVingtResultatsSansDoublonsEtAvecUnOrdreStable() {
        for (int i = 29; i >= 0; i--) {
            ajouter("Lait %02d".formatted(i), null);
        }
        var resultats = recherche.rechercher("lait");
        assertThat(resultats).hasSize(20).doesNotHaveDuplicates();
        assertThat(resultats).extracting(ProduitRechercheCompatible::libelle)
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, 20)
                        .mapToObj(i -> "Lait %02d".formatted(i)).toList());
        assertThat(recherche.rechercher("lait")).isEqualTo(resultats);
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
