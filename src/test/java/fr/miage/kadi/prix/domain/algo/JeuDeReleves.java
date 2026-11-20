package fr.miage.kadi.prix.domain.algo;

import fr.miage.kadi.prix.domain.model.Montant;
import fr.miage.kadi.prix.domain.model.ReleveDePrix;
import fr.miage.kadi.prix.domain.model.TypeDePreuve;
import fr.miage.kadi.prix.domain.model.UniteDePrix;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Fabrique de relevés pour les tests : un instant présent figé, des âges en jours. */
final class JeuDeReleves {

    static final Instant MAINTENANT = Instant.parse("2026-11-14T12:00:00Z");
    static final long CLICHY = 1;
    static final long GENNEVILLIERS = 2;

    private static long prochainId = 1;

    private JeuDeReleves() {
    }

    static ReleveDePrix releve(String prix, TypeDePreuve preuve, int ageEnJours, String contributeur) {
        return releve(CLICHY, prix, preuve, ageEnJours, contributeur);
    }

    static ReleveDePrix releve(long magasin, String prix, TypeDePreuve preuve, int ageEnJours, String contributeur) {
        return new ReleveDePrix(prochainId++, 1, magasin, Montant.de(prix), UniteDePrix.PIECE,
                MAINTENANT.minus(Duration.ofDays(ageEnJours)), preuve, contributeur, false);
    }

    static ReleveDePrix enPromotion(String prix, int ageEnJours, String contributeur) {
        return new ReleveDePrix(prochainId++, 1, CLICHY, Montant.de(prix), UniteDePrix.PIECE,
                MAINTENANT.minus(Duration.ofDays(ageEnJours)), TypeDePreuve.ETIQUETTE, contributeur, true);
    }

    static ReleveDePrix auKilo(String prix, int ageEnJours, String contributeur) {
        return new ReleveDePrix(prochainId++, 1, CLICHY, Montant.de(prix), UniteDePrix.KILOGRAMME,
                MAINTENANT.minus(Duration.ofDays(ageEnJours)), TypeDePreuve.ETIQUETTE, contributeur, false);
    }

    /** L'exemple chiffré de la fiche F2 : Lactel 1 L au Leclerc de Clichy. R4 (0,10 €) est la faute de frappe. */
    static List<ReleveDePrix> exempleLactel() {
        return List.of(
                releve("1.09", TypeDePreuve.TICKET, 3, "A"),
                releve("1.09", TypeDePreuve.ETIQUETTE, 7, "B"),
                releve("1.12", TypeDePreuve.ETIQUETTE, 12, "C"),
                releve("0.10", TypeDePreuve.SAISIE, 13, "D"),
                releve("1.15", TypeDePreuve.ETIQUETTE, 1, "E"),
                releve("1.15", TypeDePreuve.ETIQUETTE, 2, "E"),
                releve("1.15", TypeDePreuve.ETIQUETTE, 3, "E"),
                releve("1.15", TypeDePreuve.ETIQUETTE, 4, "E"),
                releve("1.15", TypeDePreuve.ETIQUETTE, 5, "E"));
    }
}
