package fr.miage.kadi.catalogue.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.kadi.catalogue.application.port.out.ProduitsCatalogueRepository;
import fr.miage.kadi.catalogue.application.port.out.RechercheProduitsCompatiblesRepository;
import fr.miage.kadi.catalogue.domain.algo.SimilariteRecherche;
import fr.miage.kadi.catalogue.domain.model.ConfianceRecherche;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.ProduitRechercheCompatible;
import fr.miage.kadi.catalogue.domain.model.ProduitTrouve;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Le classement des correspondances (file de priorité bornée à 20) est comparé à une référence exhaustive :
 * filtrer tous les libellés contenant tous les mots, tout trier selon la même règle, garder les 20 premiers.
 */
class RechercheClassementPropertiesTest {
    private static final List<String> MOTS = List.of("lait", "yaourt", "nature", "bio", "fraise", "pack", "demi",
            "ecreme", "boisson", "coco", "1 l", "500 g");

    @Property(tries = 300)
    void leTopVingtEstIdentiqueAuTriExhaustif(@ForAll("catalogues") List<String> libelles,
            @ForAll("saisies") String saisie) {
        List<ProduitRechercheCompatible> catalogue = new ArrayList<>();
        for (int index = 0; index < libelles.size(); index++) {
            catalogue.add(new ProduitRechercheCompatible(index + 1L, libelles.get(index), null));
        }
        var service = new RechercherProduitsCompatiblesService(new CorrespondancesEnMemoire(catalogue),
                new RechercherCatalogueService(catalogueVide()));

        List<Long> obtenus = service.rechercherAvecConfiance(saisie).stream()
                .filter(trouve -> trouve.confiance() == ConfianceRecherche.SUR)
                .map(ProduitTrouve::produit).map(ProduitRechercheCompatible::id).toList();

        assertThat(obtenus).isEqualTo(referenceExhaustive(catalogue, saisie));
    }

    private static List<Long> referenceExhaustive(List<ProduitRechercheCompatible> catalogue, String saisie) {
        String normalisee = SimilariteRecherche.normaliser(saisie);
        List<String> mots = List.of(normalisee.split(" "));
        Comparator<ProduitRechercheCompatible> ordre = Comparator
                .comparingDouble((ProduitRechercheCompatible p) -> -SimilariteRecherche.score(normalisee, p.libelle()))
                .thenComparingDouble(p -> -SimilariteRecherche.proximiteLibelle(normalisee, p.libelle()))
                .thenComparing(p -> SimilariteRecherche.normaliser(p.libelle()))
                .thenComparingLong(ProduitRechercheCompatible::id);
        return catalogue.stream()
                .filter(p -> mots.stream().allMatch(SimilariteRecherche.normaliser(p.libelle())::contains))
                .sorted(ordre).limit(RechercherProduitsCompatiblesService.LIMITE)
                .map(ProduitRechercheCompatible::id).toList();
    }

    @Provide
    Arbitrary<List<String>> catalogues() {
        Arbitrary<String> libelle = Arbitraries.of(MOTS).list().ofMinSize(1).ofMaxSize(5)
                .map(mots -> String.join(" ", mots));
        return libelle.list().ofMinSize(0).ofMaxSize(60);
    }

    @Provide
    Arbitrary<String> saisies() {
        return Arbitraries.of(MOTS).list().ofMinSize(1).ofMaxSize(2).map(mots -> String.join(" ", mots));
    }

    private static ProduitsCatalogueRepository catalogueVide() {
        return (ProduitsCatalogueRepository) Proxy.newProxyInstance(ProduitsCatalogueRepository.class.getClassLoader(),
                new Class<?>[] {ProduitsCatalogueRepository.class},
                (proxy, methode, arguments) -> List.class.isAssignableFrom(methode.getReturnType()) ? List.of() : null);
    }

    /** Même sémantique que la requête SQL : tous les mots normalisés présents dans le libellé normalisé. */
    private record CorrespondancesEnMemoire(List<ProduitRechercheCompatible> catalogue)
            implements RechercheProduitsCompatiblesRepository {
        @Override
        public List<ProduitRechercheCompatible> trouverParCodeBarres(String codeBarres) {
            return List.of();
        }

        @Override
        public void parcourirCorrespondances(List<String> mots, List<String> motsNormalises, boolean inclureGeneriques,
                Consumer<ProduitRechercheCompatible> consommateur) {
            catalogue.stream()
                    .filter(p -> motsNormalises.stream().allMatch(SimilariteRecherche.normaliser(p.libelle())::contains))
                    .forEach(consommateur);
        }

        @Override
        public List<ProduitRechercheCompatible> listerBesoinsGeneriques() {
            return List.of();
        }

        @Override
        public List<ProduitRechercheCompatible> listerBesoinsPersonnels(IdentifiantCompte proprietaire) {
            return List.of();
        }
    }
}
