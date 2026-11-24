package fr.miage.kadi.catalogue.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NutriScoreDesactiveIT {
    @Autowired private MockMvc mockMvc;

    @Test
    void refuseLeFiltreEtConserveLaConsultationExistanteParDefaut() throws Exception {
        mockMvc.perform(get("/api/v1/catalogue/produits"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/catalogue/produits").param("nutriScore", "A"))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/v1/catalogue/produits/par-nom").param("nutriScore", "A"))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/v1/catalogue/produits/recherche")
                        .param("q", "yaourt").param("nutriScore", "A"))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/v1/catalogue/produits/suggestions")
                        .param("q", "yaourt").param("nutriScore", "A"))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/api/v1/catalogue/produits/marques").param("nutriScore", "A"))
                .andExpect(status().isServiceUnavailable());
    }
}
