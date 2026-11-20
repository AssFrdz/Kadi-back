package fr.miage.kadi.prix.application.port.in;

import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;
import fr.miage.kadi.prix.domain.model.Montant;
import fr.miage.kadi.prix.domain.model.TypeDePreuve;
import fr.miage.kadi.prix.domain.model.UniteDePrix;
import java.time.Instant;

/**
 * Un relevé venu d'une source externe, avec de quoi retrouver ou créer son produit, son magasin et son enseigne.
 * La source et sa référence rendent l'import relançable sans doublon.
 */
public record ReleveAImporter(
        String source,
        String referenceExterne,
        String codeBarres,
        String libelleProduit,
        String referenceMagasin,
        String nomMagasin,
        String enseigne,
        Montant prix,
        UniteDePrix unite,
        Instant releveLe,
        TypeDePreuve preuve,
        String contributeur,
        boolean enPromotion) {

    public ReleveAImporter {
        if (estVide(source) || estVide(referenceExterne) || estVide(codeBarres) || estVide(libelleProduit)
                || estVide(referenceMagasin) || estVide(nomMagasin) || estVide(enseigne) || estVide(contributeur)) {
            throw new DonneeInvalideException("Un relevé à importer désigne sa source, son produit, son magasin et son contributeur");
        }
        if (prix == null || !prix.estStrictementPositif() || unite == null || releveLe == null || preuve == null) {
            throw new DonneeInvalideException("Un relevé à importer a un prix strictement positif, une unité, une date et une preuve");
        }
    }

    private static boolean estVide(String texte) {
        return texte == null || texte.isBlank();
    }
}
