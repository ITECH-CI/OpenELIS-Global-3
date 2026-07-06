# Plan d'optimisation de SampleType.js

## Problèmes identifiés

### 1. IDs hardcodés (lignes 131-133)

```javascript
var MicroscopieTB = "1368";
var followupLine1 = "1405";
var followupLine2 = "1406";
```

**Problème**: Ces IDs peuvent changer entre environnements/bases de données.

**Solution**: Utiliser les `local_abbrev` des dictionnaires pour les identifier
de manière unique:

- Microscopie TB → local_abbrev: `Microsc`
- Besoin d'identifier les followup lines

### 2. tbAspect mal affiché (ligne 1003)

**Problème**: tbAspect n'apparaît que dans le bloc followupLine2, mais devrait
apparaître dès que Microscopie TB est sélectionnée.

**Solution**: Déplacer le champ tbAspect en dehors de la conditional
followupLine2, le rendre visible dès que
`selectedTbSampleMethod.id === microscopieTBId`.

### 3. Champ manquant : Code Patient TB-RR

**Emplacement**: Après "Motif de l'examen TB", avant "N° Lab TB"

**Format**: AAAA/XX/XXX (année/numéro/séquence)

**Implémentation**:

- Ajouter dans `tbData` state: `tbPatientCode: ""`
- Créer ObservationHistoryType: `TbPatientCode`
- Ajouter validation du format
- Inclure dans l'export CSV via TBColumnBuilder

## Solution technique

### Étape 1: Modifier le backend pour retourner local_abbrev

Créer un nouveau endpoint ou modifier `/rest/Dictionary-by-ByCategory` pour
inclure `local_abbrev`:

```java
// Dans DictionaryController
@GetMapping("/Dictionary-by-ByCategory-with-abbrev")
public List<DictionaryDTO> getDictionaryByCategoryWithAbbrev(@RequestParam String category) {
    // Retourner id, value, local_abbrev
}
```

### Étape 2: Frontend - Utiliser local_abbrev pour identifier

```javascript
const displayTbAnalysisMethodOptions = (res) => {
  if (res) {
    setTbDiagnosticMethods(res);
    // Find by local_abbrev instead of hardcoded ID
    const microscopieMethod = res.find(
      (item) => item.local_abbrev === "Microsc"
    );
    if (microscopieMethod) {
      setMicroscopieTBId(microscopieMethod.id);
    }
  }
};
```

### Étape 3: Ajouter le champ Code Patient TB-RR

```javascript
// Dans tbData state
const [tbData, setTbData] = useState({
  ...
  tbPatientCode: "",
});

// Ajouter après le champ "Motif de l'examen TB" (ligne 917)
{isTb && (
  <Column lg={8} md={4} sm={4}>
    <TextInput
      value={tbData.tbPatientCode}
      onChange={(e) => {
        let value = e.target.value.toUpperCase();
        // Format: AAAA/XX/XXX
        value = value.replace(/[^0-9]/g, ''); // Keep only numbers
        if (value.length > 4) {
          value = value.slice(0, 4) + '/' + value.slice(4);
        }
        if (value.length > 7) {
          value = value.slice(0, 7) + '/' + value.slice(7);
        }
        if (value.length > 11) {
          value = value.slice(0, 11);
        }
        handleChange("tbPatientCode", value);
      }}
      labelText={intl.formatMessage({id: "observation.tb.patient.code"})}
      id={"tbPatientCode_" + index}
      placeholder="AAAA/XX/XXX"
      maxLength={11}
    />
  </Column>
)}
```

### Étape 4: Déplacer tbAspect

Déplacer le bloc tbAspect (lignes 1003-1028) pour qu'il apparaisse après le
champ "Méthode d'analyse TB" si Microscopie est sélectionnée:

```javascript
{
  /* Après la sélection de la méthode d'analyse (ligne 1082) */
}
{
  selectedTbSampleMethod.id === microscopieTBId && (
    <Column lg={8} md={4} sm={4}>
      <Select
        id={"tbAspect_" + index}
        value={tbData.tbAspect}
        onChange={(e) => handleChange("tbAspect", e.target.value)}
        required
        labelText={<FormattedMessage id="sample.tb.aspect" />}
      >
        <SelectItem value="" text="" />
        {tbSampleAspect.map((option) => (
          <SelectItem key={option.id} value={option.id} text={option.value} />
        ))}
      </Select>
    </Column>
  );
}
```

### Étape 5: Backend - Sauvegarder le nouveau champ

Dans `SamplePatientEntryServiceImpl.java`, ajouter la sauvegarde de
`TbPatientCode`:

```java
if (tbData.getTbPatientCode() != null && !tbData.getTbPatientCode().isEmpty()) {
    ObservationHistory patientCodeOH = new ObservationHistory();
    patientCodeOH.setSampleId(sample.getId());
    patientCodeOH.setObservationHistoryTypeId(
        observationHistoryTypeService.getByTypeName("TbPatientCode").getId()
    );
    patientCodeOH.setValue(tbData.getTbPatientCode());
    observationHistoryService.insert(patientCodeOH);
}
```

### Étape 6: Export CSV - Ajouter la colonne

Dans `TBColumnBuilder.java`:

```java
// Ligne 79 - Après tborderreason
add("tbpatientcode", "TB_PATIENT_CODE", Strategy.NONE);

// Dans appendObservationHistoryCrosstab (ligne 165), ajouter à la liste:
String tbObservationTypes = "''TbOrderReason'', ''TbAnalysisMethod'', ''TbDiagnosticReason'', ''TbFollowupReason'', "
        + "''TbFollowupReasonPeriodLine1'', ''TbFollowupReasonPeriodLine2'', ''TbSampleAspects'', ''TbPatientCode''";

// Et dans la définition des colonnes (ligne 187), ajouter:
+ "\n, \"TbPatientCode\" varchar(100) "
```

## Fichiers à modifier

1. **Frontend:**

   - `/frontend/src/components/addOrder/SampleType.js`
   - `/frontend/src/languages/fr.json` (ajouter traduction)

2. **Backend:**

   - Créer
     `/src/main/java/org/openelisglobal/dictionary/controller/DictionaryControllerEnhanced.java`
     (optionnel)
   - Modifier
     `/src/main/java/org/openelisglobal/sample/service/SamplePatientEntryServiceImpl.java`
   - Modifier
     `/src/main/java/org/openelisglobal/reports/action/implementation/reportBeans/TBColumnBuilder.java`

3. **Liquibase:**
   - ✅
     `/src/main/resources/liquibase/3.2.x.x/add_tb_patient_code_observation_type.xml`
     (créé)
   - ✅ `/src/main/resources/liquibase/3.2.x.x/base.xml` (modifié)

## Ordre d'implémentation

1. ✅ Créer migration Liquibase pour TbPatientCode
2. Modifier frontend SampleType.js (en cours)
3. Tester l'affichage et validation du format
4. Modifier backend pour sauvegarder
5. Modifier TBColumnBuilder pour export
6. Tester end-to-end
