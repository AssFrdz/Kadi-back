package fr.miage.kadi.prix.adapter.out.persistence;

import fr.miage.kadi.prix.application.port.out.Referentiel;
import fr.miage.kadi.prix.application.port.out.RelevesDePrix;
import fr.miage.kadi.prix.domain.model.Montant;
import fr.miage.kadi.prix.domain.model.ReleveDePrix;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/** Les deux ports de sortie du module prix, réalisés sur PostgreSQL. */
@Component
class PersistancePrixAdapter implements RelevesDePrix, Referentiel {

    /** Valeur de la colonne source pour une saisie faite dans l'application (l'import écrit OPEN_PRICES). */
    static final String SOURCE_KADI = "KADI";

    private final ReleveDePrixRepository repository;

    PersistancePrixAdapter(ReleveDePrixRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ReleveDePrix> duProduitDansLeMagasin(long produitId, long magasinId, Instant depuis) {
        return repository.findByProduitIdAndMagasinIdAndReleveLeGreaterThanEqual(produitId, magasinId, depuis)
                .stream()
                .map(PersistancePrixAdapter::versDomaine)
                .toList();
    }

    @Override
    public List<ReleveDePrix> duProduitDansLEnseigneDuMagasin(long produitId, long magasinId, Instant depuis) {
        return repository.duProduitDansLEnseigneDuMagasin(produitId, magasinId, depuis)
                .stream()
                .map(PersistancePrixAdapter::versDomaine)
                .toList();
    }

    @Override
    public ReleveDePrix enregistrer(ReleveDePrix releve, boolean confirmeMalgreAlerte) {
        ReleveDePrixEntite entite = new ReleveDePrixEntite(
                releve.produitId(),
                releve.magasinId(),
                releve.prix().valeur(),
                releve.releveLe(),
                SOURCE_KADI,
                releve.preuve(),
                releve.contributeur(),
                releve.unite(),
                releve.enPromotion(),
                confirmeMalgreAlerte);
        return versDomaine(repository.save(entite));
    }

    @Override
    public boolean produitExiste(long produitId) {
        return repository.produitExiste(produitId);
    }

    @Override
    public boolean magasinExiste(long magasinId) {
        return repository.magasinExiste(magasinId);
    }

    private static ReleveDePrix versDomaine(ReleveDePrixEntite entite) {
        return new ReleveDePrix(
                entite.getId(),
                entite.getProduitId(),
                entite.getMagasinId(),
                new Montant(entite.getPrix()),
                entite.getUnite(),
                entite.getReleveLe(),
                entite.getTypePreuve(),
                entite.getContributeur(),
                entite.isEnPromotion());
    }
}
