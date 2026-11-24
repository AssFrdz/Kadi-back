package fr.miage.kadi.prix.domain.model;

import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;

/**
 * Le contrôle d'un prix saisi face à la référence existante.
 * Sans référence, rien n'est comparé : la référence, le score z et l'écart sont absents.
 */
public record ControleDeSaisie(StatutDeSaisie statut, Montant reference, Double scoreZ, Double ecartPourcent) {

    public ControleDeSaisie {
        if (statut == null) {
            throw new DonneeInvalideException("Un contrôle de saisie a un statut");
        }
        boolean compare = reference != null && scoreZ != null && ecartPourcent != null;
        boolean rienCompare = reference == null && scoreZ == null && ecartPourcent == null;
        if (statut == StatutDeSaisie.SANS_REFERENCE ? !rienCompare : !compare) {
            throw new DonneeInvalideException("Seul un contrôle sans référence se passe de référence, de score z et d'écart");
        }
    }

    public static ControleDeSaisie sansReference() {
        return new ControleDeSaisie(StatutDeSaisie.SANS_REFERENCE, null, null, null);
    }

    public boolean demandeConfirmation() {
        return statut == StatutDeSaisie.INHABITUEL;
    }
}
