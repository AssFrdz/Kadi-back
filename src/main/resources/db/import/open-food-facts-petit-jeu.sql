-- Images verifiees le 19 novembre 2026 ; marques et formats verifies le 20 novembre 2026.
-- Les fiches non OFF existantes ne sont jamais modifiees ni revendiquees comme importees.
WITH donnees (code_barres, libelle, marque, format, image_url, image_auteur) AS (
    VALUES
    ('3017620422003', 'Nutella', 'Ferrero, Nutella', '400 g e', 'https://images.openfoodfacts.org/images/products/301/762/042/2003/front_en.879.400.jpg', 'org-ferrero-france-commerciale'),
    ('5449000000996', 'Coca-Cola', 'COCA-COLA SERVICES SA/NV', '330 ml', 'https://images.openfoodfacts.org/images/products/544/900/000/0996/front_en.1129.400.jpg', 'zeynepozturk11'),
    ('3229820129488', 'muesli raisin,datte,figue', 'Bjorg', '375 g', 'https://images.openfoodfacts.org/images/products/322/982/012/9488/front_fr.238.400.jpg', 'sebleouf')
), completes AS (
    UPDATE produit p
    SET marque = COALESCE(p.marque, donnees.marque),
        format = COALESCE(p.format, donnees.format)
    FROM produit_open_food_facts off, donnees
    WHERE off.produit_id = p.id
      AND off.identifiant_externe = donnees.code_barres
      AND p.code_barres = donnees.code_barres
      AND (p.marque IS NULL OR p.format IS NULL)
    RETURNING p.id
), ajoutes AS (
    INSERT INTO produit (code_barres, libelle, marque, format)
    SELECT code_barres, libelle, marque, format FROM donnees
    ON CONFLICT (code_barres) DO NOTHING
    RETURNING id, code_barres
)
INSERT INTO produit_open_food_facts (produit_id, identifiant_externe, image_url, image_auteur)
SELECT ajoutes.id, ajoutes.code_barres, donnees.image_url, donnees.image_auteur
FROM ajoutes JOIN donnees USING (code_barres);
