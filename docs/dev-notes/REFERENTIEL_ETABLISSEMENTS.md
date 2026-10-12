# Référentiel national des établissements (DHIS2 / MFL)

OpenELIS ne propose plus, comme sites demandeurs, que les établissements de la
liste du ministère (DHIS2 / MFL), chacun avec son **code d'établissement à 5
chiffres** dans `organization.short_name`. Ce code est exposé en FHIR comme
identifiant `http://openelis-global.org/org_shortName` : c'est lui que la BDM
utilise pour rattacher le site demandeur au référentiel national. Régions et
districts suivent le découpage du MFL.

## Contenu

| Élément                                                                                | Emplacement                                                                                                                          |
| -------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| Liste du ministère (5 487 établissements, 33 régions, 113 districts) et sa description | `src/main/resources/liquibase/3.2.x.x/data/etablissements_mfl.csv`, `etablissements_mfl.meta.json` (produits par le dépôt de la BDM) |
| Migration                                                                              | `liquibase/3.2.x.x/referentiel_etablissements_dhis2.xml`                                                                             |
| Fonctions (synchronisation, fusion, annulation)                                        | `liquibase/3.2.x.x/sql/referentiel_etablissements.sql`                                                                               |
| Table du référentiel                                                                   | `clinlims.ref_etablissement_dhis2` (clé `facility_code`, texte : zéros de tête conservés)                                            |
| Suivi de chaque décision (ancien nom, code, statut actif, parent, statut)              | `clinlims.ref_etablissement_rapprochement`                                                                                           |
| Journal des données déplacées par chaque fusion (permet de l'annuler)                  | `clinlims.ref_fusion_reference`                                                                                                      |
| Écran de réconciliation                                                                | `Administration → Organisations` : colonnes « Référentiel » et « Niveau », bouton « Réconcilier / fusionner », « Fusions récentes »  |
| API (administrateur)                                                                   | `/rest/organization-referentiel/` : `recherche`, `apercu`, `fusion`, `fusion/{id}/annulation`, `fusions`                             |
| Outils                                                                                 | `tools/referentiel-etablissements/` : `rapport_correspondance.sh`, `simulation_sur_copie.sh`, `annuler_synchronisation.sql`          |

## Colonnes ajoutées à `organization`

| Colonne         | Valeurs                                                                                                                                                                                                                                         |
| --------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ref_statut`    | `MFL` : créée depuis la liste du ministère ; `FUSIONNE` : organisation existante réconciliée avec un établissement, district ou région du MFL (automatiquement ou à la main), elle garde son id et son historique ; `LEGACY` : hors référentiel |
| `ref_niveau`    | `REGION`, `DISTRICT`, `ETABLISSEMENT` (vide : laboratoire de référence, fournisseur, service…)                                                                                                                                                  |
| `fusionne_dans` | pour une organisation absorbée par une fusion : l'organisation qui a reçu son historique                                                                                                                                                        |

La hiérarchie passe par l'organisation parente (`org_id`) : **district d'un
établissement, région d'un district**. La région d'un établissement est donc le
parent de son parent. Ce sont aussi ces liens qu'utilisent les listes « région →
district » de l'adresse du patient.

## Règles de la synchronisation

Identiques sur toute installation (`clinlims.sync_referentiel_etablissements`) :

1. **Régions et districts du MFL** : créés (types `Health Region` /
   `Health District`, code à 2 / 4 chiffres, statut `MFL`), chaque district
   rattaché à sa région ; noms tenus à jour. Les **anciennes** régions et
   districts (ancien découpage d'OpenELIS) passent `LEGACY` et sont désactivés :
   plus proposés pour les nouveaux patients ; les adresses déjà enregistrées
   restent lisibles et peuvent être reportées sur le MFL par fusion.
2. **Établissements** — seules les organisations de type site sont concernées
   (`referring clinic`, `ARV Service Loc`, `patient referral`, `EID …`,
   `RTN HIV …`, sans type région, district, laboratoire de référence,
   fournisseur ou service), plus le site du laboratoire. Une organisation est
   **rattachée** (elle garde son id, prend nom, code, statut `FUSIONNE` et son
   district) seulement sur une double preuve :

   - son code est celui d'un établissement : 5 chiffres, ou 1 à 4 chiffres dont
     les zéros de tête ont été perdus (`153` → `00153`) ;
   - **et** son nom est cohérent avec celui de l'établissement (mot significatif
     commun, variantes d'orthographe tolérées).

   À défaut, un nom identique (après normalisation) à celui d'un seul
   établissement suffit ; un code faux est alors remplacé.

3. **Même établissement revendiqué par plusieurs organisations** (code et nom
   cohérents) : elles sont **fusionnées**. Celle qui a le plus d'historique
   survit (à égalité : le site du laboratoire, puis l'active, puis le plus petit
   id) et reçoit l'historique des autres.
4. **Code présent mais nom incohérent** (ex. `00323 CLINIQUE ST DAVID` alors que
   `00323` est le CSR de LUEHOUAN) : jamais de rattachement ; le code est retiré
   (il reste dans le suivi).
5. **Établissement sans organisation** : créé (statut `MFL`, type
   `referring clinic`, district parent, identifiant FHIR).
6. **Désactivation** (jamais de suppression, seulement des organisations
   actives, toujours tracée) des **sites** non rattachés. Restent actives : les
   types fonctionnels, les organisations à types mêlés, celles sans type et le
   site du laboratoire.
7. **Site du laboratoire sans code** : signalé (`site_sans_code`).

Une organisation désactivée reste citée par ses échantillons : consultation,
modification et comptes rendus fonctionnent ; elle n'est plus proposée à la
saisie (les listes ne retiennent que les organisations actives).

La synchronisation est **rejouable** : un titulaire (statut `MFL` ou `FUSIONNE`)
garde son code, une organisation fusionnée n'est plus touchée ; seuls les noms
changés, les nouveaux établissements et les nouvelles organisations hors
référentiel donnent lieu à une décision.

Les noms de plus de 80 caractères (taille de `organization.name`) sont remplacés
par le nom court, ou tronqués.

## Site demandeur : pas de saisie libre

La migration active `restrictFreeTextRefSiteEntry` : à la saisie d'un
échantillon (et en saisie par lot, en modification d'ordonnance), le site
demandeur se choisit uniquement dans la liste ; le serveur refuse aussi de créer
un site à partir d'un nom libre envoyé directement (`SiteDemandeurReferentiel`).
Une organisation créée autrement (écran d'administration, demande électronique
ou ordre FHIR d'un tiers) reçoit le statut `LEGACY` : elle apparaît dans l'écran
pour être réconciliée.

## Réconciliation manuelle (fusion)

Pour corriger ce que la règle automatique ne peut pas trancher : un site
historique au code faux ou absent (« CHR YAMOUSSOUKRO », code `148`), un ancien
district à reporter sur le district MFL, deux organisations en double…

Dans `Administration → Organisations` : sélectionner l'organisation, bouton
**Réconcilier / fusionner**, rechercher la cible (code ou nom, filtre par
niveau), vérifier l'aperçu (historique de chacune : échantillons, demandes
électroniques, transferts, organisations rattachées, adresses de patients),
choisir l'organisation qui survit (proposée : celle qui a le plus d'historique),
saisir un motif, confirmer.

Effet :

- toutes les données qui citaient l'organisation absorbée citent la survivante :
  site demandeur des échantillons, demandes électroniques, transferts,
  commandes, sections, organisations rattachées, adresses de patients (région,
  district), site du laboratoire (`siteId`) ; adresses, types et projets sont
  complétés chez la survivante ;
- si l'absorbée portait le code d'un établissement, la survivante prend son
  identité (code, nom, district) ; statut `FUSIONNE` dès qu'un élément du
  référentiel est en jeu ;
- l'absorbée est désactivée, avec `fusionne_dans` ;
- les deux organisations et les échantillons concernés sont republiés en FHIR
  (en arrière-plan), les listes de sélection rafraîchies.

Fusion impossible si les deux organisations portent deux codes d'établissement
différents (retirer d'abord le code erroné).

**Annulation** : section « Fusions récentes » de l'écran, bouton « Annuler la
fusion ». Les données retrouvent leur organisation d'origine, les deux
organisations leur état d'avant.

En SQL :

```sql
SELECT clinlims.ref_fusionner(<id_a>, <id_b>);           -- survivante = plus d'historique
SELECT clinlims.ref_fusionner(<id_a>, <id_b>, <id_survivante>, 'motif');
SELECT clinlims.ref_annuler_fusion(<id_fusion>);
```

## Avant le déploiement sur un site

1. **Rapport de correspondance** (lecture seule) sur le serveur :
   ```bash
   sudo ./rapport_correspondance.sh etablissements_mfl.csv ~/rapport_l1
   ```
2. **Code du site du laboratoire** : si l'organisation désignée par `siteId` n'a
   pas son code à 5 chiffres, le renseigner dans
   `Administration → Organisations` **avant** la mise à jour (sinon
   l'établissement est créé en double ; une fusion corrige ensuite).
3. **Simulation** sur une copie de la base, sur le serveur (la base en service
   n'est pas touchée, la copie est supprimée à la fin) :
   ```bash
   sudo SITE_CODE=00025 ./simulation_sur_copie.sh etablissements_mfl.csv referentiel_etablissements.sql ~/simulation
   ```
   Bilan par décision, statut, niveau et type ; détail dans
   `simulation_decisions.csv`.

## Déploiement

La migration s'applique au démarrage de la nouvelle version (quelques secondes).
Ensuite :

```bash
sudo docker exec openelisglobal-database psql -U clinlims -d clinlims -c \
  "select decision, count(*) from clinlims.ref_etablissement_rapprochement group by 1 order by 1"
```

puis, en administrateur, republier en FHIR :
`https://<serveur>/api/OpenELIS-Global/OEToFhir?checkAll=true` (suivi :
`/OEToFhir/info`). Contrôle :
`GET /fhir/Organization?identifier=http://openelis-global.org/org_shortName|00394`
→ une seule organisation.

## Mise à jour du référentiel

1. Remplacer `data/etablissements_mfl.csv` et `etablissements_mfl.meta.json`.
2. Reporter la version (champ `version` de la description) dans le changeset
   `referentiel-etablissements-synchronisation-1`.
3. Au démarrage suivant : référentiel rechargé, synchronisation rejouée (noms,
   nouveaux établissements, régions et districts ; aucun doublon). Un
   établissement qui disparaît du fichier n'est ni supprimé ni désactivé.
4. `/OEToFhir?checkAll=true`.

Resynchronisation manuelle :

```bash
sudo docker exec openelisglobal-database psql -U clinlims -d clinlims -c \
  "SELECT * FROM clinlims.sync_referentiel_etablissements('manuelle')"
sudo docker restart openelisglobal-webapp
```

## Annulation complète

```bash
sudo docker exec -i openelisglobal-database psql -U clinlims -d clinlims -v ON_ERROR_STOP=1 \
  < annuler_synchronisation.sql
```

Annule toutes les fusions (de la plus récente à la plus ancienne), rend aux
organisations nom, code, statut actif et parent d'avant leur première décision,
supprime les organisations créées (désactivées si quelque chose les cite), vide
les colonnes du référentiel. Vérifié : la table `organization` retrouve
exactement son état d'origine. Puis `/OEToFhir?checkAll=true`.

## Site du laboratoire en FHIR

La propriété `siteId` (`site_information`, domaine `siteIdentity`) désigne
l'organisation où l'instance est installée. Elle est exposée en FHIR :
`Task.owner` et `DiagnosticReport.performer` =
`Organization/<fhir_uuid du site>`. L'organisation du site (et sa Location) est
écrite dans la même transaction que les Task et DiagnosticReport. `siteId` vide
ou organisation introuvable : rien n'est émis, avertissement au démarrage.
