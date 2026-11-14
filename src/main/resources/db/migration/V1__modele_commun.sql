-- Modèle commun de Kadi (dossier, section 05), fixé pour que les features avancent en parallèle.
-- Chaque feature ajoute SA migration, sans jamais modifier une migration déjà poussée.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE enseigne (
    id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE magasin (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    enseigne_id BIGINT NOT NULL REFERENCES enseigne (id),
    nom         VARCHAR(150) NOT NULL
);

-- Pas de prix ici : un prix est un relevé daté, jamais un attribut du produit.
-- Le code-barres est facultatif : un produit peut ne pas en avoir.
CREATE TABLE produit (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    libelle     VARCHAR(255) NOT NULL,
    code_barres VARCHAR(14) UNIQUE
);

CREATE TABLE releve_de_prix (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    produit_id BIGINT NOT NULL REFERENCES produit (id),
    magasin_id BIGINT NOT NULL REFERENCES magasin (id),
    prix       NUMERIC(10, 2) NOT NULL CHECK (prix >= 0),
    releve_le  TIMESTAMP WITH TIME ZONE NOT NULL,
    source     VARCHAR(50) NOT NULL
);

CREATE INDEX idx_releve_produit_magasin_date ON releve_de_prix (produit_id, magasin_id, releve_le);
