package fr.miage.kadi.prix.domain.model;

import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Une somme en euros, au centime près. Jamais de double pour de l'argent. */
public record Montant(BigDecimal valeur) implements Comparable<Montant> {

    public Montant {
        if (valeur == null) {
            throw new DonneeInvalideException("Le montant est obligatoire");
        }
        if (valeur.signum() < 0) {
            throw new DonneeInvalideException("Un montant ne peut pas être négatif : " + valeur);
        }
        if (valeur.stripTrailingZeros().scale() > 2) {
            throw new DonneeInvalideException("Un montant a au plus 2 décimales : " + valeur);
        }
        // 1.5 et 1.50 deviennent le même montant
        valeur = valeur.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static Montant de(String valeur) {
        return new Montant(new BigDecimal(valeur));
    }

    public boolean estStrictementPositif() {
        return valeur.signum() > 0;
    }

    @Override
    public int compareTo(Montant autre) {
        return valeur.compareTo(autre.valeur);
    }
}
