# OpenELIS — Interopérabilité HIE & mutualisation SHR

Version cible : **3.3.0.0**

> Document technique destiné à l'équipe d'intégration nationale et aux éditeurs
> SIS partenaires. Décrit le modèle de données métier d'OpenELIS, les
> volumétries attendues et les contrats d'API à implémenter pour interconnecter
> via une couche **HIE locale** vers un **SHR national** (Shared Health Record).

---

## 1. Modèle de données métier

OpenELIS distingue 4 entités-pivots dans le schéma PostgreSQL `clinlims`.

### 1.1 Demande (ordonnance) — `sample`

Représente la **demande d'analyses** rattachée à un patient et à un
prescripteur. C'est l'agrégat racine.

| Colonne                    | Type                                                        | Rôle                                                  |
| -------------------------- | ----------------------------------------------------------- | ----------------------------------------------------- |
| `id`                       | numeric                                                     | PK interne                                            |
| `accession_number`         | varchar                                                     | Numéro d'ordonnance (préfixe configurable, ex CHRSP…) |
| `entered_date`             | timestamp                                                   | Date de saisie                                        |
| `received_date`            | timestamp                                                   | Date de réception au laboratoire                      |
| `collection_date`          | timestamp                                                   | Date de prélèvement (peut être nulle)                 |
| `status_id`                | FK → `status_of_sample` (`ORDER`/`SAMPLE`/`EXTERNAL_ORDER`) | Statut workflow                                       |
| `priority`                 | enum                                                        | `ROUTINE`, `STAT`, …                                  |
| `external_id`              | varchar                                                     | ID externe (HIE, SIS clinique)                        |
| `patient_id`               | via `sample_human`                                          |
| `provider_id`              | via `sample_requester`                                      |
| `sampling_organization_id` | via `sample_requester` (site référant)                      |

Relations clés : `sample_human(sample_id → patient_id)`,
`sample_requester(sample_id → provider_id / organization_id)`,
`sample_project(sample_id → project_id)`.

### 1.2 Échantillon (sample item) — `sample_item`

Un même `sample` peut contenir **plusieurs prélèvements** physiques (plusieurs
types : Sang, LCR, Urines…). Chacun est un `sample_item`.

| Colonne             | Type        | Rôle                                     |
| ------------------- | ----------- | ---------------------------------------- |
| `id`                | numeric     | PK                                       |
| `samp_id`           | FK → sample | Demande mère                             |
| `type_of_sample_id` | FK          | Nature (Sang, Urines, …)                 |
| `sort_order`        | numeric     | Numéro de séquence (1, 2, …)             |
| `collection_date`   | timestamp   | Date prélèvement spécifique              |
| `status_id`         | FK status   | Statut du prélèvement (entré, rejeté, …) |

### 1.3 Analyse (test demandé) — `analysis`

C'est la **paire (sample_item, test)** : un test demandé sur un prélèvement
donné. Une demande de 5 tests sur 2 prélèvements = 10 lignes `analysis`.

| Colonne            | Type                                      | Rôle                                     |
| ------------------ | ----------------------------------------- | ---------------------------------------- |
| `id`               | numeric                                   | PK                                       |
| `sampitem_id`      | FK → sample_item                          |
| `test_id`          | FK → test                                 |
| `started_date`     | timestamp                                 | Démarrage de l'analyse                   |
| `completed_date`   | timestamp                                 | Date de réalisation                      |
| `status_id`        | FK → `status_of_sample` (type `ANALYSIS`) |
| `revision`         | text                                      | Version de l'analyse (correction)        |
| `method`           | FK                                        | Méthode utilisée                         |
| `parent_result_id` | FK self                                   | Lien parent/enfant (tests conditionnels) |
| `referred_out`     | bool                                      | Envoyé à un labo externe                 |
| `is_reportable`    | bool                                      | Doit-il figurer sur le rapport patient   |

### 1.4 Résultat — `result`

| Colonne                     | Type    | Rôle                                                    |
| --------------------------- | ------- | ------------------------------------------------------- |
| `id`                        | numeric | PK                                                      |
| `analysis_id`               | FK      | Analyse à laquelle se rattache la valeur                |
| `result_type`               | char(1) | `N` num, `R` texte, `D`/`M` dictionnaire, `A` alpha-num |
| `value`                     | text    | Valeur saisie (ou id dictionnaire si D/M)               |
| `result_signature_id`       | FK      | Validation technicien                                   |
| `min_normal` / `max_normal` | num     | Bornes de référence                                     |

---

## 2. Statuts et workflow

Tous les statuts sont dans `clinlims.status_of_sample`, typés par `status_type`
:

### 2.1 Statuts d'ordonnance (`ORDER`)

| Code | Description                                    |
| ---- | ---------------------------------------------- |
| `1`  | NotTested — aucune analyse effectuée           |
| `2`  | Started — au moins une analyse démarrée        |
| `3`  | Finalized — toutes les analyses sont terminées |
| `12` | NonConforming                                  |

### 2.2 Statuts d'échantillon (`SAMPLE`)

| Code | Description       |
| ---- | ----------------- |
| `19` | Canceled          |
| `20` | Entered (saisi)   |
| `27` | Rejected (rejeté) |

### 2.3 Statuts d'analyse (`ANALYSIS`) — les plus consultés

| Code | Description                                       |
| ---- | ------------------------------------------------- |
| `4`  | NotStarted                                        |
| `6`  | **Finalized** — résultat validé par le biologiste |
| `7`  | BiologistRejected                                 |
| `13` | NonConforming                                     |
| `14` | **Canceled** — test annulé après commande         |
| `15` | TechnicalAcceptance — accepté par technicien      |
| `16` | TechnicalRejected — rejeté par technicien         |
| `26` | SampleRejected                                    |

### 2.4 Statuts d'ordonnance externe (`EXTERNAL_ORDER`)

| Code | Description                          |
| ---- | ------------------------------------ |
| `21` | Entered — l'ordre HIE/HL7 a été créé |
| `22` | Cancelled                            |
| `23` | Realized — le patient est arrivé     |
| `24` | NonConforming                        |
| `28` | InProgress                           |

Le **cycle de vie d'une analyse** typique :
`NotStarted → TechnicalAcceptance → Finalized` (ou
`TechnicalRejected/BiologistRejected → Canceled`).

---

## 3. Volumétries — ordres de grandeur

Données mesurées sur l'instance CILNSP (chiffres à adapter selon site, mais le
**ratio** entre tables est stable) :

| Table                | Volume référence      | Volume an. estimé (labo CHR)                           |
| -------------------- | --------------------- | ------------------------------------------------------ |
| `patient`            | 7                     | 5 000 – 50 000                                         |
| `sample`             | 9                     | 30 000 – 300 000                                       |
| `sample_item`        | 9 (~1.0 / sample)     | 1.1 × `sample`                                         |
| `analysis`           | 114 (~12.7 / sample)  | 5–15 × `sample`                                        |
| `result`             | 97 (~0.85 / analysis) | ≈ `analysis` (1 résultat par test, 2-3 pour les Multi) |
| `test` (référentiel) | 368                   | Stable (~300–500 selon configuration)                  |
| `note`               | 2                     | 0.5–1.5 × `analysis`                                   |

Pour un déploiement national :

- Chaque labo régional : ~10–50k ordonnances/an → 50–500k analyses.
- Le SHR national doit absorber l'agrégat de N labos. Compter ~1–5M observations
  FHIR/an pour 50 labos.
- Index recommandés : `sample(accession_number)`, `sample(external_id)`,
  `analysis(sampitem_id, test_id)`, `analysis(status_id)`,
  `result(analysis_id)`, `patient(national_id)`.

---

## 4. Architecture cible — HIE locale + SHR national

```
┌─────────────────┐    ┌─────────────────┐    ┌──────────────────┐
│ SIS clinique    │    │ OpenELIS Labo   │    │ Pharmacie / autre│
│ (ex: DHIS2,     │    │                 │    │ SIS centre       │
│  OpenMRS,       │    │                 │    │                  │
│  custom)        │    │                 │    │                  │
└────────┬────────┘    └────────┬────────┘    └────────┬─────────┘
         │                      │                      │
         │   FHIR R4 / HL7 v2   │                      │
         └──────────────┬───────┴──────────────┬───────┘
                        │                      │
                ┌───────▼──────────────────────▼──────┐
                │      HIE LOCALE (par centre)        │
                │  • OpenHIM (router)                 │
                │  • HAPI FHIR (stockage local)       │
                │  • Identity provider (CR/MPI)       │
                │  • Terminologie (TS)                │
                └────────────────┬────────────────────┘
                                 │ FHIR R4 (sync programmée)
                                 │
                         ┌───────▼────────┐
                         │ SHR NATIONAL   │
                         │ (HAPI FHIR     │
                         │  ou IPF)       │
                         └────────────────┘
```

**Principes** :

1. **Pas d'accès direct** d'un SIS au schéma `clinlims` d'OpenELIS. Tout passe
   par la couche HIE.
2. La HIE locale **anonymise et désidentifie** au besoin avant push national
   (politique à définir).
3. OpenELIS expose des **endpoints FHIR-like** (déjà disponibles dans le projet
   via `hapi-fhir`) et des **endpoints REST internes** pour les actions métiers
   spécifiques.

---

## 5. API à implémenter pour l'interopérabilité

OpenELIS embarque déjà une couche FHIR (HAPI). Les ressources cibles pour
l'interop sont :

### 5.1 Réception d'ordres externes — `ServiceRequest`

Un SIS clinique pousse une demande d'analyse vers OpenELIS via la HIE :

```http
POST /api/OpenELIS-Global/fhir/ServiceRequest
Content-Type: application/fhir+json

{
  "resourceType": "ServiceRequest",
  "identifier": [{ "system": "https://hie.ci/orderId", "value": "ORD-2026-0001" }],
  "status": "active",
  "intent": "order",
  "subject": { "reference": "Patient/cmu:90909000000" },
  "requester": { "reference": "Practitioner/12345" },
  "code": { "coding": [{ "system": "https://loinc.org", "code": "6299-2", "display": "Urea" }] },
  "specimen": [{ "reference": "Specimen/blood" }]
}
```

Mapping côté OpenELIS :

- `Identifier.value` → `sample.external_id`
- `subject` → `patient` (lookup via `national_id` / CR)
- `code` → `test` (mapping via `test.loinc_code` ou `test.local_code`)
- Création automatique d'un `sample` + `sample_item` + `analysis`, statut
  `EXTERNAL_ORDER.Entered` (21).

### 5.2 Publication des résultats — `DiagnosticReport` + `Observation`

Quand `analysis.status_id = Finalized` (6), OpenELIS publie :

```http
POST {HIE_URL}/fhir/DiagnosticReport
{
  "resourceType": "DiagnosticReport",
  "status": "final",
  "subject": { "reference": "Patient/..." },
  "basedOn": [{ "identifier": { "value": "ORD-2026-0001" } }],
  "code": { "coding": [{ "system": "https://loinc.org", "code": "..." }] },
  "result": [{ "reference": "Observation/uuid-1" }],
  "issued": "2026-05-15T10:00:00Z"
}
```

Chaque `result` OpenELIS devient une `Observation` :

```json
{
  "resourceType": "Observation",
  "status": "final",
  "code": { "coding": [{ "system": "https://loinc.org", "code": "..." }] },
  "valueQuantity": { "value": 5.4, "unit": "mmol/L" },
  "referenceRange": [{ "low": { "value": 3.5 }, "high": { "value": 7.5 } }],
  "performer": [{ "reference": "Practitioner/..." }]
}
```

Pour les résultats **bactériologiques** (organismes + antibiogrammes), suivre le
profil **IHE LAB-3 Microbiology** :

- `DiagnosticReport.result` → `Observation` "Culture, identification".
- Observations enfants pour chaque organisme.
- Observations susceptibility (un par antibiotique × organisme).

### 5.3 Annulations & corrections

- **Annulation** (`analysis.status_id = Canceled = 14`) : publier un
  `DiagnosticReport.status = "cancelled"` avec le même `identifier`.
- **Correction** (résultat modifié après émission) :
  - Si `showAuditOnPatientReport=true` côté OpenELIS, la note interne "Résultat
    corrigé" est créée (cf §6.3 du RELEASE_NOTES).
  - HIE/SHR : publier `DiagnosticReport.status = "corrected"` +
    `Observation.status = "corrected"`. Le SHR doit conserver l'**historique des
    versions** (Provenance ressource recommandé).

### 5.4 Recherche patient (MPI/CR)

Côté SIS clinique, avant de pousser un ordre, faire une résolution d'identité
via l'HIE :

```http
GET {HIE_URL}/fhir/Patient?identifier=https://hie.ci/cmu|90909000000
```

OpenELIS expose déjà la recherche locale :

```http
GET /api/OpenELIS-Global/rest/patient-search-results?nationalID=90909000000
```

Pour l'interop, la HIE est responsable de la **réconciliation inter-SIS** (CR /
MPI) ; OpenELIS reçoit/utilise des identifiants résolus.

---

## 6. Identifiants

Trois niveaux d'identifiants à gérer :

| Identifiant            | Origine                   | Stocké dans                                         |
| ---------------------- | ------------------------- | --------------------------------------------------- |
| ID interne OpenELIS    | OpenELIS                  | `patient.id`, `sample.id`                           |
| Identifiant local site | Frappé à l'enregistrement | `patient.subject_number`, `sample.accession_number` |
| **ID national** (CMU)  | National                  | `patient.national_id`                               |
| External Order ID      | SIS clinique / HIE        | `sample.external_id`                                |
| FHIR resource ID       | HIE / SHR                 | Mapping à maintenir hors-DB                         |

Recommandation : un **système d'identifiants** FHIR par type, par exemple :

- `https://hie.<pays>/patient/cmu` pour le CMU
- `https://hie.<pays>/order/external` pour les ordres externes
- `https://openelis.<centre>.<pays>/accession` pour le numéro accession local

---

## 7. Sécurité et confidentialité

- **Authentification** : OAuth2 / SMART on FHIR entre SIS ↔ HIE ↔ SHR.
- **TLS mutuel** sur les flux HIE ↔ SHR.
- **Désidentification** au push national : Patient nom/prénom retirés ou
  remplacés par un pseudonyme stable au niveau HIE locale.
- **Auditing** : chaque échange doit générer un `AuditEvent` FHIR côté HIE (qui,
  quand, quel patient, quelle ressource).
- **Consentement** : `Consent` FHIR pour les patients qui refusent le partage
  national (à coupler avec un opt-out registré côté HIE).

---

## 8. Stratégie de synchronisation HIE → SHR

| Approche          | Fréquence  | Avantage                    | Inconvénient                 |
| ----------------- | ---------- | --------------------------- | ---------------------------- |
| Push événementiel | Temps réel | SHR à jour                  | Charge réseau, retry à gérer |
| Batch nocturne    | 1 fois/j   | Simple, fenêtre maintenance | Latence J+1                  |
| Pull SHR          | Périodique | SHR contrôle son ingestion  | HIE doit garder un _cursor_  |

Recommandation pour un déploiement national : **push événementiel filtré**
(DiagnosticReport `final`/`corrected`/`cancelled` uniquement) avec retry
exponentiel sur la HIE locale + reconciliation batch nocturne.

---

## 9. Plan de mise en œuvre pour un centre

1. **Audit OpenELIS du centre** : version, configuration site, custom
   spécifiques.
2. **Installer la HIE locale** : OpenHIM + HAPI FHIR + MPI (par ex OpenCR ou
   Santé Plus).
3. **Mapper le référentiel tests** : alimenter `test.loinc_code` pour chaque
   test du laboratoire (cf table `test`, ~300–500 lignes).
4. **Implémenter le bridge OpenELIS ↔ HIE** :
   - Endpoint réception ordres (cf §5.1)
   - Push automatique des `DiagnosticReport` à la finalisation
   - Subscription FHIR si HAPI activé
5. **Tests d'intégration** : 100 ordres bout-en-bout, mesure latence, taux
   d'échec.
6. **Bascule progressive** : sites pilotes → régions → national.

---

## 10. Référentiels à mutualiser au niveau national

Pour que les données soient comparables entre centres :

| Domaine            | Source standard recommandée                       |
| ------------------ | ------------------------------------------------- |
| Codification tests | **LOINC**                                         |
| Diagnostics        | **CIM-10** (CIM-11 progressivement)               |
| Médicaments        | **ATC** / dictionnaire national                   |
| Unités             | **UCUM**                                          |
| Identité patient   | **CMU** (national_id) + MPI local                 |
| Établissements     | **FOSA national** (table OpenELIS `organization`) |
| Personnel soignant | Registre national des professionnels              |

À chaque centre, la table `test` doit être enrichie avec `loinc_code` ; sinon
les `Observation.code` au SHR ne seront pas exploitables nationalement.

---

## 11. Limites connues d'OpenELIS pour l'interop

- **Pas de webhook natif** sur changement de statut `analysis` : à brancher via
  une tâche cron qui poll
  `analysis WHERE status_id=6 AND last_pushed_at IS NULL`, ou via Liquibase
  trigger custom.
- **`test.loinc_code`** souvent vide : à mapper manuellement.
- **Identité patient** : le `national_id` n'est pas contraint unique (cf
  `Allow duplicate national ids = true`). Le MPI HIE doit gérer.
- **Bactériologie** : modèle interne plus riche que le standard FHIR
  Microbiology, perte d'info possible (notamment notes d'interprétation libres →
  `Observation.note`).

---

## 12. Prochaines étapes côté plateforme nationale

1. Définir le **profil FHIR national** (Implementation Guide).
2. Publier le **registre des terminologies** (FHIR TS).
3. Mettre à disposition une **HIE de référence** clé en main pour les sites
   pilotes.
4. Définir la **politique de consentement** et de désidentification.
5. Spécifier l'API publique du **SHR national** (lecture pour les tableaux de
   bord MSP).
