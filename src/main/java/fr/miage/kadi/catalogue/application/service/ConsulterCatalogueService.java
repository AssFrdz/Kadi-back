package fr.miage.kadi.catalogue.application.service;

import fr.miage.kadi.catalogue.application.port.out.ProduitsCatalogueRepository;
import fr.miage.kadi.catalogue.domain.model.ProduitCatalogue;
import fr.miage.kadi.catalogue.domain.model.FiltresCatalogue;
import java.util.List;

public class ConsulterCatalogueService {

    private static final int TAILLE_PAGE = 20;
    private final ProduitsCatalogueRepository produits;

    public ConsulterCatalogueService(ProduitsCatalogueRepository produits) {
        this.produits = produits;
    }

    public PageCatalogue consulter(long dernierId, FiltresCatalogue filtres) {
        if (dernierId < 0) {
            throw new IllegalArgumentException("L'identifiant de pagination doit être positif ou nul.");
        }
        List<ProduitCatalogue> resultats = produits.trouverApres(dernierId, TAILLE_PAGE + 1, filtres);
        boolean suite = resultats.size() > TAILLE_PAGE;
        List<ProduitCatalogue> page = List.copyOf(resultats.subList(0, Math.min(resultats.size(), TAILLE_PAGE)));
        Long prochainId = suite ? page.get(page.size() - 1).id() : null;
        return new PageCatalogue(page, prochainId);
    }

    public record PageCatalogue(List<ProduitCatalogue> produits, Long prochainId) {
    }
}
