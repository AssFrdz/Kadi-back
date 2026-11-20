package fr.miage.kadi.compte.adapter.out.persistence;

import fr.miage.kadi.compte.application.port.out.VerificationEmailRepository;
import java.time.Instant;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class VerificationEmailPersistenceAdapter implements VerificationEmailRepository {

    private final JetonCompteJpaRepository jetonCompteJpaRepository;
    private final CompteUtilisateurJpaRepository compteUtilisateurJpaRepository;

    VerificationEmailPersistenceAdapter(
            JetonCompteJpaRepository jetonCompteJpaRepository,
            CompteUtilisateurJpaRepository compteUtilisateurJpaRepository) {
        this.jetonCompteJpaRepository = jetonCompteJpaRepository;
        this.compteUtilisateurJpaRepository = compteUtilisateurJpaRepository;
    }

    @Override
    @Transactional
    public void enregistrer(long compteId, String empreinte, Instant expireLe) {
        jetonCompteJpaRepository.saveAndFlush(new JetonCompteEntity(compteId, empreinte, expireLe));
    }

    @Override
    @Transactional
    public boolean verifierEtConsommer(String empreinte, Instant maintenant) {
        return jetonCompteJpaRepository
                .trouverJetonActif(empreinte, TypeJetonCompte.VERIFICATION_EMAIL, maintenant)
                .map(jeton -> {
                    CompteUtilisateurEntity compte = compteUtilisateurJpaRepository.findById(jeton.getCompteId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Le compte associé au jeton n'existe plus."));
                    compte.verifierEmail();
                    jeton.consommer(maintenant);
                    return true;
                })
                .orElse(false);
    }
}
