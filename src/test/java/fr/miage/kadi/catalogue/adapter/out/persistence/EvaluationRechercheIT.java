package fr.miage.kadi.catalogue.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.kadi.catalogue.application.port.out.ProduitsCatalogueRepository;
import fr.miage.kadi.catalogue.application.service.RechercherCatalogueService;
import fr.miage.kadi.catalogue.domain.algo.SimilariteRecherche;
import fr.miage.kadi.catalogue.domain.model.ProduitCatalogue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EvaluationRechercheIT {
    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProduitsCatalogueRepository repository;

    @Autowired
    private RechercherCatalogueService recherche;

    @Test
    void mesureLaRechercheSurUnCatalogueFictifEtCompareAuClassementExhaustif() {
        jdbc.update("DELETE FROM releve_de_prix");
        jdbc.update("DELETE FROM produit");
        List<String> noms = List.of(
                "Yaourt nature", "Yaourt fraise", "Yaourt nature 4 x 125 g",
                "Pâtes Barilla penne 500 g", "Pâtes Barilla spaghetti 1 kg",
                "Pâtes marque fictive 500 g", "Coca-Cola 6 x 33 cl",
                "Coca-Cola 1,5 l", "Lait demi-écrémé 1 l", "Lait entier 1 l",
                "Beurre doux 250 g", "Compote pommes 4 x 100 g");
        noms.forEach(nom -> jdbc.update("INSERT INTO produit(libelle) VALUES (?)", nom));
        List<ProduitCatalogue> catalogue = repository.trouverApres(0, 100);
        List<Cas> cas = List.of(
                new Cas("Yaourt nature", Set.of("Yaourt nature", "Yaourt nature 4 x 125 g")),
                new Cas("yaort", Set.of("Yaourt nature", "Yaourt fraise", "Yaourt nature 4 x 125 g")),
                new Cas("pates", Set.of("Pâtes Barilla penne 500 g", "Pâtes Barilla spaghetti 1 kg", "Pâtes marque fictive 500 g")),
                new Cas("pâtes barilla", Set.of("Pâtes Barilla penne 500 g", "Pâtes Barilla spaghetti 1 kg")),
                new Cas("coca 6", Set.of("Coca-Cola 6 x 33 cl")),
                new Cas("lait", Set.of("Lait demi-écrémé 1 l", "Lait entier 1 l")),
                new Cas("beurre", Set.of("Beurre doux 250 g")),
                new Cas("chocolat", Set.of()),
                new Cas("zzzzzz", Set.of()));

        int pertinents = 0;
        int trouves = 0;
        int renvoyes = 0;
        int candidatsPertinents = 0;
        int rappelesExhaustifs = 0;
        int totalExhaustifs = 0;
        int requetesDifferentes = 0;
        for (Cas test : cas) {
            String normalise = SimilariteRecherche.normaliser(test.saisie());
            Set<String> candidats = libelles(repository.presSelectionner(normalise, 200));
            List<ProduitCatalogue> suggestions = recherche.rechercher(test.saisie());
            Set<String> obtenus = libelles(suggestions);
            List<ProduitCatalogue> exhaustifs = classementExhaustif(catalogue, normalise);
            Set<String> attendusExhaustifs = libelles(exhaustifs);
            if (!suggestions.equals(exhaustifs)) {
                requetesDifferentes++;
            }
            pertinents += test.attendus().size();
            trouves += intersection(test.attendus(), obtenus);
            renvoyes += obtenus.size();
            candidatsPertinents += intersection(test.attendus(), candidats);
            totalExhaustifs += attendusExhaustifs.size();
            rappelesExhaustifs += intersection(attendusExhaustifs, candidats);
            System.out.printf("%s : attendus=%s, suggestions=%s, exhaustif=%s%n",
                    test.saisie(), test.attendus(), suggestions.stream().map(ProduitCatalogue::libelle).toList(),
                    exhaustifs.stream().map(ProduitCatalogue::libelle).toList());
            assertThat(suggestions).hasSizeLessThanOrEqualTo(7);
            assertThat(obtenus).as("Rappel pour %s", test.saisie()).containsAll(test.attendus());
            if (test.attendus().isEmpty()) {
                assertThat(obtenus).as("Recherche sans correspondance : %s", test.saisie()).isEmpty();
            }
            if (test.saisie().equals("coca 6")) {
                assertThat(obtenus).doesNotContain("Coca-Cola 1,5 l");
            }
        }
        System.out.printf("METRIQUES pertinents=%d trouves=%d renvoyes=%d candidatsPertinents=%d "
                        + "exhaustifs=%d candidatsExhaustifs=%d classementsDifferents=%d%n",
                pertinents, trouves, renvoyes, candidatsPertinents,
                totalExhaustifs, rappelesExhaustifs, requetesDifferentes);
        assertThat(pertinents).isEqualTo(14);
        assertThat(trouves).isEqualTo(14);
        assertThat(renvoyes).isLessThanOrEqualTo(15);
        assertThat(candidatsPertinents).isEqualTo(14);
    }

    private static List<ProduitCatalogue> classementExhaustif(List<ProduitCatalogue> catalogue, String saisie) {
        List<ProduitCatalogue> resultat = new ArrayList<>(catalogue);
        resultat.removeIf(produit -> SimilariteRecherche.scoreProduit(saisie, produit) < 0.5);
        resultat.sort(Comparator.comparingDouble((ProduitCatalogue produit) ->
                        SimilariteRecherche.scoreProduit(saisie, produit)).reversed()
                .thenComparing(produit -> SimilariteRecherche.normaliser(produit.libelle()))
                .thenComparingLong(ProduitCatalogue::id));
        return resultat.stream().limit(7).toList();
    }

    private static Set<String> libelles(List<ProduitCatalogue> produits) {
        Set<String> resultat = new HashSet<>();
        produits.forEach(produit -> resultat.add(produit.libelle()));
        return resultat;
    }

    private static int intersection(Set<String> premier, Set<String> second) {
        Set<String> communs = new HashSet<>(premier);
        communs.retainAll(second);
        return communs.size();
    }

    private record Cas(String saisie, Set<String> attendus) {
    }
}
