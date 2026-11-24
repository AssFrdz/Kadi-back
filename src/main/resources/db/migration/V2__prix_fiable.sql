-- F2 Prix fiable (doc/features/v0.1.0-F2-prix-fiable.md).
-- Uniquement ce dont le calcul du prix de référence a besoin ; le prix de référence lui-même est calculé, jamais stocké.

-- Ce qu'il faut savoir de chaque relevé pour le pondérer, le dédoublonner ou l'exclure.
-- Pas de valeur par défaut pour la preuve et le contributeur : chaque relevé doit les déclarer.
ALTER TABLE releve_de_prix
    ADD COLUMN type_preuve            VARCHAR(20) NOT NULL
        CHECK (type_preuve IN ('TICKET', 'ETIQUETTE', 'SAISIE', 'IMPORT_ENSEIGNE', 'EXPORT_FIDELITE')),
    ADD COLUMN contributeur           VARCHAR(64) NOT NULL CHECK (contributeur <> ''),
    ADD COLUMN unite                  VARCHAR(10) NOT NULL DEFAULT 'PIECE'
        CHECK (unite IN ('PIECE', 'KILOGRAMME', 'LITRE')),
    -- Le prix de référence est le prix normal : un relevé en promotion en est exclu.
    ADD COLUMN en_promotion           BOOLEAN     NOT NULL DEFAULT FALSE,
    -- Identifiant d'origine (par exemple Open Prices), pour qu'un import relancé ne crée aucun doublon.
    ADD COLUMN reference_externe      VARCHAR(50),
    -- Saisie signalée inhabituelle puis confirmée par la personne : un vrai prix, pas une erreur.
    ADD COLUMN confirme_malgre_alerte BOOLEAN     NOT NULL DEFAULT FALSE;

-- Un prix à 0 € n'existe pas en rayon ; V1 l'acceptait (prix >= 0).
ALTER TABLE releve_de_prix
    ADD CONSTRAINT releve_prix_strictement_positif CHECK (prix > 0);

-- Deux sources peuvent réutiliser le même identifiant : l'unicité porte sur le couple.
ALTER TABLE releve_de_prix
    ADD CONSTRAINT releve_reference_externe_unique UNIQUE (source, reference_externe);

-- Identifiant OpenStreetMap avec son type : un point et un bâtiment peuvent porter le même numéro.
ALTER TABLE magasin
    ADD COLUMN reference_externe VARCHAR(30) UNIQUE
        CHECK (reference_externe ~ '^(node|way|relation)/[0-9]+$');

-- Repli par enseigne : retrouver vite tous les magasins d'une enseigne (PostgreSQL n'indexe pas les clés étrangères).
CREATE INDEX idx_magasin_enseigne ON magasin (enseigne_id);
