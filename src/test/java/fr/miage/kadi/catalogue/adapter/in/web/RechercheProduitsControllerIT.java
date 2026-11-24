package fr.miage.kadi.catalogue.adapter.in.web;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Contrat commun F1/F2 de GET /api/v1/produits, tel qu'attendu par les écrans de F2. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RechercheProduitsControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void viderLeCatalogue() {
        jdbc.update("DELETE FROM releve_de_prix");
        jdbc.update("DELETE FROM produit");
    }

    @Test
    void renvoieUneListeVideSansErreurPourUneSaisieAbsenteVideOuTropCourte() throws Exception {
        ajouter("Lait demi-écrémé", null);
        for (String saisie : new String[] {"", " ", "l", " l "}) {
            rechercher(saisie).andExpect(status().isOk()).andExpect(content().json("[]"));
        }
        mockMvc.perform(get("/api/v1/produits"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void renvoieLesTroisChampsObligatoiresDuContratEtLesComplementsFacultatifs() throws Exception {
        long id = jdbc.queryForObject("INSERT INTO produit(libelle, code_barres, marque, format) "
                + "VALUES (?, ?, ?, ?) RETURNING id", Long.class, "Lactel Demi-écrémé 1 L", "3428272960015",
                "Lactel", "1 L");
        rechercher("lactel")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]", aMapWithSize(8)))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].libelle").value("Lactel Demi-écrémé 1 L"))
                .andExpect(jsonPath("$[0].codeBarres").value("3428272960015"))
                .andExpect(jsonPath("$[0].marque").value("Lactel"))
                .andExpect(jsonPath("$[0].quantite").value("1 L"))
                .andExpect(jsonPath("$[0].confiance").value("SUR"));
    }

    @Test
    void signaleUnCodeBarresExactCommeSurEtUnResultatApprocheCommeAConfirmer() throws Exception {
        ajouter("Lait demi-écrémé", "2001000000011");
        ajouter("Yaourt nature", null);
        rechercher("2001000000011").andExpect(jsonPath("$[0].confiance").value("SUR"));
        rechercher("yaort").andExpect(jsonPath("$[0].confiance").value("A_CONFIRMER"));
    }

    @Test
    void rechercheExactementUnCodeBarresQuandLaSaisieNeContientQueSixAQuatorzeChiffres() throws Exception {
        long id = ajouter("Lactel Demi-écrémé 1 L", "3428272960015");
        ajouter("Produit 3428272960015 dans le libellé", "2000000000999");
        rechercher("3428272960015")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id));
        rechercher("342827").andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void trouveLesMotsDansNimporteQuelOrdreSansTenirCompteDeLaCasseNiDesAccents() throws Exception {
        long id = ajouter("Lactel Demi-écrémé 1 L", null);
        rechercher("ECREME lactel").andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem((int) id)));
    }

    @Test
    void toléreUneFauteDeFrappe() throws Exception {
        long yaourt = ajouter("Yaourt nature", null);
        long autre = ajouter("Lessive liquide", null);
        rechercher("yaort")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) yaourt)))
                .andExpect(jsonPath("$[*].id", not(hasItem((int) autre))));
    }

    @Test
    void classeLesCorrespondancesLesPlusProchesEnPremier() throws Exception {
        ajouter("Boisson au lait de coco", null);
        long proche = ajouter("Lait", null);
        rechercher("lait").andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(proche));
    }

    private org.springframework.test.web.servlet.ResultActions rechercher(String saisie) throws Exception {
        MockHttpServletRequestBuilder requete = get("/api/v1/produits").param("recherche", saisie);
        return mockMvc.perform(requete);
    }

    private long ajouter(String libelle, String codeBarres) {
        return jdbc.queryForObject("INSERT INTO produit(libelle, code_barres) VALUES (?, ?) RETURNING id",
                Long.class, libelle, codeBarres);
    }
}
