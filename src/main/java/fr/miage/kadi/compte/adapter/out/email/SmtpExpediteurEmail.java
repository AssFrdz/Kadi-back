package fr.miage.kadi.compte.adapter.out.email;

import fr.miage.kadi.compte.application.exception.EnvoiEmailIndisponibleException;
import fr.miage.kadi.compte.application.port.out.ExpediteurEmail;
import fr.miage.kadi.compte.domain.model.AdresseEmail;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpExpediteurEmail implements ExpediteurEmail {

    private final JavaMailSender mailSender;
    private final String serveurSmtp;
    private final String adresseExpediteur;
    private final String utilisateurSmtp;
    private final String motDePasseSmtp;
    private final boolean authentificationRequise;

    public SmtpExpediteurEmail(
            JavaMailSender mailSender,
            @Value("${spring.mail.host:}") String serveurSmtp,
            @Value("${app.mail.from:}") String adresseExpediteur,
            @Value("${spring.mail.username:}") String utilisateurSmtp,
            @Value("${spring.mail.password:}") String motDePasseSmtp,
            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean authentificationRequise) {
        this.mailSender = mailSender;
        this.serveurSmtp = serveurSmtp;
        this.adresseExpediteur = adresseExpediteur;
        this.utilisateurSmtp = utilisateurSmtp;
        this.motDePasseSmtp = motDePasseSmtp;
        this.authentificationRequise = authentificationRequise;
    }

    @Override
    public void verifierDisponibilite() {
        if (serveurSmtp.isBlank() || adresseExpediteur.isBlank()) {
            throw new EnvoiEmailIndisponibleException(
                    "L'envoi d'e-mails n'est pas configuré : définir KADI_SMTP_HOST et KADI_MAIL_FROM.");
        }
        if (authentificationRequise && (utilisateurSmtp.isBlank() || motDePasseSmtp.isBlank())) {
            throw new EnvoiEmailIndisponibleException(
                    "L'authentification SMTP est activée : définir KADI_SMTP_USERNAME et KADI_SMTP_PASSWORD.");
        }
    }

    @Override
    public void envoyerLienVerification(AdresseEmail destinataire, URI lien) {
        verifierDisponibilite();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(adresseExpediteur);
        message.setTo(destinataire.valeur());
        message.setSubject("Vérifiez votre adresse e-mail Kadi");
        message.setText("""
                Bonjour,

                Pour vérifier votre adresse e-mail et activer votre compte Kadi, ouvrez ce lien :
                %s

                Ce lien expire dans 24 heures. Si vous n'êtes pas à l'origine de cette demande, ignorez cet e-mail.
                """.formatted(lien));

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new EnvoiEmailIndisponibleException(
                    "Le serveur de messagerie n'a pas pu envoyer le lien de vérification.",
                    exception);
        }
    }
}
