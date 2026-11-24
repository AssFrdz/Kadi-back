package fr.miage.kadi.catalogue.application.port.out;

import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.ProduitRechercheCompatible;
import java.util.List;
import java.util.function.Consumer;

public interface RechercheProduitsCompatiblesRepository {
    /** Produits précis portant exactement ce code-barres (un besoin générique n'en a jamais). */
    List<ProduitRechercheCompatible> trouverParCodeBarres(String codeBarres);

    /** Correspondances par mots parmi les produits précis, et aussi parmi les besoins génériques si demandé. */
    void parcourirCorrespondances(List<String> mots, List<String> motsNormalises, boolean inclureGeneriques,
            Consumer<ProduitRechercheCompatible> consommateur);

    default void parcourirCorrespondances(List<String> mots, List<String> motsNormalises,
            Consumer<ProduitRechercheCompatible> consommateur) {
        parcourirCorrespondances(mots, motsNormalises, false, consommateur);
    }

    /** Tous les besoins génériques du catalogue (quelques dizaines à quelques centaines). */
    List<ProduitRechercheCompatible> listerBesoinsGeneriques();

    /** Les besoins personnels de ce compte, quel que soit leur statut de validation ; jamais ceux des autres. */
    List<ProduitRechercheCompatible> listerBesoinsPersonnels(IdentifiantCompte proprietaire);
}
