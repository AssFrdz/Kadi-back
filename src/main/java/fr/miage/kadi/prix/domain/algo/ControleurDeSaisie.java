package fr.miage.kadi.prix.domain.algo;

import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;
import fr.miage.kadi.prix.domain.model.ControleDeSaisie;
import fr.miage.kadi.prix.domain.model.Montant;
import fr.miage.kadi.prix.domain.model.PrixDeReference;
import fr.miage.kadi.prix.domain.model.ReleveDePrix;
import fr.miage.kadi.prix.domain.model.StatutDeSaisie;
import fr.miage.kadi.prix.domain.model.UniteDePrix;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Contrôle d'un prix au moment de sa saisie (fiche F2, Contrôle à la saisie) :
 * z = (x − m) / s face à la référence existante ; |z| > 3,5 demande une confirmation.
 * Sans référence assez solide (moins de 3 contributeurs, autre unité), rien n'est comparé.
 */
public final class ControleurDeSaisie {

    private final CalculPrixDeReference calcul;
    private final ParametresPrixFiable parametres;

    public ControleurDeSaisie(ParametresPrixFiable parametres) {
        this.calcul = new CalculPrixDeReference(parametres);
        this.parametres = parametres;
    }

    public ControleDeSaisie controler(
            Montant saisi,
            UniteDePrix unite,
            List<ReleveDePrix> duMagasin,
            List<ReleveDePrix> deLEnseigne,
            Instant maintenant) {
        if (saisi == null || !saisi.estStrictementPositif() || unite == null) {
            throw new DonneeInvalideException("Le prix saisi est strictement positif et a une unité");
        }
        Optional<CalculPrixDeReference.Evaluation> evaluation = calcul.evaluer(duMagasin, deLEnseigne, maintenant);
        if (evaluation.isEmpty()
                || evaluation.get().contributeurs() < parametres.contributeursMinimum()
                || evaluation.get().prix().unite() != unite) {
            return ControleDeSaisie.sansReference();
        }

        PrixDeReference reference = evaluation.get().prix();
        BigDecimal m = reference.prix().valeur();
        double z = CalculPrixDeReference.scoreZ(saisi.valeur(), m, evaluation.get().echelle());
        double ecartPourcent = saisi.valeur().subtract(m).doubleValue() / m.doubleValue() * 100;
        StatutDeSaisie statut = Math.abs(z) > parametres.seuilAberrant() ? StatutDeSaisie.INHABITUEL : StatutDeSaisie.NORMAL;
        return new ControleDeSaisie(statut, reference.prix(), z, ecartPourcent);
    }
}
