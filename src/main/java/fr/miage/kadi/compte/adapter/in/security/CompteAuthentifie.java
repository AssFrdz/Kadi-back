package fr.miage.kadi.compte.adapter.in.security;

import fr.miage.kadi.compte.domain.model.RoleCompte;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record CompteAuthentifie(long id, String email, RoleCompte role) implements Serializable {

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
