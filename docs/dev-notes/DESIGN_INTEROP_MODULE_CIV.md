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
