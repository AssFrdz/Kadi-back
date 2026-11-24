package fr.miage.kadi.catalogue.adapter.in.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.sql.DataSource;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogueControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void preparerCatalogueIsole() {
        jdbc.update("DELETE FROM releve_de_prix");
        jdbc.update("DELETE FROM produit");
    }

    @Test
    void signaleUnCatalogueVide() throws Exception {
        mockMvc.perform(get("/api/v1/catalogue/produits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produits", hasSize(0)))
                .andExpect(jsonPath("$.prochainId").isEmpty());
    }

    @Test
    void pagineParIdentifiantEtAccepteUnProduitSansCodeBarres() throws Exception {
        for (int index = 0; index < 21; index++) {
            jdbc.update("INSERT INTO produit(libelle, code_barres) VALUES (?, NULL)", "Produit " + index);
        }
        Long vingtiemeId = jdbc.queryForObject(
                "SELECT id FROM produit ORDER BY id OFFSET 19 LIMIT 1", Long.class);

        mockMvc.perform(get("/api/v1/catalogue/produits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produits", hasSize(20)))
                .andExpect(jsonPath("$.produits[0].libelle").value("Produit 0"))
                .andExpect(jsonPath("$.produits[0].codeBarres").isEmpty())
                .andExpect(jsonPath("$.prochainId").value(vingtiemeId));

        mockMvc.perform(get("/api/v1/catalogue/produits")
                        
                        .param("apres", vingtiemeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produits", hasSize(1)))
                .andExpect(jsonPath("$.produits[0].libelle").value("Produit 20"))
                .andExpect(jsonPath("$.prochainId").isEmpty());
    }

    @Test
    void refuseUnCurseurInvalide() throws Exception {
        mockMvc.perform(get("/api/v1/catalogue/produits").param("apres", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/catalogue/produits").param("apres", "texte"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void trieLeCatalogueParNomSurToutesLesPagesEnConservantLesFiltres() throws Exception {
        jdbc.update("INSERT INTO produit(libelle, marque) VALUES (?, ?)", "Zèbre", "Test");
        jdbc.update("INSERT INTO produit(libelle, marque) VALUES (?, ?)", "Éclair", "Test");
        jdbc.update("INSERT INTO produit(libelle, marque) VALUES (?, ?)", "Banane", "Autre");
        for (int i = 0; i < 22; i++) {
            jdbc.update("INSERT INTO produit(libelle, marque) VALUES (?, ?)", "Abricot", "Test");
        }
        mockMvc.perform(get("/api/v1/catalogue/produits/par-nom"))
                .andExpect(status().isOk());
        String premiere = mockMvc.perform(get("/api/v1/catalogue/produits/par-nom")
                        .param("marque", "Test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.produits", hasSize(20)))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = new ArrayList<>(JsonPath.read(premiere, "$.produits[*].id"));
        String curseur = JsonPath.read(premiere, "$.prochainCurseur");
        String deuxieme = mockMvc.perform(get("/api/v1/catalogue/produits/par-nom")
                        .param("marque", "Test").param("apres", curseur))
                .andExpect(status().isOk()).andExpect(jsonPath("$.produits", hasSize(4)))
                .andExpect(jsonPath("$.produits[2].libelle").value("Éclair"))
                .andExpect(jsonPath("$.produits[3].libelle").value("Zèbre"))
                .andExpect(jsonPath("$.prochainCurseur").isEmpty())
                .andReturn().getResponse().getContentAsString();
        ids.addAll(JsonPath.read(deuxieme, "$.produits[*].id"));
        assertThat(new HashSet<>(ids)).hasSize(24);
        mockMvc.perform(get("/api/v1/catalogue/produits/par-nom")
                        .param("marque", "Autre").param("apres", curseur))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/catalogue/produits/par-nom")
                        .param("apres", "invalide"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compteLesMarquesSurTousLesResultatsDeLaRechercheEtDuFormat() throws Exception {
        for (int i = 0; i < 23; i++) {
            jdbc.update("INSERT INTO produit(libelle, marque, format) VALUES (?, ?, ?)",
                    "Yaourt " + i, "Alpha, alpha, Beta", i < 21 ? "500 g" : "1 kg");
        }
        jdbc.update("INSERT INTO produit(libelle, marque, format) VALUES (?, ?, ?)", "Riz", "Gamma", "500 g");
        jdbc.update("INSERT INTO produit(libelle, marque, format) VALUES (?, ?, ?)", "Yaourt", null, "500 g");
        mockMvc.perform(get("/api/v1/catalogue/produits/marques"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/catalogue/produits/marques")
                        .param("q", "yaourt").param("format", "500 g"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nom").value("Alpha"))
                .andExpect(jsonPath("$[0].nombre").value(21))
                .andExpect(jsonPath("$[1].nom").value("Beta"))
                .andExpect(jsonPath("$[1].nombre").value(21));
        mockMvc.perform(get("/api/v1/catalogue/produits/marques").param("format", "500 g"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2].nom").value("Gamma"))
                .andExpect(jsonPath("$[2].nombre").value(1));
        mockMvc.perform(get("/api/v1/catalogue/produits/marques").param("q", "ya"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/catalogue/produits/marques").param("q", "!!!"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/catalogue/produits/marques").param("q", "   ")
                        .param("format", "500 g"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
        mockMvc.perform(get("/api/v1/catalogue/produits/marques").param("q", "x".repeat(256)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importeUnPetitJeuReelSansModifierLesProduitsExistantsEtExposeSaProvenance() throws Exception {
        jdbc.update("INSERT INTO produit(libelle, code_barres) VALUES (?, ?)", "Produit existant", "3017620422003");
        ResourceDatabasePopulator importeur = new ResourceDatabasePopulator(
                new ClassPathResource("db/import/open-food-facts-petit-jeu.sql"));
        importeur.execute(dataSource);
        importeur.execute(dataSource);

        mockMvc.perform(get("/api/v1/catalogue/produits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produits", hasSize(3)))
                .andExpect(jsonPath("$.produits[0].libelle").value("Produit existant"))
                .andExpect(jsonPath("$.produits[0].marque").isEmpty())
                .andExpect(jsonPath("$.produits[0].sourceUrl").isEmpty())
                .andExpect(jsonPath("$.produits[?(@.codeBarres == '5449000000996')].sourceUrl",
                        hasItem("https://world.openfoodfacts.org/product/5449000000996")))
                .andExpect(jsonPath("$.produits[?(@.codeBarres == '5449000000996')].imageUrl",
                        hasItem("https://images.openfoodfacts.org/images/products/544/900/000/0996/front_en.1129.400.jpg")))
                .andExpect(jsonPath("$.produits[?(@.codeBarres == '5449000000996')].imageAuteur",
                        hasItem("zeynepozturk11")))
                .andExpect(jsonPath("$.produits[?(@.codeBarres == '5449000000996')].format",
                        hasItem("330 ml")));
    }

    @Test
    void completeLesChampsManquantsDuPetitJeuOffSansEcraserLesValeursExistantes() throws Exception {
        ResourceDatabasePopulator importeur = new ResourceDatabasePopulator(
                new ClassPathResource("db/import/open-food-facts-petit-jeu.sql"));
        importeur.execute(dataSource);
        jdbc.update("UPDATE produit SET marque = NULL, format = ? WHERE code_barres = ?",
                "format corrigé", "3017620422003");
        importeur.execute(dataSource);
        importeur.execute(dataSource);

        mockMvc.perform(get("/api/v1/catalogue/produits").param("marque", "nutella"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produits", hasSize(1)))
                .andExpect(jsonPath("$.produits[0].libelle").value("Nutella"))
                .andExpect(jsonPath("$.produits[0].marque").value("Ferrero, Nutella"))
                .andExpect(jsonPath("$.produits[0].format").value("format corrigé"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM produit", Integer.class)).isEqualTo(3);
    }

    @Test
    void suggestionsLocalesLimiteesTolereFauteAccentEtSaisiePartielle() throws Exception {
        jdbc.update("INSERT INTO produit(libelle) VALUES (?)", "Yaourt nature");
        jdbc.update("INSERT INTO produit(libelle) VALUES (?)", "Pâtes Barilla");
        for (int i = 0; i < 10; i++) {
            jdbc.update("INSERT INTO produit(libelle) VALUES (?)", "Nutella " + i);
        }
        mockMvc.perform(get("/api/v1/catalogue/produits/suggestions").param("q", "yaort"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/catalogue/produits/suggestions").param("q", "ya"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/catalogue/produits/suggestions").param("q", "yaort"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].libelle").value("Yaourt nature"));
        mockMvc.perform(get("/api/v1/catalogue/produits/suggestions").param("q", "pates"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].libelle").value("Pâtes Barilla"));
        mockMvc.perform(get("/api/v1/catalogue/produits/suggestions").param("q", "nut"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(7)));
        mockMvc.perform(get("/api/v1/catalogue/produits/suggestions").param("q", "zzzzzz"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/catalogue/produits/suggestions").param("q", "x".repeat(256)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechercheEtAfficheMarqueEtFormatLorsquIlsSontRenseignes() throws Exception {
        jdbc.update("INSERT INTO produit(libelle, marque, format) VALUES (?, ?, ?)",
                "Pâtes penne", "Barilla", "500 g");
        jdbc.update("INSERT INTO produit(libelle, marque, format) VALUES (?, ?, ?)",
                "Coca-Cola", "Coca-Cola", "6 x 33 cl");
        jdbc.update("INSERT INTO produit(libelle, marque, format) VALUES (?, ?, ?)",
                "Coca-Cola", "Coca-Cola", "1,5 l");
        mockMvc.perform(get("/api/v1/catalogue/produits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produits[0].marque").value("Barilla"))
                .andExpect(jsonPath("$.produits[0].format").value("500 g"));
        mockMvc.perform(get("/api/v1/catalogue/produits/suggestions")
                        .param("q", "barilla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].libelle").value("Pâtes penne"))
                .andExpect(jsonPath("$[0].marque").value("Barilla"));
        mockMvc.perform(get("/api/v1/catalogue/produits/suggestions")
                        .param("q", "pates barilla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].libelle").value("Pâtes penne"));
        mockMvc.perform(get("/api/v1/catalogue/produits/suggestions")
                        .param("q", "coca 6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].format").value("6 x 33 cl"))
                .andExpect(jsonPath("$[1].format").value("1,5 l"));
    }

    @Test
    void filtreMarqueEtFormatAvantLaPaginationDuCatalogueEtDeLaRecherche() throws Exception {
        jdbc.update("INSERT INTO produit(libelle, marque, format) VALUES (?, ?, ?)", "Yaourt absent", null, null);
        jdbc.update("INSERT INTO produit(libelle, marque, format) VALUES (?, ?, ?)", "Yaourt autre", "Autre", "500 g");
        jdbc.update("INSERT INTO produit(libelle, marque, format) VALUES (?, ?, ?)", "Yaourt autre format", "Marque test", "1 kg");
        jdbc.update("INSERT INTO produit(libelle, marque, format) VALUES (?, ?, ?)", "Yaourt marque proche", "Marque test plus", "500 g");
        for (int i = 0; i < 23; i++) {
            jdbc.update("INSERT INTO produit(libelle, marque, format) VALUES (?, ?, ?)",
                    String.format("Yaourt %02d", i), "Autre, Marque test", "Lot de 500 g");
        }
        String page = mockMvc.perform(get("/api/v1/catalogue/produits")
                        .param("marque", " marque test ").param("format", "500 G"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.produits", hasSize(20)))
                .andReturn().getResponse().getContentAsString();
        Integer prochainId = JsonPath.read(page, "$.prochainId");
        mockMvc.perform(get("/api/v1/catalogue/produits")
                        .param("marque", "marque test").param("format", "500 g")
                        .param("apres", prochainId.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.produits", hasSize(3)))
                .andExpect(jsonPath("$.prochainId").isEmpty());

        String premiere = mockMvc.perform(get("/api/v1/catalogue/produits/recherche")
                        .param("q", "yaourt").param("marque", "Marque test").param("format", "500 g")
                        .param("taille", "7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.produits", hasSize(7)))
                .andReturn().getResponse().getContentAsString();
        String curseur = JsonPath.read(premiere, "$.prochainCurseur");
        mockMvc.perform(get("/api/v1/catalogue/produits/recherche")
                        .param("q", "yaourt").param("marque", "Marque test").param("format", "500 g")
                        .param("apres", curseur))
                .andExpect(status().isOk()).andExpect(jsonPath("$.produits", hasSize(16)));
        mockMvc.perform(get("/api/v1/catalogue/produits/recherche")
                        .param("q", "yaourt").param("marque", "Autre").param("format", "500 g")
                        .param("apres", curseur))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/catalogue/produits").param("format", "%"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.produits", hasSize(0)));
        mockMvc.perform(get("/api/v1/catalogue/produits").param("marque", "x".repeat(256)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/catalogue/produits/recherche")
                        .param("q", "yaourt").param("format", "x".repeat(256)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void parcourtTousLesResultatsAuDelaDeDeuxCentsSansDoublons() throws Exception {
        for (int i = 0; i < 235; i++) {
            jdbc.update("INSERT INTO produit(libelle) VALUES (?)", String.format("Nutella %03d", i));
        }
        mockMvc.perform(get("/api/v1/catalogue/produits/recherche").param("q", "nut"))
                .andExpect(status().isOk());
        var premiere = mockMvc.perform(get("/api/v1/catalogue/produits/recherche")
                        .param("q", "nut").param("taille", "7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.produits", hasSize(7)))
                .andReturn().getResponse().getContentAsString();
        List<String> noms = new ArrayList<>(JsonPath.read(premiere, "$.produits[*].libelle"));
        String curseur = JsonPath.read(premiere, "$.prochainCurseur");
        mockMvc.perform(get("/api/v1/catalogue/produits/recherche")
                        .param("q", "coca").param("apres", curseur))
                .andExpect(status().isBadRequest());
        while (curseur != null) {
            String page = mockMvc.perform(get("/api/v1/catalogue/produits/recherche")
                            .param("q", "nut").param("apres", curseur))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            noms.addAll(JsonPath.read(page, "$.produits[*].libelle"));
            curseur = JsonPath.read(page, "$.prochainCurseur");
        }
        assertThat(noms).hasSize(235);
        assertThat(new HashSet<>(noms)).hasSize(235);
        assertThat(noms).isSorted();
        mockMvc.perform(get("/api/v1/catalogue/produits/recherche")
                        .param("q", "nut").param("apres", "invalide"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/catalogue/produits/recherche")
                        .param("q", "nut").param("taille", "21"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/catalogue/produits/recherche")
                        .param("q", "nu"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.produits", hasSize(0)));
    }

    @Test
    void neProposeUneSuiteQueSiUnHuitiemeResultatExisteEtDepartageLesDoublons() throws Exception {
        for (int i = 0; i < 7; i++) {
            jdbc.update("INSERT INTO produit(libelle) VALUES (?)", "Yaourt");
        }
        mockMvc.perform(get("/api/v1/catalogue/produits/recherche")
                        .param("q", "yaourt").param("taille", "7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.produits", hasSize(7)))
                .andExpect(jsonPath("$.prochainCurseur").isEmpty());
        jdbc.update("INSERT INTO produit(libelle) VALUES (?)", "Yaourt");
        String page = mockMvc.perform(get("/api/v1/catalogue/produits/recherche")
                        .param("q", "yaourt").param("taille", "7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.produits", hasSize(7)))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(page, "$.produits[*].id");
        String curseur = JsonPath.read(page, "$.prochainCurseur");
        String suite = mockMvc.perform(get("/api/v1/catalogue/produits/recherche")
                        .param("q", "yaourt").param("apres", curseur))
                .andExpect(status().isOk()).andExpect(jsonPath("$.produits", hasSize(1)))
                .andExpect(jsonPath("$.prochainCurseur").isEmpty())
                .andReturn().getResponse().getContentAsString();
        List<Integer> suiteIds = JsonPath.read(suite, "$.produits[*].id");
        assertThat(ids).doesNotContain(suiteIds.get(0));
    }
}
