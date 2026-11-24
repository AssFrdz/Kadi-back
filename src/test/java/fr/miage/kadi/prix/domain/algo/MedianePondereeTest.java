package fr.miage.kadi.prix.domain.algo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.kadi.prix.domain.algo.MedianePonderee.Element;
import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

/**
 * La médiane pondérée par sélection rapide, face à un oracle évident : trier, puis cumuler les poids.
 * Fiche F2, critère « identique à l'oracle par tri, sur des listes aléatoires ».
 */
class MedianePondereeTest {

    @Test
    void a_deux_temoins_de_meme_poids_la_mediane_est_le_plus_petit_prix() {
        assertThat(MedianePonderee.de(List.of(new Element<>(109, 1.0), new Element<>(10, 1.0)))).isEqualTo(10);
    }

    @Test
    void a_trois_temoins_la_majorite_l_emporte() {
        assertThat(MedianePonderee.de(List.of(
                new Element<>(109, 1.0), new Element<>(10, 1.0), new Element<>(109, 1.0)))).isEqualTo(109);
    }

    @Test
    void un_temoin_lourd_peut_peser_plus_que_plusieurs_legers() {
        assertThat(MedianePonderee.de(List.of(
                new Element<>(200, 5.0), new Element<>(100, 1.0), new Element<>(110, 1.0), new Element<>(120, 1.0))))
                .isEqualTo(200);
    }

    @Test
    void la_mediane_d_un_seul_element_est_cet_element() {
        assertThat(MedianePonderee.de(List.of(new Element<>(42, 0.3)))).isEqualTo(42);
    }

    @Test
    void la_mediane_refuse_une_liste_vide_et_les_poids_absurdes() {
        assertThatThrownBy(() -> MedianePonderee.de(List.<Element<Integer>>of())).isInstanceOf(DonneeInvalideException.class);
        assertThatThrownBy(() -> MedianePonderee.de(List.of(new Element<>(1, 0.0)))).isInstanceOf(DonneeInvalideException.class);
        assertThatThrownBy(() -> MedianePonderee.de(List.of(new Element<>(1, Double.NaN)))).isInstanceOf(DonneeInvalideException.class);
    }

    @Property(tries = 1000)
    void la_selection_rapide_donne_toujours_le_resultat_du_tri(
            @ForAll("elements") List<Element<Integer>> elements, @ForAll long graine) {
        assertThat(MedianePonderee.de(elements, new Random(graine))).isEqualTo(oracleParTri(elements));
    }

    @Property(tries = 200)
    void le_hasard_du_pivot_ne_change_jamais_la_reponse(
            @ForAll("elements") List<Element<Integer>> elements, @ForAll long graine1, @ForAll long graine2) {
        assertThat(MedianePonderee.de(elements, new Random(graine1)))
                .isEqualTo(MedianePonderee.de(elements, new Random(graine2)));
    }

    /** Des prix en centimes (beaucoup d'égalités), des poids de 0,01 à 10, de 1 à 200 éléments. */
    @Provide
    Arbitrary<List<Element<Integer>>> elements() {
        Arbitrary<Element<Integer>> element = Combinators.combine(
                        Arbitraries.integers().between(1, 50),
                        Arbitraries.doubles().between(0.01, 10.0))
                .as(Element::new);
        return element.list().ofMinSize(1).ofMaxSize(200);
    }

    /** L'oracle : O(q log q), évident, sans astuce. */
    private static Integer oracleParTri(List<Element<Integer>> elements) {
        List<Element<Integer>> tries = new ArrayList<>(elements);
        tries.sort(Comparator.comparing(Element::valeur));
        double moitie = tries.stream().mapToDouble(Element::poids).sum() / 2;
        double cumul = 0;
        for (Element<Integer> element : tries) {
            cumul += element.poids();
            if (cumul >= moitie - MedianePonderee.TOLERANCE) {
                return element.valeur();
            }
        }
        throw new IllegalStateException("la moitié est toujours atteinte");
    }
}
