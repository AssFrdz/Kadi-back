package fr.miage.kadi.prix.domain.model;

import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;

/** Un relevé exclu du calcul, avec la raison de son exclusion. */
public record ReleveEcarte(ReleveDePrix releve, MotifExclusion motif) {

    public ReleveEcarte {
        if (releve == null || motif == null) {
            throw new DonneeInvalideException("Un relevé écarté a toujours un motif");
        }
    }
}
