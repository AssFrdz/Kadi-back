# Import des prix Open Prices (France)

Remplit la base locale avec les prix français d'[Open Prices](https://prices.openfoodfacts.org) (licence ODbL : la source doit être citée dans l'application).

## 1. Extraire les prix français (Python)

```bash
cd outils/open-prices
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/python extraire_open_prices.py
```

Le script télécharge l'export (environ 80 Mo) dans `donnees/` puis écrit `donnees/releves-fr.tsv`. Le dossier `donnees/` n'est pas versionné.

## 2. Importer dans PostgreSQL (Java)

Depuis la racine du dépôt, la base lancée (`docker compose up -d`) :

```bash
./gradlew bootRun --args='--spring.profiles.active=import'
```

L'application démarre sans serveur web, importe, affiche un bilan puis s'arrête. L'import est relançable : un relevé déjà importé n'est jamais ajouté deux fois (`source` + `reference_externe`).

Les règles appliquées (normalisation des enseignes, traduction des preuves, prix refusés) sont dans `fr.miage.kadi.prix.adapter.in.importation.RegleImportOpenPrices`.
