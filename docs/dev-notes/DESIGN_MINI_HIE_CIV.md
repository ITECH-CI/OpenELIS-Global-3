# Mini-HIE local — cadrage (CIV)

> Exposer de façon **contrôlée** le serveur FHIR de la stack pour
> l'interopérabilité locale d'un centre de santé : lecture des ressources métier
> par des tiers (SIGDEP, autre EMR/LIS) et **réception** de demandes d'analyse
> (ServiceRequest + QuestionnaireResponse) qu'OpenELIS transforme en
> Sample/Analysis + Task.
>
> Rôle (2) de la vision interop unifiée (cf. `DESIGN_INTEROP_MODULE_CIV.md`).

## 1. Décisions actées (Pascal, 2026-07-13)

| #   | Sujet               | Décision                                                                                                                                                                                                                                |
| --- | ------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | Ressources exposées | Patient, ServiceRequest, Organization, DiagnosticReport, Observation, Specimen, Task, Practitioner, Encounter + **Questionnaire / QuestionnaireResponse**. Lecture **et** écriture, lecture séquentielle (pagination / `_lastUpdated`). |
| 2   | Serveur FHIR        | **Réutiliser le HAPI existant** (`external-fhir-api`, `hapiproject/hapi:v6.6.0`) — pas de RestfulServer à écrire.                                                                                                                       |
| 3   | Sécurité d'accès    | **Token** + **IP allowlist** (+ **mTLS** si pas trop lourd). Le HAPI **ne doit pas** être joignable directement sur le réseau : accès via une gateway contrôlée (nginx).                                                                |
| 4   | Réception           | **Lecture seule** pour la plupart des ressources ; **réception de ServiceRequest + QuestionnaireResponse** (sans Task) → **OE construit le Task** (suivi de l'analyse au labo).                                                         |

## 2. Architecture cible

```
Tiers du centre (SIGDEP-CV, EMR, autre LIS)
        │  FHIR R4 sur HTTPS  (token + IP allowlist [+ mTLS])
        ▼
   ┌─────────────── nginx-proxy (porte d'entrée réseau, 443) ───────────────┐
   │  location /fhir/*   (GET/search)  ── lecture contrôlée ──►  HAPI local  │  ← rôle "mini-HIE lecture"
   │  location /fhir-in/ (POST bundle)  ── réception ──►  OE webapp          │  ← rôle "réception métier"
   └────────────────────────────────────────────────────────────────────────┘
        │                                              │
        ▼ (lecture directe HAPI, sécurisée)            ▼ (écriture métier)
   HAPI external-fhir-api                         OE webapp
   (ressources déjà peuplées                      → parse ServiceRequest+QR
    par FhirTransformService)                     → crée Sample/Analysis (ElectronicOrder)
                                                  → CONSTRUIT le Task (suivi)
                                                  → persiste dans le HAPI local
```

Deux chemins distincts, **volontairement séparés** :

- **Lecture** (`/fhir/...`) : proxy nginx direct vers le HAPI. Les ressources
  sont déjà là (produites par `FhirTransformServiceImpl`). Rapide, pas de code
  métier.
- **Réception d'ordres** (`/fhir-in/...`) : **ne passe PAS** par le HAPI brut,
  mais par un **nouvel endpoint OE**, car créer un ordre doit déclencher le
  workflow métier (Sample/Analysis + Task construit par OE), pas juste stocker
  un Task fourni.

## 3. Sécurité (contrainte forte : HAPI non exposé en direct)

État actuel (audit) :

- Le HAPI **publie** aujourd'hui `8081:8080` (HTTP clair) et `8444:8443` (HTTPS
  mTLS) sur l'hôte → joignable directement. **À FERMER** en prod (retirer ces
  `ports:` du compose ; garder l'accès inter-conteneurs par le réseau
  `default`).
- Le HAPI n'a **aucune auth applicative** (CORS `*`, openapi on) — il repose sur
  l'isolation réseau Docker. Donc toute la sécurité d'accès tiers se met **au
  niveau nginx**.

Mesures nginx (nginx standard `1.15-alpine`, toutes ces directives supportées) :

1. **IP allowlist** : `allow <ip_tiers>; deny all;` sur les `location` FHIR
   (module `ngx_http_access_module`). Avec `real_ip` si derrière un autre proxy.
2. **Token** :
   `map $http_authorization $fhir_ok { default 0; "~^Bearer <token>$" 1; }`
   - `if ($fhir_ok = 0) { return 401; }`. Ou `auth_request` vers un
     micro-endpoint OE qui valide un token par client (plus souple, révoquable).
3. **mTLS (optionnel)** : `ssl_client_certificate <ca>; ssl_verify_client on;`
   sur la `location` FHIR — réutilise l'infra de certificats de la stack
   (`certs`).

Auth **de la réception** (`/fhir-in/` → OE) : réutilise le `SecurityConfig` OE
existant (Basic auth déjà actif, ou activer la chaîne x509/certificat). Le
nouvel endpoint de réception sera sous `/rest/**` (authentifié).

## 4. Réception ServiceRequest + QR → Sample + Task (le vrai travail applicatif)

**Ce qui existe déjà** (réutilisable) :

- `FhirApiWorkFlowServiceImpl.saveRemoteTaskAsLocalTask(...)` +
  `TaskWorker.handleOrderRequest()` →
  `interpreter.interpret(task, serviceRequest, patient)` →
  `IOrderPersister.persist(...)` qui crée un `ElectronicOrder` (type `FHIR`)
  puis un Sample. **Toute la chaîne FHIR→métier existe.**
- `transformToTask(Sample)` construit un Task **à partir d'un Sample OE**.

**Ce qui manque** (à construire) :

- Aujourd'hui l'import **part du Task** (`search().forResource(Task.class)` en
  polling). La cible est l'inverse : le tiers envoie **ServiceRequest + QR SANS
  Task**, et **OE construit le Task**.
- Il faut donc :
  1. Un **endpoint de réception** `POST /rest/fhir-in/order` (ou similaire) qui
     accepte un Bundle
     `{ServiceRequest, QuestionnaireResponse, Patient?, Specimen?}`.
  2. Un adaptateur qui, à partir du **ServiceRequest** (pas du Task), appelle la
     chaîne existante `interpret` → `IOrderPersister.persist` pour créer le
     Sample.
  3. Le Task est **construit par OE** via `transformToTask(sample)` sur le
     Sample nouvellement créé (le flux d'enregistrement le fait déjà) — donc
     **rien à inventer côté Task**, juste ne PAS attendre de Task entrant.
- Liaison QR : `QuestionnaireResponse.based-on → ServiceRequest` (déjà le
  pattern utilisé en polling), à reproduire pour l'entrant.

## 5. Lecture séquentielle / exposition

- La lecture passe par le HAPI (déjà capable de `search`, `_count`,
  `_lastUpdated`, `_since` sur un bundle history). Aucun code OE nécessaire —
  nginx proxifie.
- Restreindre éventuellement les **types de ressources** exposés en lecture au
  niveau nginx (whitelist de chemins `/fhir/Patient`, `/fhir/DiagnosticReport`,
  …) si on ne veut pas tout ouvrir.

## 6. Plan par phases

- **Phase A — Exposition lecture sécurisée** (rapide, peu de code) :
  - Fermer les ports HAPI publiés (compose).
  - Ajouter `location /fhir/` dans `nginx-prod.conf` : proxy vers
    `fhir.openelis.org`, IP allowlist + token (+ mTLS optionnel).
  - Doc opérateur (comment déclarer un tiers : IP + token).
- **Phase B — Réception d'ordres** (applicatif) :
  - Endpoint OE `POST /rest/fhir-in/order` (Bundle ServiceRequest+QR).
  - Adaptateur ServiceRequest→Sample réutilisant `interpret`/`IOrderPersister`.
  - OE construit le Task (via le flux d'enregistrement existant).
  - Tests bout-en-bout (un tiers simulé POST un ordre → Sample créé + Task
    présent).
- **Phase C — Durcissement / durée** :
  - Révocation de token (auth_request vers OE), audit des accès, quotas
    éventuels.
  - Restriction fine des ressources exposées en lecture.

## 7. Décisions d'implémentation (2026-07-13)

- **Token : DYNAMIQUE** (table de tokens en base OE, un par tiers, révocable à
  chaud, horodatage d'accès pour audit). nginx valide via `auth_request` vers un
  micro-endpoint OE `GET /rest/fhir-gateway/auth` qui renvoie 200/401 selon le
  token (header `Authorization`/`X-API-Key`). ⇒ inclus **dès la Phase A**.
- **mTLS : OPTIONNEL** — Phase A = token + IP allowlist seulement. mTLS ajouté
  en Phase C si les tiers peuvent présenter un certificat client.
- **Format de réception (Phase B)** : **Bundle FHIR** (transaction /
  collection), interopérable, ce que les tiers FHIR émettent.
- **Démarrage : Phase A**.

## 8. Phase A — détail d'implémentation (token dynamique + exposition lecture)

Côté OE (backend) :

- Entité `FhirGatewayToken` (table `fhir_gateway_token`) : id, token (hashé),
  client_name, is_active, created_at, last_used_at. Migration Liquibase
  idempotente
  - hbm + DAO/Service.
- Endpoint `GET /rest/fhir-gateway/auth` : lit le header
  (`Authorization: Bearer <t>` ou `X-API-Key`), vérifie le token actif, met à
  jour `last_used_at`, renvoie **200** (autorisé) ou **401**. Léger, sans corps.
  Doit être accessible SANS session (chaîne de sécurité OE : ajouter à
  `OPEN_PAGES` ou une chaîne dédiée, mais protégé par le fait qu'il ne fait que
  valider un token).
- (Admin) page de gestion des tokens (créer/révoquer par tiers) — peut venir
  après.

Côté nginx (`nginx-prod.conf`) :

- Fermer la publication des ports HAPI (`docker-compose*.yml` : retirer
  `8081:8080` / `8444:8443`).
- `location = /fhir-gateway/auth { internal; proxy_pass https://oe.openelis.org:8443/api/rest/fhir-gateway/auth; ... }`
- `location /fhir/ { auth_request /fhir-gateway/auth; allow <ip>; deny all; proxy_pass https://fhir.openelis.org:8443/fhir/; ... }`
- Propager le header d'auth à l'`auth_request`.

Résultat : un tiers autorisé (IP + token valide) peut lire les ressources FHIR ;
tout token révoqué en base bloque immédiatement l'accès (401).

## 9. Phase A — LIVRÉE (2026-07-13)

Implémentation (modèle à 2 entités : un tiers -> N jetons, pour la rotation) :

- `FhirGatewayClient` (table `fhir_gateway_client`) : tiers déclaré (nom unique,
  description, `is_active`). Désactiver le client bloque tous ses jetons.
- `FhirGatewayToken` (table `fhir_gateway_token`, `client_id` FK) : jeton HASHÉ
  (SHA-256, jamais en clair), `is_active` (révocation par jeton), `last_used_at`
  (audit). Migrations `create_fhir_gateway_token.xml` +
  `create_fhir_gateway_client.xml`.
- Validation (`validateAndTouch`) : jeton actif ET client actif -> 200.
- `FhirGatewayRestController` (`/rest/fhir-gateway`) :
  - `GET /auth` — validation pour nginx `auth_request` (200/401). Ouvert
    (OPEN_PAGES).
  - Administration (session OE) : `GET/POST /clients`,
    `POST /clients/{id}/active`, `GET/POST /clients/{id}/tokens` (POST émet un
    jeton et renvoie sa valeur EN CLAIR une seule fois),
    `POST /tokens/{id}/revoke`.
  - Page admin `#FhirGateway` : lister/créer des tiers, émettre un jeton
    (affiché une fois, copiable), révoquer un jeton, activer/désactiver un
    tiers.
- nginx `nginx-prod.conf` : `location = /fhir-gateway/auth` (internal) +
  `location /fhir/` (auth_request + IP allowlist à décommenter + proxy vers
  HAPI).
- `docker-compose.civ.yml` : ports HAPI (8081/8444) **fermés** — HAPI non
  exposé.

Testé end-to-end : sans jeton → 401 ; jeton invalide → 401 ; jeton valide
(Bearer ou X-API-Key) → 200 ; `last_used_at` mis à jour ; révocation
(`is_active=N`) → 401 immédiat.

### Exploitation (déclarer un tiers)

1. Créer un jeton (admin OE authentifié) :
   `POST /api/rest/fhir-gateway/token?clientName=SIGDEP-CV` → renvoie le jeton
   en clair (à transmettre au tiers une seule fois).
2. Optionnel : restreindre par IP — décommenter/ajuster `allow ...; deny all;`
   dans la `location /fhir/` de `nginx-prod.conf`, puis recharger nginx.
3. Le tiers lit les ressources FHIR : `GET https://<serveur>/fhir/Patient?...`
   avec header `Authorization: Bearer <jeton>` (ou `X-API-Key: <jeton>`).
4. Révoquer : passer `is_active` à `N` (ou via une future UI admin) → accès
   bloqué immédiatement.

## 10. Phase B — B1 LIVRÉE (2026-07-14)

**Réception PUSH d'ordres FHIR par un tiers**, réutilisant le **moteur d'ordre
unique** natif (celui du polling FHIR et du workflow charge virale
`StudyElectronicOrder`), pas un chemin parallèle.

Côté OE (backend) :

- `FhirInboundRestController` (`POST /rest/fhir-in/order`) : lit le corps brut
  via `HttpServletRequest.getReader()` (**pas** `@RequestBody String`, qui fait
  passer le JSON par Jackson et échoue en 400). 201 si un ordre est créé, 422
  si le Bundle est valide mais non exploitable (test/patient incomplet, ordre
  déjà reçu → `DUPLICATE_ORDER`).
- `FhirOrderReceptionService(Impl)` : parse le Bundle (Patient +
  ServiceRequest[] + QuestionnaireResponse[] + Specimen[]), attribue des ids
  locaux stables, **stampe le Patient d'un identifiant `externalId`** au format
  attendu par `TaskInterpreterImpl` (type coding `<oeFhirSystem>/genIdType` =
  `externalId`, valeur = UUID local), persiste dans le store FHIR local, puis
  pour chaque ServiceRequest construit un **Task minimal** (status REQUESTED,
  `for` patient, `basedOn` SR) et le passe au **`TaskWorker`** natif
  (`interpret` → `DBOrderExistanceChecker` → `IOrderPersister`), qui crée un
  **`ElectronicOrder` "Entered"** (`status_of_sample` id 21, EXTERNAL_ORDER).
- `SecurityConfig` OPEN_PAGES : `/rest/fhir-in/**` (sécurité appliquée en amont
  par nginx `auth_request`, même jeton que `/fhir/`).

Côté nginx (`nginx-prod.conf` + `nginx.conf` dev) :

- `location /fhir-in/` : `auth_request /fhir-gateway/auth` (jeton) + proxy vers
  `.../rest/fhir-in/` (OE, **pas** le HAPI). IP allowlist à décommenter comme
  pour `/fhir/`.

**Point clé (workflow unique)** : un Patient reçu d'un tiers n'a aucun
identifiant OE ; sans le stamp `externalId`, la reprise d'ordre plante (le
persister interroge la BD avec un externalId null → `varchar = bytea`). Le stamp
reproduit exactement ce que fait le flux natif
(`FhirApiWorkFlowServiceImpl#createIdentifierToRemoteResource`).

Testé end-to-end : POST direct (8443) et **via nginx** (443, `Authorization:
Bearer <jeton>`) → 201 + `electronic_order` "Entered" créé ; rejeu du même
numéro d'ordre → 422 `DUPLICATE_ORDER` ; sans jeton → 401. L'ordre apparaît dans
la file d'entrée d'échantillon (comme un ordre reçu par le polling natif), où
l'opérateur crée l'échantillon et OE construit le Task.

**Reste (B2, chantier suivant)** : exploiter le `QuestionnaireResponse` reçu
(actuellement stocké mais ignoré) — rattachement à l'ElectronicOrder, sélection
auto du Programme depuis le code du ServiceRequest, pré-remplissage des
`additionalQuestions` de l'entrée d'échantillon.
