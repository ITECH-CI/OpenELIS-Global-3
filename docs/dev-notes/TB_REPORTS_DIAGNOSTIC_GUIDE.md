# Guide de diagnostic des rapports TB

## Problèmes identifiés

### 1. Export CSV vide (TBOrderExport)

**Symptôme**: L'en-tête CSV est présent mais aucune ligne de données

**Causes possibles**:

1. Les tests TB ne sont pas correctement mappés à la section "TB"
2. Les résultats ne sont pas enregistrés dans la table `result`
3. Le statut des analyses n'est pas "Finalized"
4. La plage de dates ne contient aucun échantillon TB

### 2. Décomptes incorrects dans les rapports d'activité

**Symptôme**: Les chiffres dans les rapports GeneXpert et Microscopie ne
correspondent pas

**Causes possibles**:

1. Le nom du test ne correspond pas exactement à 'GeneXpert MTB/RIF'
2. Les observation_history (TbAnalysisMethod, TbDiagnosticReason) ne sont pas
   enregistrées
3. Les résultats ne sont pas liés aux bons dictionnaires

## Workflow de vérification

### Étape 1: Vérifier le mapping des tests

```bash
psql -U clinlims -d clinlims -f check_tb_tests_mapping.sql > tb_mapping_check.txt
```

**Points à vérifier**:

- [ ] La section de test "TB" existe et est active
- [ ] Les tests TB sont bien mappés à cette section
- [ ] Le test "GeneXpert MTB/RIF" existe avec ce nom EXACT
- [ ] Les tests de bacilloscopie existent

**Requête manuelle**:

```sql
SELECT t.id, t.name, t.description, ts.name as section_name
FROM test t
JOIN test_section ts ON t.test_section_id = ts.id
WHERE ts.name = 'TB' AND t.is_active = 'Y'
ORDER BY t.name;
```

### Étape 2: Vérifier le workflow de saisie des résultats

```bash
psql -U clinlims -d clinlims -f debug_tb_workflow.sql > tb_workflow_debug.txt
```

**Points à vérifier pour un échantillon TB récent**:

1. **Sample créé**: Accession number, entered_date
2. **Analysis créée**: test_id pointe vers un test TB, status_id = Finalized
3. **Result créé**: analysis_id pointe vers l'analyse, value contient un ID de
   dictionnaire
4. **ObservationHistory créées**:
   - `TbAnalysisMethod`: doit contenir l'ID du dictionnaire 'Microscopie TB' ou
     'GeneXpert MTB/RIF'
   - `TbDiagnosticReason`: doit contenir l'ID du dictionnaire (ex: 'Cas présumé
     jamais traité')

### Étape 3: Vérifier les queries SQL des rapports

#### Pour TBOrderExport (CSV):

La query dans `TBColumnBuilder.java` utilise:

```sql
WHERE ts.name = ''TB'' AND s.entered_date >= date(...)
```

**Vérifications**:

- [ ] `test_section.name = 'TB'` existe
- [ ] Les tests ont `test_section_id` pointant vers cette section
- [ ] Les dates `s.entered_date` sont dans la plage spécifiée

#### Pour TBOrderReport (GeneXpert):

La query dans `TestDAOImpl.getTbGXTestCountByResult()` utilise:

```sql
WHERE s.entered_date >= :startDate AND s.entered_date <= :endDate
  AND t.name ='GeneXpert MTB/RIF'
```

**Vérification critique**:

- [ ] Le test existe avec **EXACTEMENT** le nom 'GeneXpert MTB/RIF'
- [ ] Les résultats sont liés à des dictionnaires avec les valeurs:
  - 'MTB détecté RIF Résistant'
  - 'MTB détecté RIF sensible'
  - 'MTB détecté RIF indéterminé'
  - 'MTB non détecté'
  - 'Erreur'
  - 'Invalide'

#### Pour TBOrderReport (Microscopie):

La query dans `TestDAOImpl.getTbMicroscopyTestCountByResult()` utilise:

```sql
WHERE s.entered_date BETWEEN :startDate AND :endDate
  AND obs_analysis_method.value = CAST((SELECT id FROM dictionary WHERE dict_entry = 'Microscopie TB' LIMIT 1) AS VARCHAR)
```

**Vérifications**:

- [ ] Le dictionnaire 'Microscopie TB' existe
- [ ] Les observation_history avec `type_name = 'TbAnalysisMethod'` sont créées
- [ ] La valeur pointe vers l'ID du dictionnaire 'Microscopie TB'
- [ ] Les observation_history avec `type_name = 'TbDiagnosticReason'` sont
      créées

## Corrections à apporter

### Si le nom du test GeneXpert ne correspond pas

```sql
-- Trouver le test existant
SELECT id, name, description FROM test WHERE description LIKE '%GeneXpert%';

-- Option 1: Renommer le test (si ID = 123 par exemple)
UPDATE test SET name = 'GeneXpert MTB/RIF' WHERE id = 123;

-- Option 2: Modifier la query dans TestDAOImpl.java
-- Remplacer t.name ='GeneXpert MTB/RIF' par t.description LIKE '%GeneXpert%'
```

### Si les observation_history ne sont pas créées

**Vérifier le frontend**: Lors de la saisie TB, le formulaire doit envoyer:

- `TbAnalysisMethod`: ID du dictionnaire de la méthode choisie
- `TbDiagnosticReason`: ID du dictionnaire de la raison diagnostique
- `TbOrderReason`: ID du dictionnaire de la raison de commande

**Vérifier le backend**: Le contrôleur doit sauvegarder ces observation_history.

### Si les résultats ne sont pas dans la table result

**Vérifier**:

1. Le statut de l'analyse est bien "Finalized"
2. Les résultats ont été sauvegardés avec `result.value` contenant l'ID du
   dictionnaire
3. La jointure `LEFT JOIN result r ON a.id = r.analysis_id` fonctionne

## Tests manuels SQL

### Test 1: Compter les échantillons TB sur 30 jours

```sql
SELECT COUNT(DISTINCT s.id) as sample_count
FROM sample s
JOIN sample_item si ON s.id = si.samp_id
JOIN analysis a ON si.id = a.sampitem_id
JOIN test t ON a.test_id = t.id
JOIN test_section ts ON t.test_section_id = ts.id
WHERE ts.name = 'TB'
  AND s.entered_date >= CURRENT_DATE - INTERVAL '30 days';
```

**Résultat attendu**: > 0 si vous avez saisi des échantillons TB

### Test 2: Vérifier les résultats GeneXpert

```sql
SELECT
    s.accession_number,
    t.name as test_name,
    d.dict_entry as result_value,
    a.status_id,
    st.name as status_name
FROM sample s
JOIN sample_item si ON s.id = si.samp_id
JOIN analysis a ON si.id = a.sampitem_id
JOIN test t ON a.test_id = t.id
LEFT JOIN result r ON a.id = r.analysis_id
LEFT JOIN dictionary d ON CAST(d.id AS VARCHAR) = r.value
LEFT JOIN status_of_sample st ON a.status_id = st.id
WHERE t.name = 'GeneXpert MTB/RIF'
  AND s.entered_date >= CURRENT_DATE - INTERVAL '30 days'
ORDER BY s.entered_date DESC;
```

**Résultat attendu**:

- Des lignes avec `test_name = 'GeneXpert MTB/RIF'`
- Des `result_value` non nulles
- Des `status_name = 'Finalized'`

### Test 3: Vérifier les observation_history

```sql
SELECT
    s.accession_number,
    oht.type_name,
    d.dict_entry as value
FROM sample s
JOIN observation_history oh ON s.id = oh.sample_id
JOIN observation_history_type oht ON oh.observation_history_type_id = oht.id
LEFT JOIN dictionary d ON CAST(d.id AS VARCHAR) = oh.value
WHERE oht.type_name IN ('TbAnalysisMethod', 'TbDiagnosticReason')
  AND s.entered_date >= CURRENT_DATE - INTERVAL '30 days'
ORDER BY s.entered_date DESC, oht.type_name;
```

**Résultat attendu**:

- Des lignes avec `type_name = 'TbAnalysisMethod'` et `value = 'Microscopie TB'`
  ou `value = 'GeneXpert MTB/RIF'`
- Des lignes avec `type_name = 'TbDiagnosticReason'` et des valeurs comme 'Cas
  présumé jamais traité', etc.

## Actions recommandées

1. **Exécuter les scripts de vérification**:

   ```bash
   psql -U clinlims -d clinlims -f check_tb_tests_mapping.sql > tb_mapping.txt
   psql -U clinlims -d clinlims -f debug_tb_workflow.sql > tb_workflow.txt
   ```

2. **Analyser les résultats** et identifier quel composant manque

3. **Si les tests ne sont pas correctement mappés**: Corriger les migrations
   Liquibase

4. **Si les observation_history manquent**: Vérifier que le formulaire TB envoie
   bien ces données

5. **Si les résultats manquent**: Vérifier que la sauvegarde des résultats
   fonctionne correctement

## Fichiers à vérifier

### Backend:

- `src/main/java/org/openelisglobal/test/daoimpl/TestDAOImpl.java` (lignes
  795-922) - Queries SQL
- `src/main/java/org/openelisglobal/reports/action/implementation/TBOrderReport.java` -
  Logique rapport
- `src/main/java/org/openelisglobal/reports/action/implementation/reportBeans/TBColumnBuilder.java` -
  Query CSV export

### Liquibase migrations:

- Vérifier que les tests TB sont créés avec les bons noms
- Vérifier que la section "TB" existe
- Vérifier que les dictionnaires existent

### Frontend:

- Formulaire de saisie TB doit envoyer les observation_history
- Les résultats doivent être sauvegardés avec les bons IDs de dictionnaire
