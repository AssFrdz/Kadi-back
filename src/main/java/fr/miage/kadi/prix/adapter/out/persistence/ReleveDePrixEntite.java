package fr.miage.kadi.prix.adapter.out.persistence;

import fr.miage.kadi.prix.domain.model.TypeDePreuve;
import fr.miage.kadi.prix.domain.model.UniteDePrix;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Une ligne de la table releve_de_prix (migrations V1 et V2), telle que JPA la lit et l'écrit.
 * Le domaine ne la voit jamais : l'adaptateur la traduit en ReleveDePrix.
 */
@Entity
@Table(name = "releve_de_prix")
class ReleveDePrixEntite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "produit_id", nullable = false)
    private long produitId;

    @Column(name = "magasin_id", nullable = false)
    private long magasinId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    @Column(name = "releve_le", nullable = false)
    private Instant releveLe;

    @Column(nullable = false, length = 50)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_preuve", nullable = false, length = 20)
    private TypeDePreuve typePreuve;

    @Column(nullable = false, length = 64)
    private String contributeur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UniteDePrix unite;

    @Column(name = "en_promotion", nullable = false)
    private boolean enPromotion;

    @Column(name = "reference_externe", length = 50)
    private String referenceExterne;

    @Column(name = "confirme_malgre_alerte", nullable = false)
    private boolean confirmeMalgreAlerte;

    /** Exigé par JPA. */
    protected ReleveDePrixEntite() {
    }

    ReleveDePrixEntite(
            long produitId,
            long magasinId,
            BigDecimal prix,
            Instant releveLe,
            String source,
            TypeDePreuve typePreuve,
            String contributeur,
            UniteDePrix unite,
            boolean enPromotion,
            boolean confirmeMalgreAlerte) {
        this.produitId = produitId;
        this.magasinId = magasinId;
        this.prix = prix;
        this.releveLe = releveLe;
        this.source = source;
        this.typePreuve = typePreuve;
        this.contributeur = contributeur;
        this.unite = unite;
        this.enPromotion = enPromotion;
        this.confirmeMalgreAlerte = confirmeMalgreAlerte;
    }

    Long getId() {
        return id;
    }

    long getProduitId() {
        return produitId;
    }

    long getMagasinId() {
        return magasinId;
    }

    BigDecimal getPrix() {
        return prix;
    }

    Instant getReleveLe() {
        return releveLe;
    }

    TypeDePreuve getTypePreuve() {
        return typePreuve;
    }

    String getContributeur() {
        return contributeur;
    }

    UniteDePrix getUnite() {
        return unite;
    }

    boolean isEnPromotion() {
        return enPromotion;
    }
}
