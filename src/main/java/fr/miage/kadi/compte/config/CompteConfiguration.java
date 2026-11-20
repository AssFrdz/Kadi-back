package fr.miage.kadi.compte.config;

import fr.miage.kadi.compte.application.port.out.CompteUtilisateurRepository;
import fr.miage.kadi.compte.application.port.out.ExpediteurEmail;
import fr.miage.kadi.compte.application.port.out.MotDePasseHasher;
import fr.miage.kadi.compte.application.port.out.VerificationEmailRepository;
import fr.miage.kadi.compte.application.service.InscrireCompteService;
import fr.miage.kadi.compte.application.service.VerificationEmailService;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
class CompteConfiguration {

    @Bean
    Clock compteClock() {
        return Clock.systemUTC();
    }

    @Bean
    SecureRandom verificationTokenRandom() {
        return new SecureRandom();
    }

    @Bean
    PasswordEncoder comptePasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    MotDePasseHasher motDePasseHasher(PasswordEncoder comptePasswordEncoder) {
        return comptePasswordEncoder::encode;
    }

    @Bean
    InscrireCompteService inscrireCompteService(
            CompteUtilisateurRepository compteUtilisateurRepository,
            MotDePasseHasher motDePasseHasher,
            VerificationEmailService verificationEmailService) {
        return new InscrireCompteService(compteUtilisateurRepository, motDePasseHasher, verificationEmailService);
    }

    @Bean
    VerificationEmailService verificationEmailService(
            VerificationEmailRepository verificationEmailRepository,
            ExpediteurEmail expediteurEmail,
            Clock compteClock,
            SecureRandom verificationTokenRandom,
            @Value("${app.auth.verification-link-base}") String baseLienVerification) {
        return new VerificationEmailService(
                verificationEmailRepository,
                expediteurEmail,
                compteClock,
                verificationTokenRandom,
                baseLienVerification);
    }
}
