# Kadi : back

*Les bons comptes font les bonnes courses.*

API REST de Kadi. Le projet est présenté dans le [dossier](doc/dossier.md), et le circuit Git dans le [guide de contribution](CONTRIBUTING.md). Le front est dans [Kadi-front](https://github.com/AssFrdz/Kadi-front).

| Couche | Technologie |
|---|---|
| API REST | Spring Boot 4, documentée par OpenAPI et Swagger UI |
| Back | Java 21, Spring Data JPA, Bean Validation, architecture hexagonale |
| BD | PostgreSQL 17 avec `pg_trgm`, schéma versionné par Flyway |
| Qualité | JUnit 5, AssertJ, ArchUnit, jqwik, Testcontainers, JaCoCo |

## Lancer

Prérequis : JDK 21 et Docker.

```bash
docker compose up -d     # PostgreSQL sur localhost:5432
./gradlew bootRun
```

Pour charger le catalogue de démonstration : `./gradlew bootRun --args='--spring.profiles.active=demo'`.

**Pas de connexion : utilisateur simulé.** Conformément à la consigne du projet, Kadi n'a ni inscription ni mot de passe. La migration `V4__comptes_utilisateur.sql` crée trois profils : « Karim (consommateur) », « Sophie (consommatrice) » et « Admin Kadi » (rôle `ADMINISTRATEUR`). `GET /api/v1/utilisateurs` les liste ; le front les propose dans la liste déroulante « Utilisateur actuel » et envoie l'id choisi dans l'en-tête `X-Utilisateur-Id`. Les routes de la liste (`/api/v1/liste`, `GET /api/v1/produits?portee=liste`) exigent cet en-tête : absent, mal formé ou id inconnu, elles répondent `400` au format problem+json. Pour essayer depuis Swagger UI ou `curl` : `curl -H "X-Utilisateur-Id: 1" http://localhost:8080/api/v1/liste` (prendre un id renvoyé par `/api/v1/utilisateurs`). Cet en-tête n'est pas une preuve d'identité : il convient à une démonstration, pas à une mise en production.


- API : <http://localhost:8080/api/v1/info>
- Swagger UI : <http://localhost:8080/swagger-ui.html>
- Santé : <http://localhost:8080/actuator/health>

## Tester

```bash
./gradlew build          # compile, lance tous les tests, produit le rapport de couverture
open build/reports/jacoco/test/html/index.html
```

Docker doit tourner : les tests d'intégration démarrent eux-mêmes un PostgreSQL 17 avec Testcontainers.

Conventions : `*Test` pour les tests unitaires et de propriétés (sans Spring), `*IT` pour les tests d'intégration (`@SpringBootTest`).

### Tous les niveaux en une commande

```powershell
.\scripts\tester-tout.ps1                              # niveaux 1 à 6
.\scripts\tester-tout.ps1 -SansE2E -SansBuildComplet   # boucle rapide : niveaux 1, 2, 4 et 5
```

| Niveau | Contenu | Outil |
|---|---|---|
| 1 | Domaine, algorithmes (propriétés face à une référence exhaustive), services avec faux adaptateurs, ArchUnit | JUnit 5, jqwik |
| 2 | Migrations, persistance, API REST, utilisateur simulé (en-tête X-Utilisateur-Id), isolation des listes, concurrence, scénario de démonstration | Testcontainers PostgreSQL 17 |
| 3 | Build officiel `gradlew build` et couverture JaCoCo | Gradle |
| 4 | Composants et services Angular (états chargement, vide, erreur, 401, 403) | Vitest |
| 5 | Build de production Angular | Angular CLI |
| 6 | Parcours réels dans Chromium : scénario du dossier sur ordinateur et téléphone, autres parcours sur ordinateur | Playwright (Python) |

Le niveau 6 exige Docker Compose et que le port 8080 soit libre : il démarre PostgreSQL, recrée une base dédiée `kadi_e2e` (la base de travail `kadi` n'est jamais touchée), lance le back avec le profil `demo`, réutilise ou démarre le front, puis arrête ce qu'il a lancé. À la première exécution, il crée `front/e2e/.venv` et installe Playwright et Chromium. Le récapitulatif final indique le statut et la durée de chaque niveau ; les journaux sont dans `build/tester-tout/`.

## Qui travaille où

Un module par domaine fonctionnel, un responsable par module. Chacun reste dans son module, ce qui limite les conflits.

| Module | Responsable | v0.1.0 | v0.2.0 |
|---|---|---|---|
| `catalogue` | Amal | Catalogue et rapprochement | Historique et réduflation |
| `prix` | Adam | Relevés de prix et prix unitaire | Vérification des promotions |
| `comparaison` | Assia | Comparateur | Panier optimisé |

Les features de v1.0.0 (inflation personnelle, budget, alternatives) décideront si elles rejoignent un module existant ou en créent un.

**Migrations réservées pour v0.1.0**, afin d'éviter que deux branches prennent le même numéro : `V4` Assia. Adam a pris `V2` pour F2 ; `V3` reste à Amal.

La migration `V4__comptes_utilisateur.sql` regroupe tout le schéma de F1 : comptes (utilisateurs simulés) et jetons, catalogue (marque, format, catégorie, provenance Open Food Facts, type `PRECIS` ou `GENERIQUE` et nature `SOLIDE`, `LIQUIDE` ou `UNITAIRE` des besoins génériques) et liste personnelle (`besoin_personnalise`, `liste_courses`, `ligne_liste`). Elle remplace l'ancienne V4 et la V5 temporaire, qui n'avaient jamais été poussées. **Une base locale qui avait appliqué ces anciennes versions doit être sauvegardée puis recréée** (voir « Recréer sa base locale » ci-dessous) : Flyway refuserait sinon de démarrer (checksum V4 différent, V5 inconnue). Elle crée aussi la colonne `nom_affiche` et les trois profils simulés ; les colonnes d'authentification (mot de passe, email vérifié, jetons) ne servent plus depuis le passage à l'utilisateur simulé.


`GET /api/v1/catalogue/produits` permet, sans connexion, de consulter les produits déjà présents en base. Le résultat est paginé par identifiant, par lots de 20 : `produits` contient `id`, `libelle`, `codeBarres`, `sourceUrl`, `imageUrl`, `imageAuteur`, `marque` et `format` (ces six derniers champs peuvent être nuls) ; `prochainId` est nul à la dernière page, ou se transmet au paramètre `apres` pour lire la suite. Le catalogue peut être vide. Les filtres optionnels `marque` et `format` s'appliquent avant la pagination : `marque` correspond à une marque exacte parmi celles éventuellement séparées par des virgules, `format` cherche un texte partiel insensible à la casse. Ces champs sont créés par V4. Le catalogue ne présente que les produits précis : les besoins génériques sont proposés dans « Ma liste ».

`GET /api/v1/catalogue/produits/par-nom` parcourt le même catalogue et accepte les mêmes filtres, par pages de 20 triées selon le libellé sans distinction de casse ni d'accents usuels, puis par identifiant. Ce classement technique ne couvre pas toutes les règles linguistiques. La réponse contient `produits` et `prochainCurseur` : transmettre ce dernier à `apres` avec les mêmes filtres. Un curseur invalide ou associé à d'autres filtres renvoie 400. La pagination n'est pas un instantané : une modification du catalogue pendant le parcours peut déplacer des résultats. Ce tri ne dispose pas encore d'un index adapté ni de mesures sur un catalogue large.

`GET /api/v1/catalogue/produits/marques?q=...&format=...` renvoie les marques connues et leur nombre de produits pour la recherche et le format indiqués, indépendamment de la marque sélectionnée. Sans recherche, il compte dans tout le catalogue filtré par format ; une recherche de moins de trois caractères normalisés ne renvoie aucune marque. Les fiches sans marque ne sont pas comptées ; un produit contenant plusieurs marques est compté une fois pour chacune. Avec une recherche, seuls les produits retenus par la même présélection et le même seuil que les résultats sont comptés, y compris au-delà de la page affichée. Les marques sont regroupées sans distinction de casse ; leurs libellés restent ceux des données, sans dédoublonnage sémantique. Le calcul reparcourt les produits ou les candidats à chaque changement de recherche ou de format et la liste n'est pas paginée : son coût et son volume restent à mesurer avant un import large ou une mise en production.

Un filtre Nutri-Score expérimental, hors périmètre de F1, est désactivé par défaut (seul `NutriScoreDesactiveIT` le vérifie ; ses tests sur base isolée ont été retirés pour alléger la suite et seront à réintroduire avant toute activation). Les paramètres `nutriScore` répétés sélectionnent les notes A à E, sans inclure les fiches non renseignées ; les compteurs de marques respectent également cette sélection. L'activation exige la V6 provisoire `src/test/resources/propositions/V6__nutri_score_proposition.sql`, située hors du répertoire des migrations applicatives : ne pas l'appliquer à une base de travail ni activer ce filtre sur une base réelle. La source et la fraîcheur des scores, l'import, les performances et la migration additive restent à valider. Aucun score n'a été attribué aux fiches existantes.

Un instantané de test limité à trois notes figure dans `src/test/resources/propositions/nutri-score-petit-jeu.sql`. Les notes et la version de calcul 2023 ont été observées sur les fiches Open Food Facts le 21 novembre 2026 ; la date source conservée est celle de modification de la fiche, et ne date pas nécessairement le calcul du score. Les tests ne contactent pas Open Food Facts. L'instantané ne modifie que les fiches déjà reliées à la même provenance et au même code, ne remplace ni une version source plus récente ni une note d'une autre source, et est réexécutable. Si la fiche OFF connue localement est plus récente que l'instantané, il n'y applique pas la note, même si celle-ci est absente ou ancienne. Il n'a été exécuté que sur des bases de test isolées et ne doit pas être utilisé comme import de production. Les conditions de réutilisation et les obligations ODbL/DbCL mentionnées ci-dessous restent à vérifier avant diffusion.

Pour une future actualisation, la règle retenue est de retirer une ancienne note OFF lorsqu'une fiche OFF plus récente indique explicitement que la note n'existe plus, sans toucher aux notes d'autres sources. Un champ non fourni ou invalide ne doit pas être interprété comme une suppression. Cette actualisation n'est pas implémentée : le [schéma API OFF](https://github.com/openfoodfacts/openfoodfacts-server/blob/231f498a5f5102320165cfc2839b16a4ee2273b8/docs/api/ref/schemas/product_nutriscore.yaml) décrit les notes `a` à `e`, tandis que le [calcul OFF](https://github.com/openfoodfacts/openfoodfacts-server/blob/231f498a5f5102320165cfc2839b16a4ee2273b8/lib/ProductOpener/Food.pm) utilise aussi `unknown` notamment lorsque la catégorie ou des nutriments manquent, et `not-applicable` pour certaines catégories. Ces valeurs apparaissent dans ses tests, mais leur présence et leur fraîcheur ne sont pas encore garanties pour le flux qui sera choisi ; il faut confirmer son contrat avant toute suppression automatique.

La V6 provisoire avait été testée avec Flyway (test retiré depuis) sur une base jetable créée à partir des fichiers V1 et V4 actuels : les données et la somme de contrôle V4 sont conservées et une réexécution n'applique rien. Les tests vérifient également le refus d'un checksum V4 délibérément modifié et d'un schéma incompatible. Avec la configuration Flyway actuelle, l'ajout tardif de migrations V2/V3 **fictives** à cette base en V4 fait échouer la validation et empêche aussi l'application de V6. Ces simulations ne reproduisent pas l'ancienne V4 exacte, absente de l'historique accessible, ni les migrations V2/V3 de l'équipe. Le numéro V6 et la procédure de déploiement restent à coordonner ; ne pas déplacer la proposition dans les migrations applicatives sans accord. Le Nutri-Score n'est volontairement pas inclus dans la V4 fusionnée : aucun critère de la fiche F1 ne le demande et ses données ne sont pas alimentées.

Un test isolé de `pg_dump` et `pg_restore`, retiré depuis pour alléger la suite, restaurait des données fictives, leur séquence et l'historique Flyway créé à partir des V1/V4 actuelles ; il valide ensuite cet historique et applique la proposition de V6 à la copie. Il ne reproduit ni l'historique de la base de travail, ni l'ancienne V4, ni les V2/V3 de l'équipe, ni les rôles PostgreSQL. Ce n'est pas encore une procédure de sauvegarde et de retour arrière validée pour le déploiement.

`GET /api/v1/catalogue/produits/suggestions?q=...` est réservé aux comptes connectés. Il renvoie un tableau d'au plus sept produits locaux, vide avant trois caractères normalisés ; la saisie est limitée à 255 caractères. Aucune requête Open Food Facts n'est faite pendant la recherche. Le prototype utilise le nom, la marque et le format lorsqu'ils sont connus ; il normalise casse, accents usuels et ponctuation, présélectionne les candidats par `word_similarity` de `pg_trgm` (seuil 0,2), puis classe par similarité de Dice sur trigrammes (seuil 0,5 ; les sous-chaînes exactes reçoivent le score 1), avec départage stable. Le plafond de 200 candidats a été supprimé. Ces seuils sont provisoires : la présélection peut perdre des résultats et les données de marque ou de format manquantes ne sont pas inventées. Une recherche de format peut encore proposer d'autres formats : ils restent identifiés par leur libellé et leur champ format, mais le classement n'est pas une garantie d'équivalence. L'ajout à la liste passe par « Ma liste » (voir ci-dessous).

`GET /api/v1/catalogue/produits/recherche?q=...&taille=7` fournit la première page et un `prochainCurseur` lorsqu'il existe d'autres résultats. Ce curseur opaque se transmet à `apres` avec la même recherche et les mêmes filtres `marque` et `format` ; les pages suivantes contiennent 20 résultats par défaut (`taille` entre 1 et 20). Les suggestions et pages utilisent le même classement. « Voir tous les résultats » signifie ici les résultats retenus par la présélection et les seuils, pas toutes les fiches potentiellement pertinentes. Le curseur n'est pas un instantané : si le catalogue change pendant le parcours, des résultats peuvent être déplacés. Chaque page reparcourt les candidats et recalcule les scores ; il n'y a pas d'index adapté. Avant un import massif, concevoir l'index et mesurer les performances sur un volume représentatif, ainsi que la précision et le rappel.

`GET /api/v1/produits?recherche=...` est la recherche commune à F1 et F2 (contrat convenu avec F2), réservée aux comptes connectés. Elle renvoie toujours `200` avec un tableau d'au plus 20 éléments `{id, libelle, codeBarres}` ; `codeBarres` peut être nul. Une saisie absente, vide ou de moins de deux caractères, ou sans correspondance, renvoie `[]` ; au-delà de 255 caractères, la réponse est `400`. Une saisie de 6 à 14 chiffres uniquement est recherchée exactement comme code-barres. Sinon, chaque mot doit apparaître dans le libellé, dans n'importe quel ordre, sans tenir compte de la casse, des accents ni de l'espace des quantités (`1l` trouve `1 L`) ; les correspondances sont classées par ressemblance (Dice), puis par proximité avec le libellé entier, puis par libellé normalisé et identifiant. Lorsque des places restent disponibles, des résultats approchés de la recherche F1 (fautes de frappe, dès trois caractères normalisés) sont ajoutés après les correspondances par mots. Le contrôleur provisoire de F2 (`prix/adapter/in/web/RechercheController`) doit être retiré dans la même intégration : deux contrôleurs sur cette adresse empêchent le démarrage. Sa pertinence et ses performances n'ont pas été mesurées à l'échelle du jeu Open Prices (environ 79 000 produits) ; voir les mesures ci-dessous.

Un essai exploratoire (`VolumeRechercheCompatibleIT`) a généré 80 000 fiches fictives sur PostgreSQL isolé, sans import de données réelles. Après un échauffement, cinq mesures successives par cas donnaient les médianes suivantes sur le poste de test : deux caractères 840 ms, `lait` 797 ms, `yaort` 2 287 ms, code-barres exact 0,9 ms et recherche sans résultat 1 612 ms. Le maximum observé pour `yaort` était de 2 369 ms. Ces mesures concernent le service et la base, sans HTTP, navigateur ni concurrence ; cinq échantillons et des libellés synthétiques ne permettent ni de calculer un percentile fiable ni de conclure sur Open Prices ou la production. Le test est désactivé sauf si `KADI_TEST_RECHERCHE_VOLUMINEUX=true` est explicitement transmis ; il ne charge les données que dans une base de test jetable.

Sur un second essai isolé de ce même jeu, `EXPLAIN ANALYZE` montre un parcours séquentiel des 80 000 produits pour les correspondances `lait` (environ 566 ms côté SQL) et pour la présélection `yaort` (environ 946 ms côté SQL), avec 16 000 lignes retenues dans chaque cas. Les pages lues étaient déjà dans le cache PostgreSQL pendant ces plans. Ces observations orientent vers une étude d'indexation et de présélection, sans prouver qu'un index seul résoudra la latence : il faudra préserver les résultats et mesurer aussi le classement, le coût d'écriture et les données représentatives.

Un autre essai isolé compare les correspondances `POSITION` avec une variante `LIKE` échappée et deux index trigrammes temporaires sur les libellés brut et normalisé. Sur les cinq saisies fictives vérifiées, les identifiants retournés sont identiques, y compris pour `%` et `_`. Pour `lait`, un plan SQL utilisait les deux index : environ 17 ms contre 532 ms pour `POSITION` avec les mêmes index présents. Pour `la`, la variante parcourait encore les 80 000 lignes (environ 590 ms). La création des deux index a pris environ 2,1 s et leur taille cumulée était de 11,6 Mo sur ce jeu. Ce n'est pas une modification de la recherche applicative : l'équivalence complète, le classement, la recherche approchée, les écritures et les performances réelles restent à vérifier.

Un essai distinct de présélection approchée indexée conserve le filtre exact `word_similarity >= 0,2` après une présélection trigramme au seuil temporaire de 0,1999. Sur six saisies et le même jeu fictif, les candidats obtenus sont identiques à ceux de la requête actuelle ; cela inclut un candidat généré avec une similarité égale à `0,2::real`. Pour `yaort`, les plans SQL mesuraient environ 925 ms sans usage de l'index contre 383 ms avec la variante indexée ; celle-ci retenait encore 16 001 candidats. L'index expérimental a pris environ 1,3 s à créer et 5,8 Mo. Cette comparaison ne prouve pas l'équivalence sur tous les cas et ne mesure pas le classement Java ni le coût des écritures. Aucun de ces index n'est une migration applicative.

Pour deux caractères, un essai d'index B-tree et de limite SQL anticipée a ramené un plan de test à environ 0,9 ms sur le jeu fictif régulier. Mais l'ordre de cette variante diffère de celui du service pour des libellés tels que `La!x` et `La x` : la normalisation SQL et celle du classement Java ne sont pas équivalentes. Cette piste ne doit donc pas être appliquée telle quelle. L'index temporaire occupait environ 6,0 Mo ; les coûts d'écriture et les résultats sur d'autres données ne sont pas évalués.

Une première évaluation reproductible (`EvaluationRechercheIT`, PostgreSQL isolé) utilise 12 libellés fictifs et 9 saisies annotées, dont des fautes, formats et recherches sans correspondance. Sur ce seul jeu, 14 suggestions pertinentes attendues sur 14 sont retrouvées, parmi 15 suggestions affichées (précision 14/15, rappel 14/14) ; les 14 correspondances attendues figurent dans la présélection. Le top 7 est identique au classement exhaustif sur ces 9 saisies. À l'ancien seuil de 0,3, la précision était de 14/18 et `chocolat` proposait des Coca-Cola ; à 0,5 ce cas ne renvoie rien. Ce jeu est trop petit pour généraliser ces chiffres ou exclure des pertes sur d'autres fautes : notamment, `Yaourt nature` propose encore un yaourt à la fraise. Un jeu indépendant plus large et des mesures à volume représentatif restent nécessaires.

### Ma liste (F1)

Une seule liste par utilisateur simulé, créée au premier ajout. L'utilisateur vient de l'en-tête `X-Utilisateur-Id` (voir plus haut) ; une ligne d'un autre utilisateur répond `404`, exactement comme une ligne inexistante. Erreurs au format `application/problem+json`, avec un `detail` lisible par l'utilisateur.

**Modération (administrateur).** La migration `V5__moderation_besoins.sql` relie une proposition validée au besoin générique commun. Routes réservées au profil « Admin Kadi » (`403` pour un consommateur) : `GET /api/v1/admin/besoins?statut=EN_ATTENTE|VALIDE|REFUSE`, `GET /api/v1/admin/besoins/generiques`, `POST /api/v1/admin/besoins/{id}/validation` avec `{"libelle":"…","nature":"SOLIDE|LIQUIDE|UNITAIRE"}` ou `{"besoinExistantId":…}`, et `POST /api/v1/admin/besoins/{id}/refus` (`409` si la proposition est déjà traitée). La liste de l’auteur garde sa saisie d’origine.

| Route | Rôle |
|---|---|
| `GET /api/v1/liste` | `{ lignes: [...] }` dans l'ordre d'ajout. Chaque ligne : `id`, `type` (`PRODUIT`, `BESOIN_GENERIQUE`, `BESOIN_PERSONNALISE`), `referenceId`, `libelle` (connu à l'ajout), `marque` et `formatProduit` (produit précis), `quantite`, `unite`, `formatSouhaite`, `statutBesoin` (besoin personnel), `unitesAutorisees`. |
| `POST /api/v1/liste/lignes` | `{ type, referenceId?, libelle?, quantite, unite, formatSouhaite?, decision?, ligneAFusionner? }`. Statut `AJOUTEE` (201), `FUSIONNEE` (200) ou `CONFIRMATION_REQUISE` (200, rien n'est modifié : renvoyer la même demande avec `decision` `FUSIONNER` et `ligneAFusionner`, ou `SEPARER`). La réponse contient aussi la liste à jour. |
| `PATCH /api/v1/liste/lignes/{id}` | `{ quantite }` ; l'unité de la ligne est conservée. Renvoie la liste à jour. |
| `DELETE /api/v1/liste/lignes/{id}` | `204`. |

Règles appliquées (fiche F1, conception A1 à A7) :

- un **produit précis** se compte à l'article, sans fraction ni format souhaité ; un **besoin générique** accepte les unités de sa nature (solide : article, g, kg ; liquide : article, L, cl ; unitaire : article) ; un **besoin personnel** accepte toutes les unités ;
- une unité inconnue, une quantité nulle, négative, à plus de 3 décimales ou supérieure à 100 000 est refusée (`400`) avec la liste des valeurs acceptées ;
- même référence, même format et unité compatible : quantités additionnées (1 kg + 500 g = 1,5 kg ; 1 L + 50 cl = 1,5 L) ; unités incompatibles (3 pommes et 1 kg) : lignes distinctes ; formats écrits différemment mais proches : confirmation demandée ;
- un besoin personnel est créé `EN_ATTENTE`, privé, unique par libellé (sans distinction de casse) pour son auteur ; la modération par un administrateur n'est pas encore implémentée ;
- un produit référencé par une ligne ne peut pas être supprimé du catalogue (la ligne garde aussi le libellé connu à l'ajout) ;
- la liste du compte est verrouillée pendant chaque modification : des ajouts simultanés (double clic, deux appareils) sont tous comptés, sans doublon (`ListePersonnelleConcurrenceIT`).

Les suggestions de la liste utilisent `GET /api/v1/produits?recherche=...&portee=liste`, qui ajoute aux produits précis les besoins génériques et les besoins personnels de l'utilisateur simulé (en-tête `X-Utilisateur-Id` obligatoire). Sans `portee`, la route reste limitée aux produits précis, conformément au contrat de F2.

Le prototype antérieur (`/api/v1/liste-experimentale`, profil `liste-experimentale`, tables `*_proposition` de test) est conservé désactivé, sans tests (seul `ListeExperimentaleDesactiveeIT` vérifie que ses routes ne sont pas exposées) ; il est remplacé par les routes ci-dessus et peut être supprimé.

### Catalogue de démonstration

`src/main/resources/db/demo/R__donnees_demo.sql`, chargé uniquement avec le profil `demo`. Section F1 : 9 produits précis fictifs (codes `2001000000xxx`, plage interne jamais attribuée à un vrai produit : packs Coca-Cola 6 x 33 cl, farine, lessive, gobelets…) et 11 besoins génériques. Le scénario du dossier (« lait », « pâtes barilla », « coca 6 », « yaort ») se joue avec ces fiches et avec les 20 fiches réelles Open Food Facts de `R__images_open_food_facts.sql` (laits Lactel, pâtes Barilla, Coca-Cola, yaourts Danone…), qui remplacent depuis le 24 novembre 2026 les anciens doublons fictifs. Ces doublons sont supprimés des bases de démo existantes au prochain démarrage, sauf s'ils figurent encore dans une liste, un relevé de prix ou une proposition modérée. Le script est idempotent (`DonneesDemoScenarioIT`). Les autres features ajoutent leur section à la suite, avec leur propre plage de codes.

### Recréer sa base locale

À faire une fois après la fusion des migrations en V4, si la base locale avait appliqué une ancienne version de V4 (y compris celle sans utilisateurs simulés) ou une V5 temporaire. Ces commandes suppriment les données de la base `kadi` locale (comptes de test et fiches importées) : la sauvegarde de l'étape 1 est indispensable.

```powershell
# 1. Sauvegarde dans le conteneur, puis copie hors du dépôt
docker compose exec -T db pg_dump -U kadi -Fc -f /tmp/kadi-avant-v4.dump kadi
docker compose cp db:/tmp/kadi-avant-v4.dump "$HOME\kadi-avant-v4.dump"
# 2. Recréation de la base vide
docker compose exec -T db psql -U kadi -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE kadi WITH (FORCE)" -c "CREATE DATABASE kadi"
# 3. Démarrage : Flyway applique V1 et V4 ; le profil demo charge le catalogue de démonstration
.\gradlew.bat bootRun --args="--spring.profiles.active=demo"
```

Pour relire l'ancienne sauvegarde, la restaurer dans une base séparée : `docker compose exec -T db createdb -U kadi kadi_ancienne` puis `docker compose exec -T db pg_restore -U kadi -d kadi_ancienne /tmp/kadi-avant-v4.dump`. Elle ne se restaure pas telle quelle dans `kadi` : son historique Flyway correspond aux anciennes migrations. Les trois fiches Open Food Facts se réimportent avec le script ci-dessous.

### Petit jeu Open Food Facts

Un instantané de trois fiches réelles est disponible dans `src/main/resources/db/import/open-food-facts-petit-jeu.sql` : images vérifiées le 19 novembre 2026, marques et formats le 20 novembre 2026. Ce n'est pas un import représentatif de la France. Le profil `demo` le charge au démarrage via `db/demo/R__images_open_food_facts.sql`, qui y ajoute 17 autres fiches réelles avec photo (codes, images et auteurs vérifiés sur l'API Open Food Facts le 24 novembre 2026) ; sans ce profil, il ne s'exécute jamais. Les produits sans photo affichent une illustration générique selon leur famille (lait, boisson, pâtes…), déduite du libellé. Le script exige la V4 actuelle ; l'exécuter volontairement depuis `back` :

```powershell
Get-Content -Raw .\src\main\resources\db\import\open-food-facts-petit-jeu.sql | docker compose exec -T db psql -v ON_ERROR_STOP=1 -U kadi -d kadi
```

Il est réexécutable : les fiches existantes sans provenance OFF sont ignorées ; pour les trois fiches déjà reliées à OFF par le même identifiant et code-barres, seuls les champs marque et format manquants sont complétés. Les valeurs existantes, libellés, images et autres données sont conservés. Aucune actualisation automatique n'est prévue. Les images sont chargées depuis Open Food Facts par le navigateur ; elles peuvent disparaître et divulguent l'adresse IP du visiteur à ce service. Une illustration est affichée si elles sont indisponibles.

Les [conditions Open Food Facts](https://world.openfoodfacts.org/terms-of-use) placent la base sous [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1.0/), ses contenus individuels sous [DbCL 1.0](https://opendatacommons.org/licenses/dbcl/1.0/) et les photos sous [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/). Le catalogue attribue chaque fiche et photo à sa source et à son contributeur, avec liens vers les licences ; les photos sélectionnées peuvent avoir été recadrées ou redimensionnées par Open Food Facts. En cas de diffusion d'une base dérivée ou d'adaptation des images, les obligations de partage à l'identique doivent être étudiées. Les marques et visuels des emballages peuvent aussi relever de droits tiers : la licence des photos ne les règle pas. Vérifier ces conditions avant publication.

Pour examiner l'export CSV officiel **sans l'enregistrer ni rien importer en base**, la commande suivante lit au maximum 5 000 lignes directement en flux :

```powershell
.\gradlew.bat evaluerExportOff -PexportOff=--officiel -PlimiteOff=5000
```

Il est aussi possible d'analyser un fichier local TSV ou TSV compressé en gzip avec `-PexportOff="C:\chemin\export.csv.gz"`. La limite est le nombre de lignes lues, pas le nombre de produits acceptés. L'outil compte les fiches portant le tag `en:france`, un code numérique de 1 à 14 chiffres et un nom non vide d'au plus 255 caractères ; marque et format ne doivent pas dépasser 255 caractères. Ce sont des critères provisoires, pas une preuve de commercialisation en France. Le fichier, les données importées et les sauvegardes ne sont pas à committer sur GitHub.

Le 19 novembre 2026, la lecture des **5 000 premières lignes** de l'export a relevé 407 fiches admissibles, dont 383 ayant une URL d'image ; cet échantillon initial n'est pas représentatif du fichier entier. Le dictionnaire CSV indique `creator` (créateur de la fiche), mais pas l'auteur de chaque photo : il ne faut pas attribuer une image à `creator` par supposition. L'export est disponible en flux auprès d'[Open Food Facts](https://world.openfoodfacts.org/data) ; les recherches du catalogue utilisent uniquement la base locale.

Un importeur expérimental explicite a été testé sur une base isolée. Il exige la V4 actuelle et une URL JDBC et des identifiants transmis par les variables d'environnement `KADI_IMPORT_DB_URL`, `KADI_IMPORT_DB_USER` et `KADI_IMPORT_DB_PASSWORD` ; ne pas les committer. Depuis `back`, après configuration de ces variables et sauvegarde de la base ciblée :

```powershell
.\gradlew.bat importerExportOff -PexportOff=--officiel -PlimiteOff=5000
```

Cette commande lit au maximum les **premières** 5 000 lignes, pas 5 000 produits ; le maximum accepté actuellement est de 100 000 lignes par exécution. Elle ne permet pas de reprendre au-delà des premières lignes et ne constitue donc pas encore un import complet. En cas d'erreur, la transaction est annulée. Les codes déjà associés à une fiche non OFF ne sont pas revendiqués, les fiches OFF plus récentes ne sont pas remplacées par une ancienne version, et les catégories corrigées et photos existantes sont conservées. Les images du CSV sans auteur vérifiable ne sont pas importées. La commande affiche le nombre de fiches créées, actualisées et ignorées ; elle ne supprime pas les produits absents de l'export.

Sur une base temporaire isolée, l'import en flux des 5 000 premières lignes a créé 407 fiches, puis un second passage n'a créé aucun doublon. Aucun produit de cet essai n'a été ajouté à la base de travail. Avant un import large : prévoir reprise, contrôle d'espace, mesures de performance, stratégie de mise à jour et règles de catégorisation ; vérifier les obligations ODbL et l'attribution des images.


Les migrations sont normalement appliquées dans l'ordre. Une base ayant déjà appliqué V4 n'appliquera pas automatiquement une V2 ou une V3 ajoutée ensuite (Flyway refuse une migration antérieure manquante) : lors de l'intégration de V2 (F2) et V3 (F3), recréer les bases locales de développement, ou décider ensemble d'activer `outOfOrder` pour cette seule mise à niveau.

## Structure d'un module

```text
src/main/java/fr/miage/kadi/
├── api/                         endpoints techniques (/api/v1/info)
├── config/                      configuration transverse (CORS)
├── catalogue/ prix/ comparaison/
│   ├── domain/                  Java pur
│   │   ├── model/               objets valeur, invariants
│   │   ├── algo/                algorithmes
│   │   └── exception/           exceptions métier
│   ├── application/             cas d'utilisation, sans framework
│   │   ├── port/in/             appelés par les adaptateurs entrants
│   │   ├── port/out/            implémentés par les adaptateurs sortants
│   │   └── service/             implémentation des cas d'utilisation
│   ├── adapter/
│   │   ├── in/web/              contrôleurs, DTO, erreurs problem+json
│   │   └── out/persistence/     entités et dépôts JPA
│   └── config/                  assemblage Spring
src/main/resources/db/migration/ migrations Flyway
src/main/resources/db/demo/      catalogue de démonstration
```

Les dépendances vont toujours vers le domaine. `ArchitectureHexagonaleTest` fait échouer le build si cette règle est violée, ou si le domaine ou l'application d'un module dépend d'un autre module.

## Travailler en parallèle

**Le modèle commun.** `V1__modele_commun.sql` crée les tables `enseigne`, `magasin`, `produit` et `releve_de_prix`, sans les colonnes propres à une feature : chacun ajoute les siennes dans sa migration. Une migration poussée n'est jamais modifiée.

**Appeler un autre module.** On ne lit jamais les tables d'un autre module et on n'importe jamais son domaine. Si `comparaison` a besoin des relevés de `prix` :

1. `comparaison` déclare ce dont il a besoin dans un port de sortie, avec ses propres types (`application/port/out`).
2. Tant que `prix` n'est pas prêt, les tests branchent un faux adaptateur en mémoire sur ce port : personne n'est bloqué par personne.
3. Une fois `prix` livré, un adaptateur `comparaison/adapter/out/prix` implémente le port en appelant le port d'entrée de `prix`.

## Démarrer sa feature

1. Créer l'issue avec le modèle **Feature**, rédiger sa fiche dans `doc/features/` et la faire relire.
2. Partir de `develop` à jour : `git switch -c feature/<issue>-<nom-court>`.
3. Suivre les étapes de la section 06 du dossier : back, tests, API REST, front.
