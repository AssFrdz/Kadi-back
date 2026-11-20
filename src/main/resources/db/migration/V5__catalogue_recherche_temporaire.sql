-- Migration F1 temporaire : son numero et sa fusion avec V4 sont a coordonner.
-- Ne pas fusionner dans une V4 deja appliquee sans procedure de mise a niveau.
CREATE TABLE categorie_catalogue (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    libelle VARCHAR(150) NOT NULL,
    parent_id BIGINT REFERENCES categorie_catalogue (id),
    CONSTRAINT ck_categorie_libelle_non_vide CHECK (length(trim(libelle)) > 0),
    CONSTRAINT ck_categorie_pas_son_propre_parent CHECK (id <> parent_id)
);

ALTER TABLE produit
    ADD COLUMN marque VARCHAR(255),
    ADD COLUMN format VARCHAR(255),
    ADD COLUMN categorie_id BIGINT REFERENCES categorie_catalogue (id),
    ADD COLUMN categorie_corrigee BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE produit_open_food_facts
    ADD COLUMN identifiant_externe VARCHAR(14),
    ADD COLUMN categorie_source TEXT,
    ADD COLUMN modifie_source_le TIMESTAMP WITH TIME ZONE,
    ADD COLUMN importe_le TIMESTAMP WITH TIME ZONE;

UPDATE produit_open_food_facts off
SET identifiant_externe = p.code_barres
FROM produit p
WHERE p.id = off.produit_id;

ALTER TABLE produit_open_food_facts
    ALTER COLUMN identifiant_externe SET NOT NULL,
    ADD CONSTRAINT uq_produit_off_identifiant UNIQUE (identifiant_externe),
    ADD CONSTRAINT ck_produit_off_identifiant CHECK (identifiant_externe ~ '^[0-9]{1,14}$');

CREATE INDEX idx_produit_categorie ON produit (categorie_id);
