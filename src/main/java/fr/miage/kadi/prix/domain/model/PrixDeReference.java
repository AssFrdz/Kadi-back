package fr.miage.kadi.prix.domain.model;

import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;
import java.util.List;

/** Le résultat du calcul : un prix, la raison d'y croire, et le détail des relevés retenus et écartés. */
public record PrixDeReference(
        Montant prix,
        UniteDePrix unite,
        Confiance confiance,
        NiveauDeCalcul niveau,
        List<ReleveDePrix> retenus,
        List<ReleveEcarte> ecartes) {

    public PrixDeReference {
        if (prix == null || unite == null || confiance == null || niveau == null) {
            throw new DonneeInvalideException("Un prix de référence a un prix, une unité, une confiance et un niveau");
        }
        if (retenus == null || retenus.isEmpty()) {
            throw new DonneeInvalideException("Un prix de référence repose sur au moins un relevé retenu");
        }
        // copies non modifiables : le résultat ne change plus une fois construit
        retenus = List.copyOf(retenus);
        ecartes = ecartes == null ? List.of() : List.copyOf(ecartes);
    }
}
