package fr.miage.kadi.compte.adapter.in.security;

import fr.miage.kadi.compte.domain.model.CompteUtilisateur;
import fr.miage.kadi.compte.domain.model.RoleCompte;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class CompteUtilisateurDetails implements UserDetails {

    private final long id;
    private final String email;
    private final String motDePasseHash;
    private final boolean emailVerifie;
    private final RoleCompte role;

    CompteUtilisateurDetails(CompteUtilisateur compte) {
        this.id = compte.id();
        this.email = compte.adresseEmail().valeur();
        this.motDePasseHash = compte.motDePasseHash();
        this.emailVerifie = compte.emailVerifie();
        this.role = compte.role();
    }

    public long id() {
        return id;
    }

    public boolean emailVerifie() {
        return emailVerifie;
    }

    public RoleCompte role() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return motDePasseHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // L'adresse est vérifiée après le mot de passe pour garder une erreur de connexion uniforme.
        return true;
    }
}
