-- V5 (F1, Assia) : modération des besoins personnels par l'administrateur (REQ-039 à REQ-043, REQ-047).
-- Une proposition validée est reliée au besoin générique commun qu'elle a créé ou auquel elle a été rattachée ;
-- la ligne de l'auteur garde sa saisie d'origine (ligne_liste.libelle_origine).
ALTER TABLE besoin_personnalise
    ADD COLUMN produit_id BIGINT REFERENCES produit (id),
    ADD COLUMN modere_le  TIMESTAMP WITH TIME ZONE,
    ADD CONSTRAINT ck_besoin_valide_relie CHECK ((statut = 'VALIDE') = (produit_id IS NOT NULL)),
    ADD CONSTRAINT ck_besoin_modere_date CHECK ((statut = 'EN_ATTENTE') = (modere_le IS NULL));

CREATE INDEX idx_besoin_personnalise_statut ON besoin_personnalise (statut);
