package fr.miage.kadi.prix.domain.algo;

import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;
import java.time.Duration;
import java.time.ZoneId;

/**
 * Tous les réglages du calcul du prix de référence, au même endroit.
 * Fenêtre et demi-vie ont été recalibrées sur les données Open Prices (fiche F2, section Mesures sur les données réelles).
 */
public record ParametresPrixFiable(
        Duration fenetre,
        Duration demiViePoids,
        Duration demiVieFraicheur,
        double seuilAberrant,
        double plancherEchelle,
        double dispersionMaximale,
        int contributeursMinimum,
        double penaliteRepli,
        ZoneId fuseau) {

    /**
     * Valeurs de la fiche : fenêtre de 180 jours, demi-vies de 30 jours, seuil 3,5, plancher 2 %, dispersion 20 %, 3 contributeurs.
     * Fenêtre et demi-vie vont ensemble : avec 14 jours de demi-vie, un relevé de 6 mois pèserait 2^-13, presque rien.
     */
    public static final ParametresPrixFiable PAR_DEFAUT = new ParametresPrixFiable(
            Duration.ofDays(180),
            Duration.ofDays(30),
            Duration.ofDays(30),
            3.5,
            0.02,
            0.20,
            3,
            0.5,
            ZoneId.of("Europe/Paris"));

    public ParametresPrixFiable {
        if (fenetre == null || demiViePoids == null || demiVieFraicheur == null || fuseau == null) {
            throw new DonneeInvalideException("Les durées et le fuseau du calcul sont obligatoires");
        }
        if (fenetre.isNegative() || fenetre.isZero() || demiViePoids.isNegative() || demiViePoids.isZero()
                || demiVieFraicheur.isNegative() || demiVieFraicheur.isZero()) {
            throw new DonneeInvalideException("Les durées du calcul sont strictement positives");
        }
        if (seuilAberrant <= 0 || plancherEchelle <= 0 || dispersionMaximale <= 0 || contributeursMinimum < 2) {
            throw new DonneeInvalideException("Les seuils du calcul sont strictement positifs, et il faut au moins 2 contributeurs");
        }
        if (penaliteRepli <= 0 || penaliteRepli > 1) {
            throw new DonneeInvalideException("La pénalité de repli est comprise entre 0 (exclu) et 1");
        }
    }
}
