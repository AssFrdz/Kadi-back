package fr.miage.kadi.compte.application.service;

import fr.miage.kadi.compte.application.exception.AdresseEmailDejaUtiliseeException;
import fr.miage.kadi.compte.application.port.out.CompteUtilisateurRepository;
import fr.miage.kadi.compte.application.port.out.MotDePasseHasher;
import fr.miage.kadi.compte.domain.model.AdresseEmail;
import fr.miage.kadi.compte.domain.model.CompteUtilisateur;
import java.util.Objects;

public class InscrireCompteService {

    private final CompteUtilisateurRepository compteUtilisateurRepository;
    private final MotDePasseHasher motDePasseHasher;
    private final VerificationEmailService verificationEmailService;

    public InscrireCompteService(
            CompteUtilisateurRepository compteUtilisateurRepository,
            MotDePasseHasher motDePasseHasher,
            VerificationEmailService verificationEmailService) {
        this.compteUtilisateurRepository = compteUtilisateurRepository;
        this.motDePasseHasher = motDePasseHasher;
        this.verificationEmailService = verificationEmailService;
    }

    public CompteUtilisateur inscrire(AdresseEmail adresseEmail, String motDePasse) {
        Objects.requireNonNull(adresseEmail, "L'adresse e-mail ne peut pas être null.");
        if (motDePasse == null || motDePasse.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide.");
        }
        verificationEmailService.verifierDisponibiliteEnvoi();
        if (compteUtilisateurRepository.trouverParAdresseEmail(adresseEmail).isPresent()) {
            throw new AdresseEmailDejaUtiliseeException();
        }

        String motDePasseHash = motDePasseHasher.hasher(motDePasse);
        CompteUtilisateur compte = compteUtilisateurRepository.enregistrerCompte(adresseEmail, motDePasseHash);
        verificationEmailService.envoyerLien(compte);
        return compte;
    }
}
