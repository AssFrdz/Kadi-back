package fr.miage.kadi.prix.domain.exception;

/** Levée quand un relevé ou une consultation désigne un produit que Kadi ne connaît pas. */
public class ProduitInconnuException extends RuntimeException {

    public ProduitInconnuException(long produitId) {
        super("Produit inconnu : " + produitId);
    }
}
