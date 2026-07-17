# Import terminologique LOINC / SNOMED CT (CIV)

Infrastructure permettant d'affecter des codes terminologiques standards (LOINC,
SNOMED CT) aux entités OpenELIS, pour un FHIR réellement interopérable. État :
**mécanisme livré** ; **contenu des codes à valider/compléter** par le métier.

## Pourquoi

À l'audit : sur ce déploiement, ~22/351 tests portaient un LOINC, 0/2675 entrées
de dictionnaire, 0/178 types d'observation. Coder ~3000 items à la main serait
long et dangereux (un mauvais code est pire que pas de code). D'où une
**infrastructure d'import CSV** : codes préremplis par recherche, **validés par
un biologiste**, puis importés en dry-run + apply idempotents.

## Schéma (colonnes de codage)

Migrations idempotentes (3.2.x.x), matching par nom/clé, jamais par ID :

- `test.snomed_code` (LOINC déjà présent : `test.loinc`) —
  `add_snomed_code_to_test.xml`
- `dictionary.loinc_code` + `dictionary.snomed_code` —
  `add_snomed_code_to_dictionary.xml` (loinc_code préexistait)
- `observation_history_type.loinc_code` + `snomed_code` —
  `add_codes_to_observation_history_type.xml`

Entités/hbm mis à jour : `Test.snomedCode`, `Dictionary.snomedCode`,
`ObservationHistoryType.loincCode/snomedCode`.

## Modèles CSV

Sous `src/main/resources/terminology/` (voir le README de ce dossier pour le
format détaillé). Trois fichiers, séparateur `;`, colonne `status`
(proposed|validated) :

- `test_codes.csv` — clé `test_name` (=description) + `sample_type`. Reprend les
  LOINC existants (status=validated), le reste `proposed`.
- `dictionary_codes.csv` — clé `category` + `dict_entry`. Cible antibiotiques +
  levures (la catégorie `Bacteria`, 1545 organismes, est à ajouter par lots
  validés).
- `observation_history_type_codes.csv` — clé `type_name`. Concepts cliniques
  (CD4, aidsStage…).

**Les codes préremplis sont indicatifs** et doivent être validés (passer
`status` à `validated`) avant application.

## Mécanisme d'import

Service `dataexchange.terminology.service.TerminologyImportService` :

- `preview(target, csv)` — dry-run, **n'écrit rien**, renvoie un rapport
  détaillé.
- `apply(target, csv)` — applique en base, **idempotent** (rejouable).

Règles :

- Matching par **clé naturelle** (test: description ; dictionary:
  catégorie+entrée ; obs type: type_name). 0 correspondance → NOT_FOUND ; >1 →
  AMBIGUOUS. Le dict_entry est comparé en normalisant les blancs (certaines
  entrées en base ont un saut de ligne interne).
- Seules les lignes `status=validated` avec au moins un code non vide sont
  appliquées (SKIPPED_PROPOSED / SKIPPED_NO_CODE sinon).
- Une cellule de code vide **n'écrase jamais** une valeur existante.
- **Arbitrage des conflits** : si un code du CSV **diffère** d'une valeur déjà
  présente en base, la ligne sort en **CONFLICT** et n'est **pas** appliquée par
  défaut (le message indique ancienne → nouvelle valeur). Pour forcer
  l'écrasement, activer l'option **overwrite** (case à cocher dans l'UI,
  `?overwrite=true` en API) : les conflits deviennent alors des mises à jour. Un
  code CSV identique à l'existant sort en **NO_CHANGE**. L'application d'une
  ligne est atomique : un conflit non autorisé bloque toute la ligne (pas
  d'écriture partielle loinc/snomed).
- Chaque ligne est traitée en try/catch : une ligne en erreur (ERROR) n'arrête
  pas le lot.

Endpoints REST (`/rest/terminology-import`, authentifiés comme le reste de
`/rest/**`) :

- `GET /targets`
- `POST /preview?target=TEST|DICTIONARY|OBSERVATION_HISTORY_TYPE` (body = CSV,
  text/csv)
- `POST /apply?target=...` (body = CSV)

UI : page admin **Import terminologique** (`#TerminologyImport`) — choix de la
cible, upload/collage du CSV, **Prévisualiser (dry-run)** puis **Appliquer**
(avec confirmation), rapport tabulé paginé.

## Câblage FHIR

Les codes alimentent automatiquement les CodeableConcept FHIR :

- Test : `transformTestToCodeableConcept` émet LOINC + SNOMED (+ coding OE
  local).
- Dictionnaire : `buildDictionaryCodeableConcept` émet LOINC + SNOMED (utilisé
  par les organismes, antibiotiques, valeurs de résultat).
- ObservationHistory : à câbler là où les observations cliniques deviennent des
  ressources FHIR (extension future).

## Sécurité

Les endpoints modifient des données de référence : ils sont sous `/rest/**`
(authentifié). L'accès effectif passe par la page admin (menu réservé aux
administrateurs), cohérent avec les autres controllers admin (ex.
`/rest/fhir-sync`). Un durcissement par rôle explicite (`@PreAuthorize`) pourra
être ajouté si la politique de sécurité l'exige.

## Workflow recommandé

1. Exporter/compléter le CSV de la cible (les modèles sont un point de départ).
2. Faire **valider les codes par un biologiste** (passer `status` à
   `validated`).
3. Page admin → cible → coller/charger le CSV → **Prévisualiser** (vérifier les
   NOT_FOUND/AMBIGUOUS) → **Appliquer**.
4. Rejouer le FHIR (validation ou rejeu par date) pour propager les nouveaux
   codes.
