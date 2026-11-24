package fr.miage.kadi.catalogue.application.service;

import fr.miage.kadi.catalogue.application.port.out.ProduitsCatalogueRepository;
import fr.miage.kadi.catalogue.domain.algo.SimilariteRecherche;
import fr.miage.kadi.catalogue.domain.model.ProduitCatalogue;
import fr.miage.kadi.catalogue.domain.model.FiltresCatalogue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RechercherCatalogueService {
    private static final Comparator<Resultat> ORDRE = Comparator.comparingDouble(Resultat::score).reversed()
            .thenComparing(Resultat::nomNormalise)
            .thenComparingLong(Resultat::id);
    private final ProduitsCatalogueRepository produits;

    public RechercherCatalogueService(ProduitsCatalogueRepository produits) {
        this.produits = produits;
    }

    public List<ProduitCatalogue> rechercher(String saisie) {
        return rechercherPage(saisie, null, 7, new FiltresCatalogue(null, null)).produits();
    }

    public PageRecherche rechercherPage(String saisie, String curseur, int taille, FiltresCatalogue filtres) {
        if (saisie.length() > 255 || taille < 1 || taille > 20) {
            throw new IllegalArgumentException("Recherche ou taille de page invalide.");
        }
        String recherche = SimilariteRecherche.normaliser(saisie);
        Resultat apres = curseur == null ? null : decoder(curseur, recherche, filtres);
        if (recherche.codePointCount(0, recherche.length()) < 3) {
            return new PageRecherche(List.of(), null);
        }
        PriorityQueue<Resultat> meilleurs = new PriorityQueue<>(taille + 2, ORDRE.reversed());
        produits.parcourirCandidats(recherche, filtres, produit -> {
            double score = SimilariteRecherche.scoreProduit(recherche, produit);
            Resultat resultat = new Resultat(produit, score,
                    SimilariteRecherche.normaliser(produit.libelle()), produit.id());
            if (score < 0.5 || (apres != null && ORDRE.compare(resultat, apres) <= 0)) {
                return;
            }
            meilleurs.add(resultat);
            if (meilleurs.size() > taille + 1) {
                meilleurs.poll();
            }
        });
        List<Resultat> tries = meilleurs.stream().sorted(ORDRE).toList();
        boolean suite = tries.size() > taille;
        List<Resultat> page = tries.subList(0, Math.min(taille, tries.size()));
        String prochain = suite ? encoder(recherche, filtres, page.get(page.size() - 1)) : null;
        return new PageRecherche(page.stream().map(Resultat::produit).toList(), prochain);
    }

    public List<MarqueCatalogue> compterMarques(String saisie, String format) {
        if (saisie.length() > 255) {
            throw new IllegalArgumentException("La recherche ne peut pas dépasser 255 caractères.");
        }
        FiltresCatalogue filtres = new FiltresCatalogue(null, format);
        String recherche = SimilariteRecherche.normaliser(saisie);
        if (!saisie.isBlank() && recherche.codePointCount(0, recherche.length()) < 3) {
            return List.of();
        }
        Map<String, MarqueCatalogue> comptes = new HashMap<>();
        java.util.function.Consumer<ProduitCatalogue> compter = produit -> {
            if (!recherche.isEmpty() && SimilariteRecherche.scoreProduit(recherche, produit) < 0.5) {
                return;
            }
            if (produit.marque() == null) {
                return;
            }
            Set<String> dejaComptes = new HashSet<>();
            for (String valeur : produit.marque().split(",")) {
                String nom = valeur.trim();
                String cle = nom.toLowerCase(Locale.ROOT);
                if (!nom.isEmpty() && dejaComptes.add(cle)) {
                    comptes.compute(cle, (ignored, ancien) -> ancien == null
                            ? new MarqueCatalogue(nom, 1)
                            : new MarqueCatalogue(ancien.nom().compareTo(nom) <= 0 ? ancien.nom() : nom,
                                    ancien.nombre() + 1));
                }
            }
        };
        if (recherche.isEmpty()) {
            produits.parcourirCatalogue(filtres, compter);
        } else {
            produits.parcourirCandidats(recherche, filtres, compter);
        }
        return comptes.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue).toList();
    }

    private static String encoder(String recherche, FiltresCatalogue filtres, Resultat resultat) {
        try {
            ByteArrayOutputStream octets = new ByteArrayOutputStream();
            try (DataOutputStream sortie = new DataOutputStream(octets)) {
                sortie.writeUTF(recherche);
                sortie.writeUTF(filtres.marque());
                sortie.writeUTF(filtres.format());
                sortie.writeDouble(resultat.score());
                sortie.writeUTF(resultat.nomNormalise());
                sortie.writeLong(resultat.id());
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(octets.toByteArray());
        } catch (IOException erreur) {
            throw new IllegalStateException("Impossible de créer le curseur de recherche.", erreur);
        }
    }

    private static Resultat decoder(String curseur, String recherche, FiltresCatalogue filtres) {
        if (curseur.length() > 4096) {
            throw new IllegalArgumentException("Curseur de recherche invalide.");
        }
        try (DataInputStream entree = new DataInputStream(
                new ByteArrayInputStream(Base64.getUrlDecoder().decode(curseur)))) {
            String requete = entree.readUTF();
            String marque = entree.readUTF();
            String format = entree.readUTF();
            double score = entree.readDouble();
            String nom = entree.readUTF();
            long id = entree.readLong();
            if (!requete.equals(recherche) || !marque.equals(filtres.marque()) || !format.equals(filtres.format())
                    || !Double.isFinite(score) || score < 0.5 || score > 1
                    || id < 1 || entree.available() != 0) {
                throw new IllegalArgumentException("Curseur de recherche invalide.");
            }
            return new Resultat(null, score, nom, id);
        } catch (IOException | IllegalArgumentException erreur) {
            throw new IllegalArgumentException("Curseur de recherche invalide.", erreur);
        }
    }

    public record PageRecherche(List<ProduitCatalogue> produits, String prochainCurseur) {
    }

    public record MarqueCatalogue(String nom, long nombre) {
    }

    private record Resultat(ProduitCatalogue produit, double score, String nomNormalise, long id) {
    }
}
