package fr.miage.kadi.catalogue.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static fr.miage.kadi.UtilisateursSimulesDeTest.creer;
import static fr.miage.kadi.UtilisateursSimulesDeTest.en;
import static fr.miage.kadi.UtilisateursSimulesDeTest.idDe;

import com.jayway.jsonpath.JsonPath;
import fr.miage.kadi.UtilisateursSimulesDeTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validation des critères d'acceptation de la liste (F1) par l'API, sur PostgreSQL et avec un utilisateur simulé (en-tête X-Utilisateur-Id).
 * Chaque test indique le ou les critères REQ qu'il couvre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ListeCoursesControllerIT {
    private static final String LISTE = "/api/v1/liste";
    private static final String LIGNES = LISTE + "/lignes";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private long farineFrancine;
    private long cocaPack;
    private long pommes;
    private long haricots;
    private long lait;
    private long gobelets;

    @BeforeEach
    void preparerCatalogue() {
        farineFrancine = precis("Farine de blé Francine 1 kg", "2001000000035", "Francine", "1 kg");
        cocaPack = precis("Coca-Cola 6 x 33 cl", "2001000000042", "Coca-Cola", "6 x 33 cl");
        pommes = generique("Pommes", "SOLIDE");
        haricots = generique("Haricots verts surgelés", "SOLIDE");
        lait = generique("Lait demi-écrémé", "LIQUIDE");
        gobelets = generique("Gobelets", "UNITAIRE");
    }

    @Test
    @DisplayName("REQ-028 : sans utilisateur sélectionné, la liste répond 400 au format problem+json")
    void exigeUnUtilisateurSelectionne() throws Exception {
        mockMvc.perform(get(LISTE)).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("X-Utilisateur-Id")));
        mockMvc.perform(post(LIGNES).contentType(MediaType.APPLICATION_JSON)
                .content(ajout("PRODUIT", farineFrancine, "1", "ARTICLE"))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("REQ-001 : une liste vide au premier accès, retrouvée telle quelle en resélectionnant son profil")
    void retrouveSaListeDansUneAutreSession() throws Exception {
        RequestPostProcessor premiere = connexion("ines@example.test");
        mockMvc.perform(get(LISTE).with(premiere)).andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes", hasSize(0)));
        ajouter(premiere, ajout("PRODUIT", cocaPack, "1", "ARTICLE")).andExpect(status().isCreated());
        ajouter(premiere, ajout("BESOIN_GENERIQUE", lait, "2", "LITRE")).andExpect(status().isCreated());

        RequestPostProcessor autreAppareil = connexionExistante("ines@example.test");
        mockMvc.perform(get(LISTE).with(autreAppareil)).andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes", hasSize(2)))
                .andExpect(jsonPath("$.lignes[0].libelle").value("Coca-Cola 6 x 33 cl"))
                .andExpect(jsonPath("$.lignes[1].libelle").value("Lait demi-écrémé"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM liste_courses lc JOIN compte_utilisateur c "
                + "ON c.id = lc.compte_id WHERE c.email = 'ines@example.test'", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("REQ-003, REQ-004, REQ-010 : générique, précis et personnalisé restent distincts après sauvegarde")
    void distingueLesTroisTypesDeLigne() throws Exception {
        RequestPostProcessor session = connexion("types@example.test");
        ajouter(session, ajout("BESOIN_GENERIQUE", pommes, "3", "ARTICLE")).andExpect(status().isCreated());
        ajouter(session, ajout("PRODUIT", farineFrancine, "1", "ARTICLE")).andExpect(status().isCreated());
        ajouter(session, personnel("Sauce maison spéciale", "1", "ARTICLE")).andExpect(status().isCreated());

        mockMvc.perform(get(LISTE).with(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[0].type").value("BESOIN_GENERIQUE"))
                .andExpect(jsonPath("$.lignes[0].marque").doesNotExist())
                .andExpect(jsonPath("$.lignes[1].type").value("PRODUIT"))
                .andExpect(jsonPath("$.lignes[1].marque").value("Francine"))
                .andExpect(jsonPath("$.lignes[1].formatProduit").value("1 kg"))
                .andExpect(jsonPath("$.lignes[2].type").value("BESOIN_PERSONNALISE"))
                .andExpect(jsonPath("$.lignes[2].libelle").value("Sauce maison spéciale"))
                .andExpect(jsonPath("$.lignes[2].statutBesoin").value("EN_ATTENTE"));
    }

    @Test
    @DisplayName("REQ-024 : ajouter deux fois la même référence passe la quantité de 1 à 2 sur une seule ligne")
    void fusionneLaMemeReference() throws Exception {
        RequestPostProcessor session = connexion("fusion@example.test");
        String premiere = idAjoute(session, ajout("PRODUIT", farineFrancine, "1", "ARTICLE"));
        ajouter(session, ajout("PRODUIT", farineFrancine, "1", "ARTICLE")).andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("FUSIONNEE"))
                .andExpect(jsonPath("$.ligneId").value(premiere))
                .andExpect(jsonPath("$.lignes", hasSize(1)))
                .andExpect(jsonPath("$.lignes[0].quantite").value(2));
        ajouter(session, ajout("PRODUIT", cocaPack, "1", "ARTICLE")).andExpect(jsonPath("$.lignes", hasSize(2)));
    }

    @Test
    @DisplayName("REQ-029, REQ-032 : deux paquets de Farine Francine 1 kg, mais pas une fraction de paquet")
    void unProduitPrecisSeCompteEnArticlesEntiers() throws Exception {
        RequestPostProcessor session = connexion("farine@example.test");
        ajouter(session, ajout("PRODUIT", farineFrancine, "2", "ARTICLE")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.lignes[0].quantite").value(2))
                .andExpect(jsonPath("$.lignes[0].unite").value("ARTICLE"))
                .andExpect(jsonPath("$.lignes[0].formatProduit").value("1 kg"));
        ajouter(session, ajout("PRODUIT", farineFrancine, "0.5", "ARTICLE")).andExpect(status().isBadRequest());
        ajouter(session, ajout("PRODUIT", farineFrancine, "500", "GRAMME")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("unité de vente")));
    }

    @Test
    @DisplayName("REQ-002 : modifier la quantité et supprimer une ligne ; une quantité nulle est refusée")
    void modifieEtSupprimeUneLigne() throws Exception {
        RequestPostProcessor session = connexion("modifier@example.test");
        String id = idAjoute(session, ajout("BESOIN_GENERIQUE", pommes, "1", "KILOGRAMME"));
        mockMvc.perform(patch(LIGNES + "/" + id).with(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantite\":2.25}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lignes[0].quantite").value(2.25))
                .andExpect(jsonPath("$.lignes[0].unite").value("KILOGRAMME"));
        mockMvc.perform(patch(LIGNES + "/" + id).with(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantite\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete(LIGNES + "/" + id).with(session)).andExpect(status().isNoContent());
        mockMvc.perform(delete(LIGNES + "/" + id).with(session)).andExpect(status().isNotFound());
        mockMvc.perform(get(LISTE).with(session)).andExpect(jsonPath("$.lignes", hasSize(0)));
    }

    @Test
    @DisplayName("REQ-028 : un autre compte ne peut ni voir, ni modifier, ni supprimer ma ligne, même avec son identifiant")
    void isoleLesListesEntreComptes() throws Exception {
        RequestPostProcessor proprietaire = connexion("proprietaire@example.test");
        RequestPostProcessor intrus = connexion("intrus@example.test");
        String id = idAjoute(proprietaire, ajout("PRODUIT", farineFrancine, "1", "ARTICLE"));

        mockMvc.perform(get(LISTE).with(intrus)).andExpect(jsonPath("$.lignes", hasSize(0)));
        mockMvc.perform(patch(LIGNES + "/" + id).with(intrus)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantite\":9}")).andExpect(status().isNotFound());
        mockMvc.perform(delete(LIGNES + "/" + id).with(intrus)).andExpect(status().isNotFound());
        mockMvc.perform(get(LISTE).with(proprietaire)).andExpect(jsonPath("$.lignes", hasSize(1)))
                .andExpect(jsonPath("$.lignes[0].quantite").value(1));
    }

    @Test
    @DisplayName("REQ-038, REQ-046 : un besoin personnel reste privé et se réutilise sans doublon")
    void unBesoinPersonnelResteAuxMainsDeSonAuteur() throws Exception {
        RequestPostProcessor auteur = connexion("auteur@example.test");
        RequestPostProcessor autre = connexion("autre@example.test");
        String reponse = ajouter(auteur, personnel("Sauce maison spéciale", "1", "ARTICLE"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int besoin = JsonPath.read(reponse, "$.lignes[0].referenceId");

        ajouter(autre, "{\"type\":\"BESOIN_PERSONNALISE\",\"referenceId\":" + besoin
                + ",\"quantite\":1,\"unite\":\"ARTICLE\"}").andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/produits").with(autre).param("recherche", "sauce maison").param("portee", "liste"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));

        String ligne = JsonPath.read(reponse, "$.lignes[0].id");
        mockMvc.perform(delete(LIGNES + "/" + ligne).with(auteur)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/produits").with(auteur).param("recherche", "sauce maison").param("portee", "liste"))
                .andExpect(jsonPath("$[0].type").value("BESOIN_PERSONNALISE"))
                .andExpect(jsonPath("$[0].id").value(besoin));
        ajouter(auteur, personnel("  SAUCE maison spéciale ", "1", "ARTICLE")).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM besoin_personnalise WHERE libelle ILIKE 'sauce maison%'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("REQ-064, REQ-065, REQ-067 : format souhaité conservé tel que saisi ; 1 kg et 500 g restent distincts")
    void conserveLeFormatSouhaiteDUnBesoinGenerique() throws Exception {
        RequestPostProcessor session = connexion("format@example.test");
        ajouter(session, avecFormat(haricots, "1", "sachet de 1 kg")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.lignes[0].formatSouhaite").value("sachet de 1 kg"))
                .andExpect(jsonPath("$.lignes[0].marque").doesNotExist());
        ajouter(session, avecFormat(haricots, "1", "sachet de 500 g")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.lignes", hasSize(2)));
        ajouter(session, ajout("PRODUIT", farineFrancine, "1", "ARTICLE")
                .replace("}", ",\"formatSouhaite\":\"2 kg\"}")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("REQ-068, REQ-069 : deux formats écrits différemment demandent confirmation ; refuser les garde séparés")
    void demandeConfirmationAvantDeFusionnerDesFormatsProches() throws Exception {
        RequestPostProcessor session = connexion("confirmation@example.test");
        String premiere = idAjoute(session, avecFormat(haricots, "1", "Sachet de 1 kg"));

        ajouter(session, avecFormat(haricots, "2", "sachet de 1 KG")).andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CONFIRMATION_REQUISE"))
                .andExpect(jsonPath("$.lignesCandidates[0]").value(premiere))
                .andExpect(jsonPath("$.lignes", hasSize(1)));

        ajouter(session, avecFormat(haricots, "2", "sachet de 1 KG").replace("}", ",\"decision\":\"SEPARER\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.lignes", hasSize(2)));
    }

    @Test
    @DisplayName("REQ-075, REQ-076 : « farine » générique et « Farine Francine » précise coexistent sur deux lignes")
    void unBesoinGeneriqueNEstPasRemplaceParUnProduitPrecis() throws Exception {
        long farine = generique("Farine", "SOLIDE");
        RequestPostProcessor session = connexion("farines@example.test");
        ajouter(session, ajout("BESOIN_GENERIQUE", farine, "1", "KILOGRAMME")).andExpect(status().isCreated());
        ajouter(session, ajout("PRODUIT", farineFrancine, "1", "ARTICLE")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.lignes", hasSize(2)))
                .andExpect(jsonPath("$.lignes[0].type").value("BESOIN_GENERIQUE"))
                .andExpect(jsonPath("$.lignes[1].type").value("PRODUIT"));
    }

    @Test
    @DisplayName("A6 : un produit présent dans une liste ne peut pas être supprimé du catalogue ; la ligne reste")
    void conserveLaLigneSiOnTenteDeSupprimerLeProduit() throws Exception {
        RequestPostProcessor session = connexion("conserve@example.test");
        ajouter(session, ajout("PRODUIT", cocaPack, "1", "ARTICLE")).andExpect(status().isCreated());
        assertThatThrownBy(() -> jdbc.update("DELETE FROM produit WHERE id = ?", cocaPack))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("REQ-074, A7 : portee=liste propose besoins génériques et produits ; sans portée, F2 ne voit que les produits")
    void laRechercheDeListeAjouteLesBesoinsGeneriques() throws Exception {
        RequestPostProcessor session = connexion("recherche@example.test");
        mockMvc.perform(get("/api/v1/produits").with(session).param("recherche", "pommes").param("portee", "liste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("BESOIN_GENERIQUE"))
                .andExpect(jsonPath("$[0].unites", hasSize(3)))
                .andExpect(jsonPath("$[0].codeBarres").doesNotExist());
        mockMvc.perform(get("/api/v1/produits").with(session).param("recherche", "pommes"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mockMvc.perform(get("/api/v1/produits").with(session).param("recherche", "pomes").param("portee", "liste"))
                .andExpect(jsonPath("$[*].id", hasItem((int) pommes)))
                .andExpect(jsonPath("$[0].confiance").value("A_CONFIRMER"));
        mockMvc.perform(get("/api/v1/produits").with(session).param("recherche", "coca").param("portee", "rayon"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/produits").param("recherche", "pommes").param("portee", "liste"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/produits").param("recherche", "pommes"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/catalogue/produits").with(session))
                .andExpect(jsonPath("$.produits[*].id", not(hasItem((int) pommes))));
    }

    private ResultActions ajouter(RequestPostProcessor session, String corps) throws Exception {
        return mockMvc.perform(post(LIGNES).with(session)
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private String idAjoute(RequestPostProcessor session, String corps) throws Exception {
        return JsonPath.read(ajouter(session, corps).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.ligneId");
    }

    private static String ajout(String type, long referenceId, String quantite, String unite) {
        return "{\"type\":\"%s\",\"referenceId\":%d,\"quantite\":%s,\"unite\":\"%s\"}"
                .formatted(type, referenceId, quantite, unite);
    }

    private static String avecFormat(long besoin, String quantite, String format) {
        return "{\"type\":\"BESOIN_GENERIQUE\",\"referenceId\":%d,\"quantite\":%s,\"unite\":\"ARTICLE\",\"formatSouhaite\":\"%s\"}"
                .formatted(besoin, quantite, format);
    }

    private static String personnel(String libelle, String quantite, String unite) {
        return "{\"type\":\"BESOIN_PERSONNALISE\",\"libelle\":\"%s\",\"quantite\":%s,\"unite\":\"%s\"}"
                .formatted(libelle, quantite, unite);
    }

    private long precis(String libelle, String codeBarres, String marque, String format) {
        return jdbc.queryForObject("INSERT INTO produit(libelle, code_barres, marque, format) VALUES (?, ?, ?, ?) "
                + "RETURNING id", Long.class, libelle, codeBarres, marque, format);
    }

    private long generique(String libelle, String nature) {
        return jdbc.queryForObject("INSERT INTO produit(libelle, type_reference, nature_besoin) "
                + "VALUES (?, 'GENERIQUE', ?) RETURNING id", Long.class, libelle, nature);
    }

    /** Crée un utilisateur simulé et le sélectionne, comme la liste déroulante du front. */
    private RequestPostProcessor connexion(String email) {
        return en(creer(jdbc, email));
    }

    private RequestPostProcessor connexionExistante(String email) {
        return en(idDe(jdbc, email));
    }
}
