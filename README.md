# Kadi — Dossier de lancement

> **Rendu R1 — version à compléter**
> Ce modèle sert à préparer le dossier de lancement de Kadi. Remplacez les éléments entre crochets et supprimez les consignes avant le rendu. Le document final doit tenir en **4 pages** et être fourni en **Markdown et PDF**.

## 1. Équipe et comptes développeurs

Nous sommes 3 étudiants en Master Miage Apprentissage. Voici nos informations : 

| Nom et prénom | Compte développeur (GitHub/GitLab) |
|---|---|
| Amal Benatallah | Amalbenatallah | 
| Adam Bendjebbar | AdamBendjebbar | 
| Assia Ferradj | AssFrdz | 

## 2. Présentation du projet et caractère innovant

### Besoin

#### Le problème des ménages
L’alimentation pèse lourd dans le budget, et les ménages perdent de l’argent sans le voir. Ce n’est pas parce qu’ils ne comparent pas, c’est parce que les pièges sont invisibles :
un paquet passe de 200 g à 180 g au même prix (la « réduflation ») ;
une promotion affiche « −30 % » sur un prix de référence douteux ;
le magasin le moins cher sur un produit ne l’est pas sur tout le panier ;
Un détour de 8 km pour économiser 3 € coûte en réalité plus cher qu’il ne rapporte.
Les comparateurs existants répondent à la question « où est-ce le moins cher ? ». Aucun ne répond à la vraie question des ménages : « est-ce que je me fais avoir, et qu’est-ce que je dois faire concrètement cette semaine ? »

#### Ce qui rend Kadi innovant

| Ce qui existe | Ce que Kadi apporte en plus |
|---|---|
| Comparer le prix d’un produit | Suivre son prix au kilo ou au litre dans le temps afin de repérer une hausse du prix unitaire, notamment lorsqu’elle est liée à une réduction de quantité (réduflation). |
| Afficher les promotions | Mettre le prix de référence annoncé en regard de la règle française applicable aux annonces de réduction — le prix le plus bas pratiqué au cours des 30 jours précédents — et de l’historique des prix effectivement observés par Kadi. Cet historique permet d’éclairer l’offre, sans prétendre remplacer les données du vendeur ni établir à lui seul une infraction. |
| Comparer des paniers entre enseignes | Optimiser le panier selon des contraintes concrètes : nombre de magasins, disponibilité d’un véhicule, coût et durée des trajets. Kadi estime l’économie nette en tenant compte des déplacements, plutôt que de comparer uniquement les prix des articles. |
| Suivre un budget saisi manuellement | Estimer l’évolution des prix à partir des achats et des produits suivis par le ménage, pour décrire son inflation personnelle plutôt que de lui appliquer directement une moyenne nationale. |
| Proposer un produit moins cher | Présenter des alternatives en expliquant les différences connues, par exemple la quantité, le prix unitaire ou la composition, afin que l’utilisateur puisse juger si elles lui conviennent. |

> **Proposition de valeur :** Kadi ne dit pas seulement où c’est moins cher ; il aide à repérer ce qui mérite vérification et à décider quoi faire concrètement.


## 3. Scénarios d'usage prévus

### Scénario 1 : Sonia, mère seule avec deux enfants, 380 € de courses par mois.

Le 20 du mois, il lui reste 90 €. Elle saisit sa liste de la semaine. Kadi lui indique qu’en l’achetant entièrement dans le magasin à 600 m de chez elle, elle dépasse de 12 €. Il lui propose deux remplacements (des pâtes et des céréales d’une autre marque, au même grammage) qui la ramènent sous son budget, sans l’envoyer à l’autre bout de la ville.

**Valeur :** respecter son budget sans changer de magasin.

### Scénario 2 : Marc, retraité sans voiture.

Il ne peut pas faire trois magasins. Kadi applique la contrainte « un seul magasin, accessible à pied » et lui dit lequel des deux supermarchés de son quartier est le moins cher pour ses achats habituels, et pas pour un panier type national.

**Valeur :** une réponse adaptée à sa vie, et pas à une moyenne.

### Scénario 3 : la promotion trompeuse.

Léa voit une lessive à « −25 % ». Elle la scanne. Kadi lui montre que le prix promotionnel est encore supérieur au prix habituel de la même lessive dans l’enseigne voisine, et que le prix de référence affiché ne correspond pas aux prix relevés le mois précédent.

**Valeur :** ne plus acheter une fausse bonne affaire.

### Scénario 4 : la réduflation.

Kadi envoie une alerte : « Vos biscuits habituels sont passés de 200 g à 180 g. Le prix affiché n’a pas bougé, mais le prix au kilo a augmenté de 11 %. » Il propose un équivalent.

**Valeur :** rendre visible une hausse cachée.

### Scénario 5 : le couple qui fait ses courses en drive.

Leur panier de 40 articles coûte 112 € dans un drive et 98 € en le répartissant entre deux drives. Kadi précise que la répartition fait gagner 14 €, mais ajoute 20 minutes de trajet. Ils décident en connaissance de cause.

**Valeur :** un vrai arbitrage entre argent et temps, et pas une économie théorique.

### Scénario 6 : l’étudiant qui veut comprendre où part son argent.

Il photographie ses tickets de caisse pendant un mois. Kadi lui montre que ses prix ont augmenté de 6 % sur un an, principalement à cause des produits laitiers, alors que l’inflation nationale est plus basse.

**Valeur :** comprendre sa propre inflation pour agir dessus.

## 4. Architecture et frameworks envisagés

Kadi adopte une architecture en trois tiers : **front → API REST → back → base de données**. Le front communique avec le back par l'API et n'accède pas directement à la base de données.

```text
Front : Angular + Typescript
        │ requêtes HTTP
        ▼
API REST : OpenAPI
        │
        ▼
Back : Java + Spring Boot
        │
        ▼
Base de données : PostGre SQL
```

- **Front-end :** React
- **API REST :** 
- **Back-end :** Java, avec [framework envisagé]. Les algorithmes métier seront testables indépendamment du framework.
- **Base de données :** [moteur envisagé]. Les évolutions du schéma seront gérées par des migrations versionnées, avec [outil envisagé : Flyway, Liquibase ou autre].
- **Tests et intégration continue :** [outils et vérifications envisagés]

## 5. Protocole d'usage de l'intelligence artificielle

Nous comptons utiliser l'IA comme un interlocuteur privilégié pour challenger une idée, comparer des choix d'architecture, demander un avis sur le choix d'un algorithme plutôt qu'un autre ou encore suggérer des cas de tests.

Nous décidons des fonctionnalités, de l'architecture et nous retenons nos solutions.

Nous vérifions les propositions avec le code, les tests et des sources fiables ; une réponse de l’IA doit toujours être vérifiée.

Nous prêtons attention à comprendre ce que l'IA nous propose.


## 6. Dépôts du projet

- **Dépôt du back :** https://github.com/AssFrdz/Kadi-back.git
- **Dépôt du front :** https://github.com/AssFrdz/Kadi-front.git

