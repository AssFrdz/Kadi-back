package fr.miage.kadi.catalogue.application.port.out;

import fr.miage.kadi.catalogue.domain.model.ProduitCatalogue;
import fr.miage.kadi.catalogue.domain.model.FiltresCatalogue;
import java.util.List;
import java.util.function.Consumer;

public interface ProduitsCatalogueRepository {

    List<ProduitCatalogue> trouverApres(long dernierId, int limite);

    List<ProduitCatalogue> trouverApres(long dernierId, int limite, FiltresCatalogue filtres);

    List<ProduitCatalogue> trouverParNom(String apresNom, long apresId, int limite, FiltresCatalogue filtres);

    List<ProduitCatalogue> presSelectionner(String recherche, int limite);

    void parcourirCandidats(String recherche, FiltresCatalogue filtres, Consumer<ProduitCatalogue> consommateur);

    void parcourirCatalogue(FiltresCatalogue filtres, Consumer<ProduitCatalogue> consommateur);
}
