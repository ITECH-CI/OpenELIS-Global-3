# OpenELIS CIV 3.3.1.4 — notes de version

Cette version émet le numéro CMU des patients sous le système national, garde le
numéro CMU des patients reçus par les demandes électroniques, explique le refus
d'une demande, et date les résultats FHIR du prélèvement. Détails :
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
- Les demandes de la plateforme passent par la relève (`remote.source.uri`), pas
  par `/fhir-in/` : voir `EXPLOITATION_PASSERELLE_FHIR.md` §5.

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

## Procédure sur une instance installée

1. **Mise à jour** par l'installeur de la version 3.3.1.4
   (`sudo python3 ./setup_OpenELIS.py -m update`, sauvegarde de la base
   comprise). Aucune migration de base dans cette version.
2. **Réglages** : si le laboratoire code un examen sous un autre LOINC que la
   plateforme, ajouter l'équivalence dans `secrets/extra.properties`, par
   exemple :
   ```properties
   org.openelisglobal.loinc.equivalences=20447-9=25836-8
   ```
   puis `sudo docker restart openelisglobal-webapp`. Vérifier
   `remote.source.uri`, `remote.source.identifier` et
   `remote.source.updateStatus` (`EXPLOITATION_PASSERELLE_FHIR.md` §5).
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
