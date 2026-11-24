-- Catalogue de démonstration, chargé uniquement par le profil demo.
-- Migration répétable : elle doit rester idempotente (ON CONFLICT DO NOTHING...).
-- Les codes-barres commençant par 200 sont réservés à un usage interne : ils ne désignent aucun vrai produit.
-- Chaque feature ajoute sa section à la suite, avec sa propre plage de codes.

-- ---------------------------------------------------------------------------------------------
-- F1 · Construire ma liste (Assia) — codes 2001000000xxx
-- Scénario du dossier : « lait », « pâtes barilla », « coca 6 », plus « yaort » et des besoins génériques.
-- Les produits réels avec photo (laits, pâtes Barilla, Coca-Cola, yaourts Danone...) viennent d'Open Food Facts :
-- voir R__images_open_food_facts.sql. Ne restent ici que les fiches fictives sans équivalent réel retenu
-- (packs Coca-Cola 6 x 33 cl du scénario « coca 6 », farine, articles non alimentaires...) : leurs marques et
-- formats servent à jouer le scénario, pas à décrire l'offre réelle.
-- ---------------------------------------------------------------------------------------------

-- Anciennes fiches fictives remplacées par des fiches Open Food Facts réelles : retirées des bases de démo déjà
-- chargées, sauf si une liste, un relevé de prix ou une proposition modérée y renvoie encore.
DELETE FROM produit p
WHERE p.code_barres IN ('2001000000011', '2001000000028', '2001000000035', '2001000000042',
                        '2001000000059', '2001000000066', '2001000000080', '2001000000103')
  AND NOT EXISTS (SELECT 1 FROM ligne_liste l WHERE l.produit_id = p.id)
  AND NOT EXISTS (SELECT 1 FROM releve_de_prix r WHERE r.produit_id = p.id)
  AND NOT EXISTS (SELECT 1 FROM besoin_personnalise b WHERE b.produit_id = p.id);

INSERT INTO produit (libelle, code_barres, marque, format)
VALUES ('Coca-Cola pack 6 x 33 cl', '2001000000073', 'Coca-Cola', '6 x 33 cl'),
       ('Coca-Cola zéro pack 6 x 33 cl', '2001000000097', 'Coca-Cola', '6 x 33 cl'),
       ('Yaourt à la fraise pack 4 x 125 g', '2001000000110', 'Danone', '4 x 125 g'),
       ('Yaourt nature au lait entier 8 x 125 g', '2001000000127', 'Marque Repère', '8 x 125 g'),
       ('Farine de blé T45 1 kg', '2001000000134', 'Francine', '1 kg'),
       ('Haricots verts extra-fins surgelés 1 kg', '2001000000141', 'Picard', '1 kg'),
       ('Crème fraîche épaisse 20 cl', '2001000000158', 'Président', '20 cl'),
       ('Lessive liquide 2 L', '2001000000165', 'Le Chat', '2 L'),
       ('Gobelets carton 25 cl x 50', '2001000000172', 'Auchan', '50 gobelets')
ON CONFLICT (code_barres) DO NOTHING;

-- Besoins génériques : sans marque ni code-barres ; leur nature fixe les unités proposées.
INSERT INTO produit (libelle, type_reference, nature_besoin)
SELECT besoin.libelle, 'GENERIQUE', besoin.nature
FROM (VALUES ('Lait demi-écrémé', 'LIQUIDE'),
             ('Yaourt nature', 'SOLIDE'),
             ('Pâtes', 'SOLIDE'),
             ('Farine', 'SOLIDE'),
             ('Pommes', 'SOLIDE'),
             ('Haricots verts surgelés', 'SOLIDE'),
             ('Crème fraîche', 'LIQUIDE'),
             ('Eau minérale', 'LIQUIDE'),
             ('Lessive', 'UNITAIRE'),
             ('Gobelets', 'UNITAIRE'),
             ('Papier toilette', 'UNITAIRE')) AS besoin(libelle, nature)
WHERE NOT EXISTS (
    SELECT 1 FROM produit p WHERE p.type_reference = 'GENERIQUE' AND lower(p.libelle) = lower(besoin.libelle)
);
