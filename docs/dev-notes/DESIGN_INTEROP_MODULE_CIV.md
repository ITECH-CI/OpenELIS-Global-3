# Conception — Module d'échange de données unifié (interopérabilité CIV)

> **Statut** : cadrage / design (2026-07-10). Fusionne les chantiers 8
> (interop), 9 (conteneuriser oedatauploader) et 10 (refonte dataexport v2).
> **Objectif** : UN SEUL module d'échange de données, simple / efficace /
> robuste, paramétrable et piloté depuis OpenELIS, avec exposition API des
> ressources FHIR métier (ServiceRequest, Patient, Organization,
> DiagnosticReport, Observation, Specimen, Task, Practitioner, Encounter…).
>
> Ce document part de l'audit détaillé des 2 composants existants
> (`oedatauploader` + `dataexport`) et du FHIR métier déjà présent dans OE.

---

## 1. Existant — synthèse des audits

### 1.1 `oedatauploader` (repo externe, app Spring Boot autonome)

- **Stack** : Spring Boot 2.7.18 (fin de support), Java 11, ~3090 lignes, 28
  classes. `@EnableScheduling`, datasource unique = `clinlims`.
- **Approche** : REST **JSON custom + JWT** vers un serveur **consolidé**.
  **HAPI retiré** → aucun FHIR (les colonnes `*_fhir_uuid` restantes sont du
  legacy).
- **3 flux** (`@Scheduled`, `fixedDelay`) :
  - **DataSync** (sortant, /5min) : analyses/résultats → `POST /syncOrders`.
  - **Eorder pull** (entrant, /60s) : import demandes Charge Virale →
    `GET /v1/vl-requests` → écrit dans tables OE natives (electronic_order,
    patient, organization…) + ACK.
  - **Eorder push** (bidirectionnel, /60s+30s) : remonte statuts/résultats
    (`/v1/vl-requests/events`), pull statuts (`/v1/vl-requests/statuses`),
    reconciliation analysis_id par fenêtre de dates.
- **Machine à états CDC** (à conserver — c'est le point fort) : tables
  `analysis_sync_status` (`upload_flag` 1=TO_INSERT/2=TO_UPDATE/3=UP_TO_DATE/
  4=IN_PROGRESS) et `eorder_sync_status` (`sync_flag` 0..6), alimentées par des
  **triggers PostgreSQL** (`optimized_sync_triggers.sql`,
  `eorder_sync_triggers.sql`) qui passent `upload_flag 3→2` sur changement —
  découplage OE/uploader sans toucher au code OE. + table plate
  `vl_eorder_request_flat` (~60 col), checkpoint de pagination
  `eorder_pull_checkpoint`.
- **Robustesse opérationnelle** (mature, à réutiliser) : reset stale
  IN_PROGRESS, verrous AtomicBoolean avec vol après timeout, circuit breaker (5
  échecs / cooldown 5min), retry ACK, anti-régression de statut, SAVEPOINTs PG
  pour inserts idempotents.
- **Fragilités** : SQL **en dur massif** couplé au schéma physique OE (casse
  silencieuse si le schéma change) ; **écriture directe** dans les tables OE
  natives (duplique la logique métier de `DBOrderPersister`/`TaskWorker`) ;
  secrets DB+API **en clair**, transport **HTTP** ; `@Transactional` sur
  méthodes privées silencieusement ignorés (bug latent) ; checksum de payload
  trivial ; pas de refresh JWT proactif ; legacy résiduel (`vl_analysis_record`,
  `resource_sync_status`, colonnes fhir_uuid mortes).

### 1.2 `dataexport` (submodule, lib couplée à OE)

- **Stack** : HAPI FHIR **4.2.0** (2020), Java 11, ~900 lignes, 2 modules Maven
  (core JPA + api). Ne compile que dans le contexte OE.
- **Approche** : lit le **store FHIR local** (HAPI), exporte le **delta**
  (`lastUpdated` depuis le dernier succès) → repackage en **Bundle transaction
  (PUT par ID logique = upsert)** → POST vers un serveur **FHIR distant**.
  Type-agnostique. Ne touche **jamais** aux entités OE.
- **Ressources exportées** (par défaut) :
  `Task, Patient, ServiceRequest, DiagnosticReport, Observation, Specimen, Practitioner, Encounter`.
- **État** : modèle JPA propre (`data_export_task`, `data_export_attempt`, enum
  `DataExportStatus` GENERATED→…→SUCCEEDED/FAILED/INCOMPLETE), suivi
  event-driven (`DataExportStatusEvent` + `@EventListener`).
- **Déclenchement** : `DataExportTaskCheckerServiceImpl` (`@Scheduled` /60s,
  polling `maxDataExportInterval`) + hook manuel
  `FhirExportController POST /dataexport/fhir`
  - en tandem après transformation (`FhirTransformationController`).
- **Transport** : `FhirClientFetcher` (interface, impl = `FhirUtil` côté OE),
  auth **Basic** (`fhirstore.username/password`) + headers
  `Server-Name`/`Server-Code`. Classification erreurs transient/fatal + probe
  `/metadata`.
- **Fragilités** : duplique la persistance OE (`data_export_*` +
  `persistence.xml`
  - `@EnableJpaRepositories org.itech`) ; converter statut mort ; `runningTasks`
    en mémoire (inopérant multi-instance) ; **2 mécanismes redondants**
    (Subscriptions REST-hook + polling) non coordonnés ; stack obsolète ; auth
    Basic seulement.

### 1.3 FHIR métier déjà présent dans OpenELIS (clé !)

- `FhirTransformServiceImpl` (1668 lignes) **transforme déjà** les entités OE en
  **toutes** les ressources FHIR cibles (Patient, ServiceRequest,
  DiagnosticReport, Observation, Specimen, Task, Practitioner, Organization) et
  les **persiste dans le store HAPI local** (conteneur `fhir.openelis.org`).
- Déclenché par événements (`SampleFhirTransformEventListener`) + endpoints de
  transformation en masse (`FhirTransformationController`).
- **Conséquence** : l'exposition API des ressources FHIR métier **existe déjà en
  germe** — le store FHIR local EST le point d'exposition potentiel.

---

## 2. Constat architectural

Les deux composants font le MÊME métier (échanger données OE ↔ extérieur) par
deux chemins **opposés** :

|                  | Source                  | Format            | Cible                | Sens               |
| ---------------- | ----------------------- | ----------------- | -------------------- | ------------------ |
| `oedatauploader` | SQL direct `clinlims`   | JSON custom + JWT | **SC OpenELIS**      | DataSync↑, Eorder↕ |
| `dataexport`     | store FHIR local (HAPI) | FHIR Bundle       | serveur FHIR distant | export↑            |

Redondance à éliminer. Mais elles ne servent PAS le même besoin (voir §2.1) : la
consolidation (JSON) et l'interopérabilité (FHIR) sont **deux usages distincts**
qui coexisteront.

## 2.1 Clarifications métier (2026-07-10) — corrige les hypothèses initiales

**Scope = serveur consolidé OpenELIS (SC)**, PAS SIGDEP-3 (autre chantier,
process JSON/JWT similaire, hors scope).

**Le cœur de l'interop est aujourd'hui sur le SC**, pas dans oedatauploader.
oedatauploader ne fait que **lire du JSON** (= ressources FHIR **aplaties** par
le SC) et renvoyer résultats/statuts **en JSON** ; **c'est le SC qui reconstruit
les ressources FHIR**. Le SC OpenELIS ne reçoit pas de JSON aujourd'hui pour
tout — mais on a la matière pour moderniser.

**Répartition des flux actuels (à MAINTENIR) :**

- **Flux 1 — interop CV (demandes charge virale)** : SIGDEP crée une demande →
  SC OpenELIS **aplatit/audite/présente** et **expose des API** → OpenELIS local
  **lit** ces demandes (JSON) → renvoie résultats/statuts au SC (JSON) → le SC
  **met à jour / reconstruit** les ressources FHIR. **Seul flux interopérable
  actuel** (uniquement les demandes CV émises depuis SIGDEP).
- **Flux 2 — remontée analyses** : OpenELIS local envoie ses données d'analyse
  au SC (JSON). **N'intervient PAS dans l'interop** — pure **consolidation**.

**Décision de transport (nuance clé)** : pour la **consolidation** (flux 1 et 2
vers le SC), on **garde JSON** — c'est de la consolidation de données, pas de
l'interop ; JSON minimise le payload et la même structure code les deux côtés.
Le **FHIR** est réservé à l'**interopérabilité** (rôles 2 et 3 ci-dessous).

## 2.2 Ce que le module unifié doit apporter EN PLUS

Le module (côté **site local**, comme oedatauploader) remplit **3 rôles** :

| Rôle                               | Transport                                  | Direction                                   | Statut                 |
| ---------------------------------- | ------------------------------------------ | ------------------------------------------- | ---------------------- |
| **1. Consolidation SC**            | JSON/JWT                                   | analyses↑, e-orders CV↕                     | existant, à moderniser |
| **2. Mini-HIE local (NOUVEAU)**    | **FHIR (endpoints exposés par le MODULE)** | lecture par les systèmes du centre de santé | nouveau                |
| **3. Push FHIR distant (NOUVEAU)** | FHIR Bundle                                | ↑ vers serveurs FHIR distants               | dataexport modernisé   |

- **Rôle 2 — mini-HIE local** : OpenELIS expose ses ressources FHIR pour une
  lecture / interop **locale** dans un centre de santé (ou même sur un serveur
  internet). ⚠️ **Le HAPI local (`fhir.openelis.org`) NE doit PAS être
  accessible sur le réseau** — uniquement conteneurs Docker + hôte. Donc **c'est
  le module qui expose ses propres endpoints FHIR** (façade réseau sécurisée) ;
  HAPI reste interne.
- **Rôle 3** : pousser les ressources FHIR vers d'éventuels serveurs FHIR
  distants.

## 2.3 Ressources FHIR & modèle — corrections de conception (DANS LE SCOPE)

**Ressources cibles** : ServiceRequest, Patient, Organization, DiagnosticReport,
Observation, Specimen, **Task**, Practitioner, Encounter, **Questionnaire**,
**QuestionnaireResponse**.

- **Questionnaire / QuestionnaireResponse** : pas utilisés dans le flux actuel
  (les e-orders SIGDEP sont lus en JSON). **Dans la nouvelle architecture, ils
  sont utilisés et exposés** dans le mini-HIE local : un système externe qui
  envoie une demande électronique vers OE (FHIR) envoie **`ServiceRequest` +
  `QuestionnaireResponse`** (+ autres) pour les infos structurées accompagnant
  la demande.
- 🔴 **Correction du modèle Task (à faire dans ce chantier)** : aujourd'hui,
  **toutes les infos de la demande sont stockées dans Task, et le Task est créé
  en amont** (côté émetteur) en cas d'interop → **INCORRECT**. Cible : **le
  système externe n'envoie PAS de Task** ; **OpenELIS construit le Task
  lui-même** pour le **suivi de l'analyse au labo** (le Task = objet de suivi
  labo, produit au labo). Les infos de demande vont dans ServiceRequest +
  QuestionnaireResponse, pas dans Task.

## 2.4 🔴 PRIORITÉ FONDATION — fiabiliser la population FHIR locale

Avant d'exposer (rôle 2) ou de pousser (rôle 3), la **transformation
métier→FHIR** d'OE (`FhirTransformServiceImpl`, 1668 l., +
`SampleFhirTransformEventListener`) doit être **auditée et fiabilisée** :

- **problèmes constatés** : données manquantes / incomplètes dans les ressources
  produites, **workflow de transformation qui plante souvent**.
- **objectif** : optimiser la transformation (algo, ressources, robustesse) ET
  proposer un **monitoring facile depuis OpenELIS** (état des transformations,
  échecs, ressources incomplètes).
- **rationale** : exposer/pousser du FHIR incomplet = propager des données
  incorrectes. C'est la **fondation** de tout le reste.

---

## 3. Vision cible

Un module d'échange **unique**, paramétrable, piloté depuis OE :

- **Moteur de synchronisation unique** (delta / CDC) + **adaptateurs de
  transport** enfichables (voir §4 pour la voie).
- **Bidirectionnel** : export (sortant) + import (entrant, e-orders / demandes).
- **Exposition API des ressources FHIR métier** : ServiceRequest, Patient,
  Organization, DiagnosticReport, Observation, Specimen, Task, Practitioner,
  Encounter (+ extensibles).
- **Piloté depuis OpenELIS** : écrans de config (activer/désactiver un flux,
  choisir les cibles/URLs, intervalles, ressources exposées, credentials).
- **Sécurisé** : secrets hors code (cohérent avec l'installeur durci), TLS/mTLS
  ou OAuth, pas de HTTP clair.
- **Stack alignée** : Java 21, Spring Boot 3.x, **une seule** version HAPI
  (celle d'OE = 6.6.2) → supprime le double runtime FHIR.
- **État de sync unifié** : un seul modèle (aujourd'hui : `upload_flag` vs
  `DataExportTask/Attempt` = 2 mécanismes).

---

## 4. Architecture retenue — hybride PAR CONCEPTION (pas par compatibilité)

**Deux transports coexistent, chacun pour son usage** (décidé 2026-07-10) :

- **Transport JSON/JWT** → **consolidation** vers le SC OpenELIS (flux 1 CV +
  flux 2 analyses). Payload minimal, structure partagée. On **modernise**
  l'existant (SQL en dur → services, machine à états unifiée, secrets, TLS) sans
  changer le protocole de fond.
- **Transport FHIR** → **interopérabilité** : mini-HIE local (rôle 2, endpoints
  exposés par le module) + push distant (rôle 3). FHIR standard (ressources +
  Bundle).

Ce n'est PAS un choix de compatibilité transitoire : consolidation ≠ interop,
les deux transports sont **durables**. Le module est un **hub** avec un moteur
de sync commun et des **adaptateurs de transport** enfichables
(JSON-consolidation, FHIR-interop).

**Décidé aussi** : module **côté site local** (comme oedatauploader), déployé
par l'installeur CIV. Reste à décider en conception : app autonome vs intégré à
OE (voir §5).

---

## 5. Décisions déjà prises (rappel)

- Fusion oedatauploader + dataexport en **un module** (chantiers 8/9/10).
- **FHIR reconstitué depuis le métier en transactionnel** (cohérent avec la
  décision backup : les tables `hfj_*` ne sont pas sauvegardées, régénérées
  depuis le métier). → renforce la voie « le métier OE est la source de vérité,
  le FHIR est une projection ».
- Emplacement (app autonome vs intégré à OE) : **à décider après ce cadrage**.
  - _App autonome_ (socle oedatauploader) : découplé, conteneur séparé,
    redéployable seul. Mais SQL direct fragile → devrait plutôt appeler les
    services OE ou l'API FHIR d'OE que le SQL brut.
  - _Intégré à OE_ : une stack/CI/version, pas de conteneur en plus, réutilise
    directement `FhirTransformService`/`FhirPersistanceService`. Mais couple
    l'échange au cycle de vie d'OE.

---

## 6. Plan d'attaque proposé (ordre par priorité, à valider)

> **ÉTAT D'AVANCEMENT (2026-07-16, après audits + travail mini-HIE).** ~60% de
> la cible est DÉJÀ livré, souvent sans avoir été rattaché à ce doc. Décisions
> structurantes tranchées : **module INTÉGRÉ à OE** (pas d'app autonome — cf. la
> mini-HIE déjà intégrée) ; **harmonisation Java 21** (comme OE) pour la fusion.
>
> | Étape du plan                                      | État                         | Où                                                                                         |
> | -------------------------------------------------- | ---------------------------- | ------------------------------------------------------------------------------------------ |
> | Phase 0.1 audit transfo                            | ✅ fait                      | audits 2026-07-16                                                                          |
> | Phase 0.2 **modèle Task**                          | ✅ **déjà conforme**         | voir ci-dessous                                                                            |
> | Phase 0.3 monitoring (dont ressources incomplètes) | ✅ fait                      | `fhir_sync_status` + `SUCCESS_INCOMPLETE` (commit c3d355ba2) ; robustesse NPE transfo idem |
> | Phase 1.4 emplacement                              | ✅ décidé = **intégré à OE** | —                                                                                          |
> | Phase 1.5 moteur sync unifié                       | ⏳ à faire (incrément 4)     | CDC uploader à rapatrier                                                                   |
> | Phase 1.6 adaptateurs transport                    | ⏳ à faire (incrément 4)     | JSON/JWT à moderniser                                                                      |
> | Phase 2.7 **mini-HIE local** (rôle 2)              | ✅ fait                      | gateway lecture sécurisée + réception SR+QR sans Task (commits `feat(mini-hie)` A/B/C)     |
> | Phase 2.8 **push distant** (rôle 3)                | ✅ fait                      | `FhirPushTarget` multi-cibles + supervision                                                |
> | Phase 3.9 config OE                                | 🟡 partiel                   | #FhirGateway + #FhirPushTargets faits ; reste config consolidation                         |
> | Phase 3.10 migration/nettoyage legacy              | ⏳ à faire (incrément 4)     | vl_analysis_record, colonnes fhir_uuid mortes…                                             |
> | Phase 3.11 sécurité                                | 🟡 partiel                   | gateway (jeton/quota/audit) fait ; reste secrets/HTTPS consolidation                       |
> | Phase 3.12 CI ghcr                                 | ✅ fait                      | chantier 7 (publish + release + installeur)                                                |
>
> **Modèle Task (Phase 0.2) — CONSTAT : la cible §2.3 est atteinte.**
>
> - Le §2.3 vise le flux d'INTEROP : « le système externe n'envoie PAS de Task ;
>   OE construit le Task ». C'est **exactement** ce que fait la réception
>   mini-HIE (`FhirOrderReceptionServiceImpl` : reçoit
>   ServiceRequest+QuestionnaireResponse sans Task, construit un Task minimal
>   via `TaskWorker`). ✅
> - `transformToTask` (transfo interne OE, `FhirTransformServiceImpl:670`) est
>   un Task de **suivi labo** (status mappé sur le statut du sample, authoredOn,
>   priority, basedOn→ServiceRequest, output→DiagnosticReport, for→Patient) — il
>   ne porte PAS d'infos de demande (pas de code test, reasonCode, input
>   clinique). Déjà conforme, **rien à corriger**.
> - `FhirReferralServiceImpl.createReferralTask` porte
>   reasonCode/focus/restriction/ description, MAIS c'est le workflow FHIR
>   **normatif de référence** (envoi d'un échantillon vers un labo tiers) où ces
>   champs sont **requis** — ce n'est pas le « modèle Task incorrect » du §2.3.
>   Légitime, à conserver.
>
> **RESTE À FAIRE** : incrément 4 = **fusion de `oedatauploader` dans OE** (le
> gros : rapatrier DataSync/EorderSync comme code OE, réutiliser les triggers
> CDC + DBOrderPersister/TaskWorker au lieu du SQL direct, Java 21, nettoyer le
> legacy) ; incrément 5 = config OE + sécurité de la partie consolidation.
> Détails des audits uploader/dataexport dans la mémoire projet et le transcript
> 2026-07-16.

## 6.1 Modèle e-order **générique** (incrément 4a) — décision MESURÉE (2026-07-16)

**Exigence Pascal** : la gestion des demandes électroniques doit être
**générique pour tout type d'examen** (pas seulement charge virale). Objectif :
charger vite les infos d'une demande dans le formulaire OE **sans requête au
serveur FHIR**.

**Ce que fait le dépôt avancé** (`/Users/pascal/dev/OpenELIS-Global-2`,
`develop`, read-only — porté par un collègue) : table plate
`clinlims.vl_eorder_request_flat` (53 colonnes dont 19 cliniques CV **en dur** :
hiv*status, arv*\*, cd4*\*, prior_vl*\*, pregnancy…) comme **cache de lecture**
→ liste (LEFT JOIN electronic_order) + pré-remplissage formulaire
(`SampleEntryByProjectController. loadDataFromFlatTable`). **DDL de la table
plate hors OpenELIS** : créée/remplie par `oedatauploader` (ajoute aussi
`sync_flag`/`collection_date`/`organization_id` sur electronic_order
hors-Liquibase). OE ne l'écrit que pour `local_status` (rejet). Fort couplage CV
: DTO `VlOrderDisplayItem`, SQL, libellés FR codés en dur.

**Banc d'essai** (`src/test/.../order/EorderLoadBenchmark.java`, `@Ignore`,
lancé via JUnitCore ; JSON `electronic_order.data` réaliste ~2 Ko = Patient +
ServiceRequest + QuestionnaireResponse 20 items ; warmup JIT + 20000 it.) :

| Stratégie                            | µs/demande | vs colonnes |
| ------------------------------------ | ---------- | ----------- |
| Colonnes plates (Map)                | 0,34       | référence   |
| Parse **Jackson** (arbre JSON)       | 10,3       | **31×**     |
| Parse **HAPI** (Bundle + extract QR) | 46,4       | **138×**    |

**Conclusion** : le **parsing n'est PAS le goulot** (46 µs pour pré-remplir UNE
demande = imperceptible). Le vrai coût que la table plate élimine = les **appels
REST FHIR live** (`fhirClient.read()` ServiceRequest/Patient dans
`StudyElectronicOrdersController`) répétés dans la **liste** (N × round-trips
réseau 1-20 ms). Jackson est 4,5× plus rapide que HAPI.

**DÉCISION (Pascal, sur mesures) — PAS de table plate :**

1. **Détail / pré-remplissage formulaire** : lire `electronic_order.data` (JSON
   déjà en base, aucun appel FHIR distant). Le JSON porte déjà tout le
   spécifique par type d'examen → **générique par nature**. Extraction via parse
   (négligeable).
2. **Liste** : ajouter quelques **colonnes dénormalisées sur
   `electronic_order`** (labno/accession, nom du site demandeur, date de
   collecte — le reste : patient/gender/birthDate/statut/priorité est **déjà**
   accessible via la many-to-one `patient` lazy=false + `status_id`). Remplies
   **à la réception**. PAS 53 colonnes CV, PAS de table séparée à synchroniser.
3. **Renseignements structurés** post-création sample : `observation_history`
   natif (ObservationType piloté DB) — ajouter les types manquants par
   **données**.

**Implications :**

- OE (formulaire, rapports) : ✅ rien ne casse (notre fork ne lit PAS de table
  plate ; cf. absence de
  `VlOrderDisplayItem`/`EorderFlatQueryService`/`searchCvOrders`).
- Supprime le besoin de `vl_eorder_request_flat` **et** sa mécanique de synchro
  (triggers/uploader) côté OE. Le couplage CV fort disparaît.
- `StudyElectronicOrdersController` (notre fork) fait aujourd'hui parse `data` +
  3 `fhirClient.read()` : à refondre pour lire `data` + colonnes dénormalisées,
  sans appel FHIR distant.
- État `electronic_order.data` déjà peuplé à la réception (`TaskWorker:197`
  `eOrder.setData(message)`) → la source du chargement rapide existe déjà.

**IMPLÉMENTÉ (4a, 2026-07-16, local non poussé)** — 3 colonnes d'affichage
génériques sur `electronic_order` :

- Migration idempotente `3.2.x.x/add_eorder_display_columns.xml`
  (`requesting_facility_name`, `collection_date`, `test_name` ; `columnExists` +
  `MARK_RAN`) + mapping `ElectronicOrder.hbm.xml`/entité.
- Remplissage à la réception : `TaskWorker.populateDisplayFields` (best-effort
  strict, `catch(Exception)` — ne fait jamais échouer la réception). `test_name`
  = `getLocalizedTestName().getLocalizedValue()` (MÊME source que l'affichage,
  pour un libellé identique dénormalisé/repli).
- Liste sans FHIR : `StudyElectronicOrdersController.convertToDisplayItem` lit
  d'abord les colonnes ; le bloc de lecture FHIR (dont l'appel au serveur
  **distant** pour l'Encounter) ne s'exécute plus que si l'une des 3 colonnes de
  liste est nulle (demande **ancienne**, avant migration).

**Caveats assumés (best-effort, à connaître) :**

- `collection_date` n'est dénormalisée que si le **ServiceRequest entrant porte
  `occurrenceDateTime`**. Historiquement (flux CV via SIGDEP) la date venait
  d'un **Encounter distant**, pas du SR → pour ces demandes la colonne reste
  nulle et le repli distant se déclenche encore. Pour le flux **PUSH mini-HIE**
  (cible de 4a), l'émetteur maîtrise le SR et peut inclure `occurrenceDateTime`.
  OE ne pose pas d'Encounter sur ses propres SR (`FhirTransformServiceImpl:568`
  TODO).
- `requesting_facility_name` = `ServiceRequest.requester.display` (le
  **demandeur**). Le repli, lui, dérive le site du `recipient` (Organization) ou
  d'une location extension patient — concepts différents ; léger risque de
  libellé distinct entre une demande récente et une ancienne.
- `referringLabNumber` et l'UPID OpenMRS **ne sont PAS** dénormalisés et **ne
  conditionnent PAS** le repli : ils ne sont pas affichés dans la liste
  `studyElectronicOrderView.jsp` (UPID de la liste vient de l'entité Patient
  locale). Les inclure dans la garde annulait tout le bénéfice (bug corrigé).

Le moteur de sync — 4b/c/d — réutilisera `observation_history` +
`electronic_order` au lieu de la table plate.

## 6.2 Moteur de remontée consolidée SORTANT (incrément 4b) — IMPLÉMENTÉ (local)

**Objectif** : porter dans OE le flux DataSync sortant de `oedatauploader`
(remontée analyses/résultats → serveur consolidé), en `@Scheduled`, Java 21,
sans SQL en dur, en réutilisant les services métier. Cartographies : uploader
sortant + existant OE + contrat SC (`/Users/pascal/dev/oedatarepo`), 2026-07-16.

**Décisions (Pascal) :**

1. **Détection "quoi remonter" = triggers CDC + `analysis_sync_status`**
   rapatriés fidèlement de l'uploader (choix : capture TOUT changement,
   découplage, éprouvé).
2. **Payload construit via services métier** (`AnalysisService`/`ResultService`/
   `SampleService`/`PatientService`), pas de SQL en dur.
3. **Contrat SC** : le SC est maintenu en interne mais gardera **toujours**
   l'ancien contrat (serveurs legacy). Analyse SC : `POST /api/syncOrders`, JWT
   `ROLE_PUSHER`, Jackson **tolérant** (pas de `FAIL_ON_UNKNOWN_PROPERTIES`),
   **pas de versionnement**. Verdict : modernisation **ADDITIVE seulement**
   possible (ajouter des champs OK ; **renommer un champ consommé = échec
   SILENCIEUX**, données perdues). → **additif + `schemaVersion`**.

**Implémenté (package `org.openelisglobal.dataexchange.sync`, local non poussé)
:**

- Migration `create_analysis_sync_status.xml` (table + index unique
  `analysis_id` requis par le `ON CONFLICT` + index partiel de file) +
  `sql/ analysis_sync_cdc_triggers.sql` (12 fonctions/triggers, idempotents, `~`
  délimiteur, `runOnChange`). Enregistrée dans `3.2.x.x/base.xml`.
- Entité/hbm `AnalysisSyncStatus` (flags 1-4, sans version optimiste) +
  DAO/service ; transitions d'état par **UPDATE ciblé** dans un service
  `@Transactional` **PUBLIC** (⚠️ bug uploader corrigé : ses méthodes de statut
  étaient `private` → proxy Spring inopérant → transactions ignorées).
- DTO `dataexchange.sync.dto` alignés sur le contrat SC, **épurés** des champs
  que le SC ignore (`orders[].orderMetadata/analyses`,
  `analysisDTOs[].analysisMetadata`) et du `checksum` trivial ; **ajout**
  `schemaVersion` (additif, pour un futur SC).
- `ConsolidatedServerClient` : JWT Basic→Bearer, POST `/syncOrders`, **retry
  unique sur 401**, ACK = 2xx, bean `CloseableHttpClient` OE (Apache HttpClient
  4).
- `DataSyncPayloadBuilderImpl` `@Transactional(readOnly)` (⚠️ ValueHolders lazy)
  : mappe Analysis/Result/Sample/Patient/Organization → DTO.
- `DataSyncTask` `@Scheduled` (5 min, feature flag
  `org.openelisglobal.consolidated.sync.enabled=false` par défaut) : reset stale
  → drainage par lots (borne `maxBatchesPerRun`) → mark IN_PROGRESS → build →
  POST → mark UP_TO_DATE / TO_UPDATE.
- **Mapping ajouté** : `Organization.datimOrgCode`/`datimOrgName` (colonnes
  `datim_org_code`/`datim_org_name` déjà en base varchar(25)/(100), jusque-là
  lues seulement en SQL brut) — le SC rattache les sites via DATIM puis DHIS2.
- Propriétés `org.openelisglobal.consolidated.sync.*` documentées dans
  `application.properties`.

**Robustesse (fidèle à l'uploader, minimaliste sur le flux sortant)** : reset
stale IN_PROGRESS (crash/restart), retry par re-mise en file (flag 2), retry
401, anti- régression (triggers 3→2 seulement). PAS de circuit breaker/verrou
(comme l'uploader — la protection concurrentielle = `@Scheduled fixedDelay`
mono-thread).

**Caveats assumés** : le flux sortant écrit UNIQUEMENT `analysis_sync_status`
(aucune écriture métier) ; les triggers CDC touchent 10 tables métier (analysis,
result, sample, sample_item, patient, patient_identity, observation_history,
organization, sample_organization, sample_requester) — surveiller la charge sur
gros volumes.

**Tradeoffs assumés (revue de code 2026-07-16) :**

- **Livraison at-least-once** : si le POST réussit mais `markUpToDate` échoue
  (incident DB), les lignes restent flag=4 → `resetStaleInProgress` les rejoue →
  le SC re-reçoit le lot. **Absorbé car le SC upsert par
  `analysisLocalId`+`labId`** (pas de duplication de ligne), mais c'est bien de
  l'at-least-once, pas de l'exactly-once. Fidèle à l'uploader.
- **UN seul résultat par analyse** : limitation du **contrat SC** (upsert par
  `analysisLocalId`, une seule colonne résultat). On privilégie un résultat
  **reportable** (mieux que le plus petit id), mais la bactériologie
  multi-résultats (groupes/ATB/flore) n'est PAS entièrement remontée par ce
  flux. À revoir si le SC évolue. L'uploader legacy avait la même limite.
- **Pas de dead-letter** : un rejet SC **définitif** (400 payload invalide) est
  re-tenté à chaque tick (comme un échec transitoire) — pas de distinction
  4xx/5xx. À superviser ; un compteur d'échecs/quarantaine est un chantier
  ultérieur.
- **UPDATE de flag sans garde de valeur** : sûr tant que le scheduler
  `fixedDelay` reste mono-thread (jamais de run concurrent). Ne PAS ajouter
  `@Async` sur la tâche sans ajouter d'abord une garde `AND upload_flag = ...`
  sur les transitions.
- `labUuid` (attribution du labo côté SC) doit être renseigné via
  `org.openelisglobal.consolidated.sync.labUuid`, sinon la remontée part
  non-attribuée.

**Reste (incréments suivants)** : 4d = push statuts (events) ; 4e = nettoyage
legacy (`vl_analysis_record`, colonnes `*_fhir_uuid` mortes) + secrets/HTTPS.
Écrans de config OE = incrément 5.

## 6.3 Pull Eorder ENTRANT (incrément 4c) — IMPLÉMENTÉ (local)

**Objectif** : importer les demandes VL servies par le serveur consolidé (SC).
Décision initiale « via `DBOrderPersister`/`TaskWorker` » **révisée après
analyse du contrat SC**.

**Contrat SC (cartographié)** :
`GET /api/v1/vl-requests?platformUuid&since&limit` sert du **JSON PLAT** (entité
`VlElectronicRequestFlat` sérialisée, ~90 champs camelCase), pagination par
**curseur composite `"<updatedAt>|<id>"`** (keyset, pas de `hasMore`/`total` —
arrêt sur `count == 0`). Auth JWT `ROLE_PUSHER`. **ACK obligatoire**
(`POST /ack`, clé `requestUuid` + `eventUuid`) : c'est l'ACK "OK" qui passe la
demande à `IMPORTED_IN_OPENELIS` et la **retire du flux** (sinon re-livraison à
chaque pull).

**Décision de mapping (RÉVISÉE) : JSON → services métier OE directement, PAS de
FHIR.** Le SC a déjà aplati du FHIR→JSON en amont ; le JSON plat ne porte plus
les systèmes de codes FHIR (LOINC/CIEL), seulement des libellés/valeurs résolus.
Reconstruire un Bundle pour le réinjecter dans `receiveOrderBundle` serait un
aller-retour **lossy et inutile**. On mappe donc vers les services métier
(`ElectronicOrderService`/`PatientService`/`OrganizationService`), esprit « pas
de SQL brut » conservé, sans la contrainte FHIR.

**Niveau d'import (décision Pascal) : `electronic_order` "Entered" + patient/org
seulement** (comme l'uploader ; sample/analyse créés PLUS TARD à la saisie OE).
Découverte clé : l'uploader ne créait déjà qu'un `electronic_order` puis
réconciliait a posteriori — le moteur natif rend cette réconciliation caduque
(lien direct `external_id`↔sample à la saisie).

**Implémenté (package `dataexchange.sync`, local non poussé) :**

- DTO : `EorderRequestDTO` (miroir partiel typé, `@JsonIgnoreProperties`),
  `VlRequestPullResponse` (demandes en **JsonNode BRUT** → data verbatim, rien
  perdu), `VlRequestAckDTO`.
- `EorderPullCheckpointService` + migration `create_eorder_pull_checkpoint.xml`
  (table 1 ligne, curseur opaque, idempotente).
- `ConsolidatedServerClient` étendu : `pullRequests` (GET paginé) + `ackRequest`
  (POST), réutilise le JWT + retry 401 du flux 4b.
- `EorderImportServiceImpl` `@Transactional` : dédup/création patient (subject →
  national, via person/patient/identity — MÊME pattern que `DBOrderPersister`),
  dédup/création organisation (short_name → name), `ElectronicOrder`
  (external_id=requestUuid, data=JSON brut, status Entered, type FHIR, +colonnes
  d'affichage 4a). **Idempotent par `external_id`**.
- `DataPullTask` `@Scheduled` (60 s, feature flag
  `org.openelisglobal.consolidated.pull.enabled=false` par défaut) : verrou
  anti-chevauchement avec **vol après timeout**, **circuit breaker** (5 échecs /
  cooldown 5 min), drainage paginé (borne `maxPagesPerRun`), import → **ACK**,
  avance checkpoint par page. **`eventUuid` d'ACK déterministe** (dérivé du
  requestUuid, UUID v3) → retry d'ACK idempotent côté SC.

**Ce que le moteur natif rend caduc vs l'uploader** : INSERT SQL manuels,
SAVEPOINTs, `reconcileMissingAnalysisIds`, table plate. **Reproduit** : pull
paginé + checkpoint, ACK + retry, verrou + circuit breaker.

**Revue de code (2026-07-16) — 2 bugs majeurs trouvés + corrigés :**

- **`getPatientBySubjectNumber` cassé** : son HQL cible `Patient.subjectNumber`,
  propriété INEXISTANTE (le subject vit dans `patient_identity`) → exception
  runtime → TOUT import portant un `patientSubjectNumber` (cas nominal)
  échouait. **Corrigé** : résolution par IDENTITÉ subject via
  `getPatientIdentitiesByValueAndType(subject, subjectTypeId)` (+ fallback
  external_id) — fixe aussi la dédup subject-only.
- **Checkpoint avançait par page même sur échec** → une demande en échec passait
  derrière le curseur keyset et n'était jamais re-servie (perte). **Corrigé** :
  `processOne` renvoie un booléen (import acquis ET ACK confirmé) ; le
  checkpoint n'avance QUE si la page entière a réussi, sinon le run s'arrête et
  la page est rejouée au prochain tick. Ferme aussi la boucle de re-ACK
  redondante.
- Vérifié contre la source SC : l'ACK "FAILED" **bump `updatedAt`** côté SC →
  une demande échouée revient au run suivant (avec `updatedAt` frais) ; combiné
  à l'`eventUuid = f(requestUuid, importStatus)`, l'ACK "OK" après un "FAILED"
  est bien retraité. Aussi corrigé : garde fail-fast si l'utilisateur système
  `serviceUser` est absent.

**Caveats assumés (résiduels)** : (a) `ElectronicOrder` de ce fork n'expose pas
de setter `organization_id` typé → l'organisation est résolue/créée (dédup
référentiel) et son nom porté par `requesting_facility_name` + `data`, mais pas
liée sur la ligne e-order (rattachement à la création du sample). (b) `testName`
laissé null (le SC ne fournit qu'un libellé générique 'charge virale'). (c)
`electronic_order.external_id` a un index NON unique (comme le flux natif) : un
doublon reste théoriquement possible si le verrou de pull est **volé** alors que
le run précédent est encore vivant (le vol après timeout ne tue pas l'ancien
thread). Mitigation possible (chantier ultérieur) : index unique partiel sur
`external_id` — à ne poser qu'après vérification d'absence de doublons legacy.
(d) Livraison at-least-once (re-import neutralisé par idempotence
`external_id`).

**Reste (incréments suivants)** : 4e = nettoyage legacy + secrets/HTTPS. Config
OE = incrément 5.

## 6.4 Push des STATUTS/RÉSULTATS e-order (incrément 4d) — IMPLÉMENTÉ (local)

**Objectif** : remonter au serveur consolidé (SC) le statut/résultat d'une
demande CV importée (4c) quand OE la traite. Ferme la boucle du flux Eorder.

**Simplification majeure vs uploader** : l'uploader devait **réconcilier a
posteriori** (jointure heuristique patient + test VL + fenêtre de dates, 50
tentatives) faute de lien direct e-order↔sample. **OE POSE ce lien nativement**
: `Accessioner` fixe `sample.clinical_order_id = electronic_order.id` (+
`sample.referring_id = external_id`) à la saisie. →
`reconcileMissingAnalysisIds` **ÉLIMINÉ**.

**Contrat SC `POST /v1/vl-requests/events` (cartographié)** :

- **Idempotence = `eventUuid` seul** (return avant traitement). Rejeu identique
  = no-op ; nouveau statut = **eventUuid différent**.
- **Anti-régression = BLOCAGE TOTAL** de l'event : ordre
  `ACCEPTED < IN_PROGRESS < COMPLETED < RELEASED` ; terminal
  (CANCELLED/REJECTED/FAILED) non écrasable par un statut normal. Un statut « en
  retard » est perdu.
- **Update partiel** : seuls les champs non-null écrasent l'existant.
- **RELEASED + résultat** (nouveau/jamais envoyé) → `RESULT_READY` +
  **republication FHIR asynchrone** côté SC (~2 min, si `interop.sync.enabled`).

**Implémenté (package `dataexchange.sync`, local non poussé) :**

- Migration `create_eorder_sync_status.xml` (table dédiée : request_uuid,
  analysis_id, electronic_order_id, lab_status, **last_sent_lab_status**,
  **last_sent_result**, last_event_uuid, sync_flag 0/1/2/3/4/6) +
  `sql/ eorder_status_cdc_triggers.sql`. Triggers sur analysis/result qui
  **UPSERT directement** la ligne de suivi via le lien natif
  `sample.clinical_order_id → electronic_order.external_id` (fonction
  `eorder_status_upsert`) — pose sync_flag=2, anti-régression de flag (2
  seulement depuis 1/3). Idempotents, `runOnChange`, délimiteur `~`.
- Entité/hbm/DAO/service `EorderSyncStatus` (générateur `identity` = SERIAL ;
  jamais d'insert Hibernate, le trigger insère ; le service ne fait qu'UPDATE ;
  transitions publiques `@Transactional`).
- `EorderEventBuilder` `@Transactional(readOnly)` : mappe statut OE→labStatus
  **par ENUM `AnalysisStatus`** (robuste, PAS par libellé localisé comme
  l'uploader) — Finalized→RELEASED, TechnicalAcceptance→COMPLETED,
  BiologistRejected/ SampleRejected→REJECTED, Canceled→CANCELLED,
  TechnicalRejected→FAILED, + priorité aux dates released/completed. Résultat
  reportable, unité `copies/mL`, `testResultInt` nettoyé. **eventUuid
  déterministe** = hash(requestUuid + labStatus + result + releasedDate).
- `ConsolidatedServerClient.pushEvent` (POST /events, JWT + retry 401
  réutilisés).
- `DataPushTask` `@Scheduled` (60 s, flag
  `org.openelisglobal.consolidated.push.enabled=false`) : draine sync*flag IN
  (2,6), build → **anti-régression** (ne pousse pas un statut inférieur au
  last_sent SAUF nouveau résultat/dates) → push → markSynced (mémorise
  last_sent*\*) / markSendFailed. Verrou vol-timeout + circuit breaker.

**Non porté (volontairement)** : `reconcileMissingAnalysisIds` (lien natif),
`fixStaleElectronicOrderStatuses`, `pullStatusUpdatesFromConsolidated` (pull
inverse `POST /statuses` — optionnel, chantier ultérieur si resync SC→OE
requis).

**Revue de code (2026-07-16, vs source réelle du SC) — 5 corrections :**

1. **Désync silencieuse (max)** : le SC répond **HTTP 200 même sur une
   régression REFUSÉE** (journalise l'eventUuid + `return`, sans exception) et
   bloque **en bloc** sans exception résultat/date. Ma garde d'origine poussait
   une régression dès qu'il y avait un résultat → j'aurais marqué SYNCED un
   statut ignoré par le SC. **Corrigé** : anti-régression **inconditionnelle**
   alignée sur le SC. La correction biologiste (RELEASED inchangé, résultat
   changé) n'est PAS une régression → passe par la garde "rien de nouveau à
   envoyer".
2. **`completedDate` inconditionnel** neutralisait l'ancienne garde
   `hasNewDates` (jamais nul une fois posé) → supprimée avec le fix #1.
3. **Perte sur crash** : ajout de `resetStaleInProgress` (réarme les lignes
   IN_PROGRESS orphelines >5 min, comme le flux analysis_sync) — sinon une ligne
   restait coincée en flag 4 après un crash entre markInProgress et markSynced.
4. **Perte de changement concurrent** : le trigger réarme désormais aussi depuis
   flag **4** (`WHERE sync_flag IN (1,3,4)`), et `markSynced` est **optimiste**
   (ne passe à 3 que si la ligne est restée en 4) → un changement survenu
   PENDANT le push n'est plus perdu.
5. Index UNIQUE explicite sur `request_uuid` (fiabilise le `ON CONFLICT`) ;
   mapping statut : rejet/annulation évalués AVANT la priorité aux dates (une
   analyse re-rejetée avec un `released_date` résiduel remonte REJECTED, pas
   RELEASED) ; `completedDate` ajouté au hash de l'eventUuid.

**Reste (incréments suivants)** : 4e = nettoyage legacy (`vl_analysis_record`,
colonnes `*_fhir_uuid` mortes) + secrets/HTTPS. Config OE = incrément 5.

**Phase 0 — FONDATION : fiabiliser la population FHIR locale (PRIORITAIRE)**

1. **Auditer** `FhirTransformServiceImpl` + `SampleFhirTransformEventListener` :
   ressources produites, données manquantes/incomplètes, points de crash du
   workflow, ressources non couvertes (Questionnaire/QuestionnaireResponse).
2. **Corriger le modèle Task** : Task produit au labo (suivi analyse) ; infos de
   demande → ServiceRequest + QuestionnaireResponse. Adapter la transformation.
3. **Monitoring depuis OE** : écran/état des transformations FHIR (succès/échec,
   ressources incomplètes, file de retry).

**Phase 1 — Conception du module unifié** 4. **Décider l'emplacement** (app
autonome vs intégré à OE). 5. **Moteur de sync unifié** : un seul modèle d'état.
Réutiliser la machine à états CDC de l'uploader (triggers PG, flags) pour la
consolidation ; le delta `lastUpdated` de dataexport pour le FHIR. Unifier. 6.
**Adaptateurs de transport** : (a) JSON/JWT consolidation SC (moderniser
l'existant, retirer le SQL en dur / l'écriture directe des tables OE au profit
des services OE) ; (b) FHIR (Bundle) pour interop.

**Phase 2 — Interopérabilité FHIR** 7. **Exposition FHIR locale (mini-HIE)** :
endpoints REST FHIR exposés PAR LE MODULE (HAPI reste interne) ; ressources
exposées ; réception de demandes externes (ServiceRequest +
QuestionnaireResponse, sans Task). 8. **Push FHIR distant** : moderniser
dataexport (Java 21, HAPI 6.6.2, auth).

**Phase 3 — Intégration & exploitation** 9. **Écrans de config OE** :
activer/désactiver chaque flux, cibles/URLs, intervalles, ressources exposées,
credentials. 10. **Migration** : bascule depuis les 2 composants (état de sync,
e-orders en cours), nettoyage du legacy (`vl_analysis_record`, colonnes
fhir_uuid mortes, 2 mécanismes redondants dataexport). 11. **Sécurité** :
secrets hors code, TLS/mTLS/OAuth, pas de HTTP clair. 12. **Conteneurisation +
CI ghcr** (rejoint le chantier 7).

---

## 7. Fichiers pivots (référence)

- Uploader : `DataSyncServiceImpl.java`, `EorderSyncServiceImpl.java` (1640 l.,
  cœur e-order), `optimized_sync_triggers.sql`, `eorder_sync_triggers.sql`,
  entités `AnalysisSyncStatus`/`EorderSyncStatus`/`EorderRequestFlat`.
- Dataexport : `dataexport-api/.../DataExportServiceImpl.java` (cœur FHIR
  delta), `DataExportTaskCheckerServiceImpl.java` (scheduler),
  `DataExportAttempt.java` / `DataExportTask.java` (état).
- OE : `FhirTransformServiceImpl.java` (métier→FHIR, 1668 l.), `FhirUtil.java`
  (client, `FhirClientFetcher` impl), `FhirPersistanceServiceImpl.java`,
  `RegisterFhirHooksTask.java` (provisioning + subscriptions),
  `FhirExportController.java` / `FhirTransformationController.java`
  (déclencheurs).
