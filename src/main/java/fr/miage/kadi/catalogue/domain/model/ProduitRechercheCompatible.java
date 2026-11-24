package fr.miage.kadi.catalogue.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Référence renvoyée par la recherche commune F1/F2 : produit précis, besoin générique du catalogue
 * ou besoin personnel de l'utilisateur. La marque et le format peuvent être absents : ils ne sont jamais inventés.
 */
public record ProduitRechercheCompatible(long id, String libelle, String codeBarres, String marque, String format,
        TypeReference type, NatureBesoin nature) {

    public ProduitRechercheCompatible {
        Objects.requireNonNull(type, "Le type de référence est obligatoire.");
        if ((type == TypeReference.BESOIN_GENERIQUE) != (nature != null)) {
            throw new IllegalArgumentException("Seul un besoin générique a une nature, et il en a toujours une.");
        }
    }

    public ProduitRechercheCompatible(long id, String libelle, String codeBarres) {
        this(id, libelle, codeBarres, null, null);
    }

    public ProduitRechercheCompatible(long id, String libelle, String codeBarres, String marque, String format) {
        this(id, libelle, codeBarres, marque, format, TypeReference.PRODUIT, null);
    }

    /**
     * Unités proposées à l'ajout : un produit précis se vend à l'article (sachet, pack, bouteille entière),
     * un besoin générique selon sa nature, un besoin personnel dans toutes les unités connues.
     */
    public List<UniteQuantite> unitesAutorisees() {
        return switch (type) {
            case PRODUIT -> List.of(UniteQuantite.ARTICLE);
            case BESOIN_GENERIQUE -> nature.unitesAutorisees();
            case BESOIN_PERSONNALISE -> List.of(UniteQuantite.values());
        };
    }
}
