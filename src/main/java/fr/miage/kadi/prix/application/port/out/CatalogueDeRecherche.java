package fr.miage.kadi.prix.application.port.out;

import fr.miage.kadi.prix.application.port.in.RechercherPourLaSaisie.MagasinTrouve;
import fr.miage.kadi.prix.application.port.in.RechercherPourLaSaisie.ProduitTrouve;
import java.util.List;

/** Les produits et magasins dont le nom contient tous les mots donnés (déjà en minuscules). */
public interface CatalogueDeRecherche {

    List<ProduitTrouve> produitsContenant(List<String> mots, int limite);

    List<ProduitTrouve> produitDeCodeBarres(String codeBarres);

    List<MagasinTrouve> magasinsContenant(List<String> mots, int limite);
}
