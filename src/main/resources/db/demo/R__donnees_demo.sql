-- Catalogue de démonstration, chargé uniquement par le profil demo.
-- Migration répétable : elle doit rester idempotente (ON CONFLICT DO NOTHING...).
-- Les codes-barres commençant par 200 sont réservés à un usage interne : ils ne désignent aucun vrai produit.
-- Chaque feature ajoute sa section à la suite, avec sa propre plage de codes.

-- ---------------------------------------------------------------------------------------------
-- F1 · Construire ma liste (Assia) — codes 2001000000xxx
-- Scénario du dossier : « lait », « pâtes barilla », « coca 6 », plus « yaort » et des besoins génériques.
-- Fiches fictives de démonstration : marques et formats servent à jouer le scénario, pas à décrire l'offre réelle.
-- ---------------------------------------------------------------------------------------------
INSERT INTO produit (libelle, code_barres, marque, format)
VALUES ('Lait demi-écrémé UHT 1 L', '2001000000011', 'Lactel', '1 L'),
       ('Lait demi-écrémé UHT pack 6 x 1 L', '2001000000028', 'Lactel', '6 x 1 L'),
       ('Lait entier bio 1 L', '2001000000035', 'Carrefour Bio', '1 L'),
       ('Spaghetti n°5', '2001000000042', 'Barilla', '500 g'),
       ('Penne rigate n°73', '2001000000059', 'Barilla', '500 g'),
       ('Fusilli n°98', '2001000000066', 'Barilla', '1 kg'),
       ('Coca-Cola pack 6 x 33 cl', '2001000000073', 'Coca-Cola', '6 x 33 cl'),
       ('Coca-Cola bouteille 1,5 L', '2001000000080', 'Coca-Cola', '1,5 L'),
       ('Coca-Cola zéro pack 6 x 33 cl', '2001000000097', 'Coca-Cola', '6 x 33 cl'),
       ('Yaourt nature pack 4 x 125 g', '2001000000103', 'Danone', '4 x 125 g'),
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
