package fr.miage.kadi.prix.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** La recherche provisoire des écrans de F2, sur un vrai PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RechercheApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void preparer() {
        long enseigne = jdbc.queryForObject("INSERT INTO enseigne (nom) VALUES ('Enseigne Zorglub') RETURNING id", Long.class);
        jdbc.update("INSERT INTO magasin (enseigne_id, nom) VALUES (?, 'Zorglub Clichy')", enseigne);
        jdbc.update("INSERT INTO produit (libelle, code_barres) VALUES ('Lait Zorglub demi-écrémé 1 L', '2999999999971')");
        jdbc.update("INSERT INTO produit (libelle, code_barres) VALUES ('Lait Zorglub entier 1 L', '2999999999972')");
    }

    @Test
    void tous_les_mots_tapes_doivent_apparaitre_quel_que_soit_leur_ordre_et_la_casse() throws Exception {
        mockMvc.perform(get("/api/v1/produits").param("recherche", "DEMI zorglub"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].libelle").value("Lait Zorglub demi-écrémé 1 L"));
    }

    @Test
    void un_code_barres_tape_trouve_son_produit_exactement() throws Exception {
        mockMvc.perform(get("/api/v1/produits").param("recherche", "2999999999972"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].libelle").value("Lait Zorglub entier 1 L"));
    }

    @Test
    void un_texte_trop_court_ou_des_caracteres_speciaux_ne_renvoient_rien_de_faux() throws Exception {
        mockMvc.perform(get("/api/v1/produits").param("recherche", "z"))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/produits").param("recherche", "zorglub %"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void un_magasin_se_trouve_par_son_nom_ou_son_enseigne() throws Exception {
        mockMvc.perform(get("/api/v1/magasins").param("recherche", "zorglub clichy"))
                .andExpect(jsonPath("$[0].nom").value("Zorglub Clichy"))
                .andExpect(jsonPath("$[0].enseigne").value("Enseigne Zorglub"));
    }
}
