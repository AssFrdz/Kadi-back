package fr.miage.kadi.compte.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.miage.kadi.compte.EmailTestConfiguration;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(EmailTestConfiguration.class)
class AuthentificationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailTestConfiguration.EnvoiEmailFictif envoiEmailFictif;

    @Test
    void connecteUnCompteVerifieEtConserveLaSessionJusquaLaDeconnexion() throws Exception {
        String email = "assia-" + UUID.randomUUID() + "@example.test";
        String motDePasse = "MotDePasse-de-test-2026";
        inscrireEtVerifier(email, motDePasse);
        MockHttpSession sessionAnonyme = new MockHttpSession();
        String identifiantSessionAnonyme = sessionAnonyme.getId();

        MvcResult connexion = mockMvc.perform(post("/api/v1/auth/connexion")
                        .session(sessionAnonyme)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","motDePasse":"%s"}
                                """.formatted(email, motDePasse)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("UTILISATEUR"))
                .andExpect(jsonPath("$.emailVerifie").value(true))
                .andExpect(jsonPath("$.motDePasseHash").doesNotExist())
                .andReturn();

        MockHttpSession session = (MockHttpSession) connexion.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getId()).isNotEqualTo(identifiantSessionAnonyme);

        mockMvc.perform(get("/api/v1/auth/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(post("/api/v1/auth/deconnexion")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/session"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refuseLesComptesNonVerifiesEtLesIdentifiantsIncorrectsDeLaMemeManiere() throws Exception {
        String email = "assia-" + UUID.randomUUID() + "@example.test";
        String motDePasse = "MotDePasse-de-test-2026";
        inscrire(email, motDePasse);

        String refusCompteNonVerifie = refuserConnexion(email, motDePasse);
        String refusMotDePasseIncorrect = refuserConnexion(email, "MauvaisMotDePasse-2026");
        String refusCompteInconnu = refuserConnexion("inconnu@example.test", motDePasse);

        assertThat(refusCompteNonVerifie).isEqualTo(refusMotDePasseIncorrect)
                .isEqualTo(refusCompteInconnu);
    }

    @Test
    void exigeUnJetonCsrfPourLaConnexion() throws Exception {
        mockMvc.perform(post("/api/v1/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"assia@example.test","motDePasse":"MotDePasse-de-test-2026"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void exigeUnJetonCsrfPourLaDeconnexion() throws Exception {
        mockMvc.perform(post("/api/v1/auth/deconnexion"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protegeLaLectureDeLaSession() throws Exception {
        mockMvc.perform(get("/api/v1/auth/session"))
                .andExpect(status().isUnauthorized());
    }

    private void inscrireEtVerifier(String email, String motDePasse) throws Exception {
        inscrire(email, motDePasse);
        URI lien = envoiEmailFictif.dernierLien();
        String jeton = lien.getQuery().substring("token=".length());
        mockMvc.perform(post("/api/v1/auth/verification-email/confirmation")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s"}
                                """.formatted(jeton)))
                .andExpect(status().isNoContent());
    }

    private void inscrire(String email, String motDePasse) throws Exception {
        mockMvc.perform(post("/api/v1/auth/inscription")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","motDePasse":"%s"}
                                """.formatted(email, motDePasse)))
                .andExpect(status().isCreated());
    }

    private String refuserConnexion(String email, String motDePasse) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/connexion")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","motDePasse":"%s"}
                                """.formatted(email, motDePasse)))
                .andExpect(status().isUnauthorized())
                .andReturn();
        return result.getResponse().getContentAsString();
    }
}
