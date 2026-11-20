package fr.miage.kadi.compte.application.service;

import fr.miage.kadi.compte.application.exception.JetonVerificationInvalideException;
import fr.miage.kadi.compte.application.port.out.ExpediteurEmail;
import fr.miage.kadi.compte.application.port.out.VerificationEmailRepository;
import fr.miage.kadi.compte.domain.model.CompteUtilisateur;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

public class VerificationEmailService {

    private static final Duration DUREE_VALIDITE = Duration.ofHours(24);
    private static final int TAILLE_JETON_OCTETS = 32;

    private final VerificationEmailRepository verificationEmailRepository;
    private final ExpediteurEmail expediteurEmail;
    private final Clock horloge;
    private final SecureRandom generateurAleatoire;
    private final String baseLienVerification;

    public VerificationEmailService(
            VerificationEmailRepository verificationEmailRepository,
            ExpediteurEmail expediteurEmail,
            Clock horloge,
            SecureRandom generateurAleatoire,
            String baseLienVerification) {
        this.verificationEmailRepository = verificationEmailRepository;
        this.expediteurEmail = expediteurEmail;
        this.horloge = horloge;
        this.generateurAleatoire = generateurAleatoire;
        this.baseLienVerification = baseLienVerification;
    }

    public void envoyerLien(CompteUtilisateur compte) {
        Objects.requireNonNull(compte, "Le compte ne peut pas être null.");
        byte[] alea = new byte[TAILLE_JETON_OCTETS];
        generateurAleatoire.nextBytes(alea);
        String jeton = Base64.getUrlEncoder().withoutPadding().encodeToString(alea);
        Instant maintenant = horloge.instant();

        verificationEmailRepository.enregistrer(
                compte.id(),
                empreinte(jeton),
                maintenant.plus(DUREE_VALIDITE));

        URI lien = URI.create(baseLienVerification + "?token=" + jeton);
        expediteurEmail.envoyerLienVerification(compte.adresseEmail(), lien);
    }

    public void verifierDisponibiliteEnvoi() {
        expediteurEmail.verifierDisponibilite();
    }

    public void confirmer(String jeton) {
        if (jeton == null || !jeton.matches("[A-Za-z0-9_-]{43}")) {
            throw new JetonVerificationInvalideException();
        }
        if (!verificationEmailRepository.verifierEtConsommer(empreinte(jeton), horloge.instant())) {
            throw new JetonVerificationInvalideException();
        }
    }

    private String empreinte(String jeton) {
        try {
            byte[] empreinte = MessageDigest.getInstance("SHA-256")
                    .digest(jeton.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(empreinte);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("L'algorithme SHA-256 est indisponible.", exception);
        }
    }
}
