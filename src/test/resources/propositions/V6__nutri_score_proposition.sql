-- V6 provisoire : chargee uniquement par les tests Flyway isoles.
ALTER TABLE produit
    ADD COLUMN nutri_score VARCHAR(1),
    ADD COLUMN nutri_score_source VARCHAR(50),
    ADD COLUMN nutri_score_version VARCHAR(20),
    ADD COLUMN nutri_score_source_modifie_le TIMESTAMP WITH TIME ZONE,
    ADD COLUMN nutri_score_observe_le DATE,
    ADD CONSTRAINT ck_produit_nutri_score_proposition
        CHECK (nutri_score IS NULL OR nutri_score IN ('A', 'B', 'C', 'D', 'E')),
    ADD CONSTRAINT ck_produit_nutri_score_provenance_proposition CHECK (
        (nutri_score IS NULL AND nutri_score_source IS NULL AND nutri_score_version IS NULL
            AND nutri_score_source_modifie_le IS NULL AND nutri_score_observe_le IS NULL)
        OR
        (nutri_score IS NOT NULL AND nutri_score_source IS NOT NULL AND length(trim(nutri_score_source)) > 0
            AND nutri_score_version IS NOT NULL AND length(trim(nutri_score_version)) > 0
            AND nutri_score_source_modifie_le IS NOT NULL AND nutri_score_observe_le IS NOT NULL)
    );
