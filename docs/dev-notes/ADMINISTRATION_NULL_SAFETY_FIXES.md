# Corrections de Sécurité Null - Menu Administration

## Problème Identifié

Le menu d'administration générait plusieurs erreurs critiques empêchant le bon
fonctionnement:

```
ERROR -- Class: ResultsLoadUtility, Method: loadChildTestItems, Error: Error loading child tests for parent test: Cannot parse null string
ERROR -- Class: java.lang.Integer, Method: parseInt, Line: -1, Message: Cannot parse null string
ERROR -- Class: DictionaryServiceImpl, Method: getDictionaryById, Line: 145, Message: Cannot invoke "String.trim()" because "dictionaryId" is null
ERROR -- Class: java.lang.NumberFormatException, Method: forInputString, Line: -1, Message: For input string: "null"
```

**Impact**: Les fonctionnalités d'administration des tests ne fonctionnaient
plus correctement.

## Causes Racines

### 1. DictionaryServiceImpl.getDictionaryById() - Ligne 145

**Problème**: Appel de `dictionaryId.trim()` sans vérification null

```java
public Dictionary getDictionaryById(String dictionaryId) {
    return getBaseObjectDAO().getDictionaryById(dictionaryId.trim()); // ❌ NPE si dictionaryId est null
}
```

### 2. DictionaryDAOImpl.getDataForId() - Ligne 254

**Problème**: Appel de `Integer.parseInt(dictionaryId)` sans validation

```java
Query<Dictionary> query = entityManager.unwrap(Session.class).createQuery(sql, Dictionary.class);
query.setParameter("id", Integer.parseInt(dictionaryId)); // ❌ Exception si null ou "null"
return query.uniqueResult();
```

### 3. ResultsLoadUtility - Ligne 1053

**Problème**: Comparateur avec vérification null incorrecte

```java
public int compare(TestResult o1, TestResult o2) {
    if (GenericValidator.isBlankOrNull(o1.getSortOrder())
            || GenericValidator.isBlankOrNull(o2.getSortOrder())) {
        return 1; // ❌ Retourne mais continue à exécuter parseInt en-dessous!
    }
    return Integer.parseInt(o1.getSortOrder()) - Integer.parseInt(o2.getSortOrder());
}
```

### 4. ResultsLoadUtility - Ligne 943

**Problème**: Pas de protection contre NumberFormatException

```java
if (!GenericValidator.isBlankOrNull(testResults.get(0).getSignificantDigits())) {
    testItem.setSignificantDigits(Integer.parseInt(testResults.get(0).getSignificantDigits()));
    // ❌ Pas de try-catch si la valeur n'est pas un nombre valide
}
```

## Solutions Appliquées

### 1. DictionaryServiceImpl.getDictionaryById()

**Fichier**:
[DictionaryServiceImpl.java:144-148](src/main/java/org/openelisglobal/dictionary/service/DictionaryServiceImpl.java#L144-L148)

**Correction**:

```java
@Override
@Transactional(readOnly = true)
public Dictionary getDictionaryById(String dictionaryId) {
    // ✅ Validation null et empty
    if (dictionaryId == null || dictionaryId.trim().isEmpty()) {
        return null;
    }
    return getBaseObjectDAO().getDictionaryById(dictionaryId.trim());
}
```

**Comportement**:

- Retourne `null` si `dictionaryId` est null ou vide
- Évite NullPointerException
- Appelle le DAO seulement avec des IDs valides

### 2. DictionaryDAOImpl.getDataForId()

**Fichier**:
[DictionaryDAOImpl.java:248-272](src/main/java/org/openelisglobal/dictionary/daoimpl/DictionaryDAOImpl.java#L248-L272)

**Correction**:

```java
@Override
@Transactional(readOnly = true)
public Dictionary getDataForId(String dictionaryId) throws LIMSRuntimeException {
    // ✅ Validation complète avant parsing
    if (dictionaryId == null || dictionaryId.trim().isEmpty() || "null".equals(dictionaryId)) {
        return null;
    }

    String sql = "from Dictionary d where d.id = :id";
    try {
        Query<Dictionary> query = entityManager.unwrap(Session.class).createQuery(sql, Dictionary.class);
        query.setParameter("id", Integer.parseInt(dictionaryId.trim()));
        return query.uniqueResult();

    } catch (NumberFormatException ignored) {
        // ✅ Gestion de format invalide
        LogEvent.logWarn(this.getClass().getSimpleName(), "getDataForId",
                "Invalid dictionary ID format: " + dictionaryId);
        return null;
    } catch (HibernateException e) {
        handleException(e, "getDataForId");
    }
    return null;
}
```

**Comportement**:

- Retourne `null` si l'ID est null, vide, ou littéralement "null"
- Capture `NumberFormatException` si l'ID n'est pas un entier
- Log un warning pour le débogage
- Évite les crashes

### 3. ResultsLoadUtility - Comparateur TestResult (Ligne 1045-1068)

**Fichier**:
[ResultsLoadUtility.java:1045-1068](src/main/java/org/openelisglobal/result/action/util/ResultsLoadUtility.java#L1045-L1068)

**Correction**:

```java
Collections.sort(testResults, new Comparator<TestResult>() {
    @Override
    public int compare(TestResult o1, TestResult o2) {
        // ✅ Gestion correcte des null - push à la fin
        boolean o1Blank = GenericValidator.isBlankOrNull(o1.getSortOrder());
        boolean o2Blank = GenericValidator.isBlankOrNull(o2.getSortOrder());

        if (o1Blank && o2Blank) {
            return 0; // Les deux null - égaux
        }
        if (o1Blank) {
            return 1; // o1 null - pousser à la fin
        }
        if (o2Blank) {
            return -1; // o2 null - pousser à la fin
        }

        // ✅ Try-catch pour format invalide
        try {
            return Integer.parseInt(o1.getSortOrder()) - Integer.parseInt(o2.getSortOrder());
        } catch (NumberFormatException ignored) {
            // Si le parsing échoue, traiter comme égaux
            return 0;
        }
    }
});
```

**Comportement**:

- Trie les `TestResult` par `sortOrder`
- Met les éléments sans `sortOrder` à la fin
- Gère les formats invalides sans crasher
- Retourne 0 (égal) si le parsing échoue

### 4. ResultsLoadUtility - Significant Digits (Ligne 941-950)

**Fichier**:
[ResultsLoadUtility.java:941-950](src/main/java/org/openelisglobal/result/action/util/ResultsLoadUtility.java#L941-L950)

**Correction**:

```java
if (!testResults.isEmpty() && NUMERIC_RESULT_TYPE.equals(testResults.get(0).getTestResultType())
        && !GenericValidator.isBlankOrNull(testResults.get(0).getSignificantDigits())) {
    try {
        // ✅ Try-catch ajouté pour protection
        testItem.setSignificantDigits(Integer.parseInt(testResults.get(0).getSignificantDigits()));
    } catch (NumberFormatException ignored) {
        // Ignorer les valeurs invalides - utiliser la valeur par défaut
        LogEvent.logWarn(this.getClass().getSimpleName(), "setDictionaryResults",
                "Invalid significant digits value: " + testResults.get(0).getSignificantDigits());
    }
}
```

**Comportement**:

- Parse les chiffres significatifs seulement s'ils sont présents
- Capture `NumberFormatException` si la valeur n'est pas un entier
- Log un warning pour le débogage
- Continue sans crasher (utilise la valeur par défaut)

## Warnings Non-Critiques

### TestCatalogRestController - Test Sans Résultat Actif

**Message**:

```
WARN -- Class: TestCatalogRestController, Method: createTestList,
Warning: test that doesn't have an active test result found. Possibly issue with data in database
```

**Localisation**:
[TestCatalogRestController.java:120-123](src/main/java/org/openelisglobal/testconfiguration/controller/rest/TestCatalogRestController.java#L120-L123)

**Code Existant** (déjà correct):

```java
if (testResults.size() > 0) {
    catalog.setSignificantDigits(
            testService.getPossibleTestResults(test).get(0).getSignificantDigits());
} else {
    LogEvent.logWarn(this.getClass().getSimpleName(), "createTestList",
            "test that doesn't have an active test result found. Possibly issue with data in database");
    catalog.setSignificantDigits("0"); // ✅ Valeur par défaut
}
```

**Statut**: ✅ **Déjà géré correctement** - Ce n'est qu'un warning pour signaler
des tests numériques sans résultats configurés. Le code met "0" par défaut et
continue.

**Action**: Aucune modification nécessaire. Ce warning aide à identifier les
tests incomplets dans la base de données.

### 5. TestCatalogRestController.getDictionaryValue() - Ligne 188

**Fichier**:
[TestCatalogRestController.java:184-203](src/main/java/org/openelisglobal/testconfiguration/controller/rest/TestCatalogRestController.java#L184-L203)

**Correction**:

```java
private String getDictionaryValue(TestResult testResult) {

    if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResult.getTestResultType())) {
        Dictionary dictionary = dictionaryService.getDataForId(testResult.getValue());

        // ✅ Vérification null ajoutée
        if (dictionary == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "getDictionaryValue",
                    "Dictionary not found for ID: " + testResult.getValue());
            return null;
        }

        String displayValue = dictionary.getLocalizedName();

        if ("unknown".equals(displayValue)) {
            displayValue = !org.apache.commons.validator.GenericValidator.isBlankOrNull(dictionary.getDictEntry())
                    ? dictionary.getDictEntry()
                    : dictionary.getLocalAbbreviation();
        }

        if (testResult.getIsQuantifiable()) {
            displayValue += " Qualifiable";
        }
        return displayValue;
    }

    return null;
}
```

**Comportement**:

- Retourne `null` si le dictionnaire n'est pas trouvé
- Log un warning pour identifier les IDs invalides
- Évite `NullPointerException` lors de l'accès au catalogue de tests
- Le code appelant (`CollectionUtils.addIgnoreNull`) gère déjà les valeurs null

### 6. TestCatalogController.getDictionaryValue() - Ligne 187

**Fichier**:
[TestCatalogController.java:183-202](src/main/java/org/openelisglobal/testconfiguration/controller/TestCatalogController.java#L183-L202)

**Correction**: Identique à TestCatalogRestController ci-dessus.

**Comportement**: Même protection null-safe que la version REST.

## Tests de Validation

### Test 1: Dictionary Lookup avec ID Null

```java
// Avant: NullPointerException
// Après: Retourne null proprement
Dictionary dict = dictionaryService.getDictionaryById(null);
// Expected: dict == null, no exception
```

### Test 2: Dictionary Lookup avec String "null"

```java
// Avant: NumberFormatException: For input string: "null"
// Après: Retourne null avec warning
Dictionary dict = dictionaryDAO.getDataForId("null");
// Expected: dict == null, warning logged
```

### Test 3: Tri TestResults avec SortOrder Null

```java
// Avant: NullPointerException dans parseInt
// Après: Les tests sans sortOrder sont mis à la fin
List<TestResult> results = getTestResults();
Collections.sort(results, comparator);
// Expected: Tri correct sans exception
```

### Test 4: SignificantDigits Invalide

```java
// Avant: NumberFormatException non gérée
// Après: Warning logged, valeur par défaut utilisée
testItem.setSignificantDigits(Integer.parseInt("abc"));
// Expected: Warning logged, pas de crash
```

### Test 5: Test Catalog avec Dictionary Manquant

```java
// Avant: NullPointerException: Cannot invoke "Dictionary.getLocalizedName()" because "dictionary" is null
// Après: Retourne null avec warning, continue le traitement
String value = getDictionaryValue(testResult);
// Expected: value == null, warning logged, catalogue s'affiche
```

## Impact

### Fonctionnalités Restaurées

1. ✅ **Menu Administration → Catalogue de Tests**: Fonctionne sans erreurs même
   avec des dictionnaires manquants
2. ✅ **Gestion des Tests Bactériologiques**: Chargement des tests enfants sans
   crash
3. ✅ **Configuration des Résultats**: Gestion des dictionnaires avec IDs
   invalides
4. ✅ **Tri des Tests**: Fonctionne même avec des `sortOrder` manquants
5. ✅ **Affichage du Catalogue**: Les tests s'affichent même si certains
   dictionnaires sont manquants

### Bénéfices

1. **Robustesse**: Le système ne crash plus sur des données invalides
2. **Logs Informatifs**: Les warnings aident à identifier les problèmes de
   données
3. **Expérience Utilisateur**: Les fonctionnalités d'administration sont
   utilisables
4. **Maintenabilité**: Code plus défensif et facile à déboguer

## Fichiers Modifiés

| Fichier                                                                                                                             | Lignes    | Modification                             |
| ----------------------------------------------------------------------------------------------------------------------------------- | --------- | ---------------------------------------- |
| [DictionaryServiceImpl.java](src/main/java/org/openelisglobal/dictionary/service/DictionaryServiceImpl.java)                        | 144-148   | Validation null avant trim()             |
| [DictionaryDAOImpl.java](src/main/java/org/openelisglobal/dictionary/daoimpl/DictionaryDAOImpl.java)                                | 248-272   | Validation + catch NumberFormatException |
| [ResultsLoadUtility.java](src/main/java/org/openelisglobal/result/action/util/ResultsLoadUtility.java)                              | 1045-1068 | Comparateur null-safe                    |
| [ResultsLoadUtility.java](src/main/java/org/openelisglobal/result/action/util/ResultsLoadUtility.java)                              | 941-950   | Protection NumberFormatException         |
| [TestCatalogRestController.java](src/main/java/org/openelisglobal/testconfiguration/controller/rest/TestCatalogRestController.java) | 184-203   | Validation dictionary null               |
| [TestCatalogController.java](src/main/java/org/openelisglobal/testconfiguration/controller/TestCatalogController.java)              | 183-202   | Validation dictionary null               |

## Prochaines Étapes Recommandées

### Nettoyage de Données (Optionnel)

Si vous voulez identifier les données invalides dans la base de données:

```sql
-- Trouver les tests sans sortOrder
SELECT id, description
FROM test
WHERE sort_order IS NULL OR sort_order = '';

-- Trouver les test_result sans sortOrder valide
SELECT id, test_id, sort_order
FROM test_result
WHERE sort_order IS NULL
   OR sort_order = ''
   OR sort_order NOT SIMILAR TO '[0-9]+';

-- Trouver les dictionary avec IDs invalides (vérification)
SELECT id, dict_entry
FROM dictionary
WHERE id IS NULL;
```

### Prévention Future

1. **Contraintes Base de Données**: Ajouter des contraintes NOT NULL sur les
   colonnes critiques
2. **Validation Frontend**: Valider les entrées utilisateur avant soumission
3. **Tests Unitaires**: Ajouter des tests pour les cas null/invalides
4. **Documentation**: Documenter les champs obligatoires vs optionnels

## Conclusion

Toutes les méthodes critiques ont été rendues null-safe et gèrent maintenant
gracieusement les valeurs invalides. Le menu d'administration fonctionne à
nouveau normalement sans crasher sur des données incomplètes ou corrompues.

Les warnings sont conservés pour aider à identifier les problèmes de données,
mais le système continue de fonctionner même en présence de ces anomalies.
