package fr.miage.kadi.prix.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Les requêtes du module prix. Les tables communes produit et magasin ne sont pas mappées ici
 * (les autres modules les utilisent aussi) : on les interroge en SQL natif.
 */
interface ReleveDePrixRepository extends JpaRepository<ReleveDePrixEntite, Long> {

    /** Requête dérivée du nom par Spring Data ; servie par l'index (produit, magasin, date) de V1. */
    List<ReleveDePrixEntite> findByProduitIdAndMagasinIdAndReleveLeGreaterThanEqual(
            long produitId, long magasinId, Instant depuis);

    /** Tous les magasins de la même enseigne que ce magasin, lui compris (index magasin(enseigne_id) de V2). */
    @Query(value = """
            SELECT r.*
            FROM releve_de_prix r
            JOIN magasin m ON m.id = r.magasin_id
            WHERE r.produit_id = :produitId
              AND m.enseigne_id = (SELECT enseigne_id FROM magasin WHERE id = :magasinId)
              AND r.releve_le >= :depuis
            """, nativeQuery = true)
    List<ReleveDePrixEntite> duProduitDansLEnseigneDuMagasin(
            @Param("produitId") long produitId, @Param("magasinId") long magasinId, @Param("depuis") Instant depuis);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM produit WHERE id = :id)", nativeQuery = true)
    boolean produitExiste(@Param("id") long id);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM magasin WHERE id = :id)", nativeQuery = true)
    boolean magasinExiste(@Param("id") long id);
}
