# OpenELIS CIV 3.3.1.2 — notes de version

Publiée le 01/10/2026, republiée le 08/10/2026 (tag `v3.3.1.2`, GitHub Release
avec l'installeur offline). Cette version corrige les constats remontés par les
utilisateurs après les exercices de restauration et de saisie de routine (LNSP,
CHU Angré, AIBEF), et fiabilise la restauration d'une base.

## À faire après la mise à jour

1. **Sites déjà restaurés depuis la base d'un autre site** : vérifier
   `Admin → Informations du site` (nom du site, préfixe labo, info site) et
   ressaisir les valeurs si elles appartiennent à l'ancien site.
2. **Biologiste** : valider l'arrondi des plages SI sur le compte rendu (2
   décimales, comme la valeur SI).
3. **Tests à plage décimale** (ex. Glycémie 0.7-1.1 affichée « 1-1 ») : ajuster
   les chiffres significatifs dans
   `Admin → Gestion des tests → Modifier les tests`.
4. **Sites qui exposent leurs données FHIR** (BDM, mini-HIE) : lancer le
   rattrapage
   `GET /api/OpenELIS-Global/OEToFhir?checkAll=true&waitForResults=true`
   (session admin), puis créer le client de passerelle (`#FhirGateway`). Voir
   `EXPLOITATION_PASSERELLE_FHIR.md`.

## Corrections importantes

- **Résultats par unité — filtre n° labo** : un résultat saisi après filtrage
  pouvait être enregistré sur une autre analyse (régression des images du
  14/09). Corrigé ; le filtre n° labo est désormais appliqué par le serveur.
- **N° labo avec tiret** (ex. `1252-26`) : compte rendu patient (plantait),
  étiquettes, Modifier l'ordonnance, page Validation, rapport de rejets, PDF des
  plans de travail, rapports par plage.
- **Exports CSV** :
  - export de routine en erreur (500) ;
  - valeurs pouvant se retrouver sous la mauvaise colonne de test ;
  - export d'étude en échec une fois sur deux ;
  - une connexion à la base retenue à chaque export ;
  - accents illisibles sous Excel (UTF-8).
- **Modifier l'ordonnance** :
  - demande impossible à modifier dès que la date de prochaine visite était
    passée ;
  - coordonnées du prescripteur, date de prélèvement et réponses au
    questionnaire non enregistrées.
- **Restauration d'une base** :
  - démarrage bloqué par un ancien trigger d'oedatauploader ;
  - infos du site écrasées par le fichier de configuration du conteneur ;
  - serveur FHIR non démarré avec un ancien dump.
- **Gestion des utilisateurs** :
  - création impossible sans rôle global coché ;
  - identifiants avec chiffres refusés ;
  - message d'erreur générique au lieu du motif ;
  - double pagination et totaux faux ;
  - accents cassés (« RÃ©sultats »).
- **Échanges FHIR** :
  - le lot FHIR d'un échantillon était rejeté par le serveur FHIR quand son site
    demandeur avait été créé depuis l'écran d'administration (référence de type
    non valide) ;
  - le Specimen n'était plus mis à jour dès qu'un compte rendu existait (date de
    réception, statut) ;
  - dates de demande et de réception remplacées par l'heure de la transformation
    ;
  - organisation exposée inactive alors qu'elle est active ;
  - rattrapage bloqué après un `/PatientToFhir` (verrou jamais libéré).
- **Modifier les tests** : limites, tranche d'âge, type de résultat et LOINC
  repris tels qu'enregistrés (avant : valeurs arrondies réécrites à la
  sauvegarde, « Nouveau-né » par défaut, type non sélectionné).

## Nouveautés

- **Compte rendu patient (biochimie)** : valeur SI et son unité sous le
  résultat, plage de référence SI sous la plage conventionnelle (petite police).
- **Plans de travail** : filtres n° labo et période de réception.
- **Étiquettes** : option « Nom du patient »
  (`Admin → Configuration code-barres`).
- **Export des demandes électroniques** par période (la page était vide).
- **Exports CSV** : listes limitées aux études et unités actives, filtre par
  unité sur l'export de routine.
- **Saisie des résultats numériques** : la virgule est acceptée et convertie en
  point ; l'alerte « hors plage valide » s'affiche en quittant le champ et non
  plus pendant la frappe.
- **Passerelle FHIR** dans l'installeur : `https://<hôte>/fhir/` en lecture pour
  les tiers autorisés (jeton par client, ressources autorisées, journal
  d'accès), liens de pagination sur l'hôte public.
- **Site demandeur exposé en FHIR** : `Task.requester` et
  `ServiceRequest.locationReference` (Location gérée par l'Organization du site)
  ; toute organisation reçoit un identifiant FHIR à sa création ; rattrapage
  `/OrganizationToFhir`.
- **Script de restauration** `restore_OpenELIS.sh`, installé dans
  `/var/lib/openelis-global/` : dump de sécurité, restauration, remise à zéro et
  reconstruction du FHIR, vérifications (voir `INSTALL_CIV.md` §4).

## Migrations de base (automatiques au démarrage)

- `extend-obervation-history-value-size-1` réécrite (compatible avec les
  triggers existants).
- `allow-digits-in-username-charset-1` : ajoute `0-9` aux caractères autorisés
  dans les identifiants (sans toucher une valeur déjà personnalisée).
- `assign-organization-fhir-uuid-1` : attribue un identifiant FHIR aux
  organisations qui n'en ont pas.
