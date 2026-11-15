package fr.miage.kadi.prix.application.port.in;

import fr.miage.kadi.prix.domain.model.Montant;
import fr.miage.kadi.prix.domain.model.TypeDePreuve;
import fr.miage.kadi.prix.domain.model.UniteDePrix;
import java.time.Instant;

/**
 * Ce que la personne envoie quand elle saisit un prix.
 * Sans date, le relevé est daté de l'instant de l'enregistrement.
 */
public record DemandeDeReleve(
        long produitId,
        long magasinId,
        Montant prix,
        UniteDePrix unite,
        TypeDePreuve preuve,
        String contributeur,
        boolean enPromotion,
        Instant releveLe,
        boolean confirmerMalgreAlerte) {
}
