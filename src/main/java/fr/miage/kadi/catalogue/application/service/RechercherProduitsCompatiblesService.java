package fr.miage.kadi.catalogue.application.service;

import fr.miage.kadi.catalogue.application.port.out.RechercheProduitsCompatiblesRepository;
import fr.miage.kadi.catalogue.domain.algo.SimilariteRecherche;
import fr.miage.kadi.catalogue.domain.model.ConfianceRecherche;
import fr.miage.kadi.catalogue.domain.model.FiltresCatalogue;
import fr.miage.kadi.catalogue.domain.model.IdentifiantCompte;
import fr.miage.kadi.catalogue.domain.model.ProduitRechercheCompatible;
import fr.miage.kadi.catalogue.domain.model.ProduitTrouve;
import fr.miage.kadi.catalogue.domain.model.TypeReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Recherche commune F1/F2 (GET /api/v1/produits).
 * <ul>
 * <li>Portée par défaut (F2, catalogue) : produits précis uniquement.</li>
 * <li>Portée « liste » (F1) : produits précis, besoins génériques du catalogue et besoins personnels du compte.</li>
 * </ul>
 * Les correspondances de tous les mots sont {@link ConfianceRecherche#SUR} et classées par ressemblance ;
 * les résultats approchés (fautes de frappe) suivent, {@link ConfianceRecherche#A_CONFIRMER}.
 */
public class RechercherProduitsCompatiblesService {
    static final int LIMITE = 20;
    static final double SEUIL_APPROCHE = 0.5;
    private static final Comparator<Resultat> ORDRE = Comparator.comparingDouble(Resultat::score).reversed()
            .thenComparing(Comparator.comparingDouble(Resultat::proximite).reversed())
            .thenComparing(Resultat::nomNormalise)
            .thenComparing(resultat -> resultat.produit().type())
            .thenComparingLong(resultat -> resultat.produit().id());

    private final RechercheProduitsCompatiblesRepository produits;
    private final RechercherCatalogueService rechercheCatalogue;

    public RechercherProduitsCompatiblesService(RechercheProduitsCompatiblesRepository produits,
            RechercherCatalogueService rechercheCatalogue) {
        this.produits = produits;
        this.rechercheCatalogue = rechercheCatalogue;
    }

    public List<ProduitRechercheCompatible> rechercher(String saisie) {
        return rechercherAvecConfiance(saisie).stream().map(ProduitTrouve::produit).toList();
    }

    /** Portée par défaut : produits précis uniquement (contrat F2). */
    public List<ProduitTrouve> rechercherAvecConfiance(String saisie) {
        return rechercher(saisie, null);
    }

    /** Portée « liste » : ajoute les besoins génériques et les besoins personnels de ce compte, jamais ceux des autres. */
    public List<ProduitTrouve> rechercherPourListe(String saisie, IdentifiantCompte proprietaire) {
        return rechercher(saisie, Objects.requireNonNull(proprietaire, "Le propriétaire est obligatoire."));
    }

    private List<ProduitTrouve> rechercher(String saisie, IdentifiantCompte proprietaire) {
        boolean liste = proprietaire != null;
        String texte = Objects.requireNonNull(saisie, "La recherche est obligatoire.").trim();
        if (texte.codePointCount(0, texte.length()) < 2) {
            return List.of();
        }
        if (texte.matches("[0-9]{6,14}")) {
            return produits.trouverParCodeBarres(texte).stream()
                    .map(produit -> new ProduitTrouve(produit, ConfianceRecherche.SUR)).toList();
        }

        String normalisee = SimilariteRecherche.normaliser(texte);
        List<String> mots = Arrays.asList(texte.split("\\s+"));
        List<String> motsNormalises = normalisee.isEmpty() ? List.of() : Arrays.asList(normalisee.split(" "));
        PriorityQueue<Resultat> meilleurs = new PriorityQueue<>(LIMITE + 1, ORDRE.reversed());
        produits.parcourirCorrespondances(mots, motsNormalises, liste,
                produit -> retenir(meilleurs, resultat(normalisee, produit)));

        List<ProduitRechercheCompatible> personnels = liste ? produits.listerBesoinsPersonnels(proprietaire) : List.of();
        for (ProduitRechercheCompatible besoin : personnels) {
            if (contientTousLesMots(besoin.libelle(), motsNormalises)) {
                retenir(meilleurs, resultat(normalisee, besoin));
            }
        }

        List<ProduitTrouve> resultats = new ArrayList<>(meilleurs.stream().sorted(ORDRE)
                .map(resultat -> new ProduitTrouve(resultat.produit(), ConfianceRecherche.SUR)).toList());
        boolean approcheAutorisee = normalisee.codePointCount(0, normalisee.length()) >= 3 && texte.length() <= 255;
        if (resultats.size() < LIMITE && approcheAutorisee) {
            Set<String> dejaRetenus = new HashSet<>();
            resultats.forEach(trouve -> dejaRetenus.add(cle(trouve.produit())));
            if (liste) {
                List<ProduitRechercheCompatible> candidats = new ArrayList<>(personnels);
                candidats.addAll(produits.listerBesoinsGeneriques());
                candidats.stream()
                        .filter(candidat -> !dejaRetenus.contains(cle(candidat)))
                        .map(candidat -> resultat(normalisee, candidat))
                        .filter(resultat -> resultat.score() >= SEUIL_APPROCHE)
                        .sorted(ORDRE)
                        .forEach(resultat -> ajouterApproche(resultats, dejaRetenus, resultat.produit()));
            }
            for (var produit : rechercheCatalogue.rechercherPage(
                    texte, null, LIMITE, new FiltresCatalogue(null, null)).produits()) {
                ajouterApproche(resultats, dejaRetenus, new ProduitRechercheCompatible(produit.id(), produit.libelle(),
                        produit.codeBarres(), produit.marque(), produit.format()));
            }
        }
        return List.copyOf(resultats.subList(0, Math.min(LIMITE, resultats.size())));
    }

    private static void retenir(PriorityQueue<Resultat> meilleurs, Resultat resultat) {
        meilleurs.add(resultat);
        if (meilleurs.size() > LIMITE) {
            meilleurs.poll();
        }
    }

    private static void ajouterApproche(List<ProduitTrouve> resultats, Set<String> dejaRetenus,
            ProduitRechercheCompatible produit) {
        if (resultats.size() < LIMITE && dejaRetenus.add(cle(produit))) {
            resultats.add(new ProduitTrouve(produit, ConfianceRecherche.A_CONFIRMER));
        }
    }

    private static Resultat resultat(String normalisee, ProduitRechercheCompatible produit) {
        return new Resultat(produit, SimilariteRecherche.score(normalisee, produit.libelle()),
                SimilariteRecherche.proximiteLibelle(normalisee, produit.libelle()),
                SimilariteRecherche.normaliser(produit.libelle()));
    }

    private static boolean contientTousLesMots(String libelle, List<String> motsNormalises) {
        String nom = SimilariteRecherche.normaliser(libelle);
        return !motsNormalises.isEmpty() && motsNormalises.stream().allMatch(nom::contains);
    }

    /** Un besoin personnel et un produit peuvent avoir le même identifiant numérique : le type les distingue. */
    private static String cle(ProduitRechercheCompatible produit) {
        return (produit.type() == TypeReference.BESOIN_PERSONNALISE ? "P" : "C") + produit.id();
    }

    private record Resultat(ProduitRechercheCompatible produit, double score, double proximite,
            String nomNormalise) {
    }
}
