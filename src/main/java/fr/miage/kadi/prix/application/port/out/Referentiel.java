package fr.miage.kadi.prix.application.port.out;

/** Les produits et les magasins connus de Kadi. */
public interface Referentiel {

    boolean produitExiste(long produitId);

    boolean magasinExiste(long magasinId);
}
