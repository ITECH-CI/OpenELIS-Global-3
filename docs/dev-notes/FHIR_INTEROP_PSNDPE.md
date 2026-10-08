# Exposition FHIR OpenELIS pour PSNDPE — rapport d'écart & livraison

Contexte : un connecteur externe parcourt l'API FHIR R4 d'OpenELIS et pousse les
ressources vers le dépôt central (SHR), qui les valide contre un IG national
(rejet 422 si non conforme). Ce document = état des lieux + ce qui a été
implémenté + requêtes de validation.

Audit réalisé sur l'instance live (HAPI 6.6.0, R4) : 30 Patient, 207
ServiceRequest, 161 Observation, 133 DiagnosticReport, 15 Specimen.

## Rapport d'écart (exigences 1→5)

| #   | Exigence                                             | Statut                | Notes                                                                                                                                                                                                                                                                                                                                                                                |
| --- | ---------------------------------------------------- | --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 1   | Endpoint R4 HTTPS + /metadata + ressources peuplées  | **FAIT**              | HAPI `hapiproject/hapi:v6.6.0`, R4 (4.0.1), `GET /metadata` → CapabilityStatement. Exposé en HTTPS via nginx `location /fhir/` (TLS terminé nginx ; HAPI non publié en prod). ServiceRequest/Observation/DiagnosticReport/Patient/Practitioner/Specimen peuplés. **Organization = 0** (à peupler si l'IG l'exige).                                                                   |
| 2   | Pagination + `_lastUpdated` + `_sort`                | **FAIT** (natif HAPI) | Bundle `searchset` + liens `self`/`next` ; `_lastUpdated=ge<ts>` opérationnel ; `meta.lastUpdated` régénéré par HAPI à chaque PUT (re-transformation sur modif de résultat/commande) ; `_sort=_lastUpdated` OK. Limite : `max_page_size=200`.                                                                                                                                        |
| 3   | Identité patient (matricule CMU/CNAM + OID national) | **FAIT (implémenté)** | Avant : le nationalId sortait uniquement avec le system OpenELIS interne. Après : identifiant CMU exposé avec l'OID national `urn:oid:1.3.6.1.4.1.53864.1.3` (configurable), `use=official`. Type d'identité dédié `CMU` + repli sur nationalId. **Validé live** (voir requête 1).                                                                                                   |
| 4   | Codage LOINC/UCUM + statuts + références             | **PARTIEL**           | Références DiagnosticReport→Observation→ServiceRequest→Patient : **conformes**. Statuts : **conformes**. **UCUM : implémenté** (system/code UCUM sur les Quantity ; 44/49 unités mappées). **LOINC : déficit de DONNÉES** — seulement 22/368 tests ont un LOINC ; le code pose déjà le LOINC quand présent. **meta.profile** : non déclaré (en attente des URLs de profils de l'IG). |
| 5   | Auth compte de service LECTURE                       | **DISPONIBLE**        | Gateway FHIR existante (`#FhirGateway`) : jeton opaque par client, lecture seule (GET/HEAD), restriction par ressource, quota, audit, HTTPS. Provisionner un client dédié au connecteur (procédure ci-dessous). Pas d'OAuth2/scopes (jeton opaque = équivalent fonctionnel).                                                                                                         |

## Ce qui a été implémenté (diff résumé)

**Exigence 3 — identité CMU/OID :**

- `FhirConfig` : property `org.openelisglobal.cmu.identifier.system` (défaut
  `urn:oid:1.3.6.1.4.1.53864.1.3`).
- `PatientService.getCMUNumber(patient)` : type d'identité `CMU` puis repli sur
  nationalId (qui héberge historiquement le code CMU).
- `FhirTransformServiceImpl.createPatientIdentifiers` : ajoute l'identifiant CMU
  avec l'OID (en plus des identifiants OE internes) ; `createIdentifier` marque
  CMU + nationalId en `use=OFFICIAL` ; le chemin de recherche retour reconnaît
  l'OID CMU.
- Migration `add_cmu_patient_identity_type.xml` : type d'identité `CMU`.

**Exigence 4 — UCUM :**

- `UnitOfMeasure.ucumCode` (colonne `ucum_code` déjà présente en base, désormais
  mappée) + `ResultService.getUcumCode(result)`.
- `FhirTransformServiceImpl.buildQuantity` : Quantity avec
  `system = http://unitsofmeasure.org` + `code` UCUM (fallback : libellé seul si
  pas de code). Appliqué aux résultats numériques et charge virale.
- Migration `populate_ucum_codes.xml` : mapping initial 44/49 unités (5 unités
  ambiguës laissées NULL : `num/champ`, `Unité`, `ppl`, `ppm`, `/champ`).

**Tests ajoutés :**

- `FhirPatientIdentifierTest` : CMU/nationalId en `use=OFFICIAL`, system interne
  en `USUAL`, OID par défaut correct.
- `PatientServiceTest.getCMUNumber_*` : repli sur nationalId, null → "".
- `UnitOfMeasureServiceTest.getUnitOfMeasureById_shouldExposeUcumCode` : code
  UCUM mappé et lisible.

## Requêtes de validation

**Requête 1 (identité CMU) — VALIDÉE LIVE :**

```
GET /fhir/Patient?identifier=urn:oid:1.3.6.1.4.1.53864.1.3|<matricule>
```

→ renvoie le(s) patient(s) portant ce matricule CMU (testé avec `90909000000` →
2 patients ; 8 patients au total portent l'identifiant CMU `use=official`). NB :
les patients doivent avoir été (re)transformés après le déploiement de ce
changement (`GET /PatientToFhir?checkAll=true` pour un backfill).

**Requête 2 (DiagnosticReport paginé + liens + subject CMU) :**

```
GET /fhir/DiagnosticReport?_count=5&_lastUpdated=ge2026-01-01
```

→ Bundle `searchset` paginé (liens self/next), chaque DiagnosticReport avec
`result` → Observation, `basedOn` → ServiceRequest, `subject` → Patient
(résoluble par l'identifiant CMU une fois les patients re-transformés). L'UCUM
apparaît sur les Observation numériques re-transformées après ce changement.

## Reste à faire (hors code)

- **LOINC (données)** : 329 tests actifs sans LOINC à mapper par les
  biologistes. Export pour faciliter la saisie :
  ```sql
  SELECT t.id, t.description, ts.name AS section
  FROM clinlims.test t LEFT JOIN clinlims.test_section ts ON ts.id = t.test_section_id
  WHERE t.is_active = 'Y' AND (t.loinc IS NULL OR t.loinc = '')
  ORDER BY ts.name, t.description;
  ```
- **UCUM (données)** : valider/compléter le mapping des 5 unités ambiguës + les
  nouvelles unités (colonne `ucum_code`, éditable).
- **meta.profile** : à déclarer une fois les URLs de profils de l'IG national
  fournies (point d'injection identifié : à chaque
  `new ServiceRequest/Observation/DiagnosticReport`).
- **OID CMU** : confirmer `urn:oid:1.3.6.1.4.1.53864.1.3` avec l'équipe interop
  (ajustable via la property, sans recompilation).
- **Backfill** (à faire après déploiement) : voir la procédure détaillée
  ci-dessous.
- **Site demandeur** : Organization + Location, voir ci-dessous (lot BDM pilote,
  octobre 2026).

## Backfill (re-transformation de l'existant) — procédure vérifiée

Les changements (identifiant CMU, UCUM) ne s'appliquent qu'aux ressources
(re)transformées APRÈS déploiement. Les ressources déjà dans le HAPI gardent
leur ancienne forme jusqu'à re-transformation. HAPI régénère `meta.lastUpdated`
à chaque PUT ; le connecteur reverra donc ces ressources via `_lastUpdated`.

Endpoints (session admin) :

- `GET /OEToFhir?checkAll=true&waitForResults=true` — re-transforme **tout** :
  phase 1 patients (avec échantillon), phase 2 échantillons ET leurs objets
  rattachés (ServiceRequest, Observation, DiagnosticReport). C'est le backfill
  complet. **Synchrone** avec `waitForResults=true` (la requête ne rend la main
  qu'à la fin ; peut être longue).
- `GET /PatientToFhir?checkAll=true` — re-transforme uniquement les patients
  (plus rapide si seul l'identifiant CMU est concerné).
- `GET /OEToFhir/info` — état de la transformation en cours (objectType, phase,
  running).

**Validé** : après un `OEToFhir?checkAll=true`, les Observations numériques
portent l'UCUM (`valueQuantity.system = http://unitsofmeasure.org` + `code`) et
les patients l'identifiant CMU.

⚠️ **Piège d'exploitation** : le contrôleur refuse une nouvelle transformation
tant qu'une précédente est marquée « en cours » (garde `inProcess()` /
`info.running`). Si une exécution a été interrompue/a échoué sans libérer ce
verrou, tout nouvel appel `OEToFhir`/`PatientToFhir` **renvoie immédiatement**
(<0,1 s) l'`info` figé (ex. `phase=Finished`, `objectType=Patient`) **sans rien
faire** — symptôme : aucun log de transformation, `batchSize` = valeur par
défaut et non celle demandée. **Cause trouvée (corrigée en octobre 2026)** :
`/PatientToFhir` ne libérait jamais le verrou, même après une exécution réussie.
Le verrou est désormais libéré en fin d'exécution (y compris sur erreur) par
`/PatientToFhir`, `/OEToFhir` et `/OrganizationToFhir`. Sur une version
antérieure, le contournement reste de redémarrer la webapp.

### Rattrapage des organisations (sites demandeurs)

À passer **avant** celui des échantillons, dont les ServiceRequest et Task
référencent les sites :

1. Au démarrage, le changeset `assign-organization-fhir-uuid-1` attribue un
   `fhir_uuid` aux organisations qui n'en ont pas (automatique).
2. `GET /OrganizationToFhir?batchSize=200` écrit toutes les organisations et
   leur Location dans le HAPI. Mesuré en dev : 1 694 organisations, 9 lots, 42
   s.
3. `GET /OEToFhir?checkAll=true&waitForResults=true` réécrit ensuite patients et
   échantillons. Ce même appel enchaîne de lui-même l'étape 2 en premier : il
   suffit à lui seul.

Contrôle : `GET /fhir/Organization?_summary=count` et
`GET /fhir/Location?_summary=count` > 0.

## Site demandeur (Organization + Location)

Une instance OpenELIS est un laboratoire ; le **site demandeur** d'un
échantillon (centre de santé, hôpital) est une organisation OE de type «
referring clinic ». Il est exposé ainsi :

| Ressource                       | Champ                  | Valeur                                                    |
| ------------------------------- | ---------------------- | --------------------------------------------------------- |
| `Task` (une par bon)            | `requester`            | `Organization/<fhir_uuid du site>`                        |
| `ServiceRequest` (une par test) | `locationReference`    | `Location/<id dérivé du site>`                            |
| `Location`                      | `managingOrganization` | `Organization/<fhir_uuid du site>`                        |
| `Organization`                  | —                      | nom, `active`, identifiants `org_uuid` / `org_code`, type |

- **Pourquoi une Location** : en R4, `ServiceRequest.locationReference`
  n'accepte que `Reference(Location)`. Le HAPI 6.6 rejette une Organization sur
  ce champ (HAPI-0931, 422), et avec elle **tout le lot FHIR de l'échantillon**.
  C'était le cas depuis la 3.3.1.0 pour les sites créés depuis l'écran
  d'administration (les seuls à avoir un UUID).
- **Id de la Location** : UUID déterministe dérivé de celui de l'organisation
  (`UUID.nameUUIDFromBytes("Location:" + fhir_uuid)`), stable d'une
  transformation à l'autre.
- **Écriture** : l'Organization et sa Location partent dans la **même
  transaction** que les ServiceRequest et la Task qui les référencent (saisie,
  validation, rattrapage). Une organisation est aussi réécrite depuis l'écran
  d'administration des organisations. Un échec d'écriture FHIR ne fait jamais
  échouer la saisie (transformation asynchrone, tracée dans `#FhirSyncMonitor`).
- **UUID** : toute organisation reçoit un `fhir_uuid` à sa création, quel que
  soit le chemin (`OrganizationServiceImpl`), et le garde.
- **Lecture côté connecteur** :
  - `GET /fhir/Task?...&_include=Task:requester` → Task + Organization du site ;
  - depuis un ServiceRequest :
    `GET /fhir/Location/<id>?_include=Location:organization` (le ServiceRequest
    n'a pas de paramètre de recherche `location` en R4, donc pas de `_include`
    direct).
- **Hors périmètre** : l'identité du laboratoire lui-même (performer,
  Organization « labo », code DHIS2) est déclarée par la configuration du
  connecteur de la BDM, pas par OpenELIS.

## Dates

- `ServiceRequest.authoredOn` = date de la demande saisie sur le bon
  (observation `requestDate`), sinon date de saisie de l'échantillon ; précision
  au jour.
- `Specimen.receivedTime` = date et heure de réception de l'échantillon ; absent
  si non renseigné.
- `DiagnosticReport.effective[x]` = prélèvement de l'échantillon ;
  `DiagnosticReport.issued` = dernière validation de ses analyses.
- Plus aucune de ces dates ne vaut l'heure de la transformation : un rattrapage
  ne les décale plus.

## Provisionner le compte de service LECTURE (connecteur)

Via l'écran admin `#FhirGateway` (ou l'API) :

1. Créer un client dédié :
   `POST /rest/fhir-gateway/clients?name=Connecteur-PSNDPE`.
2. Restreindre au périmètre lecture : politique du client → ressources
   autorisées
   `Patient,Practitioner,Specimen,ServiceRequest,Task,DiagnosticReport,Observation,Organization,Location`
   (`Task` : lue par le connecteur de la BDM ; `Location` : cible de
   `ServiceRequest.locationReference`) (+ quota si besoin).
3. Émettre un jeton : `POST /rest/fhir-gateway/clients/{id}/tokens` → jeton en
   clair une seule fois.
4. Fournir au connecteur : URL de base `https://<host>/fhir/`, header
   `Authorization: Bearer <jeton>`, rôle = lecture seule (GET/HEAD), périmètre =
   ressources ci-dessus. Journal d'accès consultable dans `#FhirGateway`.
