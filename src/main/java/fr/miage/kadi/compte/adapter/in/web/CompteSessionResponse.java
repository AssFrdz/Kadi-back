package fr.miage.kadi.compte.adapter.in.web;

import fr.miage.kadi.compte.adapter.in.security.CompteAuthentifie;
import fr.miage.kadi.compte.domain.model.RoleCompte;

public record CompteSessionResponse(long id, String email, RoleCompte role, boolean emailVerifie) {

    static CompteSessionResponse depuis(CompteAuthentifie compte) {
        return new CompteSessionResponse(compte.id(), compte.email(), compte.role(), true);
    }
}
