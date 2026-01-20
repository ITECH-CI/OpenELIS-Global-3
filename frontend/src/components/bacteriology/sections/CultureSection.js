import React, { useState, useMemo } from "react";
import { Grid, Column, Section, RadioButtonGroup, RadioButton, Toggle, Select, SelectItem } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import OrganismList from "../organisms/OrganismList";

const CultureSection = ({
  accessionNumber,
  testResults = [],
  cultureResult,
  onCultureResultChange,
  organisms = [],
  onOrganismsChange,
  disabled = false,
}) => {
  const [showCulture, setShowCulture] = useState(false);

  // Filter culture tests from testResults
  const cultureTests = useMemo(() => {
    return testResults.filter((test) =>
      test.testName?.toLowerCase().includes("culture") &&
      test.isCultureTest === true
    );
  }, [testResults]);

  // Determine culture type from tests
  const cultureType = useMemo(() => {
    if (cultureTests.length === 0) return null;

    const cultureTest = cultureTests[0];
    // Check if it's a Neisseria gonorrhoeae specialized culture
    if (cultureTest.cultureType === "NEISSERIA_GONORRHOEAE") {
      return "NEISSERIA_GONORRHOEAE";
    }
    return "NORMAL";
  }, [cultureTests]);

  // Get dictionary results for culture test if available
  const cultureResultOptions = useMemo(() => {
    if (cultureTests.length > 0 && cultureTests[0].dictionaryResults) {
      return cultureTests[0].dictionaryResults;
    }
    return [];
  }, [cultureTests]);

  // Show organisms only if culture is positive or NG positive
  const isCulturePositive =
    cultureResult === "positive" ||
    cultureResult === "Positive" ||
    (cultureResultOptions.length > 0 &&
     cultureResultOptions.find(opt => opt.id === cultureResult)?.value?.toLowerCase().includes("positive"));

  // Show antibiogram directly for NG positive/negative
  const showAntibiogramDirectly =
    cultureResult &&
    (cultureResult.toLowerCase().includes("neisseria") ||
     (cultureResultOptions.length > 0 &&
      cultureResultOptions.find(opt => opt.id === cultureResult)?.value?.toLowerCase().includes("neisseria")));

  // If culture tests exist, show directly without toggle
  const hasCultureTest = cultureTests.length > 0;

  const handleToggleChange = (checked) => {
    setShowCulture(checked);
    if (!checked) {
      // Réinitialiser les champs de culture quand on désactive
      onCultureResultChange("");
      onOrganismsChange([]);
    }
  };

  // Use accessionNumber as prefix for unique IDs
  const idPrefix = accessionNumber ? accessionNumber.replace(/[^a-zA-Z0-9]/g, '_') : 'culture';

  // Don't show section if no culture tests and toggle is off
  if (!hasCultureTest && !showCulture) {
    return (
      <Section>
        <h3 style={{ marginBottom: "1rem" }}>
          <FormattedMessage id="bacteriology.section.culture" />
        </h3>
        <Grid>
          <Column lg={8} md={4} sm={4}>
            <Toggle
              id={`toggle_culture_${idPrefix}`}
              labelText="Effectuer une culture ?"
              labelA="Non"
              labelB="Oui"
              toggled={showCulture}
              onToggle={handleToggleChange}
              disabled={disabled}
            />
          </Column>
        </Grid>
      </Section>
    );
  }

  // Render culture results based on test configuration
  const renderCultureResults = () => {
    // If we have dictionary results from the test, use Select dropdown
    if (cultureResultOptions.length > 0) {
      return (
        <Column lg={8} md={4} sm={4}>
          <Select
            id={`cultureResult_${idPrefix}`}
            labelText="Résultat de la culture"
            value={cultureResult || ""}
            onChange={(e) => onCultureResultChange(e.target.value)}
            disabled={disabled}
          >
            <SelectItem value="" text="Sélectionner..." />
            {cultureResultOptions.map((option) => (
              <SelectItem
                key={option.id}
                value={option.id}
                text={option.value}
              />
            ))}
          </Select>
        </Column>
      );
    }

    // Otherwise, use radio buttons (default behavior)
    return (
      <Column lg={8} md={4} sm={4}>
        <RadioButtonGroup
          legendText="Résultat de la culture"
          name={`cultureResult_${idPrefix}`}
          valueSelected={cultureResult || ""}
          onChange={onCultureResultChange}
          disabled={disabled}
        >
          <RadioButton
            id={`culture_negative_${idPrefix}`}
            labelText="Négative"
            value="negative"
          />
          <RadioButton
            id={`culture_positive_${idPrefix}`}
            labelText="Positive"
            value="positive"
          />
          <RadioButton
            id={`culture_contaminated_${idPrefix}`}
            labelText="Contaminée"
            value="contaminated"
          />
        </RadioButtonGroup>
      </Column>
    );
  };

  return (
    <Section>
      <h3 style={{ marginBottom: "1rem" }}>
        <FormattedMessage id="bacteriology.section.culture" />
      </h3>
      <Grid>
        {!hasCultureTest && (
          <Column lg={8} md={4} sm={4}>
            <Toggle
              id={`toggle_culture_${idPrefix}`}
              labelText="Effectuer une culture ?"
              labelA="Non"
              labelB="Oui"
              toggled={showCulture}
              onToggle={handleToggleChange}
              disabled={disabled}
            />
          </Column>
        )}

        {(hasCultureTest || showCulture) && (
          <>
            {renderCultureResults()}

            {/* Show organism list for normal positive cultures */}
            {isCulturePositive && !showAntibiogramDirectly && (
              <Column lg={16} md={8} sm={4}>
                <div style={{ marginTop: "2rem" }}>
                  <OrganismList
                    accessionNumber={accessionNumber}
                    organisms={organisms}
                    onChange={onOrganismsChange}
                    disabled={disabled}
                  />
                </div>
              </Column>
            )}

            {/* For NG cultures, show antibiogram directly */}
            {showAntibiogramDirectly && (
              <Column lg={16} md={8} sm={4}>
                <div style={{ marginTop: "2rem", padding: "1rem", backgroundColor: "#f4f4f4", borderRadius: "4px" }}>
                  <p style={{ fontStyle: "italic", color: "#666" }}>
                    Pour les cultures Neisseria gonorrhoeae, l'antibiogramme sera effectué directement.
                  </p>
                  <OrganismList
                    accessionNumber={accessionNumber}
                    organisms={organisms}
                    onChange={onOrganismsChange}
                    disabled={disabled}
                  />
                </div>
              </Column>
            )}
          </>
        )}
      </Grid>
    </Section>
  );
};

export default CultureSection;
