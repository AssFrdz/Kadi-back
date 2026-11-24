-- V4 (F1, Assia) : comptes (utilisateurs simulés), catalogue de recherche et liste de courses personnelle.
-- Regroupe l'ancienne V4 et les V5 temporaires (dont les utilisateurs simulés et la modération des besoins),
-- jamais fusionnées dans develop.
-- Une base locale ayant appliqué ces anciennes versions doit être sauvegardée puis recréée (voir le README).

-- ---------------------------------------------------------------------------------------------
-- Comptes
-- Consigne du projet : pas de vraie connexion. Le front choisit l'utilisateur courant dans une liste
-- déroulante et transmet son id dans l'en-tête X-Utilisateur-Id. Seuls les comptes ayant un nom affiché
-- sont proposés. Les colonnes mot de passe et email vérifié ne servent plus : le hash factice 'SIMULE'
-- respecte seulement la contrainte NOT NULL.
-- ---------------------------------------------------------------------------------------------
CREATE TABLE compte_utilisateur (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email                VARCHAR(320) NOT NULL,
    mot_de_passe_hash    VARCHAR(255) NOT NULL,
    email_verifie        BOOLEAN NOT NULL DEFAULT FALSE,
    role                 VARCHAR(20) NOT NULL DEFAULT 'UTILISATEUR',
    nom_affiche          VARCHAR(100),
    cree_le              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_compte_email_non_vide CHECK (length(trim(email)) > 0),
    CONSTRAINT ck_compte_mot_de_passe_hash_non_vide CHECK (length(trim(mot_de_passe_hash)) > 0),
    CONSTRAINT ck_compte_role CHECK (role IN ('UTILISATEUR', 'ADMINISTRATEUR'))
);

CREATE UNIQUE INDEX uq_compte_email_insensible_casse ON compte_utilisateur (lower(email));

INSERT INTO compte_utilisateur (email, mot_de_passe_hash, email_verifie, role, nom_affiche)
VALUES ('karim@kadi.local', 'SIMULE', TRUE, 'UTILISATEUR', 'Karim (consommateur)'),
       ('sophie@kadi.local', 'SIMULE', TRUE, 'UTILISATEUR', 'Sophie (consommatrice)'),
       ('admin@kadi.local', 'SIMULE', TRUE, 'ADMINISTRATEUR', 'Admin Kadi');

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

-- ---------------------------------------------------------------------------------------------
-- Catalogue : marque, format, catégorie, type de référence et provenance Open Food Facts
-- ---------------------------------------------------------------------------------------------
CREATE TABLE categorie_catalogue (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    libelle VARCHAR(150) NOT NULL,
    parent_id BIGINT REFERENCES categorie_catalogue (id),
    CONSTRAINT ck_categorie_libelle_non_vide CHECK (length(trim(libelle)) > 0),
    CONSTRAINT ck_categorie_pas_son_propre_parent CHECK (id <> parent_id)
);

-- Un besoin générique (« yaourt nature ») est une ligne de produit de type GENERIQUE : il n'a pas
-- de code-barres et sa nature détermine les unités proposées. La recherche commune F1/F2 ne le
-- renvoie qu'avec le paramètre portee=liste.
ALTER TABLE produit
    ADD COLUMN marque VARCHAR(255),
    ADD COLUMN format VARCHAR(255),
    ADD COLUMN categorie_id BIGINT REFERENCES categorie_catalogue (id),
    ADD COLUMN categorie_corrigee BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN type_reference VARCHAR(20) NOT NULL DEFAULT 'PRECIS',
    ADD COLUMN nature_besoin VARCHAR(20),
    ADD CONSTRAINT ck_produit_type_reference CHECK (type_reference IN ('PRECIS', 'GENERIQUE')),
    ADD CONSTRAINT ck_produit_nature_besoin CHECK (
        (type_reference = 'PRECIS' AND nature_besoin IS NULL)
        OR (type_reference = 'GENERIQUE' AND nature_besoin IS NOT NULL
            AND nature_besoin IN ('SOLIDE', 'LIQUIDE', 'UNITAIRE'))),
    ADD CONSTRAINT ck_produit_generique_sans_code_barres CHECK (type_reference = 'PRECIS' OR code_barres IS NULL);

CREATE INDEX idx_produit_categorie ON produit (categorie_id);
CREATE UNIQUE INDEX uq_besoin_generique_libelle ON produit (lower(libelle)) WHERE type_reference = 'GENERIQUE';

CREATE TABLE produit_open_food_facts (
    produit_id          BIGINT PRIMARY KEY REFERENCES produit (id) ON DELETE CASCADE,
    image_url           TEXT,
    image_auteur        VARCHAR(255),
    identifiant_externe VARCHAR(14) NOT NULL,
    categorie_source    TEXT,
    modifie_source_le   TIMESTAMP WITH TIME ZONE,
    importe_le          TIMESTAMP WITH TIME ZONE,
    CHECK ((image_url IS NULL AND image_auteur IS NULL)
        OR (image_url IS NOT NULL AND image_auteur IS NOT NULL)),
    CONSTRAINT uq_produit_off_identifiant UNIQUE (identifiant_externe),
    CONSTRAINT ck_produit_off_identifiant CHECK (identifiant_externe ~ '^[0-9]{1,14}$')
);

-- ---------------------------------------------------------------------------------------------
-- Liste de courses : une seule liste personnelle par compte
-- ---------------------------------------------------------------------------------------------
-- Besoin créé volontairement par son auteur. Il reste privé tant qu'un administrateur ne l'a pas validé.
-- Modération (REQ-039 à REQ-043, REQ-047) : une proposition validée est reliée au besoin générique commun
-- qu'elle a créé ou auquel elle a été rattachée (produit_id) ; la ligne de l'auteur garde sa saisie d'origine
-- (ligne_liste.libelle_origine). modere_le date la décision de l'administrateur.
CREATE TABLE besoin_personnalise (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    compte_id  BIGINT NOT NULL REFERENCES compte_utilisateur (id) ON DELETE CASCADE,
    libelle    VARCHAR(255) NOT NULL,
    statut     VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    cree_le    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    produit_id BIGINT REFERENCES produit (id),
    modere_le  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_besoin_personnalise_libelle_non_vide CHECK (length(trim(libelle)) > 0),
    CONSTRAINT ck_besoin_personnalise_statut CHECK (statut IN ('EN_ATTENTE', 'VALIDE', 'REFUSE')),
    CONSTRAINT ck_besoin_valide_relie CHECK ((statut = 'VALIDE') = (produit_id IS NOT NULL)),
    CONSTRAINT ck_besoin_modere_date CHECK ((statut = 'EN_ATTENTE') = (modere_le IS NULL))
);

CREATE UNIQUE INDEX uq_besoin_personnalise_compte_libelle ON besoin_personnalise (compte_id, lower(libelle));
CREATE INDEX idx_besoin_personnalise_statut ON besoin_personnalise (statut);

CREATE TABLE liste_courses (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    compte_id  BIGINT NOT NULL UNIQUE REFERENCES compte_utilisateur (id) ON DELETE CASCADE,
    cree_le    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Le produit référencé ne peut pas être supprimé tant qu'une ligne y renvoie (RESTRICT) :
-- la ligne reste dans la liste de son utilisateur, avec le libellé connu au moment de l'ajout.
CREATE TABLE ligne_liste (
    id                     UUID PRIMARY KEY,
    liste_id               BIGINT NOT NULL REFERENCES liste_courses (id) ON DELETE CASCADE,
    type_reference         VARCHAR(20) NOT NULL,
    produit_id             BIGINT REFERENCES produit (id),
    besoin_personnalise_id BIGINT REFERENCES besoin_personnalise (id),
    libelle_origine        VARCHAR(255) NOT NULL,
    quantite               NUMERIC(12, 3) NOT NULL,
    unite                  VARCHAR(20) NOT NULL,
    format_souhaite        VARCHAR(255),
    ajoutee_le             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_ligne_type_reference CHECK (type_reference IN ('PRODUIT', 'BESOIN_GENERIQUE', 'BESOIN_PERSONNALISE')),
    CONSTRAINT ck_ligne_reference CHECK (
        (type_reference = 'BESOIN_PERSONNALISE' AND besoin_personnalise_id IS NOT NULL AND produit_id IS NULL)
        OR (type_reference <> 'BESOIN_PERSONNALISE' AND produit_id IS NOT NULL AND besoin_personnalise_id IS NULL)),
    CONSTRAINT ck_ligne_quantite_positive CHECK (quantite > 0),
    CONSTRAINT ck_ligne_unite CHECK (unite IN ('ARTICLE', 'GRAMME', 'KILOGRAMME', 'LITRE', 'CENTILITRE')),
    CONSTRAINT ck_ligne_article_entier CHECK (unite <> 'ARTICLE' OR quantite = trunc(quantite)),
    CONSTRAINT ck_ligne_produit_precis_sans_format CHECK (type_reference <> 'PRODUIT' OR format_souhaite IS NULL),
    CONSTRAINT ck_ligne_libelle_non_vide CHECK (length(trim(libelle_origine)) > 0)
);

CREATE INDEX idx_ligne_liste_ordre ON ligne_liste (liste_id, ajoutee_le, id);
CREATE INDEX idx_ligne_liste_produit ON ligne_liste (produit_id);
CREATE INDEX idx_ligne_liste_besoin ON ligne_liste (besoin_personnalise_id);
 