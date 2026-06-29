# Modifications SampleType.js

## Modifications à appliquer manuellement

### 1. Charger le mapping des IDs TB au démarrage

**Ligne ~790 - Dans le useEffect initial, ajouter:**

```javascript
useEffect(() => {
  componentMounted.current = true;

  // Load TB dictionary mapping to avoid hardcoded IDs
  getFromOpenElisServer("/rest/tb-dictionary-mapping", (mapping) => {
    if (mapping) {
      setMicroscopieTBId(mapping["Microsc"] || "");
      setFollowupLine1Id(mapping["TB Line1"] || "");
      setFollowupLine2Id(mapping["TB Line2"] || "");
    }
  });

  getFromOpenElisServer(
    "/rest/referral-reasons",
    displayReferralReasonsOptions
  );
  // ... reste du code existant
}, []);
```

### 2. Ajouter tbPatientCode dans l'état tbData

**Ligne ~110 - Modifier l'initialisation de tbData:**

```javascript
const [tbData, setTbData] = useState(
  sample?.tbData != null
    ? sample.tbData
    : {
        tbOrderReason: "",
        tbDiagnosticReason: "",
        tbFollowupReason: "",
        tbFollowupPeriodLine1: "",
        tbFollowupPeriodLine2: "",
        tbAspect: "",
        tbSpecimenNature: "",
        tbSubjectNumber: "",
        selectedTbMethod: "",
        tbPatientCode: "", // AJOUTER CETTE LIGNE
      }
);
```

### 3. Remplacer toutes les références hardcodées

**Ligne ~525 - Dans handleFollowupreason:**

```javascript
function handleFollowupreason(e) {
  const value = e.target.value;
  const selectedOption = e.target.options[e.target.selectedIndex];
  const label =
    selectedOption.text === "Examen de suivi 1ère ligne (TB Sensible)"
      ? followupLine1Id // CHANGER de followupLine1
      : followupLine2Id; // CHANGER de followupLine2
  setFollowupReason(value);
  if (value) {
    getFromOpenElisServer(
      `/rest/Dictionary-by-ByCategory?category=${encodeURIComponent(label)}`,
      displayTbFollowupLinesOptions
    );
  }
}
```

**Ligne ~558 - Dans le useEffect pour followupReason:**

```javascript
useEffect(() => {
  if (tbData.tbFollowupReason && reasons.length > 0) {
    const selectedReason = reasons.find(
      (item) => item.id === tbData.tbFollowupReason
    );
    if (selectedReason) {
      const label =
        selectedReason.value === "Examen de suivi 1ère ligne (TB Sensible)"
          ? followupLine1Id // CHANGER de followupLine1
          : followupLine2Id; // CHANGER de followupLine2
      getFromOpenElisServer(
        `/rest/Dictionary-by-ByCategory?category=${encodeURIComponent(label)}`,
        displayTbFollowupLinesOptions
      );
    }
  }
}, [tbData.tbFollowupReason, reasons, followupLine1Id, followupLine2Id]); // AJOUTER followupLine1Id, followupLine2Id aux dépendances
```

**Ligne ~612 - Dans le useEffect pour tbData.selectedTbMethod:**

```javascript
useEffect(() => {
  componentMounted.current = true;
  if (tbData.selectedTbMethod !== "" && tbData.selectedTbMethod != null) {
    if (isTb) {
      getFromOpenElisServer(
        `/MicrobiologyTb/panel_test?method=${tbData.selectedTbMethod}`,
        fetchSampleTypeTests
      );
      if (tbData.selectedTbMethod === microscopieTBId) {
        // CHANGER de MicroscopieTB
        getFromOpenElisServer(
          `/rest/Dictionary-by-ByCategory?category=TB Sample Aspects`,
          fetchTbSampleAspects
        );
      }
    } else {
      getFromOpenElisServer(
        `/rest/sample-type-tests?sampleType=${tbData.selectedTbMethod}`,
        fetchSampleTypeTests
      );
    }
  }
  return () => {
    componentMounted.current = false;
  };
}, [tbData.selectedTbMethod, isTb, microscopieTBId]); // AJOUTER microscopieTBId aux dépendances
```

**Ligne ~664 - Dans le useEffect pour selectedTbSampleMethod:**

```javascript
useEffect(() => {
  componentMounted.current = true;
  if (selectedTbSampleMethod.id !== "" && selectedTbSampleMethod.id != null) {
    if (isTb) {
      getFromOpenElisServer(
        `/MicrobiologyTb/panel_test?method=${selectedTbSampleMethod.id}`,
        fetchSampleTypeTests
      );
      if (selectedTbSampleMethod.id === microscopieTBId) {
        // CHANGER de MicroscopieTB
        getFromOpenElisServer(
          `/rest/Dictionary-by-ByCategory?category=TB Sample Aspects`,
          fetchTbSampleAspects
        );
      }
    }
  }
  return () => {
    componentMounted.current = false;
  };
}, [selectedTbSampleMethod.id, isTb, microscopieTBId]); // AJOUTER microscopieTBId aux dépendances
```

### 4. Ajouter le champ Code Patient TB-RR dans l'UI

**Ligne ~917 - Après le Select "Motif de l'examen TB", AJOUTER:**

```javascript
            </Column>

            {/* Nouveau champ Code Patient TB-RR */}
            <Column lg={8} md={4} sm={4}>
              <TextInput
                value={tbData.tbPatientCode || ""}
                onChange={(e) => {
                  let value = e.target.value.replace(/[^0-9]/g, ''); // Garder seulement les chiffres

                  // Format: AAAA/XX/XXX
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
                labelText={intl.formatMessage({
                  id: "observation.tb.patient.code",
                })}
                id={"tbPatientCode_" + index}
                placeholder="AAAA/XX/XXX"
                maxLength={11}
              />
            </Column>

            {tbData.tbDiagnosticReason === tbReasonFollowUp && (
```

### 5. Corriger l'affichage de tbAspect

**Ligne ~1003 - SUPPRIMER le bloc tbAspect existant dans followupLine2**

**Ligne ~1082 - Après le Select "Méthode d'analyse TB", AJOUTER:**

```javascript
              </Select>
            </Column>

            {/* Afficher tbAspect dès que Microscopie TB est sélectionnée */}
            {selectedTbSampleMethod.id === microscopieTBId && (
              <Column lg={8} md={4} sm={4}>
                <Select
                  id={"tbAspect_" + index}
                  value={tbData.tbAspect}
                  onChange={(e) =>
                    handleChange("tbAspect", e.target.value)
                  }
                  required
                  labelText={
                    <FormattedMessage id="sample.tb.aspect" />
                  }
                >
                  <SelectItem value="" text="" />
                  {tbSampleAspect.map((option) => {
                    return (
                      <SelectItem
                        key={option.id}
                        value={option.id}
                        text={option.value}
                      />
                    );
                  })}
                </Select>
              </Column>
            )}
          </div>
```

### 6. Ajouter la traduction française

**Dans `/frontend/src/languages/fr.json`, AJOUTER:**

```json
{
  "observation.tb.patient.code": "Code Patient TB-RR (AAAA/XX/XXX)"
}
```

## Résumé des changements

✅ **Backend:**

- Endpoint `/rest/tb-dictionary-mapping` créé dans DisplayListController
- Migration Liquibase pour `TbPatientCode` observation type créée

🔄 **Frontend à modifier:**

1. Charger le mapping TB au démarrage (ligne ~790)
2. Ajouter `tbPatientCode` dans tbData (ligne ~110)
3. Remplacer 5 références hardcodées (lignes 525, 558, 612, 664, 1003)
4. Ajouter le champ Code Patient TB-RR (après ligne 917)
5. Déplacer tbAspect pour affichage conditionnel correct (après ligne 1082)
6. Ajouter traduction fr.json

⏳ **À faire après:**

- Modifier backend pour sauvegarder TbPatientCode
- Mettre à jour TBColumnBuilder pour export CSV
