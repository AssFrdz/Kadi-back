package fr.miage.kadi.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigurationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protegeLesRoutesApiQuiNeSontPasPubliques() throws Exception {
        mockMvc.perform(get("/api/v1/private-probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exigeUnJetonCsrfPourLesRequetesDeModification() throws Exception {
        mockMvc.perform(post("/api/v1/private-probe"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/private-probe").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void autoriseLePrevolCorsAvecIdentifiantsDepuisUneOrigineConnue() throws Exception {
        mockMvc.perform(options("/api/v1/private-probe")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void exposeUnJetonCsrfNonMiseEnCachePourLeNavigateur() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
    }
}
