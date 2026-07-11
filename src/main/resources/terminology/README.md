# Import terminologique (LOINC / SNOMED CT)

Ce dossier contient les **modèles CSV** d'affectation de codes terminologiques
standards (LOINC, SNOMED CT) aux entités OpenELIS. Ils alimentent les colonnes
de codage utilisées par le mapping FHIR, pour l'interopérabilité.

## Principe

1. Les CSV sont **préremplis** avec des codes proposés (colonne `status` =
   `proposed`), à partir de recherches terminologiques.
2. Un **biologiste valide** chaque ligne : il corrige le code si besoin et passe
   `status` à `validated`. Une cellule de code vide = pas de code (ligne
   ignorée).
3. L'import (page admin _Import terminologique_) propose un **dry-run**
   (prévisualisation : lignes à mettre à jour / introuvables / ignorées) puis
   **Appliquer**. L'opération est **idempotente** (rejouable) et **matche par
   clé naturelle** (jamais par ID) : robuste entre environnements.
4. Seules les lignes `status=validated` avec au moins un code non vide sont
   appliquées. `status=proposed` est ignoré à l'application (mais visible en
   dry-run).

## Format commun

- Séparateur : `;` (point-virgule). Encodage UTF-8. En-tête obligatoire.
- Colonnes de code : `loinc`, `snomed` (l'une ou les deux, selon le fichier).
- Colonne `status` : `proposed` | `validated`. Défaut `proposed` si
  absente/vide.
- Une cellule de code vide n'écrase jamais une valeur existante (pas de
  suppression via import ; l'effacement se fait manuellement en base si
  nécessaire).
- Les lignes commençant par `#` sont des commentaires, ignorées.

## Fichiers

### `test_codes.csv` — codage des tests (LOINC + SNOMED)

Clé : `test_name` + `sample_type` (un test peut exister pour plusieurs types
d'échantillon ; le nom seul serait ambigu). `sample_type` vide = matche le test
quel que soit le type.

```
test_name;sample_type;loinc;snomed;status
Viral Load;;25836-8;;validated
Amylase(Serum);Serum;1798-8;;proposed
```

### `dictionary_codes.csv` — codage des entrées de dictionnaire (LOINC + SNOMED)

Clé : `category` (nom de la catégorie) + `dict_entry`. Utile surtout pour les
organismes (catégorie `Bacteria`, `Yeasts`) et les antibiotiques
(`Bacteriology Antibiotics`, `Therapeutic Antibiotics`).

```
category;dict_entry;loinc;snomed;status
Bacteria;Escherichia coli;;112283007;proposed
Bacteriology Antibiotics;Amoxicillin;;372687004;proposed
```

### `observation_history_type_codes.csv` — codage des types d'observation clinique

Clé : `type_name`. Concepts cliniques (aidsStage, cd4Count…) : LOINC pour les
observables quantifiables, SNOMED pour les concepts.

```
type_name;loinc;snomed;status
cd4Count;24467-3;;proposed
aidsStage;;62479008;proposed
```

## Rappels

- **Ne jamais inventer un code.** En cas de doute, laisser vide et
  `status=proposed` : le biologiste tranchera. Un mauvais code est pire que pas
  de code.
- Les codes proposés dans ces modèles sont **indicatifs** et doivent être
  validés avant application en production.
