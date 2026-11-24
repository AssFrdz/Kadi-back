-- Instantane de test, lu le 21 novembre 2026 sur les fiches Open Food Facts.
-- Les dates sont celles de modification des fiches, pas du calcul nutritionnel.
-- Base : Open Food Facts, ODbL 1.0 ; contenus individuels : DbCL 1.0.
-- Ce fichier n'est pas une migration et ne doit pas etre execute sur une base reelle.
WITH donnees (code_barres, note, version, modifie_le, observe_le) AS (
    VALUES
        ('3017620422003', 'E', '2023', to_timestamp(1795028350), DATE '2026-11-21'),
        ('5449000000996', 'E', '2023', to_timestamp(1795028965), DATE '2026-11-21'),
        ('3229820129488', 'A', '2023', to_timestamp(1792315168), DATE '2026-11-21')
)
UPDATE produit p
SET nutri_score = donnees.note,
    nutri_score_source = 'open_food_facts',
    nutri_score_version = donnees.version,
    nutri_score_source_modifie_le = donnees.modifie_le,
    nutri_score_observe_le = donnees.observe_le
FROM produit_open_food_facts off, donnees
WHERE off.produit_id = p.id
  AND off.identifiant_externe = donnees.code_barres
  AND p.code_barres = donnees.code_barres
  AND (off.modifie_source_le IS NULL OR off.modifie_source_le <= donnees.modifie_le)
  AND (p.nutri_score IS NULL OR
       (p.nutri_score_source = 'open_food_facts' AND p.nutri_score_source_modifie_le < donnees.modifie_le));
