package fr.miage.kadi.catalogue.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ListeExperimentaleDesactiveeIT {
    @Autowired private MockMvc mockMvc;

    @Test
    void lesRoutesExperimentalesNeSontPasExposeesParDefaut() throws Exception {
        mockMvc.perform(get("/api/v1/liste-experimentale"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/liste-experimentale/produits")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"produitId\":1,\"quantite\":1}"))
                .andExpect(status().isNotFound());
    }
}
