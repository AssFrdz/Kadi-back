package fr.miage.kadi.prix.application.service;

import fr.miage.kadi.prix.application.port.in.RechercherPourLaSaisie;
import fr.miage.kadi.prix.application.port.out.CatalogueDeRecherche;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Recherche provisoire : découpe le texte tapé en mots, puis les cherche tous dans les noms. */
public class RechercheService implements RechercherPourLaSaisie {

    static final int LIMITE = 20;
    private static final int MOTS_MAXIMUM = 5;

    private final CatalogueDeRecherche catalogue;

    public RechercheService(CatalogueDeRecherche catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    public List<ProduitTrouve> produits(String recherche) {
        String texte = nettoyer(recherche);
        if (texte.length() < 2) {
            return List.of();
        }
        // un code-barres se tape en chiffres seulement : correspondance exacte
        if (texte.matches("[0-9]{6,14}")) {
            return catalogue.produitDeCodeBarres(texte);
        }
        return catalogue.produitsContenant(mots(texte), LIMITE);
    }

    @Override
    public List<MagasinTrouve> magasins(String recherche) {
        String texte = nettoyer(recherche);
        if (texte.length() < 2) {
            return List.of();
        }
        return catalogue.magasinsContenant(mots(texte), LIMITE);
    }

    private static String nettoyer(String recherche) {
        return recherche == null ? "" : recherche.strip().toLowerCase(Locale.FRENCH);
    }

    private static List<String> mots(String texte) {
        return Arrays.stream(texte.split("\\s+")).limit(MOTS_MAXIMUM).toList();
    }
}
