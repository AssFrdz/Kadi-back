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

Pour tester l'inscription et la vérification d'adresse en local sans véritable fournisseur mail :

```powershell
docker compose --profile email-test up -d db mailpit
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

L'interface Mailpit est disponible sur <http://127.0.0.1:8025> et reste accessible uniquement depuis la machine locale. Le front doit également tourner (`npm start`) ; ouvrir <http://127.0.0.1:4200/inscription>, créer le compte, puis ouvrir le message reçu dans Mailpit et suivre son lien de vérification. Le profil `local` envoie uniquement vers Mailpit.

- API : <http://localhost:8080/api/v1/info>
- Swagger UI : <http://localhost:8080/swagger-ui.html>
- Santé : <http://localhost:8080/actuator/health>

## Tester

```bash
./gradlew build          # compile, lance tous les tests, produit le rapport de couverture
open build/reports/jacoco/test/html/index.html
```

Docker doit tourner : les tests d'intégration démarrent eux-mêmes un PostgreSQL 17 avec Testcontainers.

Conventions : `*Test` pour les tests unitaires (sans Spring), `*IT` pour les tests d'intégration (`@SpringBootTest`).

## Qui travaille où

Un module par domaine fonctionnel, un responsable par module. Chacun reste dans son module, ce qui limite les conflits.

| Module | Responsable | v0.1.0 | v0.2.0 |
|---|---|---|---|
| `catalogue` | Amal | Catalogue et rapprochement | Historique et réduflation |
| `prix` | Adam | Relevés de prix et prix unitaire | Vérification des promotions |
| `comparaison` | Assia | Comparateur | Panier optimisé |

Les features de v1.0.0 (inflation personnelle, budget, alternatives) décideront si elles rejoignent un module existant ou en créent un.

**Migrations réservées pour v0.1.0**, afin d'éviter que deux branches prennent le même numéro : `V2` Amal, `V3` Adam, `V4` Assia.

La migration `V4__comptes_utilisateur.sql` prépare les tables des comptes et des empreintes de jetons pour F1. L'inscription est disponible via `POST /api/v1/auth/inscription` ; elle crée un compte non vérifié et envoie un lien de confirmation. Le navigateur transmet le jeton du lien dans `{"token":"..."}` à `POST /api/v1/auth/verification-email/confirmation`, qui répond `204 No Content`.

La connexion utilise `POST /api/v1/auth/connexion` avec `{"email":"...","motDePasse":"..."}`. Elle renvoie `200` avec l'identité et le rôle du compte, puis conserve l'authentification dans un cookie de session HTTP-only. Seuls les comptes qui ont vérifié leur adresse peuvent ouvrir une session ; les comptes non vérifiés et les identifiants invalides reçoivent la même erreur `401`. `GET /api/v1/auth/session` renvoie les informations du compte connecté et `401` sans session. `POST /api/v1/auth/deconnexion` invalide la session et répond `204 No Content`. Tous les POST d'authentification exigent un jeton CSRF obtenu via `GET /api/v1/auth/csrf`.

`GET /api/v1/catalogue/produits` permet à un compte connecté de consulter les produits déjà présents en base. Le résultat est paginé par identifiant, par lots de 20 : `produits` contient `id`, `libelle`, `codeBarres`, `sourceUrl`, `imageUrl`, `imageAuteur`, `marque` et `format` (ces six derniers champs peuvent être nuls) ; `prochainId` est nul à la dernière page, ou se transmet au paramètre `apres` pour lire la suite. Le catalogue peut être vide. Les filtres optionnels `marque` et `format` s'appliquent avant la pagination : `marque` correspond à une marque exacte parmi celles éventuellement séparées par des virgules, `format` cherche un texte partiel insensible à la casse. La lecture de ces nouveaux champs exige la migration temporaire V5.

`GET /api/v1/catalogue/produits/suggestions?q=...` est réservé aux comptes connectés. Il renvoie un tableau d'au plus sept produits locaux, vide avant trois caractères normalisés ; la saisie est limitée à 255 caractères. Aucune requête Open Food Facts n'est faite pendant la recherche. Le prototype utilise le nom, la marque et le format lorsqu'ils sont connus ; il normalise casse, accents usuels et ponctuation, présélectionne les candidats par `word_similarity` de `pg_trgm` (seuil 0,2), puis classe par similarité de Dice sur trigrammes (seuil 0,5 ; les sous-chaînes exactes reçoivent le score 1), avec départage stable. Le plafond de 200 candidats a été supprimé. Ces seuils sont provisoires : la présélection peut perdre des résultats et les données de marque ou de format manquantes ne sont pas inventées. Une recherche de format peut encore proposer d'autres formats : ils restent identifiés par leur libellé et leur champ format, mais le classement n'est pas une garantie d'équivalence. Aucun ajout à une liste n'est encore disponible.

`GET /api/v1/catalogue/produits/recherche?q=...&taille=7` fournit la première page et un `prochainCurseur` lorsqu'il existe d'autres résultats. Ce curseur opaque se transmet à `apres` avec la même recherche et les mêmes filtres `marque` et `format` ; les pages suivantes contiennent 20 résultats par défaut (`taille` entre 1 et 20). Les suggestions et pages utilisent le même classement. « Voir tous les résultats » signifie ici les résultats retenus par la présélection et les seuils, pas toutes les fiches potentiellement pertinentes. Le curseur n'est pas un instantané : si le catalogue change pendant le parcours, des résultats peuvent être déplacés. Chaque page reparcourt les candidats et recalcule les scores ; il n'y a pas d'index adapté. Avant un import massif, concevoir l'index et mesurer les performances sur un volume représentatif, ainsi que la précision et le rappel.

Une première évaluation reproductible (`EvaluationRechercheIT`, PostgreSQL isolé) utilise 12 libellés fictifs et 9 saisies annotées, dont des fautes, formats et recherches sans correspondance. Sur ce seul jeu, 14 suggestions pertinentes attendues sur 14 sont retrouvées, parmi 15 suggestions affichées (précision 14/15, rappel 14/14) ; les 14 correspondances attendues figurent dans la présélection. Le top 7 est identique au classement exhaustif sur ces 9 saisies. À l'ancien seuil de 0,3, la précision était de 14/18 et `chocolat` proposait des Coca-Cola ; à 0,5 ce cas ne renvoie rien. Ce jeu est trop petit pour généraliser ces chiffres ou exclure des pertes sur d'autres fautes : notamment, `Yaourt nature` propose encore un yaourt à la fraise. Un jeu indépendant plus large et des mesures à volume représentatif restent nécessaires.

### Petit jeu Open Food Facts

Un instantané de trois fiches réelles est disponible dans `src/main/resources/db/import/open-food-facts-petit-jeu.sql` : images vérifiées le 19 novembre 2026, marques et formats le 20 novembre 2026. Ce n'est ni le jeu de démonstration fictif ni un import représentatif de la France. Il ne s'exécute jamais au démarrage. Le script exige la migration temporaire V5 ; après sa mise à niveau, l'exécuter volontairement depuis `back` :

```powershell
Get-Content -Raw .\src\main\resources\db\import\open-food-facts-petit-jeu.sql | docker compose exec -T db psql -v ON_ERROR_STOP=1 -U kadi -d kadi
```

Il est réexécutable : les fiches existantes sans provenance OFF sont ignorées ; pour les trois fiches déjà reliées à OFF par le même identifiant et code-barres, seuls les champs marque et format manquants sont complétés. Les valeurs existantes, libellés, images et autres données sont conservés. Aucune actualisation automatique n'est prévue. Les images sont chargées depuis Open Food Facts par le navigateur ; elles peuvent disparaître et divulguent l'adresse IP du visiteur à ce service. Une illustration est affichée si elles sont indisponibles.

Les [conditions Open Food Facts](https://world.openfoodfacts.org/terms-of-use) placent la base sous [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1.0/), ses contenus individuels sous [DbCL 1.0](https://opendatacommons.org/licenses/dbcl/1.0/) et les photos sous [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/). Le catalogue attribue chaque fiche et photo à sa source et à son contributeur, avec liens vers les licences ; les photos sélectionnées peuvent avoir été recadrées ou redimensionnées par Open Food Facts. En cas de diffusion d'une base dérivée ou d'adaptation des images, les obligations de partage à l'identique doivent être étudiées. Les marques et visuels des emballages peuvent aussi relever de droits tiers : la licence des photos ne les règle pas. Vérifier ces conditions avant publication.

**Attention : V4 a été complétée localement pour ce prototype, mais elle était déjà appliquée à certaines bases.** Ne pas redémarrer le back sur ces bases avec cette V4 modifiée : Flyway détectera un checksum différent. Ne pas utiliser `repair` seul : il ne crée pas la nouvelle table. Préparer et valider une procédure de mise à niveau préservant les données et l'historique avant toute intervention ; coordonner aussi V2 et V3 avec l'équipe.

La migration **V5 temporaire** prépare les champs marque, format, catégorie et provenance utiles à la recherche complète ; elle ne charge aucun produit. Elle sera appliquée automatiquement par Flyway au prochain démarrage sur une base en V4 : coordonner son numéro avec l'équipe avant de la partager et sauvegarder une base durable avant redémarrage. La fusion souhaitée dans V4 n'est pas encore faite : toute base ayant appliqué V4 ou V5 aura besoin d'une procédure de mise en cohérence testée ; ne pas supprimer une migration appliquée ni lancer `repair` pour masquer cette divergence. Les règles de catégorisation, l'import complet et les filtres par catégorie ou type ne sont pas encore implémentés.

Pour examiner l'export CSV officiel **sans l'enregistrer ni rien importer en base**, la commande suivante lit au maximum 5 000 lignes directement en flux :

```powershell
.\gradlew.bat evaluerExportOff -PexportOff=--officiel -PlimiteOff=5000
```

Il est aussi possible d'analyser un fichier local TSV ou TSV compressé en gzip avec `-PexportOff="C:\chemin\export.csv.gz"`. La limite est le nombre de lignes lues, pas le nombre de produits acceptés. L'outil compte les fiches portant le tag `en:france`, un code numérique de 1 à 14 chiffres et un nom non vide d'au plus 255 caractères ; marque et format ne doivent pas dépasser 255 caractères. Ce sont des critères provisoires, pas une preuve de commercialisation en France. Le fichier, les données importées et les sauvegardes ne sont pas à committer sur GitHub.

Le 19 novembre 2026, la lecture des **5 000 premières lignes** de l'export a relevé 407 fiches admissibles, dont 383 ayant une URL d'image ; cet échantillon initial n'est pas représentatif du fichier entier. Le dictionnaire CSV indique `creator` (créateur de la fiche), mais pas l'auteur de chaque photo : il ne faut pas attribuer une image à `creator` par supposition. L'export est disponible en flux auprès d'[Open Food Facts](https://world.openfoodfacts.org/data) ; les recherches du catalogue utilisent uniquement la base locale.

Un importeur expérimental explicite a été testé sur une base isolée. Il ne fonctionne qu'après application de V5 et exige une URL JDBC et des identifiants transmis par les variables d'environnement `KADI_IMPORT_DB_URL`, `KADI_IMPORT_DB_USER` et `KADI_IMPORT_DB_PASSWORD` ; ne pas les committer. Depuis `back`, après configuration de ces variables et sauvegarde de la base ciblée :

```powershell
.\gradlew.bat importerExportOff -PexportOff=--officiel -PlimiteOff=5000
```

Cette commande lit au maximum les **premières** 5 000 lignes, pas 5 000 produits ; le maximum accepté actuellement est de 100 000 lignes par exécution. Elle ne permet pas de reprendre au-delà des premières lignes et ne constitue donc pas encore un import complet. En cas d'erreur, la transaction est annulée. Les codes déjà associés à une fiche non OFF ne sont pas revendiqués, les fiches OFF plus récentes ne sont pas remplacées par une ancienne version, et les catégories corrigées et photos existantes sont conservées. Les images du CSV sans auteur vérifiable ne sont pas importées. La commande affiche le nombre de fiches créées, actualisées et ignorées ; elle ne supprime pas les produits absents de l'export.

Sur une base temporaire isolée, l'import en flux des 5 000 premières lignes a créé 407 fiches, puis un second passage n'a créé aucun doublon. Aucun produit de cet essai n'a été ajouté à la base de travail. Avant un import large : prévoir reprise, contrôle d'espace, mesures de performance, stratégie de mise à jour et règles de catégorisation ; vérifier les obligations ODbL et l'attribution des images.

L'envoi utilise SMTP standard, sans fournisseur imposé. Configurer `KADI_SMTP_HOST`, `KADI_SMTP_PORT`, `KADI_SMTP_USERNAME`, `KADI_SMTP_PASSWORD`, `KADI_SMTP_AUTH`, `KADI_SMTP_STARTTLS`, `KADI_MAIL_FROM` et, si nécessaire, `KADI_VERIFICATION_LINK_BASE`. Si l'authentification SMTP est activée (par défaut), l'hôte, l'expéditeur, l'identifiant et le mot de passe sont obligatoires. Sans cette configuration, l'inscription répond `503 Service Unavailable` avant de créer le compte. Les tests utilisent un expéditeur fictif et ne valident pas la livraison réelle. La réinitialisation de mot de passe et les contrôles d'accès aux futures ressources privées restent à implémenter.

Les migrations sont normalement appliquées dans l'ordre : coordonner l'intégration de V2 et V3 avant d'appliquer V4 à une base partagée ou durable. Une base ayant déjà appliqué V4 ne doit pas être supposée appliquer automatiquement des migrations V2 ou V3 ajoutées ensuite.

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
