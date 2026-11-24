package fr.miage.kadi.config;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/** Sans authentification : routes publiques, pas de CSRF, et l'en-tête de l'utilisateur simulé autorisé en CORS. */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigurationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void lesRoutesApiNExigentPasDeConnexion() throws Exception {
        mockMvc.perform(get("/api/v1/info")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/utilisateurs")).andExpect(status().isOk());
    }

    @Test
    void autoriseLEnTeteDeLUtilisateurSimuleDepuisUneOrigineConnue() throws Exception {
        mockMvc.perform(options("/api/v1/liste")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-utilisateur-id"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsStringIgnoringCase("x-utilisateur-id")));
    }

    @Test
    void listeLesTroisUtilisateursSimules() throws Exception {
        mockMvc.perform(get("/api/v1/utilisateurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nomAffiche == 'Karim (consommateur)')].role").value("UTILISATEUR"))
                .andExpect(jsonPath("$[?(@.nomAffiche == 'Sophie (consommatrice)')].role").value("UTILISATEUR"))
                .andExpect(jsonPath("$[?(@.nomAffiche == 'Admin Kadi')].role").value("ADMINISTRATEUR"));
    }
}
