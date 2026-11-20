package fr.miage.kadi.prix.application.service;

import fr.miage.kadi.prix.application.port.in.ConsulterPrixDeReference;
import fr.miage.kadi.prix.application.port.in.DemandeDeReleve;
import fr.miage.kadi.prix.application.port.in.EnregistrerReleve;
import fr.miage.kadi.prix.application.port.in.ResultatEnregistrement;
import fr.miage.kadi.prix.application.port.out.Referentiel;
import fr.miage.kadi.prix.application.port.out.RelevesDePrix;
import fr.miage.kadi.prix.domain.algo.CalculPrixDeReference;
import fr.miage.kadi.prix.domain.algo.ControleurDeSaisie;
import fr.miage.kadi.prix.domain.algo.ParametresPrixFiable;
import fr.miage.kadi.prix.domain.exception.DonneeInvalideException;
import fr.miage.kadi.prix.domain.exception.MagasinInconnuException;
import fr.miage.kadi.prix.domain.exception.ProduitInconnuException;
import fr.miage.kadi.prix.domain.model.ControleDeSaisie;
import fr.miage.kadi.prix.domain.model.PrixDeReference;
import fr.miage.kadi.prix.domain.model.ReleveDePrix;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Les deux actions de F2 : consulter un prix fiable, enregistrer une saisie contrôlée.
 * Va chercher les données par les ports de sortie, fait calculer le domaine, ne contient aucun calcul lui-même.
 * L'horloge est injectée : en test, une horloge figée rend les résultats indépendants du jour.
 */
public class PrixFiableService implements ConsulterPrixDeReference, EnregistrerReleve {

    private final RelevesDePrix releves;
    private final Referentiel referentiel;
    private final ParametresPrixFiable parametres;
    private final Clock horloge;
    private final CalculPrixDeReference calcul;
    private final ControleurDeSaisie controleur;

    public PrixFiableService(RelevesDePrix releves, Referentiel referentiel, ParametresPrixFiable parametres, Clock horloge) {
        this.releves = releves;
        this.referentiel = referentiel;
        this.parametres = parametres;
        this.horloge = horloge;
        this.calcul = new CalculPrixDeReference(parametres);
        this.controleur = new ControleurDeSaisie(parametres);
    }

    @Override
    public Optional<PrixDeReference> consulter(long produitId, long magasinId) {
        verifierQueToutExiste(produitId, magasinId);
        Instant maintenant = horloge.instant();
        Instant depuis = maintenant.minus(parametres.fenetre());
        return calcul.calculer(
                releves.duProduitDansLeMagasin(produitId, magasinId, depuis),
                releves.duProduitDansLEnseigneDuMagasin(produitId, magasinId, depuis),
                maintenant);
    }

    @Override
    public ResultatEnregistrement enregistrer(DemandeDeReleve demande) {
        if (demande == null) {
            throw new DonneeInvalideException("La saisie est vide");
        }
        verifierQueToutExiste(demande.produitId(), demande.magasinId());
        Instant maintenant = horloge.instant();
        Instant releveLe = demande.releveLe() == null ? maintenant : demande.releveLe();
        if (releveLe.isAfter(maintenant)) {
            throw new DonneeInvalideException("Un relevé ne peut pas être daté dans le futur : " + releveLe);
        }
        // le relevé refuse lui-même un prix nul, un contributeur vide…
        ReleveDePrix nouveau = new ReleveDePrix(
                null,
                demande.produitId(),
                demande.magasinId(),
                demande.prix(),
                demande.unite(),
                releveLe,
                demande.preuve(),
                demande.contributeur(),
                demande.enPromotion());

        Instant depuis = maintenant.minus(parametres.fenetre());
        List<ReleveDePrix> duMagasin = releves.duProduitDansLeMagasin(demande.produitId(), demande.magasinId(), depuis);
        List<ReleveDePrix> deLEnseigne = releves.duProduitDansLEnseigneDuMagasin(demande.produitId(), demande.magasinId(), depuis);
        ControleDeSaisie controle = controleur.controler(nouveau.prix(), nouveau.unite(), duMagasin, deLEnseigne, maintenant);

        if (controle.demandeConfirmation() && !demande.confirmerMalgreAlerte()) {
            // rien n'est écrit : la personne doit d'abord confirmer
            return new ResultatEnregistrement(controle, null);
        }
        ReleveDePrix enregistre = releves.enregistrer(nouveau, controle.demandeConfirmation());
        return new ResultatEnregistrement(controle, enregistre);
    }

    private void verifierQueToutExiste(long produitId, long magasinId) {
        if (!referentiel.produitExiste(produitId)) {
            throw new ProduitInconnuException(produitId);
        }
        if (!referentiel.magasinExiste(magasinId)) {
            throw new MagasinInconnuException(magasinId);
        }
    }
}
