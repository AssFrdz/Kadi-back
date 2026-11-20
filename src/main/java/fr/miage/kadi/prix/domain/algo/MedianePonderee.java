package fr.miage.kadi.prix.domain.algo;

import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Médiane pondérée : le plus petit élément x tel que les éléments inférieurs ou égaux à x
 * pèsent au moins la moitié du poids total.
 *
 * Calcul par sélection rapide : à chaque tour, un pivot sépare les éléments plus petits, égaux et plus grands,
 * et on ne continue que du côté qui contient la moitié du poids. O(q) en moyenne, O(q²) au pire, O(q) en mémoire.
 * Le pivot aléatoire ne change que la vitesse : la réponse est unique par définition.
 */
public final class MedianePonderee {

    /** Tolérance des comparaisons de poids, qui sont des double (0,1 + 0,2 ≠ 0,3). */
    public static final double TOLERANCE = 1e-9;

    public record Element<T>(T valeur, double poids) {
    }

    private MedianePonderee() {
    }

    public static <T extends Comparable<? super T>> T de(List<Element<T>> elements) {
        return de(elements, ThreadLocalRandom.current());
    }

    public static <T extends Comparable<? super T>> T de(List<Element<T>> elements, RandomGenerator hasard) {
        if (elements == null || elements.isEmpty()) {
            throw new DonneeInvalideException("La médiane pondérée demande au moins un élément");
        }
        double total = 0;
        for (Element<T> element : elements) {
            if (element.valeur() == null || !(element.poids() > 0) || Double.isInfinite(element.poids())) {
                throw new DonneeInvalideException("Chaque élément a une valeur et un poids strictement positif et fini");
            }
            total += element.poids();
        }
        double moitie = total / 2;

        List<Element<T>> restants = new ArrayList<>(elements);
        // poids des éléments déjà écartés parce que plus petits que la médiane
        double poidsDejaEnDessous = 0;
        while (true) {
            T pivot = restants.get(hasard.nextInt(restants.size())).valeur();
            List<Element<T>> plusPetits = new ArrayList<>();
            List<Element<T>> plusGrands = new ArrayList<>();
            double poidsPlusPetits = 0;
            double poidsEgaux = 0;
            for (Element<T> element : restants) {
                int comparaison = element.valeur().compareTo(pivot);
                if (comparaison < 0) {
                    plusPetits.add(element);
                    poidsPlusPetits += element.poids();
                } else if (comparaison > 0) {
                    plusGrands.add(element);
                } else {
                    poidsEgaux += element.poids();
                }
            }
            if (poidsDejaEnDessous + poidsPlusPetits >= moitie - TOLERANCE) {
                // la moitié est atteinte avant le pivot : la médiane est parmi les plus petits
                restants = plusPetits;
            } else if (poidsDejaEnDessous + poidsPlusPetits + poidsEgaux >= moitie - TOLERANCE) {
                return pivot;
            } else {
                poidsDejaEnDessous += poidsPlusPetits + poidsEgaux;
                restants = plusGrands;
            }
        }
    }
}
