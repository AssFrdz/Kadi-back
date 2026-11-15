## Identification de la feature
| Version | Responsable | Algorithme(s) |
|---|---|---|
| v0.1.0 / v0.2.0 / v1.0.0 | Prénom NOM (`@compte-github`) | <nom de l'algorithme> |

## Issue associée
Closes #
<!-- L'issue est fermée automatiquement à la fusion dans la branche par défaut. -->

## Spécification de la feature
### Besoin et scénarios d'usage
<!-- Quel problème de l'utilisateur ? Pourquoi est-ce difficile à résoudre « à la main » ? -->
- *En tant que …*, je … **afin de …**

### Modèle
<!-- Entités et objets valeur, invariants (règles toujours vraies), où ils sont vérifiés et lien avec l'entité pivot. -->

### Algorithme
<!-- Principe, pseudo-code ou déroulé sur un petit exemple, complexité en temps et en mémoire,
     alternatives écartées et pourquoi, structures de données choisies. -->

### Impact sur les couches
- **BD** : migration `V<n>__<nom>.sql` (tables, contraintes)
- **Back** : module `<module>` (domain, application, adapters)
- **API** : `MÉTHODE /api/v1/...` (codes de retour)
- **Front** : <écran>

### Critères d'acceptation
- [ ] <comportement observable et testable>
- [ ] <cas d'erreur>

### Limites connues
<!-- Ce qui n'est pas traité, les simplifications assumées et les pistes pour plus tard. -->

### Description des modifications
<!-- Résumé de ce qui a été implémenté et lien vers la fiche de la feature. -->

## Checklist (dans l'ordre de la démarche)
<!-- Indiquer « sans objet » et justifier dans la description lorsqu'un point ne s'applique pas. -->
- [ ] Fiche `doc/features/...` validée par un autre membre
- [ ] Migration Flyway ajoutée si nécessaire, sans modifier une migration existante
- [ ] Domaine en Java pur, ports et services sans Spring, adaptateurs ; ArchUnit vert
- [ ] Tests : algorithme (cas limites, propriétés), services et intégration ; `./gradlew build` vert
- [ ] Endpoints REST documentés et visibles dans Swagger UI ; erreurs `problem+json`
- [ ] Front : scénario du dossier jouable de bout en bout
- [ ] Jeu de démonstration (`db/demo`) mis à jour si nécessaire

## Tests réalisés
<!-- Commandes, résultats, vérifications manuelles et éventuelles limites.
     Indiquer le nombre de tests par type, les propriétés vérifiées et la couverture du module. -->

## Impact éventuel
<!-- Migration, contrat d'API, autre module ou front concerné. -->

## Usage de l'IA sur cette PR
<!-- Outil, objet de la demande, ce qui a été repris, modifié ou rejeté. -->

## Part IA par compétence (en %)
<!-- Expliquer chaque objectif non tenu. -->
| Compétence | Part IA | Plafond |
|---|---|---|
| Idéation | % | 30 % |
| Conception | % | 30 % |
| Spécification | % | 40 % |
| Algorithmique | % | 30 % |
| Validation | % | 20 % |
| Code | % | 70 % |
| Tests | % | 60 % |
| CI/CD | % | 80 % |
| Rédaction | % | 70 % |
| Débogage | % | 70 % |
