package fr.miage.kadi.compte.adapter.out.persistence;

import fr.miage.kadi.compte.application.port.out.CompteUtilisateurRepository;
import fr.miage.kadi.compte.domain.model.AdresseEmail;
import fr.miage.kadi.compte.domain.model.CompteUtilisateur;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CompteUtilisateurPersistenceAdapter implements CompteUtilisateurRepository {

    private final CompteUtilisateurJpaRepository jpaRepository;

    public CompteUtilisateurPersistenceAdapter(CompteUtilisateurJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CompteUtilisateur enregistrerCompte(AdresseEmail adresseEmail, String motDePasseHash) {
        CompteUtilisateurEntity compte = jpaRepository.saveAndFlush(
                new CompteUtilisateurEntity(adresseEmail.valeur(), motDePasseHash));
        return convertir(compte);
    }

    @Override
    public Optional<CompteUtilisateur> trouverParAdresseEmail(AdresseEmail adresseEmail) {
        return jpaRepository.findByEmailIgnoreCase(adresseEmail.valeur()).map(this::convertir);
    }

    private CompteUtilisateur convertir(CompteUtilisateurEntity compte) {
        return new CompteUtilisateur(
                compte.getId(),
                new AdresseEmail(compte.getEmail()),
                compte.getMotDePasseHash(),
                compte.isEmailVerifie(),
                compte.getRole());
    }
}
