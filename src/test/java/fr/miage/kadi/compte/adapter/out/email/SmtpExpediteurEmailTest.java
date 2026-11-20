package fr.miage.kadi.compte.adapter.out.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import fr.miage.kadi.compte.application.exception.EnvoiEmailIndisponibleException;
import fr.miage.kadi.compte.domain.model.AdresseEmail;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpExpediteurEmailTest {

    @Test
    void refuseUnEnvoiQuandLeServeurOuLExpediteurNestPasConfigure() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpExpediteurEmail expediteur = new SmtpExpediteurEmail(mailSender, "", "", "", "", true);

        assertThatThrownBy(() -> expediteur.envoyerLienVerification(
                new AdresseEmail("assia@example.test"),
                URI.create("https://example.test/verification-email?token=jeton")))
                .isInstanceOf(EnvoiEmailIndisponibleException.class)
                .hasMessageContaining("KADI_SMTP_HOST");
        verifyNoInteractions(mailSender);
    }

    @Test
    void refuseUnEnvoiQuandLAuthentificationSmtpEstIncomplete() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpExpediteurEmail expediteur = new SmtpExpediteurEmail(
                mailSender, "smtp.example.test", "noreply@example.test", "", "", true);

        assertThatThrownBy(() -> expediteur.verifierDisponibilite())
                .isInstanceOf(EnvoiEmailIndisponibleException.class)
                .hasMessageContaining("KADI_SMTP_USERNAME");
        verifyNoInteractions(mailSender);
    }

    @Test
    void envoieUnMessageContenantLeLienDeConfirmation() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpExpediteurEmail expediteur = new SmtpExpediteurEmail(
                mailSender, "smtp.example.test", "noreply@example.test", "assia", "secret", true);

        expediteur.envoyerLienVerification(
                new AdresseEmail("assia@example.test"),
                URI.create("https://example.test/verification-email?token=jeton"));

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertThat(message.getValue().getFrom()).isEqualTo("noreply@example.test");
        assertThat(message.getValue().getTo()).containsExactly("assia@example.test");
        assertThat(message.getValue().getText()).contains("https://example.test/verification-email?token=jeton");
    }

    @Test
    void transformeUneErreurSmtpEnErreurExplicite() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MailSendException erreurSmtp = new MailSendException("Connexion refusée");
        doThrow(erreurSmtp).when(mailSender).send(any(SimpleMailMessage.class));
        SmtpExpediteurEmail expediteur = new SmtpExpediteurEmail(
                mailSender, "smtp.example.test", "noreply@example.test", "assia", "secret", true);

        assertThatThrownBy(() -> expediteur.envoyerLienVerification(
                new AdresseEmail("assia@example.test"),
                URI.create("https://example.test/verification-email?token=jeton")))
                .isInstanceOf(EnvoiEmailIndisponibleException.class)
                .hasCause(erreurSmtp);
    }
}
