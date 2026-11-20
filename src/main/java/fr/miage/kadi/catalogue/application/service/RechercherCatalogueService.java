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

    private record Resultat(ProduitCatalogue produit, double score, String nomNormalise, long id) {
    }
}
