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
