package fr.miage.kadi.prix.application.port.in;

import fr.miage.kadi.prix.domain.model.ControleDeSaisie;
import fr.miage.kadi.prix.domain.model.ReleveDePrix;

/** Le contrôle de la saisie, et le relevé enregistré ; le relevé est absent si une confirmation est attendue. */
public record ResultatEnregistrement(ControleDeSaisie controle, ReleveDePrix releve) {

    public boolean estEnregistre() {
        return releve != null;
    }
}
