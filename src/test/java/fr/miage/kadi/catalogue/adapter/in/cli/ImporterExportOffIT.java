package fr.miage.kadi.catalogue.adapter.in.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Savepoint;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImporterExportOffIT {
    private static final String EN_TETE =
            "code\tproduct_name\tbrands\tquantity\tcountries_tags\tcategories_tags\tlast_modified_t\timage_url\n";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MockMvc mockMvc;

    @TempDir
    Path dossier;

    @Test
    void importeActualiseSansEcraserAutresSourcesImagesOuCategories() throws Exception {
        jdbc.update("INSERT INTO produit(libelle, code_barres) VALUES (?, ?)", "Fiche locale", "111");
        Long categorie = jdbc.queryForObject("INSERT INTO categorie_catalogue(libelle) VALUES (?) RETURNING id",
                Long.class, "Correction");
        Long id = jdbc.queryForObject("""
                INSERT INTO produit(libelle, code_barres, categorie_id, categorie_corrigee)
                VALUES (?, ?, ?, true) RETURNING id
                """, Long.class, "Ancienne fiche", "222", categorie);
        jdbc.update("""
                INSERT INTO produit_open_food_facts(produit_id, identifiant_externe, image_url, image_auteur,
                                                    modifie_source_le)
                VALUES (?, ?, ?, ?, to_timestamp(100))
                """, id, "222", "https://images.openfoodfacts.org/photo.jpg", "auteur-test");
        Path fichier = dossier.resolve("donnees.tsv");
        Files.writeString(fichier, EN_TETE
                + "111\tFiche externe\t\t\ten:france\t\t200\t\n"
                + "222\tFiche mise a jour\tMarque\t500 g\ten:france\ten:test\t200\t\n"
                + "333\tFiche nouvelle\t\t\ten:france\t\t100\thttps://exemple.test/image.jpg\n"
                + "333\tVersion plus ancienne\t\t\ten:france\t\t50\t\n"
                + "444\tNon francaise\t\t\ten:belgium\t\t100\t\n", StandardCharsets.UTF_8);
        Connection connexion = DataSourceUtils.getConnection(dataSource);
        var premier = ImporterExportOff.importer(connexion, fichier.toString(), 20);
        assertThat(premier.distincts()).isEqualTo(3);
        assertThat(premier.crees()).isEqualTo(1);
        assertThat(premier.actualises()).isEqualTo(1);
        assertThat(premier.conflits()).isEqualTo(1);
        assertThat(premier.rapport().imagesSansCredit()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT libelle FROM produit WHERE code_barres = '111'", String.class))
                .isEqualTo("Fiche locale");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM produit p JOIN produit_open_food_facts off ON p.id = off.produit_id
                WHERE p.id = ? AND p.libelle = 'Fiche mise a jour' AND p.marque = 'Marque'
                  AND p.format = '500 g' AND p.categorie_id = ? AND p.categorie_corrigee
                  AND off.image_auteur = 'auteur-test' AND off.categorie_source = 'en:test'
                """, Integer.class, id, categorie)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM produit p JOIN produit_open_food_facts off ON p.id = off.produit_id
                WHERE p.code_barres = '333' AND p.libelle = 'Fiche nouvelle' AND off.image_url IS NULL
                """, Integer.class)).isEqualTo(1);

        var second = ImporterExportOff.importer(connexion, fichier.toString(), 20);
        assertThat(second.crees()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM produit WHERE code_barres = '333'", Integer.class))
                .isEqualTo(1);
        Path ancien = dossier.resolve("ancien.tsv");
        Files.writeString(ancien, EN_TETE + "222\tAncien\t\t\ten:france\t\t50\t\n", StandardCharsets.UTF_8);
        var resultatAncien = ImporterExportOff.importer(connexion, ancien.toString(), 20);
        assertThat(resultatAncien.anciens()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT libelle FROM produit WHERE id = ?", String.class, id))
                .isEqualTo("Fiche mise a jour");
    }

    @Test
    void annuleTouteEcritureSiLeFichierEstInvalide() throws Exception {
        Path fichier = dossier.resolve("invalide.tsv");
        Files.writeString(fichier, EN_TETE
                + "777\tAvant erreur\t\t\ten:france\t\t100\t\n"
                + "888\tDate invalide\t\t\ten:france\t\tinvalide\t\n", StandardCharsets.UTF_8);
        Connection connexion = DataSourceUtils.getConnection(dataSource);
        Savepoint avant = connexion.setSavepoint();
        assertThatThrownBy(() -> ImporterExportOff.importer(connexion, fichier.toString(), 10))
                .isInstanceOf(IOException.class).hasMessageContaining("Date source invalide");
        connexion.rollback(avant);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM produit WHERE code_barres IN ('777','888')", Integer.class))
                .isZero();
    }

    @Test
    void lesChampsImportesSontRecherchablesEtVisiblesDansLApi() throws Exception {
        Path fichier = dossier.resolve("produit.tsv");
        Files.writeString(fichier, EN_TETE
                + "321123\tPates penne\tMarque fictive\t500 g\ten:france\ten:pasta\t100\t\n",
                StandardCharsets.UTF_8);
        ImporterExportOff.importer(DataSourceUtils.getConnection(dataSource), fichier.toString(), 10);
        mockMvc.perform(get("/api/v1/catalogue/produits/suggestions")
                        .with(user("test")).param("q", "marque fictive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].libelle").value("Pates penne"))
                .andExpect(jsonPath("$[0].marque").value("Marque fictive"))
                .andExpect(jsonPath("$[0].format").value("500 g"));
    }
}
