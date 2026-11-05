# Guide de contribution Kadi

## 1. Structure des branches

```text
main
└── develop
```

- **main** : version stable, uniquement les versions livrées et taguées (`v0.1.0`, `v0.2.0`, `v1.0.0`…)
- **develop** : branche d'intégration (les features partent d'ici et y reviennent)

### Création de `develop`

> À faire une seule fois par dépôt, par une seule personne.

```bash
git switch main
git pull
git switch -c develop
git push -u origin develop
```

## 2. Prérequis

- Installer Git
- Installer Visual Studio Code
- Posséder un compte GitHub
- Être invité comme collaborateur (ou membre de la team) sur les **deux dépôts** et accepter les invitations

Authentification GitHub :

```bash
gh auth login
```

Ou utiliser un token personnel (PAT) ou une clé SSH.

## 3. Installation

### Environnement local

```bash
mkdir Kadi
cd Kadi
mkdir front
mkdir back
```

### Configuration Git

```bash
git config --global user.name "Votre Nom"
git config --global user.email "votre@email.com"
git config --global pull.rebase true
git config --global core.editor "code --wait"
```

- `pull.rebase true` : évite les merges inutiles lors des pulls
- `core.editor "code --wait"` : évite l'ouverture de Vim par défaut

### Clonage des dépôts

Frontend :

```bash
cd front
git clone https://github.com/AssFrdz/Kadi-front.git
cd ..
```

Backend :

```bash
cd back
git clone https://github.com/AssFrdz/Kadi-back.git
cd ..
```

### Arborescence obtenue

```text
Kadi/
├── front/
│   └── Kadi-front/
└── back/
    └── Kadi-back/
```

Ensuite : ouvrir les projets dans VS Code et installer les dépendances (ex. `npm install`). Les instructions spécifiques sont dans le `README.md` de chaque dépôt.

Pour remonter d'un dossier :

```bash
cd ..
```

## 4. Créer une feature

### Étape 1 : créer une issue

Créer une Issue GitHub à partir du modèle **Feature** et noter son numéro (ex. `#12`).

### Étape 2 : partir d'un `develop` à jour

```bash
git switch develop
git pull --rebase origin develop
git switch -c feature/<n°issue>-<nom-court>
```

Exemple :

```bash
git switch -c feature/12-account-creation
```

**Règles de nommage** : numéro d'issue réel, nom explicite, kebab-case, pas de tiret final.

| ✅ Correct | ❌ Incorrect |
|---|---|
| `feature/12-account-creation` | `feature/test` |
| `feature/18-authentication-api` | `feature/account_creation` |
| | `feature/12-account-` |

### Étape 3 : développer et commiter régulièrement

```bash
git status
git add .
git commit -m "feat(database): account creation"
```

#### Convention de commits

Format : `type(scope): description`

| Type | Description |
|---|---|
| `feat` | Nouvelle fonctionnalité |
| `fix` | Correction de bug |
| `docs` | Documentation |
| `refactor` | Refactoring |
| `test` | Ajout ou modification de tests |
| `chore` | Maintenance technique |

```bash
git commit -m "feat(auth): login endpoint"
git commit -m "fix(api): validation error"
git commit -m "docs(readme): update installation"
```

#### Fichiers à ne jamais commiter

Vérifier qu'ils sont dans `.gitignore` :

```text
.env
node_modules/
dist/
```

> ⚠️ Ne jamais commiter de secrets, clés API ou mots de passe.

### Étape 4 : se synchroniser avec `develop`

```bash
git fetch origin
git merge origin/develop
```

#### Gestion des conflits

Git peut afficher des marqueurs comme `<<<<<<< HEAD`, `=======` et `>>>>>>> develop`.

Procédure :

1. Ouvrir le fichier concerné.
2. Choisir le contenu à conserver.
3. Supprimer les marqueurs Git.
4. Sauvegarder.

Puis :

```bash
git add <fichier>
git commit
```

#### Si Vim apparaît

- Enregistrer : `Échap`, puis `:wq`, puis `Entrée`
- Quitter sans enregistrer : `Échap`, puis `:q!`, puis `Entrée`
- Éviter `:wq!`

#### Annuler un merge

```bash
git merge --abort
```

### Vérifications avant push

- Lancer l'application et vérifier que tout fonctionne.
- Exécuter les tests.

### Push de la branche

```bash
git push -u origin feature/<n°issue>-<nom-court>
```

Exemple :

```bash
git push -u origin feature/12-account-creation
```

### Création de la Pull Request

- Créer une PR vers **`develop`** — ⚠️ jamais vers `main`.
- Cliquer sur **Compare & Pull Request**.
- Dans la description, ajouter `Closes #12` (ferme l'issue après fusion).
- Demander au minimum un relecteur.

**Modèle de PR** : description des modifications, tests réalisés, impact éventuel, usage de l'IA, répartition de la part IA par compétence.

Une PR reste modifiable tant qu'elle n'est pas fusionnée : il suffit de pousser de nouveaux commits sur la même branche.

## 5. Revue de code

Avant de relire une PR :

```bash
git switch develop
git pull --rebase origin develop
git fetch
```

### Bonnes pratiques

- Ne jamais approuver sa propre PR.
- Vérifier : qualité du code, tests, respect des conventions, absence de secrets.

### Après le merge

Suppression de la branche distante :

```bash
git push origin --delete <branche>
```

Suppression locale :

```bash
git branch -d <branche>
```

## 6. Règles GitHub à mettre en place

**Settings → Branches → Branch protection rules** (pour `main` et `develop`)

**Exiger :**

- Une Pull Request avant fusion
- Au moins 1 approbation
- Rejet des approbations obsolètes
- Branche à jour avant fusion
- Validation des checks CI

**Interdire :**

- Les pushes directs
- Les force-pushes

### Organisation GitHub

- Créer une Team GitHub ayant accès aux dépôts Front et Back.
- Ajouter un fichier `CODEOWNERS` pour définir automatiquement les relecteurs.

### Templates GitHub

```text
.github/
├── PULL_REQUEST_TEMPLATE.md
└── ISSUE_TEMPLATE/
    ├── feature.yml
    └── bug.yml
```

Ajouter également `CONTRIBUTING.md`.

### Gestion des releases

La fusion `develop → main` n'a lieu que lors d'une release.

## 7. Commandes utiles

| Action | Commande |
|---|---|
| État du dépôt | `git status` |
| Historique simplifié | `git log --oneline` |
| Mettre de côté | `git stash` |
| Restaurer | `git stash pop` |
| Lister les branches | `git branch -a` |
| Branche courante | `git branch` |
| Récupérer les références | `git fetch` |
| Changer de branche | `git switch <nom-branche>` |
| Supprimer branche locale | `git branch -d <nom-branche>` |
| Supprimer branche distante | `git push origin --delete <nom-branche>` |

## Workflow résumé

```text
Issue
  ↓
feature/xx-nom-feature
  ↓
Développement
  ↓
Commits réguliers
  ↓
Synchronisation avec develop
  ↓
Push
  ↓
Pull Request → develop
  ↓
Code Review
  ↓
Merge dans develop
  ↓
Release
  ↓
Merge develop → main
  ↓
Tag vX.Y.Z
```

Cette organisation garantit un historique Git propre, des revues systématiques et un processus de livraison maîtrisé pour le projet Kadi.
