# OpenELIS CIV 3.3.1.4 — notes de version

Cette version émet le numéro CMU des patients sous le système national, garde le
numéro CMU des patients reçus par les demandes électroniques, explique le refus
d'une demande, en suit les étapes jusqu'au résultat, laisse la plateforme
déposer et annuler ses demandes par la passerelle, et date les résultats FHIR du
prélèvement. Les demandes de routine arrivent dans une fenêtre « Demandes reçues
», séparée des demandes d'étude charge virale ; une demande peut aussi être
saisie localement. Un résultat corrigé sort en `corrected`. Détails :
`FHIR_INTEROP_PSNDPE.md` (« Numéro CMU », « Dates ») et
`EXPLOITATION_PASSERELLE_FHIR.md` (§5, demandes électroniques).

Le numéro CMU, le matricule CNAM et le numéro de sécurité sociale sont le même
numéro, celui de l'assuré.

## Ce qui change pour le laboratoire

- **Fiche patient** : le champ s'appelle « Numéro CMU (matricule CNAM, n° de
  sécurité sociale) ». Il accepte un champ vide ou 13 chiffres ; espaces, tirets
  et points sont retirés à l'enregistrement. Une valeur non conforme est refusée
  avec un message. En saisie de demande, un patient déjà enregistré avec un
  ancien numéro non conforme n'est pas bloqué ; le numéro se corrige dans sa
  fiche.
- **Demandes électroniques** : relevées toutes les 60 s au lieu de 120 s. Le
  patient d'une demande reçue est repris s'il existe déjà avec le même numéro
  CMU, au lieu d'être créé une seconde fois ; sa fiche n'est pas modifiée, seul
  son numéro CMU est complété. Deux patients existants portant le même numéro ne
  sont pas fusionnés.
- Une demande dont l'examen n'est connu que sous un autre code LOINC peut être
  acceptée grâce à une équivalence réglée (voir « Réglages »).
- **Fenêtre « Demandes reçues »** (menu des demandes électroniques, avec un
  compteur des demandes à traiter) : ouverte sur les demandes à traiter, triées
  par priorité puis par ancienneté, rafraîchie toutes les 60 s. Une ligne par
  demande : date, priorité, patient et numéro CMU (badge « identité provisoire »
  sans numéro), établissement, prescripteur, examens, origine, étape,
  ancienneté. Filtres par étape, origine, établissement et période ; recherche
  par numéro CMU, nom ou numéro de demande.
  - « Prendre en charge » ouvre le formulaire de saisie pré-rempli avec **tous**
    les examens de la demande. Un examen à un seul type d'échantillon possible
    est placé d'office ; s'il en admet plusieurs, l'opérateur choisit. Le
    patient peut être remplacé par un patient existant (recherche du
    formulaire). Une fois le bon enregistré, la ligne passe en lecture seule
    avec le numéro de laboratoire.
  - « Refuser » et « Annuler », avec un motif de la liste des événements
    qualité, relayé au demandeur ; « Voir le détail » montre les renseignements
    cliniques et la note du prescripteur.
- **Demandes d'étude** : l'écran des demandes d'étude charge virale ne montre
  plus que les demandes SIGDEP ; les demandes de routine de la plateforme n'y
  apparaissent plus.
- **« Nouvelle demande »** (désactivée par défaut) : un opérateur titulaire du
  droit « Enregistrer des demandes » (accueil, salle de prélèvement), sans
  saisie d'échantillon ni accès aux résultats, enregistre une demande : patient,
  prescripteur, établissement, examens, numéro de venue facultatif. Elle rejoint
  les demandes à traiter ; rien n'est émis avant la prise en charge.
- **Annulation par le demandeur** : une demande pas encore prise en charge
  quitte la liste à traiter ; une demande déjà prise en charge n'est pas annulée
  : la ligne et le bon (note) affichent « annulée par le demandeur », et le
  laboratoire décide.
- **Résultat corrigé** : un résultat validé puis modifié n'est plus émis tant
  qu'il n'est pas revalidé (la dernière version validée reste en place) ; une
  fois revalidé, il sort en `corrected`, et son compte rendu aussi.
- **Administration** : l'administration de la passerelle FHIR (clients, jetons,
  écriture, journal) et la réconciliation du référentiel des établissements sont
  réservées à l'administrateur (elles étaient ouvertes à tout utilisateur
  connecté).

## Ce qui change pour la plateforme

- `Patient.identifier` porte `https://hie.ci/patient/cmu|<numéro>` (et la même
  valeur sous `urn:oid:1.3.6.1.4.1.53864.1.3`), `use=official`, seulement pour
  un numéro conforme ; un numéro mal formé n'est pas émis. Les références au
  patient (`subject`, `for`) portent le même identifiant.
  `GET /fhir/Patient?identifier=https://hie.ci/patient/cmu|<numéro>` retrouve le
  patient.
- À la réception, le numéro CMU est lu sous `https://hie.ci/patient/cmu`,
  `urn:oid:1.3.6.1.4.1.53864.1.3` ou `urn:oid:1.3.6.1.4.1.53864.1.2`.
- Une demande refusée (`Task.status = rejected`) porte un motif court en
  `statusReason.text`, par exemple `examen non reconnu : LOINC 20447-9` ou
  `patient non identifiable`, sans donnée nominative. Avec
  `remote.source.updateStatus=true`, il est écrit sur la Task de la plateforme.
- `Observation.effective[x]` vaut la date du prélèvement (comme
  `DiagnosticReport.effective[x]`), y compris pour les isolats, antibiogrammes
  et flores de bactériologie ; `Observation.issued` reste la date de validation.
  Ces dates ne bougent plus d'une transformation à l'autre.
- La Task d'une demande reçue suit le bon d'OpenELIS : `accepted`, puis
  `in-progress` (échantillon reçu, puis en analyse), puis `completed` avec le
  compte rendu en `output` ; ou `rejected` / `cancelled`. L'étape est aussi
  codée en `businessStatus`
  (`https://hie.ci/fhir/CodeSystem/etape-demande-examen` : `acceptee`,
  `echantillon-recu`, `en-analyse`, `resultat-disponible`, `refusee`,
  `annulee`). Une étape ne recule jamais ; un état terminal n'est jamais
  remplacé.
- **Écriture par la passerelle** : un seul client de passerelle peut être
  autorisé à écrire, et seulement par `PUT /fhir/<Type>/<id>`, pour ses types
  autorisés et des identifiants qui commencent par son préfixe (`bdms-`). La
  plateforme y écrit `Patient`, `Specimen`, `ServiceRequest`, puis la `Task`, et
  OpenELIS relève ces demandes sur son propre serveur. `POST`, `DELETE`,
  `PATCH`, transaction et identifiants hors préfixe restent refusés (403) ;
  chaque écriture est journalisée. Les demandes de la plateforme ne passent pas
  par `/fhir-in/` : voir `EXPLOITATION_PASSERELLE_FHIR.md` §5.
- **Routine** : une Task étiquetée `https://hie.ci/echanges/origine|plateforme`
  (ou dont l'identifiant a le préfixe `bdms-`) est une demande de routine. Une
  catégorie explicite (`ROUTINE`, `ETUDE_CV`) peut être lue dans une extension
  dont l'URL se règle (`org.openelisglobal.eorder.categorie.extension`, vide par
  défaut).
- **Annulation** : la plateforme passe sa Task à `cancelled` par un `PUT`
  conditionnel (`If-Match`), transmis tel quel au serveur FHIR ; sur une version
  périmée, le serveur répond **409** (`HAPI-0989`), pas 412. La relève lit ces
  Task (`bdms-*`, `owner` = le laboratoire) ; une Task `cancelled` ne crée ni ne
  réactive jamais une demande, et rien d'autre n'est écrit dessus.
- **Rejet technique** : un bon en rejet technique passe la Task reçue à
  `rejected` / `refusee`, avec son motif ; `failed` n'est jamais écrit sur une
  Task reçue.
- **Correction** : un résultat corrigé puis revalidé sort en
  `Observation.status = corrected`, son compte rendu en
  `DiagnosticReport.status = corrected`, sous les mêmes identifiants ; la Task
  reçue ne change pas (`completed`). La correction est enregistrée en base
  (`result.correction_date`) : une reconstruction du serveur FHIR la retrouve.
- **Demande saisie localement** : après sa prise en charge, son numéro de venue
  sort en `ServiceRequest.encounter.identifier` (référence logique, système
  `http://openelis-global.org/venue` par défaut).

## Réglages

Valeurs par défaut entre parenthèses. Sur une instance installée, une valeur
propre à l'instance s'écrit dans
`/var/lib/openelis-global/secrets/extra.properties` (conservé par les mises à
jour, prime sur `common.properties`), puis
`sudo docker restart openelisglobal-webapp`.

| Réglage                                              | Rôle                                                                             |
| ---------------------------------------------------- | -------------------------------------------------------------------------------- |
| `org.openelisglobal.cmu.identifier.system`           | système national du numéro CMU (`https://hie.ci/patient/cmu`)                    |
| `org.openelisglobal.cmu.identifier.system.compat`    | même valeur aussi émise sous (`urn:oid:1.3.6.1.4.1.53864.1.3`) ; vide : non émis |
| `org.openelisglobal.cmu.format`                      | format après normalisation (`^[0-9]{13}$`)                                       |
| `org.openelisglobal.cmu.identifier.systems.accepted` | systèmes lus à l'entrée (les trois systèmes ci-dessus)                           |
| `org.openelisglobal.loinc.equivalences`              | équivalences `demandé=codé`, ex. `20447-9=25836-8` (vide)                        |
| `org.openelisglobal.remote.poll.frequency`           | cadence de la relève en ms (`60000` dans la configuration CIV)                   |
| `org.openelisglobal.remote.source.updateStatus`      | `true`, **obligatoire** pour un laboratoire raccordé (posé par l'installeur)     |
| `org.openelisglobal.eorder.plateforme.idPrefix`      | préfixe des Task de la plateforme (`bdms-`)                                      |
| `org.openelisglobal.eorder.categorie.extension`      | URL de l'extension de catégorie explicite (vide : non lue)                       |
| `org.openelisglobal.eorder.saisieLocale.enabled`     | « Nouvelle demande » (`false`)                                                   |
| `org.openelisglobal.eorder.venue.identifier.system`  | système du numéro de venue (`http://openelis-global.org/venue`)                  |

## Procédure sur une instance installée

1. **Mise à jour** par l'installeur de la version 3.3.1.4
   (`sudo python3 ./setup_OpenELIS.py -m update`, sauvegarde de la base
   comprise). Migrations automatiques :

   - colonnes `allow_write` (`N`) et `write_id_prefix` des clients de passerelle
     : aucun client n'écrit tant qu'un administrateur ne l'a pas autorisé ;
   - colonnes `categorie` et `origine` des demandes électroniques ; les demandes
     existantes sont classées d'après leur Task (plateforme : routine ; les
     autres : SIGDEP, étude charge virale) ;
   - colonne `annulation_demandeur` des demandes électroniques, colonne
     `correction_date` des résultats ;
   - droit « Enregistrer des demandes » et déclaration des adresses de «
     Demandes reçues ».

   Avant et après la mise à jour, compter les demandes par origine (des nombres
   seulement) :

   ```sql
   SELECT coalesce(categorie, '-') AS categorie, coalesce(origine, '-') AS origine,
          count(*) FILTER (WHERE data LIKE '%"id":"bdms-%') AS task_bdms,
          count(*) AS demandes
     FROM clinlims.electronic_order GROUP BY 1, 2 ORDER BY 1, 2;
   ```

2. **Réglages** : si le laboratoire code un examen sous un autre LOINC que la
   plateforme, ajouter l'équivalence dans `secrets/extra.properties`, par
   exemple :
   ```properties
   org.openelisglobal.loinc.equivalences=20447-9=25836-8
   ```
   puis `sudo docker restart openelisglobal-webapp`. Pour un laboratoire
   raccordé à la plateforme (`EXPLOITATION_PASSERELLE_FHIR.md` §5, « Remise par
   la passerelle ») : dans `#FhirGateway`, client de la plateforme avec
   `Patient`, `Specimen`, `ServiceRequest`, `Task` cochés, écriture autorisée,
   préfixe `bdms-` ; relève sur le serveur de l'instance
   (`config/REMOTE_FHIR_SOURCE` = `http://fhir.openelis.org:8080/fhir/`,
   `config/FHIR_IDENTIFIER` = `Organization/<fhir_uuid du laboratoire>`, à
   communiquer à la plateforme pour `Task.owner`). « Nouvelle demande » :
   `org.openelisglobal.eorder.saisieLocale.enabled=true` dans
   `secrets/extra.properties`, puis donner le droit « Enregistrer des demandes »
   aux opérateurs concernés (Administration → Utilisateurs).
3. **Relevé des numéros CMU** : exécuter la requête de `FHIR_INTEROP_PSNDPE.md`
   (« Relevé de l'existant ») :
   ```bash
   sudo docker exec -i openelisglobal-database psql -U clinlims -d clinlims < releve_cmu.sql
   ```
   Elle donne des nombres seulement. Les patients au statut « non conforme (type
   CMU) » ne sont pas émis avec leur numéro tant que leur fiche n'est pas
   corrigée ; rien n'est corrigé ni fusionné automatiquement.
4. **Rattrapage FHIR** (session administrateur), pour réémettre les patients
   avec leur numéro CMU et les Observations avec la date du prélèvement :
   `https://<serveur>/api/OpenELIS-Global/OEToFhir?checkAll=true` (suivi :
   `/OEToFhir/info`, jusqu'à `"running":false`).

## Limites connues

- `POST /fhir-in/order` est refusé (403) par la passerelle, qui n'accepte en
  écriture que le `PUT` décrit ci-dessus. Cette voie n'est pas utilisée par la
  plateforme, qui dépose ses demandes par la passerelle puis la relève.
- Hors périmètre : le référencement entre laboratoires reste la voie pour un
  examen que le laboratoire ne réalise pas.
- Résultat corrigé : la bactériologie (isolats, antibiogrammes) ne reçoit pas
  encore le marqueur de correction ; une modification de ces données après
  validation ne sort pas en `corrected`.
- Au démarrage, la relève des annulations remonte 24 h en arrière : une
  annulation posée par le demandeur pendant un arrêt d'OpenELIS de plus de 24 h
  n'est pas relevée.
