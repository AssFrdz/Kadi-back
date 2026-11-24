package fr.miage.kadi.catalogue.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Besoin personnel créé par un utilisateur et soumis à l'administrateur. {@code libelleCommun} est le libellé
 * du besoin générique commun une fois la proposition validée ; il peut différer de la saisie d'origine.
 */
public record PropositionBesoin(long id, String libelle, String auteur, StatutProposition statut, Instant creeLe,
        String libelleCommun) {

    public static final int LONGUEUR_MAXIMALE = 255;

    public PropositionBesoin {
        Objects.requireNonNull(libelle, "Le libellé de la proposition est obligatoire.");
        Objects.requireNonNull(statut, "Le statut de la proposition est obligatoire.");
    }

    public boolean estEnAttente() {
        return statut == StatutProposition.EN_ATTENTE;
    }

    /**
     * Libellé publié dans le catalogue commun : la correction de l'administrateur si elle est renseignée,
     * sinon la saisie d'origine. Les espaces superflus sont retirés ; la casse est conservée.
     */
    public String libellePublie(String correction) {
        String choisi = correction == null || correction.isBlank() ? libelle : correction;
        String nettoye = choisi.strip().replaceAll("\\s+", " ");
        if (nettoye.isEmpty()) {
            throw new IllegalArgumentException("Le libellé validé ne peut pas être vide.");
        }
        if (nettoye.length() > LONGUEUR_MAXIMALE) {
            throw new IllegalArgumentException(
                    "Le libellé validé ne peut pas dépasser " + LONGUEUR_MAXIMALE + " caractères.");
        }
        return nettoye;
    }
}
