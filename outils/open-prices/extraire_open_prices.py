"""Extrait les prix français d'Open Prices dans un fichier TSV lu par l'import Java (profil Spring « import »).

Ce script ne fait que choisir et aplatir des colonnes : toutes les règles métier
(normalisation des enseignes, traduction des preuves, prix invalides) sont dans l'import Java, où elles sont testées.

Données : https://huggingface.co/datasets/openfoodfacts/open-prices (licence ODbL : citer Open Prices).
"""

import argparse
import pathlib
import urllib.request

import pandas as pd
import pyarrow.parquet as pq

URL_EXPORT = "https://huggingface.co/datasets/openfoodfacts/open-prices/resolve/main/prices.parquet"
DOSSIER = pathlib.Path(__file__).parent / "donnees"

COLONNES = [
    "id", "product_code", "product_product_name", "product_name", "price", "price_per",
    "price_is_discounted", "date", "owner", "proof_type", "location_osm_type", "location_osm_id",
    "location_osm_name", "location_osm_brand", "location_osm_address_country_code",
    "currency", "type", "duplicate_of",
]

ENTETE = [
    "id", "code_barres", "libelle", "prix", "prix_par", "en_promotion", "date", "contributeur",
    "type_preuve", "osm_type", "osm_id", "magasin", "marque",
]


def absent(valeur):
    return valeur is None or (not isinstance(valeur, str) and pd.isna(valeur))


def texte(valeur):
    """Une cellule TSV ne contient ni tabulation ni retour à la ligne ; une valeur absente est vide."""
    if absent(valeur):
        return ""
    return " ".join(str(valeur).split())


def entier(valeur):
    """Les identifiants OpenStreetMap arrivent parfois en flottant (4966187139.0)."""
    return "" if absent(valeur) else str(int(valeur))


def premier(*valeurs):
    return next((v for v in valeurs if not absent(v) and str(v).strip()), None)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--parquet", type=pathlib.Path, default=DOSSIER / "prices.parquet",
                        help="export Open Prices déjà téléchargé (sinon il est téléchargé ici)")
    parser.add_argument("--sortie", type=pathlib.Path, default=DOSSIER / "releves-fr.tsv")
    args = parser.parse_args()

    if not args.parquet.exists():
        args.parquet.parent.mkdir(parents=True, exist_ok=True)
        print(f"Téléchargement de l'export Open Prices vers {args.parquet}…")
        urllib.request.urlretrieve(URL_EXPORT, args.parquet)

    table = pq.read_table(args.parquet, columns=COLONNES).to_pandas()
    france = table[
        (table.location_osm_address_country_code.str.upper() == "FR")
        & (table.currency == "EUR")
        & (table.type == "PRODUCT")
        & table.product_code.notna()
        & table.duplicate_of.isna()
    ]

    args.sortie.parent.mkdir(parents=True, exist_ok=True)
    # TSV écrit à la main : texte() a retiré tabulations et retours à la ligne, aucun échappement n'est nécessaire
    with args.sortie.open("w", encoding="utf-8", newline="\n") as fichier:
        fichier.write("\t".join(ENTETE) + "\n")
        for ligne in france.itertuples(index=False):
            cellules = [
                entier(ligne.id),
                texte(ligne.product_code),
                texte(premier(ligne.product_product_name, ligne.product_name)),
                texte(ligne.price),
                texte(ligne.price_per),
                "true" if not absent(ligne.price_is_discounted) and bool(ligne.price_is_discounted) else "false",
                texte(ligne.date),
                texte(ligne.owner),
                texte(ligne.proof_type),
                texte(ligne.location_osm_type),
                entier(ligne.location_osm_id),
                texte(ligne.location_osm_name),
                texte(ligne.location_osm_brand),
            ]
            fichier.write("\t".join(cellules) + "\n")
    print(f"{len(france)} relevés français écrits dans {args.sortie}")


if __name__ == "__main__":
    main()
