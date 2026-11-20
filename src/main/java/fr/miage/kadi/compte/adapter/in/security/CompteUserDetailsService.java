package fr.miage.kadi.compte.adapter.in.security;

import fr.miage.kadi.compte.application.port.out.CompteUtilisateurRepository;
import fr.miage.kadi.compte.domain.model.AdresseEmail;
import fr.miage.kadi.compte.domain.model.CompteUtilisateur;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CompteUserDetailsService implements UserDetailsService {

    private final CompteUtilisateurRepository compteUtilisateurRepository;

    public CompteUserDetailsService(CompteUtilisateurRepository compteUtilisateurRepository) {
        this.compteUtilisateurRepository = compteUtilisateurRepository;
    }

    @Override
    public CompteUtilisateurDetails loadUserByUsername(String email) {
        CompteUtilisateur compte = compteUtilisateurRepository
                .trouverParAdresseEmail(new AdresseEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants incorrects."));
        return new CompteUtilisateurDetails(compte);
    }
}
