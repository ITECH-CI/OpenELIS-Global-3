# OpenELIS CIV 3.3.1.3 — notes de version

Publiée le 12/10/2026 (tag `v3.3.1.3`, GitHub Release avec l'installeur
offline). Cette version aligne les sites demandeurs, régions et districts sur le
**référentiel national des établissements** (liste du ministère, DHIS2 / MFL) et
expose le laboratoire en FHIR. Détails : `REFERENTIEL_ETABLISSEMENTS.md`.

## Avant la mise à jour

1. **Code du site du laboratoire** : dans `Administration → Organisations`,
   renseigner le code d'établissement à 5 chiffres de l'organisation du
   laboratoire (celle de `siteId`) si elle ne l'a pas. Sinon l'établissement est
   créé en double (corrigeable ensuite par une fusion).
2. **Conseillé** : rapport de correspondance et simulation sur une copie de la
   base, sur le serveur (`tools/referentiel-etablissements/`,
   `REFERENTIEL_ETABLISSEMENTS.md`). La base en service n'est pas touchée.

## Après la mise à jour

1. Republier en FHIR (session administrateur) :
   `https://<serveur>/api/OpenELIS-Global/OEToFhir?checkAll=true` (suivi :
   `/OEToFhir/info`).
2. Dans `Administration → Organisations`, réconcilier à la main les sites
   désactivés que des échantillons citent encore (bouton « Réconcilier /
   fusionner ») : leur code ou leur nom ne permettait pas un rattachement
   automatique sûr.

## Nouveautés

- **Référentiel des établissements** chargé à la mise à jour : 5 487
  établissements, 33 régions, 113 districts, avec leur code. Hiérarchie
  établissement → district → région (organisation parente). Les listes de sites
  demandeurs et de régions / districts (adresse du patient) ne proposent plus
  que le référentiel ; anciens sites, régions et districts désactivés, jamais
  supprimés (les données existantes restent consultables).
- **Rattachement automatique** des organisations existantes, sur double preuve
  (code — y compris aux zéros de tête perdus — et nom cohérent) : elles gardent
  leur historique et prennent le nom et le code du ministère. Les doublons d'un
  même établissement sont fusionnés.
- **Statut et niveau** de chaque organisation (MFL, fusionnée MFL, hors
  référentiel ; région, district, établissement), visibles dans l'écran
  Organisations.
- **Réconciliation manuelle** : fusion de deux organisations depuis l'écran
  Organisations (aperçu de l'historique, choix de l'organisation qui le garde),
  annulable ; tout l'historique suit (échantillons, demandes électroniques,
  adresses de patients…).
- **Plus de site demandeur saisi librement** : à la saisie d'un échantillon, en
  saisie par lot et en modification d'ordonnance, le site se choisit dans la
  liste ; le serveur refuse aussi tout nom libre.
- **FHIR** : le laboratoire (organisation `siteId`) est `Task.owner` et
  `DiagnosticReport.performer` ; le code d'établissement est exposé comme
  identifiant `org_shortName`.

## Migrations de base (automatiques au démarrage)

- tables `ref_etablissement_dhis2` (référentiel),
  `ref_etablissement_rapprochement` (suivi des décisions),
  `ref_fusion_reference` (journal des fusions) ;
- colonnes `organization.ref_statut`, `ref_niveau`, `fusionne_dans` ;
- synchronisation avec le référentiel (quelques secondes ; annulable :
  `tools/referentiel-etablissements/annuler_synchronisation.sql`) ;
- `restrictFreeTextRefSiteEntry` = `true`.
