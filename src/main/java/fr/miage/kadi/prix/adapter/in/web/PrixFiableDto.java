package fr.miage.kadi.prix.adapter.in.web;

import fr.miage.kadi.prix.domain.model.ControleDeSaisie;
import fr.miage.kadi.prix.domain.model.PrixDeReference;
import fr.miage.kadi.prix.domain.model.ReleveDePrix;
import fr.miage.kadi.prix.domain.model.ReleveEcarte;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Les réponses JSON de l'API du prix fiable. Le contributeur n'est jamais renvoyé :
 * c'est un identifiant personnel, inutile pour juger un prix.
 */
final class PrixFiableDto {

    private PrixFiableDto() {
    }

    record ReleveReponse(Long id, BigDecimal prix, String unite, Instant releveLe, String preuve, boolean enPromotion) {

        static ReleveReponse de(ReleveDePrix releve) {
            return new ReleveReponse(releve.id(), releve.prix().valeur(), releve.unite().name(), releve.releveLe(),
                    releve.preuve().name(), releve.enPromotion());
        }
    }

    record ReleveEcarteReponse(ReleveReponse releve, String motif) {

        static ReleveEcarteReponse de(ReleveEcarte ecarte) {
            return new ReleveEcarteReponse(ReleveReponse.de(ecarte.releve()), ecarte.motif().name());
        }
    }

    record ConfianceReponse(double score, String niveau) {
    }

    /** Réponse de GET …/prix-de-reference. */
    record PrixDeReferenceReponse(
            BigDecimal prix,
            String unite,
            ConfianceReponse confiance,
            String niveauDeCalcul,
            List<ReleveReponse> retenus,
            List<ReleveEcarteReponse> ecartes) {

        static PrixDeReferenceReponse de(PrixDeReference reference) {
            return new PrixDeReferenceReponse(
                    reference.prix().valeur(),
                    reference.unite().name(),
                    new ConfianceReponse(reference.confiance().score(), reference.confiance().niveau().name()),
                    reference.niveau().name(),
                    reference.retenus().stream().map(ReleveReponse::de).toList(),
                    reference.ecartes().stream().map(ReleveEcarteReponse::de).toList());
        }
    }

    /** Le verdict du contrôle de saisie, renvoyé avec le relevé enregistré (201) ou dans l'erreur 422. */
    record ControleReponse(String statut, BigDecimal reference, Double scoreZ, Double ecartPourcent, boolean demandeConfirmation) {

        static ControleReponse de(ControleDeSaisie controle) {
            return new ControleReponse(
                    controle.statut().name(),
                    controle.reference() == null ? null : controle.reference().valeur(),
                    controle.scoreZ(),
                    controle.ecartPourcent(),
                    controle.demandeConfirmation());
        }
    }

    /** Réponse de POST /api/v1/releves quand le relevé est enregistré. */
    record ReleveEnregistreReponse(ReleveReponse releve, ControleReponse controle) {
    }
}
