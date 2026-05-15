# Release Notes

## 3.2.3.13 — depuis la version 3.2.2 (CILNSP / fork Pkom)

Cette note regroupe les évolutions livrées dans la branche `develop` depuis
le dernier déploiement stable (basé sur la version 3.2.2 / `3.2.3.12`).

---

### Bactériologie classique

#### Workflow et saisie
- Refonte de la saisie bacterio : nouveau composant `BacteriologyResultEntry`
  avec sections Macroscopie, Microscopie, Culture et identification des
  organismes (sélection cherchable, capsule, gram type, mode de regroupement).
- Test « Nombre de flore » avec un tableau détaillé (numéro, gram, mode,
  autre caractère) sous le test parent.
- Fix : flore persistée sur le bon `analysisId`, déduplication des
  `test_result` et des tests enfants émis en double, régression de statut
  corrigée au chargement de résultats existants.
- Validation biologique : nouvelle note d'interprétation par échantillon
  (persistée comme `SAMPLE_INTERPRETATION` dans `observation_history`).

#### Sample entry
- Sélecteur de type d'échantillon recherchable (Carbon `ComboBox` + filtre
  par sous-chaîne, largeur doublée). Le changement de type réinitialise les
  panels précédemment sélectionnés.
- Type d'ordonnance (« Ordonnance Interne / Externe ») : `<Select>` câblé
  correctement (`handleOrderType`) et déduplication de l'insertion
  `BacterioTypeExamens` dans la persistance.
- Option « Afficher Quantité / Unité de mesure » par configuration
  (`SHOW_SAMPLE_QUANTITY_AND_UOM`).
- Champ dédié « Numéro CMU » dans la recherche patient (`SamplePatientEntry`
  + `SampleEdit?type=readwrite`), aligné sur `nationalId`.

#### Rapport patient bactériologie
- Refonte complète du JRXML (`BacterioPatientReport`) :
  - Mise en page A4 (au lieu de Letter), pied de page et remarques tiennent
    sur la page.
  - Regroupement par culture : pour chaque culture, on émet `[test root] ->
    [Nombre de germes] -> [organismes + antibiogrammes]`, séparateur entre
    blocs de cultures, légende S/I/R unique en fin de section.
  - Tableaux antibiogramme et nombre de flore avec bordures noires fines.
  - Distinction libellé (gris foncé) / valeur (gras noir).
  - Sections MACRO / MICRO / CULTURE compactées.
  - En-tête patient resserré, première ligne (Code / CMU / Age / Sexe)
    abaissée, bordure top noire pleine sur « Site référant ».
  - Pied de page : « Type d'ordonnance » lu depuis `BacterioTypeExamens`
    (Internal/External), « Remarques générales du laboratoire »
    alimentées par la note d'interprétation, antibiogramme masqué tant
    que la culture n'est pas biologiquement validée.

#### Migrations Liquibase bacterio
- `Microscopie - Frottis Coloration Gram - Bactéries - Nombre de flore`
  lié au sample type **Sang (hémoculture)** et au panel **ECB-Sang**.
- Ajout des tests **Hématies / Leucocytes (Etat frais Quantitatif) / Nombre
  de flore** au panel **ECB-Crachat**.
- Ajout de **Culture - Sécrétions urétrales** au panel **ECB-PU** ; ajout
  de **Nombre de flore** au sample type **Sécrétions urétrales**.
- Suppression de 6 tests non pertinents pour le type **Pus**, association
  de 5 tests de microscopie aux ECB **Urines / LCR / LP**.
- Modalité **Absence** ajoutée au test
  *Microscopie - Frottis Coloration Gram - Bactéries*.

---

### Tests conditionnels (Goutte Epaisse → Densité parasitaire)

- Nouvelle page admin **« Test conditionnel (parent → enfant) »**
  (`/admin#ConditionalTestConfig`, section *Organisations des tests*).
  Permet de configurer un enfant qui n'apparaît que lorsque le test parent
  retourne une valeur déclenchante précise.
- Page Logbook (`SearchResultForm`) :
  - Filtrage des lignes : un test enfant est masqué tant que son parent
    n'a pas la valeur déclenchante, et apparaît immédiatement **après**
    le parent une fois déclenché.
  - À la sauvegarde, l'analysis enfant virtuel est matérialisé à la volée
    (`materializeVirtualChildAnalyses`), évitant l'erreur
    *No row with identifier* sur `analysisId=""`.

---

### Assignation Méthodes ↔ Tests

- Nouvelle table `clinlims.test_method (id, test_id, method_id, is_active,
  lastupdated)` distincte de `tb_method_test`.
- Page admin **« Assigner des méthodes aux tests »**
  (`/admin#MethodTestMapping`). Deux modes : par test (cocher des méthodes)
  ou par méthode (cocher des tests), ComboBox cherchable, filtre live,
  badge *(inactive)* sur les méthodes inactives. À la sauvegarde, les
  méthodes assignées sont automatiquement activées.
- REST : `/rest/method-test-map/{methods,tests,methods-for-test/{id},
  tests-for-method/{id},all,save-for-test,save-for-method}`.
- **Filtrage dans Logbook** : la liste *Test method* (zone Referral) est
  filtrée selon le `testId` de la ligne. Fallback rétro-compatible si
  aucun mapping n'est défini.

---

### Configuration tests (Admin)

- Fix critique de la page **« Modifier les tests »**
  (`/admin#TestModifyEntry`) qui ne sauvegardait rien :
  - Le validator lisait `dictionary[i].value` au lieu de `id`.
  - `UnitOfMeasureDAOImpl.getUnitOfMeasureById` plantait sur
    `Integer.parseInt("")`.
  - Notification de succès affichée avant le reload (1,5 s) au lieu d'un
    reload immédiat qui masquait l'erreur.

---

### Rapport patient routine (PatientReportCDI_vreduit)

- Configuration **`showAuditOnPatientReport`** (`false` par défaut,
  *Admin > Configurations > Result Configuration*) qui contrôle :
  - le bandeau *Rapport corrigé* (banner haut de page) ;
  - l'inclusion des analyses au statut *Canceled* ;
  - la note interne *Résultat corrigé* ajoutée à chaque modification dans
    Logbook (filtrée par regex).
- Refonte de la mise en page du tableau des tests :
  - Colonne **Date Réalisation** retirée ; la date est désormais affichée
    dans l'en-tête de section (sous la ligne *Analyseur*), à la place de
    l'ancienne ligne *Méthode*.
  - Méthode (`analysisMethod`) affichée sous le nom du test, en italique
    6pt entre parenthèses.
  - Largeurs réallouées : Test, Résultat et Antécédent élargis.

---

### Recherche patient

- Champ dédié **Numéro CMU** (`nationalId`) dans `SearchPatientForm`
  (utilisé par SamplePatientEntry et SampleEdit). Si rempli, sa valeur
  prime sur `patientId` pour le paramètre `nationalID` de la recherche.

---

### Infrastructure / DB

- Migration `resync_all_sequences.xml` (`runAlways=true`) : à chaque
  démarrage Tomcat, toutes les séquences `clinlims.<table>_seq` sont
  resynchronisées sur `MAX(id)+1`. Corrige la racine du bug *duplicate
  key value violates unique constraint* qui empêchait la création de
  nouvelles méthodes (`system_module`, `system_role_module`).
- Nouveau type d'observation `BacterioTypeExamens` et clés i18n
  `report.bacterio.ordonnancetype.in/out`.

---

### Divers

- Nettoyage de `System.out.println` de debug dans
  `NonConformityRestController` et `LogbookResultsRestController`.
- Corrections i18n FR/EN : *Code du patient*, *Gestion des prescripteurs*,
  ajout des clés `methodTestMapping.*`, `conditional.test.*`,
  `report.bacterio.ordonnancetype.*`.

---

### Notes de déploiement

1. Redémarrer Tomcat après déploiement : la migration
   `resync_all_sequences` s'exécutera automatiquement (sécurise les
   séquences DB).
2. La table `clinlims.test_method` est créée vide ; les méthodes existantes
   continuent d'apparaître sans filtrage côté Logbook tant qu'aucun mapping
   n'est défini (fallback).
3. La propriété `showAuditOnPatientReport` est créée à `false` ; pour
   retrouver le comportement historique (bandeau corrigé + tests annulés
   imprimés), passer la valeur à `true` dans
   *Admin > Configurations > Result Configuration*.
