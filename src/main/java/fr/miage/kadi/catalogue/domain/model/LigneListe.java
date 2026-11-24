package fr.miage.kadi.catalogue.domain.model;

import java.util.UUID;

public record LigneListe(UUID id, ContenuLigne contenu) {

    public LigneListe {
        if (id == null) {
            throw new IllegalArgumentException("L'identifiant de la ligne ne peut pas être null.");
        }

        if (contenu == null) {
            throw new IllegalArgumentException("Le contenu de la ligne ne peut pas être null.");
        }
    }

    public LigneListe avecContenu(ContenuLigne nouveauContenu) {
        return new LigneListe(id, nouveauContenu);
    }
}
