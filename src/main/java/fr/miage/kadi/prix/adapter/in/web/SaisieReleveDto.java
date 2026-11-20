package fr.miage.kadi.prix.adapter.in.web;

import fr.miage.kadi.prix.domain.model.TypeDePreuve;
import fr.miage.kadi.prix.domain.model.UniteDePrix;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Le corps de POST /api/v1/releves, tel que le front l'envoie.
 * Les champs facultatifs prennent une valeur par défaut : pièce, saisie sans preuve, pas de promotion, date de l'enregistrement.
 */
record SaisieReleveDto(
        @NotNull @Positive Long produitId,
        @NotNull @Positive Long magasinId,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal prix,
        UniteDePrix unite,
        TypeDePreuve preuve,
        @NotBlank @Size(max = 64) String contributeur,
        Boolean enPromotion,
        Instant releveLe,
        Boolean confirmerMalgreAlerte) {
}
