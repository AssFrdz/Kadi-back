-- Socle de persistance des comptes ; l'inscription et la connexion restent à implémenter.
CREATE TABLE compte_utilisateur (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email                VARCHAR(320) NOT NULL,
    mot_de_passe_hash    VARCHAR(255) NOT NULL,
    email_verifie        BOOLEAN NOT NULL DEFAULT FALSE,
    role                 VARCHAR(20) NOT NULL DEFAULT 'UTILISATEUR',
    cree_le              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_compte_email_non_vide CHECK (length(trim(email)) > 0),
    CONSTRAINT ck_compte_mot_de_passe_hash_non_vide CHECK (length(trim(mot_de_passe_hash)) > 0),
    CONSTRAINT ck_compte_role CHECK (role IN ('UTILISATEUR', 'ADMINISTRATEUR'))
);

CREATE UNIQUE INDEX uq_compte_email_insensible_casse ON compte_utilisateur (lower(email));

CREATE TABLE jeton_compte (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    compte_id         BIGINT NOT NULL REFERENCES compte_utilisateur (id) ON DELETE CASCADE,
    type              VARCHAR(30) NOT NULL,
    jeton_hash        VARCHAR(255) NOT NULL UNIQUE,
    expire_le         TIMESTAMP WITH TIME ZONE NOT NULL,
    consomme_le       TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_jeton_type CHECK (type IN ('VERIFICATION_EMAIL', 'REINITIALISATION_MDP')),
    CONSTRAINT ck_jeton_hash_non_vide CHECK (length(trim(jeton_hash)) > 0)
);

CREATE INDEX idx_jeton_compte_id ON jeton_compte (compte_id);

CREATE TABLE produit_open_food_facts (
    produit_id BIGINT PRIMARY KEY REFERENCES produit (id) ON DELETE CASCADE,
    image_url TEXT,
    image_auteur VARCHAR(255),
    CHECK ((image_url IS NULL AND image_auteur IS NULL)
        OR (image_url IS NOT NULL AND image_auteur IS NOT NULL))
);
