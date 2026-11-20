package fr.miage.kadi.prix.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Le scénario de Karim (dossier, section 03) de bout en bout par HTTP : contexte complet, PostgreSQL, Flyway.
 * Les relevés de l'exemple Lactel sont insérés avant chaque test, et tout est annulé après.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrixFiableApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long lait;
    private long yaourt;
    private long clichy;

    @BeforeEach
    void preparer() {
        long enseigne = jdbc.queryForObject("INSERT INTO enseigne (nom) VALUES ('E.Leclerc (test API)') RETURNING id", Long.class);
        clichy = jdbc.queryForObject("INSERT INTO magasin (enseigne_id, nom) VALUES (?, 'Leclerc Clichy (test API)') RETURNING id",
                Long.class, enseigne);
        lait = jdbc.queryForObject("INSERT INTO produit (libelle, code_barres) VALUES ('Lait test API', '2999999999981') RETURNING id", Long.class);
        yaourt = jdbc.queryForObject("INSERT INTO produit (libelle, code_barres) VALUES ('Yaourt test API', '2999999999982') RETURNING id", Long.class);
        inserer("1.09", 3, "TICKET", "A");
        inserer("1.09", 7, "ETIQUETTE", "B");
        inserer("1.12", 12, "ETIQUETTE", "C");
        inserer("0.10", 13, "SAISIE", "D");
        for (int jour = 1; jour <= 5; jour++) {
            inserer("1.15", jour, "ETIQUETTE", "E");
        }
    }

    @Test
    void le_prix_de_reference_du_lait_est_celui_de_la_fiche() throws Exception {
        mockMvc.perform(get("/api/v1/produits/{p}/magasins/{m}/prix-de-reference", lait, clichy))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prix").value(1.09))
                .andExpect(jsonPath("$.confiance.niveau").value("ELEVEE"))
                .andExpect(jsonPath("$.niveauDeCalcul").value("MAGASIN"))
                .andExpect(jsonPath("$.ecartes[0].releve.prix").value(0.10))
                .andExpect(jsonPath("$.ecartes[0].motif").value("ABERRANT"))
                .andExpect(jsonPath("$.retenus[0].contributeur").doesNotExist());
    }

    @Test
    void un_produit_sans_releve_n_a_pas_de_prix_plutot_qu_un_prix_invente() throws Exception {
        mockMvc.perform(get("/api/v1/produits/{p}/magasins/{m}/prix-de-reference", yaourt, clichy))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Pas encore de prix connu"));
    }

    @Test
    void un_produit_inconnu_renvoie_404_au_format_problem_json() throws Exception {
        mockMvc.perform(get("/api/v1/produits/{p}/magasins/{m}/prix-de-reference", -1, clichy))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void karim_tape_0_10_recoit_422_puis_confirme_et_obtient_201() throws Exception {
        mockMvc.perform(saisie("0.10", false))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Prix inhabituel"))
                .andExpect(jsonPath("$.controle.statut").value("INHABITUEL"))
                .andExpect(jsonPath("$.controle.reference").value(1.09));

        mockMvc.perform(saisie("0.10", true))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.releve.id").isNumber())
                .andExpect(jsonPath("$.controle.statut").value("INHABITUEL"));
    }

    @Test
    void un_prix_normal_est_enregistre_directement() throws Exception {
        mockMvc.perform(saisie("1.10", false))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.controle.statut").value("NORMAL"))
                .andExpect(jsonPath("$.releve.preuve").value("ETIQUETTE"));
    }

    @Test
    void une_saisie_invalide_renvoie_400() throws Exception {
        mockMvc.perform(post("/api/v1/releves").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produitId\": %d, \"magasinId\": %d, \"prix\": -1.00, \"contributeur\": \"Karim\"}".formatted(lait, clichy)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/releves").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produitId\": %d, \"magasinId\": %d, \"prix\": 1.10}".formatted(lait, clichy)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void une_date_future_renvoie_400_au_format_problem_json() throws Exception {
        String demain = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        mockMvc.perform(post("/api/v1/releves").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produitId\": %d, \"magasinId\": %d, \"prix\": 1.10, \"contributeur\": \"Karim\", \"releveLe\": \"%s\"}"
                                .formatted(lait, clichy, demain)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Saisie invalide"));
    }

    private org.springframework.test.web.servlet.RequestBuilder saisie(String prix, boolean confirmer) {
        return post("/api/v1/releves").contentType(MediaType.APPLICATION_JSON).content("""
                {"produitId": %d, "magasinId": %d, "prix": %s, "preuve": "ETIQUETTE", "contributeur": "Karim", "confirmerMalgreAlerte": %s}
                """.formatted(lait, clichy, prix, confirmer));
    }

    private void inserer(String prix, int ageEnJours, String preuve, String contributeur) {
        jdbc.update("""
                INSERT INTO releve_de_prix (produit_id, magasin_id, prix, releve_le, source, type_preuve, contributeur, unite)
                VALUES (?, ?, ?::numeric, now() - make_interval(days => ?), 'TEST', ?, ?, 'PIECE')
                """, lait, clichy, prix, ageEnJours, preuve, contributeur);
    }
}
