package fr.miage.kadi.catalogue.domain.model;

public record ProduitCatalogue(long id, String libelle, String codeBarres,
        String sourceUrl, String imageUrl, String imageAuteur, String marque, String format) {
}
