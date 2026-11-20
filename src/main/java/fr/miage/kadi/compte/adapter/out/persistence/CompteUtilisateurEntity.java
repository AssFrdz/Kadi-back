package fr.miage.kadi.compte.adapter.out.persistence;

import fr.miage.kadi.compte.domain.model.RoleCompte;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "compte_utilisateur")
class CompteUtilisateurEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "mot_de_passe_hash", nullable = false, length = 255)
    private String motDePasseHash;

    @Column(name = "email_verifie", nullable = false)
    private boolean emailVerifie;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private RoleCompte role;

    protected CompteUtilisateurEntity() {
    }

    CompteUtilisateurEntity(String email, String motDePasseHash) {
        this.email = email;
        this.motDePasseHash = motDePasseHash;
        this.emailVerifie = false;
        this.role = RoleCompte.UTILISATEUR;
    }

    Long getId() {
        return id;
    }

    String getEmail() {
        return email;
    }

    String getMotDePasseHash() {
        return motDePasseHash;
    }

    boolean isEmailVerifie() {
        return emailVerifie;
    }

    void verifierEmail() {
        this.emailVerifie = true;
    }

    RoleCompte getRole() {
        return role;
    }
}
