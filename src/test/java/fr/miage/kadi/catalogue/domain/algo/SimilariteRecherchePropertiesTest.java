package fr.miage.kadi.catalogue.domain.algo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

/**
 * Propriétés de la similarité de Dice, comparée à une implémentation de référence exhaustive
 * écrite directement à partir de la définition (2 |A ∩ B| / (|A| + |B|) sur les trigrammes).
 */
class SimilariteRecherchePropertiesTest {

    @Property
    void leScoreEstToujoursCompriEntreZeroEtUn(@ForAll @AlphaChars @StringLength(min = 1, max = 30) String saisie,
            @ForAll @AlphaChars @StringLength(min = 1, max = 60) String libelle) {
        double score = SimilariteRecherche.score(SimilariteRecherche.normaliser(saisie), libelle);
        assertThat(score).isBetween(0.0, 1.0);
    }

    @Property
    void unLibelleQuiContientLaSaisieObtientLeScoreMaximal(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String saisie,
            @ForAll @AlphaChars @StringLength(max = 20) String avant,
            @ForAll @AlphaChars @StringLength(max = 20) String apres) {
        String normalisee = SimilariteRecherche.normaliser(saisie);
        assertThat(SimilariteRecherche.score(normalisee, avant + " " + saisie + " " + apres)).isEqualTo(1.0);
    }

    @Property
    void laProximiteDuLibelleEstSymetriqueEtEgaleALaReferenceExhaustive(
            @ForAll @AlphaChars @StringLength(min = 1, max = 25) String premier,
            @ForAll @AlphaChars @StringLength(min = 1, max = 25) String second) {
        String a = SimilariteRecherche.normaliser(premier);
        String b = SimilariteRecherche.normaliser(second);
        double proximite = SimilariteRecherche.proximiteLibelle(a, second);
        assertThat(proximite).isCloseTo(diceReference(a, b), org.assertj.core.data.Offset.offset(1e-12));
        assertThat(proximite).isCloseTo(SimilariteRecherche.proximiteLibelle(b, premier),
                org.assertj.core.data.Offset.offset(1e-12));
    }

    @Property
    void laNormalisationEstIdempotente(@ForAll @StringLength(max = 40) String texte) {
        String normalise = SimilariteRecherche.normaliser(texte);
        assertThat(SimilariteRecherche.normaliser(normalise)).isEqualTo(normalise);
    }

    @Property
    void laNormalisationIgnoreCasseEtAccentsUsuels(
            @ForAll @StringLength(max = 30) @net.jqwik.api.constraints.Chars({'a', 'e', 'i', 'o', 'u', 'c', ' ', '-'})
            String texte) {
        String accentue = texte.replace('e', 'é').replace('a', 'à').replace('c', 'ç').replace('o', 'ô');
        assertThat(SimilariteRecherche.normaliser(accentue.toUpperCase(java.util.Locale.ROOT)))
                .isEqualTo(SimilariteRecherche.normaliser(texte));
    }

    private static double diceReference(String premier, String second) {
        Set<String> a = trigrammes(premier);
        Set<String> b = trigrammes(second);
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int communs = 0;
        for (String trigramme : a) {
            if (b.contains(trigramme)) {
                communs++;
            }
        }
        return 2.0 * communs / (a.size() + b.size());
    }

    private static Set<String> trigrammes(String texte) {
        Set<String> resultat = new HashSet<>();
        String entoure = "  " + texte + " ";
        for (int debut = 0; debut + 3 <= entoure.length(); debut++) {
            resultat.add(entoure.substring(debut, debut + 3));
        }
        return resultat;
    }
}
