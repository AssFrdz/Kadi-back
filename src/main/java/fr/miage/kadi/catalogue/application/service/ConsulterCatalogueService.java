package fr.miage.kadi.catalogue.application.service;

import fr.miage.kadi.catalogue.application.port.out.ProduitsCatalogueRepository;
import fr.miage.kadi.catalogue.domain.model.ProduitCatalogue;
import fr.miage.kadi.catalogue.domain.model.FiltresCatalogue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
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

    public PageCatalogueParNom consulterParNom(String curseur, FiltresCatalogue filtres) {
        Position position = curseur == null ? null : decoder(curseur, filtres);
        List<ProduitCatalogue> resultats = produits.trouverParNom(
                position == null ? null : position.nom(), position == null ? 0 : position.id(),
                TAILLE_PAGE + 1, filtres);
        boolean suite = resultats.size() > TAILLE_PAGE;
        List<ProduitCatalogue> page = List.copyOf(resultats.subList(0, Math.min(resultats.size(), TAILLE_PAGE)));
        ProduitCatalogue dernier = suite ? page.get(page.size() - 1) : null;
        return new PageCatalogueParNom(page, dernier == null ? null : encoder(dernier, filtres));
    }

    private static String encoder(ProduitCatalogue produit, FiltresCatalogue filtres) {
        try {
            ByteArrayOutputStream octets = new ByteArrayOutputStream();
            try (DataOutputStream sortie = new DataOutputStream(octets)) {
                sortie.writeUTF(filtres.marque());
                sortie.writeUTF(filtres.format());
                sortie.writeUTF(produit.libelle());
                sortie.writeLong(produit.id());
                if (!filtres.nutriScores().isEmpty()) {
                    sortie.writeUTF(filtres.cleNutriScores());
                }
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(octets.toByteArray());
        } catch (IOException erreur) {
            throw new IllegalStateException("Impossible de créer le curseur du catalogue.", erreur);
        }
    }

    private static Position decoder(String curseur, FiltresCatalogue filtres) {
        if (curseur.length() > 4096) {
            throw new IllegalArgumentException("Curseur du catalogue invalide.");
        }
        try (DataInputStream entree = new DataInputStream(
                new ByteArrayInputStream(Base64.getUrlDecoder().decode(curseur)))) {
            String marque = entree.readUTF();
            String format = entree.readUTF();
            String nom = entree.readUTF();
            long id = entree.readLong();
            String nutriScores = entree.available() == 0 ? "" : entree.readUTF();
            if (!marque.equals(filtres.marque()) || !format.equals(filtres.format()) || id < 1
                    || !nutriScores.equals(filtres.cleNutriScores())
                    || nom.length() > 255 || entree.available() != 0) {
                throw new IllegalArgumentException("Curseur du catalogue invalide.");
            }
            return new Position(nom, id);
        } catch (IOException | IllegalArgumentException erreur) {
            throw new IllegalArgumentException("Curseur du catalogue invalide.", erreur);
        }
    }

    public record PageCatalogue(List<ProduitCatalogue> produits, Long prochainId) {
    }

    public record PageCatalogueParNom(List<ProduitCatalogue> produits, String prochainCurseur) {
    }

    private record Position(String nom, long id) {
    }
}
