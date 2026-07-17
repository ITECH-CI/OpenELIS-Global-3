# Mapping FHIR de la bactériologie classique (CIV)

État : **livré** sur `feature/fhir-foundation-civ`. Ce document décrit le
mapping produit, ses invariants, et les points en attente de validation métier.

## Contexte

La bactériologie classique ne stocke pas ses résultats dans la table `result`
(vue par `transformResultToObservation`) mais dans des tables dédiées :
`bacteriology_organism`, `bacteriology_antibiogram`,
`bacteriology_flora`(+`_detail`), via `bacteriology_result_group` (hiérarchie
CULTURE → ORGANISM → ANTIBIOGRAM). Ces données étaient donc **absentes du
FHIR**. Le mapping les expose désormais.

Point d'entrée :
`FhirTransformServiceImpl.buildBacteriologyObservations(analysis, cultureAnchor)`,
appelé dans `transformPersistObjectsUnderSamples` pour chaque analyse Finalized.
La validation bactério
(`BacteriologyResultController.validateBacteriologyResults`) et le rejeu par
date déclenchent ce chemin.

## Ressources produites

Toutes sont des `Observation` rattachées au `DiagnosticReport.result` de
l'analyse culture, avec `specimen`, `subject`, `effective`/`issued` (date de
release).

### 1. Isolat / organisme identifié (une par `bacteriology_organism`)

- `code` : LOINC **634-6** (_Bacteria identified_)
- `value[x]` : CodeableConcept de l'organisme (dictionnaire → LOINC/SNOMED
  référentiel si présents ; sinon texte libre saisi)
- `derivedFrom` → l'Observation « culture » (1er `Result` de l'analyse) ou, à
  défaut, le `ServiceRequest` de l'analyse
- `basedOn` → ServiceRequest de l'analyse
- `component` : `organism_type` (BACTERIA/YEAST), `capsule_presence` (booléen)

### 2. Sensibilité / antibiogramme (une par `bacteriology_antibiogram`)

- `code` : antibiotique (dictionnaire) + LOINC **18769-0** (_Microbial
  susceptibility_)
- `value[x]` : S/I/R via
  `http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation`
  (Susceptible / Intermediate / Resistant)
- `derivedFrom` → l'Observation isolat correspondante
- `component` : `27196-9` diamètre d'inhibition (mm), `20578-1` CMI
  (`micValue`), ajoutés **seulement si renseignés**

### 3. Nombre de flore (une par `bacteriology_flora`)

- `code` : le test « nombre de flore » associé
  (`transformTestToCodeableConcept`)
- `value[x]` : le compte (`valueQuantity` si numérique, sinon `valueString`)
- `derivedFrom` → la culture
- `component` par `bacteriology_flora_detail` : `gram_type`, `grouping_mode`,
  `other_characteristic` (chacun résolu via dictionnaire, ajouté si renseigné)

La flore est indépendante des organismes : une culture peut n'avoir que de la
flore (ex. flore polymicrobienne sans identification).

## Invariants

- **Idempotence** : les entités bactério n'ont pas de colonne `fhir_uuid`. L'id
  FHIR est déterministe :
  `UUID.nameUUIDFromBytes(analysisFhirUuid + "/" + table + "/" + id)` (UUID v3).
  Un rejeu réécrit les mêmes ids (PUT), sans doublon.
- **Neutralité** : une analyse sans organisme ni flore ne produit aucune
  Observation supplémentaire (aucune dépendance à un nom de section).
- **Non bloquant** : le déclenchement à la validation est enveloppé
  (recordPending → markSuccess/markFailed), une erreur FHIR ne casse pas la
  validation métier.

## Codage LOINC / SNOMED

- LOINC : émis quand le dictionnaire porte `loinc_code`, plus les LOINC
  génériques fixes ci-dessus.
- SNOMED : nouvelle colonne `dictionary.snomed_code` (migration
  `add_snomed_code_to_dictionary.xml`). Quand renseignée,
  `buildDictionaryCodeableConcept` émet un coding `http://snomed.info/sct`. **À
  alimenter** dans les dictionnaires référentiels (organismes, antibiotiques) —
  la colonne existe, la saisie/import reste à faire.
- Un coding OE local (`.../dictionary_entry`) est toujours présent en repli.

## Points en attente de validation métier (NE PAS modifier sans accord)

1. **Display de l'antibiotique** : la value display reprend le `dictEntry` brut
   (ex. `"Fusidic acid 10μg-Steroidals"`,
   `"Amoxicillin/Clavulanic acid 20/10μg-Beta-lactam+Inhibitors"`), qui inclut
   le dosage et la classe. Un libellé court serait plus lisible côté
   consommateur FHIR, mais ce choix touche au métier (référentiel antibiotiques)
   et nécessite une validation avant changement. **Statu quo pour l'instant.**
2. **Alimentation SNOMED** : décider de la source (import terminologique vs
   saisie admin) et du périmètre (organismes prioritaires, antibiotiques).

## Inspection locale (dev)

Le HAPI local expose un connecteur HTTP en clair sur `127.0.0.1:8081` (dev only,
voir `volume/tomcat/hapi_server.xml` et `dev.docker-compose.yml`). Exemples :

```
http://127.0.0.1:8081/fhir/Observation?identifier=http%3A%2F%2Fopenelis-global.org%2Fbacteriology_organism%7C
http://127.0.0.1:8081/fhir/Observation?identifier=http%3A%2F%2Fopenelis-global.org%2Fbacteriology_antibiogram%7C
http://127.0.0.1:8081/fhir/Observation?identifier=http%3A%2F%2Fopenelis-global.org%2Fbacteriology_flora%7C
http://127.0.0.1:8081/fhir/DiagnosticReport?result=Observation/<isolateId>
```
