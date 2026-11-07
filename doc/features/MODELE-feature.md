# F<n> — <Nom de la fonctionnalité>

| Information | Valeur |
|---|---|
| Version prévue | <v0.1.0 / v0.2.0 / v1.0.0> |
| Responsable | <Prénom NOM — @compte GitHub> |
| Issue | <#numéro et lien> |
| Algorithme | <Nom précis de l'algorithme> |
| Statut | Idéation / Back / Tests / API / Front / Terminée |

> Une feature est portée par une seule personne, comprend un algorithme non trivial et touche le schéma de données, le back, l'API REST et le front. Compléter et faire relire cette fiche pendant l'idéation, avant de commencer le back.

## 1. Idéation

### Besoin

<Quel problème concret l'utilisateur rencontre-t-il ? Pourquoi une simple consultation ou gestion de données ne suffit-elle pas ?>

### Scénario d'usage

> En tant que <type d'utilisateur>, je veux <action ou objectif>, afin de <valeur apportée>.

**Déroulement :** <Décrire les étapes utiles et le résultat observé par l'utilisateur.>

### Algorithme retenu

**Nom :** <Nom reconnu de l'algorithme ou description précise de la méthode.>

**Principe :**

<Expliquer les étapes de l'algorithme. Ajouter un petit exemple d'entrée, le déroulement et le résultat attendu. Le cœur de l'algorithme sera implémenté dans une classe Java pure, indépendante du framework.>

**Complexité :**

- Temps : <O(...)>, avec <définition des paramètres utilisés>.
- Mémoire : <O(...)>, avec <définition des paramètres utilisés>.

**Structures de données utilisées :** <Les nommer et expliquer leur rôle.>

**Alternatives étudiées et raisons du choix :**

| Alternative | Avantage | Limite ou raison de l'écarter |
|---|---|---|
| <Option> | <Avantage> | <Raison> |

### Modèle de données et invariants

<Décrire les données créées ou modifiées, leurs relations et leur lien avec le modèle commun de Kadi.>

| Donnée / entité | Champs utiles et types | Règles de validité |
|---|---|---|
| <Nom> | <Champs> | <Contraintes> |

**Invariants :** <Règles qui doivent rester vraies dans toutes les situations. Préciser où elles sont garanties.>

### Impact prévu sur les quatre couches

- **Schéma / BD :** <Tables, colonnes, relations, contraintes et migration prévue `V<n>__<nom>.sql`.>
- **Back :** <Modèle, accès aux données et service concernés ; emplacement de l'algorithme dans une classe Java pure.>
- **API REST :** <Endpoints envisagés sous `/api/v1`, données reçues/retournées et codes HTTP attendus.>
- **Front :** <Écran ou interaction qui consommera l'API, sans accès direct à la BD.>

### Critères d'acceptation

Écrire des résultats observables et vérifiables. Chaque critère devra être couvert par au moins un test adapté.

- [ ] **CA1 — <Nom court> :** Étant donné <contexte>, lorsque <action>, alors <résultat vérifiable>.
- [ ] **CA2 — <Cas limite> :** <Résultat attendu pour une entrée vide, absente, minimale ou maximale pertinente.>
- [ ] **CA3 — <Erreur / donnée invalide> :** <Comportement attendu et, pour l'API, code HTTP.>

**Critères concernant les propriétés ou invariants de l'algorithme :**

- [ ] <Ex. le résultat respecte toujours les contraintes définies.>
- [ ] <Ex. le résultat est déterministe pour des entrées identiques.>

### Limites et hypothèses

- **Hypothèses :** <Ce que la feature suppose sur les données, sources ou entrées.>
- **Limites connues :** <Ce que cette version ne traite pas.>
- **Dépendances ou prérequis :** <Données ou fonctions déjà disponibles ; sinon « aucun ».>

## 2. Validation de l'idéation

- [ ] Le besoin et le scénario sont compréhensibles.
- [ ] L'algorithme est identifié, expliqué et non trivial.
- [ ] La complexité en temps et en mémoire est justifiée.
- [ ] Les alternatives et les limites sont explicitées.
- [ ] Le modèle, les invariants et l'impact sur les quatre couches sont décrits.
- [ ] Les critères d'acceptation sont vérifiables et couvrent les cas limites utiles.
- [ ] La fiche est relue et validée par un autre membre de l'équipe.

**Relecteur ou relectrice :** <Nom>
**Date et remarques :** <À compléter>

> Ne pas commencer l'étape suivante avant la validation de cette étape.

## 3. Back

À compléter après validation de l'idéation.

- **Migration ajoutée :** `<V<n>__<nom>.sql>` / <à préciser>
- **Éléments du modèle et de persistance :** <À préciser>
- **Service / cas d'usage :** <À préciser>
- **Classe Java pure contenant l'algorithme :** <Nom complet de la classe>
- **Validation :** <Build et contrôles effectués, résultat>

> Une migration déjà poussée ne se modifie pas : toute évolution du schéma fait l'objet d'une nouvelle migration.

## 4. Tests

### Tests unitaires de l'algorithme

| Cas | Entrée | Résultat attendu / propriété vérifiée | Test |
|---|---|---|---|
| Nominal | <À préciser> | <À préciser> | <Nom du test> |
| Limite | <À préciser> | <À préciser> | <Nom du test> |
| Invalide ou absent | <À préciser> | <À préciser> | <Nom du test> |
| Invariant | <À préciser> | <À préciser> | <Nom du test> |

### Autres tests prévus

- **Service / logique applicative :** <Cas avec dépôts simulés ou autres dépendances contrôlées.>
- **Persistance / migration :** <Vérifications utiles.>
- **API :** <Requêtes, réponses, validations et erreurs HTTP à tester.>
- **Validation de l'algorithme :** <Comparaison à une solution de référence, propriété ou autre preuve empirique, si pertinent.>

**Résultat des tests :** <Commande exécutée, résultat et date.>

> L'étape est terminée lorsque les tests unitaires de l'algorithme couvrent les cas nominaux, les cas limites et les invariants pertinents, et qu'ils réussissent.

## 5. API REST

| Méthode et endpoint | Entrée | Résultat attendu | Codes HTTP / erreurs |
|---|---|---|---|
| `<GET/POST/...> /api/v1/<ressource>` | <À préciser> | <À préciser> | <200, 201, 400, 404, ...> |

- **Documentation OpenAPI / Swagger :** <Lien ou emplacement>
- **Format des erreurs :** <Format structuré utilisé par le projet>
- **Vérifications API réalisées :** <Tests et résultat>

> Ne pas commencer le front avant que le back soit testé et l'API exposée et documentée.

## 6. Front

- **Écran / parcours :** <À préciser>
- **Données consommées depuis l'API :** <Endpoints concernés>
- **États et erreurs présentés à l'utilisateur :** <Chargement, absence de résultat, erreur, etc.>
- **Vérification du scénario complet :** <Étapes et résultat>

Le front consomme uniquement l'API REST et n'accède jamais directement à la base de données.

## 7. Bilan de la feature

- **État final :** <Terminée / Partielle / Non livrée>
- **Version effectivement livrée :** <Version ou « non livrée »>
- **Écarts par rapport à l'idéation :** <Aucun ou description et justification>
- **Tests et contrôles finaux :** <Commandes exécutées et résultat>
- **Pull request :** <Lien>
- **Limites restantes / suite envisagée :** <À préciser>

### Usage de l'IA

<Indiquer l'outil utilisé et l'aide demandée, ainsi que les éléments repris, modifiés ou rejetés. Si l'usage de l'IA est déclaré dans la pull request ou dans un journal d'équipe, ajouter le lien et suivre le protocole du projet.>
