package fr.miage.kadi.compte.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.miage.kadi.compte.application.port.out.CompteUtilisateurRepository;
import fr.miage.kadi.compte.domain.model.AdresseEmail;
import fr.miage.kadi.compte.domain.model.CompteUtilisateur;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import fr.miage.kadi.compte.EmailTestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(EmailTestConfiguration.class)
class InscriptionCompteControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompteUtilisateurRepository compteUtilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailTestConfiguration.EnvoiEmailFictif envoiEmailFictif;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void inscriptionPubliqueCreeUnCompteSansExposerLeHashEtNecessiteCsrf() throws Exception {
        String email = "assia-" + UUID.randomUUID() + "@example.test";
        String motDePasse = "MotDePasse-de-test-2026";
        String json = """
                {"email":"%s","motDePasse":"%s"}
                """.formatted(email, motDePasse);

        mockMvc.perform(post("/api/v1/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/auth/inscription")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.emailVerifie").value(false))
                .andExpect(jsonPath("$.motDePasseHash").doesNotExist())
                .andExpect(jsonPath("$.motDePasse").doesNotExist());

        CompteUtilisateur compte = compteUtilisateurRepository
                .trouverParAdresseEmail(new AdresseEmail(email))
                .orElseThrow();
        assertThat(passwordEncoder.matches(motDePasse, compte.motDePasseHash())).isTrue();
        assertThat(envoiEmailFictif.dernierLien()).isNotNull();
    }

    @Test
    void refuseUnEmailMalFormeEtUnMotDePasseVide() throws Exception {
        mockMvc.perform(post("/api/v1/auth/inscription")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"pas-un-email","motDePasse":"mot-de-passe"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/inscription")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"assia@example.test","motDePasse":" "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refuseUneAdresseEmailDejaUtiliseeAvecUnConflit() throws Exception {
        String email = "assia-" + UUID.randomUUID() + "@example.test";
        String json = """
                {"email":"%s","motDePasse":"MotDePasse-de-test-2026"}
                """.formatted(email);

        mockMvc.perform(post("/api/v1/auth/inscription")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/inscription")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","motDePasse":"AutreMotDePasse-2026"}
                                """.formatted(email.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cette adresse e-mail est déjà utilisée."));
    }

    @Test
    void uneAutreRoutePriveeResteProtegee() throws Exception {
        mockMvc.perform(post("/api/v1/private-probe").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirmeUneAdresseAvecLeJetonDuLienEtNePermetPasDeLeRejouer() throws Exception {
        String email = "assia-" + UUID.randomUUID() + "@example.test";
        String json = """
                {"email":"%s","motDePasse":"MotDePasse-de-test-2026"}
                """.formatted(email);
        mockMvc.perform(post("/api/v1/auth/inscription")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        String token = envoiEmailFictif.dernierLien().getQuery().substring("token=".length());
        MockHttpServletRequestBuilder confirmation = MockMvcRequestBuilders
                .post("/api/v1/auth/verification-email/confirmation")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token":"%s"}
                        """.formatted(token));

        mockMvc.perform(confirmation)
                .andExpect(status().isNoContent());
        assertThat(compteUtilisateurRepository.trouverParAdresseEmail(new AdresseEmail(email))
                .orElseThrow().emailVerifie()).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT consomme_le IS NOT NULL FROM jeton_compte WHERE compte_id = "
                        + "(SELECT id FROM compte_utilisateur WHERE email = ?)",
                Boolean.class, email)).isTrue();

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/auth/verification-email/confirmation")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s"}
                                """.formatted(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exigeUnJetonCsrfPourLaConfirmation() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/auth/verification-email/confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}
                                """))
                .andExpect(status().isForbidden());
    }

}
