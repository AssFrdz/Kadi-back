package fr.miage.kadi.catalogue.adapter.in.web;

import static fr.miage.kadi.UtilisateursSimulesDeTest.en;
import static fr.miage.kadi.UtilisateursSimulesDeTest.idDe;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Modération des besoins personnels par l'API, sur PostgreSQL, avec les profils simulés de la V4. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ModerationBesoinsControllerIT {
    private static final String ADMIN = "/api/v1/admin/besoins";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private RequestPostProcessor karim;
    private RequestPostProcessor sophie;
    private RequestPostProcessor admin;

    @BeforeEach
    void choisirLesProfils() {
        karim = en(idDe(jdbc, "karim@kadi.local"));
        sophie = en(idDe(jdbc, "sophie@kadi.local"));
        admin = en(idDe(jdbc, "admin@kadi.local"));
    }

    @Test
    @DisplayName("REQ-040 : un consommateur ne peut ni voir ni valider les propositions (403)")
    void reserveALAdministrateur() throws Exception {
        long id = proposer(karim, "Sauce tomatte maison");
        mockMvc.perform(get(ADMIN).with(karim)).andExpect(status().isForbidden());
        mockMvc.perform(post(ADMIN + "/" + id + "/validation").with(karim)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nature\":\"LIQUIDE\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ADMIN).with(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].auteur").value("Karim (consommateur)"));
    }

    @Test
    @DisplayName("REQ-039, REQ-042 : validé avec un libellé corrigé, le besoin est proposé à tous ; l'auteur garde sa saisie")
    void publieLeLibelleCorrigeEtConserveLaSaisieDOrigine() throws Exception {
        long id = proposer(karim, "Sauce tomatte maison");

        mockMvc.perform(post(ADMIN + "/" + id + "/validation").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"libelle\":\"Sauce tomate maison\",\"nature\":\"LIQUIDE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rattache").value(false));

        mockMvc.perform(get("/api/v1/produits").with(sophie).param("recherche", "sauce tomate").param("portee", "liste"))
                .andExpect(jsonPath("$[?(@.type == 'BESOIN_GENERIQUE')].libelle", hasItem("Sauce tomate maison")));
        mockMvc.perform(get("/api/v1/liste").with(karim))
                .andExpect(jsonPath("$.lignes[0].libelle").value("Sauce tomatte maison"))
                .andExpect(jsonPath("$.lignes[0].statutBesoin").value("VALIDE"));
        mockMvc.perform(post(ADMIN + "/" + id + "/refus").with(admin)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("REQ-041, REQ-047 : refusé, le besoin reste dans la liste et les suggestions de son auteur, et privé")
    void refuseEtLaisseLeBesoinPrive() throws Exception {
        long id = proposer(karim, "Truc de grand-mère");

        mockMvc.perform(post(ADMIN + "/" + id + "/refus").with(admin)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/liste").with(karim))
                .andExpect(jsonPath("$.lignes[0].statutBesoin").value("REFUSE"));
        mockMvc.perform(get("/api/v1/produits").with(karim).param("recherche", "truc de grand").param("portee", "liste"))
                .andExpect(jsonPath("$[*].libelle", hasItem("Truc de grand-mère")));
        mockMvc.perform(get("/api/v1/produits").with(sophie).param("recherche", "truc de grand").param("portee", "liste"))
                .andExpect(jsonPath("$[*].libelle", not(hasItem("Truc de grand-mère"))));
    }

    /** L'utilisateur ajoute une saisie inconnue à sa liste : elle devient une proposition en attente. */
    private long proposer(RequestPostProcessor utilisateur, String libelle) throws Exception {
        mockMvc.perform(post("/api/v1/liste/lignes").with(utilisateur).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"BESOIN_PERSONNALISE\",\"libelle\":\"%s\",\"quantite\":1,\"unite\":\"ARTICLE\"}"
                                .formatted(libelle)))
                .andExpect(status().isCreated());
        String liste = mockMvc.perform(get("/api/v1/liste").with(utilisateur)).andReturn().getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(liste, "$.lignes[0].referenceId")).longValue();
    }
}
