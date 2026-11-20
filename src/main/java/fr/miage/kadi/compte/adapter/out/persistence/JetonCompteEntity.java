package fr.miage.kadi.compte.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "jeton_compte")
class JetonCompteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "compte_id", nullable = false)
    private long compteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TypeJetonCompte type;

    @Column(name = "jeton_hash", nullable = false, length = 255)
    private String jetonHash;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    @Column(name = "consomme_le")
    private Instant consommeLe;

    protected JetonCompteEntity() {
    }

    JetonCompteEntity(long compteId, String jetonHash, Instant expireLe) {
        this.compteId = compteId;
        this.type = TypeJetonCompte.VERIFICATION_EMAIL;
        this.jetonHash = jetonHash;
        this.expireLe = expireLe;
    }

    long getCompteId() {
        return compteId;
    }

    void consommer(Instant maintenant) {
        this.consommeLe = maintenant;
    }
}
