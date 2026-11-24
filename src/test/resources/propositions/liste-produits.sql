-- Proposition de test uniquement : ce fichier n'est pas une migration Flyway.
-- Tranche limitee aux produits achetes en nombre entier, sans besoins generiques.
CREATE TABLE regle_vente_produit_proposition (
    produit_id BIGINT PRIMARY KEY REFERENCES produit (id),
    unite VARCHAR(20) NOT NULL,
    CONSTRAINT ck_unite_vente_proposition CHECK (unite = 'ARTICLE')
);

CREATE TABLE liste_personnelle_proposition (
    compte_id BIGINT PRIMARY KEY REFERENCES compte_utilisateur (id)
);

CREATE TABLE ligne_produit_proposition (
    id UUID PRIMARY KEY,
    compte_id BIGINT NOT NULL REFERENCES liste_personnelle_proposition (compte_id),
    produit_id BIGINT NOT NULL REFERENCES produit (id),
    quantite NUMERIC NOT NULL,
    CONSTRAINT ck_quantite_produit_proposition CHECK (
        quantite > 0 AND quantite <= 9223372036854775807 AND quantite = trunc(quantite)
    ),
    CONSTRAINT uq_ligne_produit_proposition UNIQUE (compte_id, produit_id)
);
