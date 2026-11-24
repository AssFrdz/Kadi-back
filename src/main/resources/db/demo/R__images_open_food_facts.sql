-- Fiches Open Food Facts réelles avec photo, chargées par le profil demo pour que le catalogue affiche des images.
-- Les trois premières reprennent db/import/open-food-facts-petit-jeu.sql ; les suivantes remplacent les anciennes
-- fiches fictives du scénario F1 (laits, pâtes Barilla, Coca-Cola, yaourts...) par de vrais produits vendus en France.
-- Réexécutable, ne modifie pas les fiches non OFF existantes.
-- Photos CC BY-SA 3.0, auteurs indiqués (uploader de la photo de face sélectionnée sur Open Food Facts) ;
-- chargées par le navigateur depuis images.openfoodfacts.org.
-- Trois premières : images vérifiées le 19 novembre 2026, marques et formats le 20 novembre 2026.
-- Suivantes : codes, images (HTTP 200) et auteurs vérifiés sur l'API Open Food Facts le 24 novembre 2026 ;
-- libellés, marques et formats légèrement normalisés pour l'affichage (marque principale seulement).
-- Les fiches non OFF existantes ne sont jamais modifiees ni revendiquees comme importees.
WITH donnees (code_barres, libelle, marque, format, image_url, image_auteur) AS (
    VALUES
    ('3017620422003', 'Nutella', 'Ferrero, Nutella', '400 g e', 'https://images.openfoodfacts.org/images/products/301/762/042/2003/front_en.879.400.jpg', 'org-ferrero-france-commerciale'),
    ('5449000000996', 'Coca-Cola', 'COCA-COLA SERVICES SA/NV', '330 ml', 'https://images.openfoodfacts.org/images/products/544/900/000/0996/front_en.1129.400.jpg', 'zeynepozturk11'),
    ('3229820129488', 'muesli raisin,datte,figue', 'Bjorg', '375 g', 'https://images.openfoodfacts.org/images/products/322/982/012/9488/front_fr.238.400.jpg', 'sebleouf'),
    ('3428271940035', 'Lait demi-écrémé pack 6 x 1 L', 'Lactel', '6 x 1 L', 'https://images.openfoodfacts.org/images/products/342/827/194/0035/front_fr.4.400.jpg', 'kiliweb'),
    ('3428273980046', 'Lait Matin Léger 1 L', 'Lactel', '1 L', 'https://images.openfoodfacts.org/images/products/342/827/398/0046/front_en.150.400.jpg', 'foodless'),
    ('3560070586073', 'Lait entier bio 1 L', 'Carrefour Bio', '1 L', 'https://images.openfoodfacts.org/images/products/356/007/058/6073/front_fr.132.400.jpg', 'org-carrefour'),
    ('8076800195057', 'Spaghetti n°5', 'Barilla', '500 g', 'https://images.openfoodfacts.org/images/products/807/680/019/5057/front_en.3881.400.jpg', 'knuffelcookie'),
    ('8076802085738', 'Penne rigate n°73', 'Barilla', '500 g', 'https://images.openfoodfacts.org/images/products/807/680/208/5738/front_en.3506.400.jpg', 'org-barilla-france-sa'),
    ('8076800105988', 'Fusilli n°98', 'Barilla', '1 kg', 'https://images.openfoodfacts.org/images/products/807/680/010/5988/front_en.255.400.jpg', 'shaky76'),
    ('5449000000439', 'Coca-Cola bouteille 1,5 L', 'Coca-Cola', '1,5 L', 'https://images.openfoodfacts.org/images/products/544/900/000/0439/front_en.292.400.jpg', 'super-pj'),
    ('5449000214799', 'Coca-Cola sans sucres canette 33 cl', 'Coca-Cola', '330 ml', 'https://images.openfoodfacts.org/images/products/544/900/021/4799/front_en.363.400.jpg', 'g123k'),
    ('5449000131805', 'Coca-Cola Zero canette 33 cl', 'Coca-Cola', '330 ml', 'https://images.openfoodfacts.org/images/products/544/900/013/1805/front_en.810.400.jpg', 'foodless'),
    ('3033490004521', 'Yaourt nature pack 4 x 125 g', 'Danone', '4 x 125 g', 'https://images.openfoodfacts.org/images/products/303/349/000/4521/front_fr.111.400.jpg', 'org-danone-france'),
    ('3033490004514', 'Yaourt nature pack 8 x 125 g', 'Danone', '8 x 125 g', 'https://images.openfoodfacts.org/images/products/303/349/000/4514/front_fr.54.400.jpg', 'kiliweb'),
    ('3329770075313', 'Yop à la fraise pack 6 x 180 g', 'Yoplait', '6 x 180 g', 'https://images.openfoodfacts.org/images/products/332/977/007/5313/front_fr.53.400.jpg', 'freshtrack'),
    ('3083681027860', 'Haricots verts extra-fins 4/4', 'Cassegrain', '390 g', 'https://images.openfoodfacts.org/images/products/308/368/102/7860/front_fr.81.400.jpg', 'org-bonduelle-europe-long-life'),
    ('3270190153917', 'Haricots verts extra-fins', 'Carrefour', '660 g', 'https://images.openfoodfacts.org/images/products/327/019/015/3917/front_fr.142.400.jpg', 'org-carrefour'),
    ('3451790439353', 'Crème fraîche épaisse entière 30 % MG', 'Elle & Vire', '324 g', 'https://images.openfoodfacts.org/images/products/345/179/043/9353/front_en.87.400.jpg', 'kiliweb'),
    ('3254381025887', 'Eau de source 1 L', 'Cristaline', '1 L', 'https://images.openfoodfacts.org/images/products/325/438/102/5887/front_fr.115.400.jpg', 'org-gie-sources-alma'),
    ('3228021170046', 'Emmental râpé', 'Président', '200 g', 'https://images.openfoodfacts.org/images/products/322/802/117/0046/front_fr.154.400.jpg', 'org-lactalis-fromages')
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
