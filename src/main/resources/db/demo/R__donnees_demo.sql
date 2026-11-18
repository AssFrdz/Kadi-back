-- Catalogue de démonstration, chargé uniquement par le profil demo.
-- Migration répétable : elle doit rester idempotente (ON CONFLICT DO NOTHING...).

-- ---------------------------------------------------------------------------
-- F2 Prix fiable (doc/features/v0.1.0-F2-prix-fiable.md)
-- Données inventées et présentées comme telles : une enseigne, des magasins et des produits marqués « démonstration »,
-- des codes-barres en 200… (plage réservée aux codes internes, jamais attribuée à un vrai produit), source DEMO.
-- Les dates sont relatives au chargement : recharger la base avant une démonstration pour garder des relevés récents.
-- ---------------------------------------------------------------------------

INSERT INTO enseigne (nom) VALUES ('E.Leclerc (démonstration)') ON CONFLICT (nom) DO NOTHING;

INSERT INTO magasin (enseigne_id, nom)
SELECT e.id, m.nom
FROM enseigne e
CROSS JOIN (VALUES ('Leclerc Clichy (démonstration)'),
                   ('Leclerc Gennevilliers (démonstration)'),
                   ('Leclerc Levallois (démonstration)')) AS m (nom)
WHERE e.nom = 'E.Leclerc (démonstration)'
  AND NOT EXISTS (SELECT 1 FROM magasin WHERE magasin.nom = m.nom);

INSERT INTO produit (libelle, code_barres) VALUES
    ('Lait demi-écrémé 1 L (démonstration)', '2000000000015'),
    ('Pâtes spaghetti 500 g (démonstration)', '2000000000022'),
    ('Café moulu 250 g (démonstration)', '2000000000039'),
    ('Yaourt nature x4 (démonstration)', '2000000000046')
ON CONFLICT (code_barres) DO NOTHING;

-- Un relevé par ligne : code-barres, magasin, prix, âge en jours, preuve, contributeur, promotion, référence (idempotence)
INSERT INTO releve_de_prix
    (produit_id, magasin_id, prix, releve_le, source, type_preuve, contributeur, unite, en_promotion, reference_externe)
SELECT p.id, m.id, r.prix, now() - make_interval(days => r.age), 'DEMO', r.preuve, r.contributeur, 'PIECE', r.promotion, r.reference
FROM (VALUES
    -- Scénario de Karim (dossier, section 03) : l'exemple chiffré de la fiche.
    -- 1,09 € attendu, 0,10 € écarté comme aberrant, E plafonné à un seul témoin, confiance élevée (0,65).
    ('2000000000015', 'Leclerc Clichy (démonstration)', 1.09, 3,  'TICKET',    'demo:A', false, 'lait-R1'),
    ('2000000000015', 'Leclerc Clichy (démonstration)', 1.09, 7,  'ETIQUETTE', 'demo:B', false, 'lait-R2'),
    ('2000000000015', 'Leclerc Clichy (démonstration)', 1.12, 12, 'ETIQUETTE', 'demo:C', false, 'lait-R3'),
    ('2000000000015', 'Leclerc Clichy (démonstration)', 0.10, 13, 'SAISIE',    'demo:D', false, 'lait-R4'),
    ('2000000000015', 'Leclerc Clichy (démonstration)', 1.15, 1,  'ETIQUETTE', 'demo:E', false, 'lait-R5'),
    ('2000000000015', 'Leclerc Clichy (démonstration)', 1.15, 2,  'ETIQUETTE', 'demo:E', false, 'lait-R6'),
    ('2000000000015', 'Leclerc Clichy (démonstration)', 1.15, 3,  'ETIQUETTE', 'demo:E', false, 'lait-R7'),
    ('2000000000015', 'Leclerc Clichy (démonstration)', 1.15, 4,  'ETIQUETTE', 'demo:E', false, 'lait-R8'),
    ('2000000000015', 'Leclerc Clichy (démonstration)', 1.15, 5,  'ETIQUETTE', 'demo:E', false, 'lait-R9'),
    -- Repli : un seul témoin à Clichy, trois autres dans l'enseigne. Prix de l'enseigne, confiance divisée par deux.
    ('2000000000022', 'Leclerc Clichy (démonstration)',        0.95, 2,  'SAISIE',    'demo:F', false, 'pates-R1'),
    ('2000000000022', 'Leclerc Gennevilliers (démonstration)', 0.89, 4,  'TICKET',    'demo:G', false, 'pates-R2'),
    ('2000000000022', 'Leclerc Levallois (démonstration)',     0.89, 9,  'ETIQUETTE', 'demo:H', false, 'pates-R3'),
    ('2000000000022', 'Leclerc Gennevilliers (démonstration)', 0.90, 15, 'ETIQUETTE', 'demo:I', false, 'pates-R4'),
    -- Exclusions expliquées : une promotion et un relevé hors fenêtre (200 jours) ne comptent pas.
    ('2000000000039', 'Leclerc Clichy (démonstration)', 3.49, 6,   'ETIQUETTE', 'demo:J', false, 'cafe-R1'),
    ('2000000000039', 'Leclerc Clichy (démonstration)', 2.49, 4,   'ETIQUETTE', 'demo:K', true,  'cafe-R2'),
    ('2000000000039', 'Leclerc Clichy (démonstration)', 2.99, 200, 'TICKET',    'demo:L', false, 'cafe-R3')
    -- Le yaourt n'a aucun relevé : « pas encore de prix connu ici ».
) AS r (code_barres, magasin, prix, age, preuve, contributeur, promotion, reference)
JOIN produit p ON p.code_barres = r.code_barres
JOIN magasin m ON m.nom = r.magasin
ON CONFLICT (source, reference_externe) DO NOTHING;
