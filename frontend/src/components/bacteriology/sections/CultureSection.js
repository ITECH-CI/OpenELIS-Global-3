import { useMemo } from "react";
import {
  Grid,
  Column,
  Section,
  RadioButtonGroup,
  RadioButton,
  Select,
  SelectItem,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";
import OrganismList from "../organisms/OrganismList";

const CultureSection = ({
  accessionNumber,
  testResults = [],
  cultures = {},
  onCulturesChange,
  disabled = false,
}) => {
  // Filter culture tests from testResults
  const cultureTests = useMemo(() => {
    return testResults.filter(
      (test) =>
        test.testName?.toLowerCase().includes("culture") &&
        test.isCultureTest === true,
    );
  }, [testResults]);

  // Only show culture section if at least one culture test was selected in the ordonnance
  if (cultureTests.length === 0) {
    return null;
  }

  // Handle culture result change for a specific test
  const handleCultureResultChange = (testId, value) => {
    const updatedCultures = {
      ...cultures,
      [testId]: {
        ...cultures[testId],
        cultureResult: value,
        // Clear organisms if result is negative or contaminated
        organisms:
          value === "negative" || value === "contaminated"
            ? []
            : cultures[testId]?.organisms || [],
      },
    };
    onCulturesChange(updatedCultures);
  };

  // Handle organisms change for a specific test
  const handleOrganismsChange = (testId, organisms) => {
    const updatedCultures = {
      ...cultures,
      [testId]: {
        ...cultures[testId],
        organisms: organisms,
      },
    };
    onCulturesChange(updatedCultures);
  };

  // Use accessionNumber as prefix for unique IDs
  const idPrefix = accessionNumber
    ? accessionNumber.replace(/[^a-zA-Z0-9]/g, "_")
    : "culture";

  return (
    <Section>
      <h3 style={{ marginBottom: "1rem" }}>
        <FormattedMessage id="bacteriology.section.culture" />
      </h3>

      {/* Render each culture test separately */}
      {cultureTests.map((cultureTest, index) => {
        const testId = cultureTest.testId;
        const uniqueCultureKey = `${cultureTest.analysisId}-${testId}`;
        const cultureData = cultures[testId] || {
          cultureResult: "",
          organisms: [],
        };
        const cultureResult = cultureData.cultureResult;
        const organisms = cultureData.organisms || [];

        // Get dictionary results for this culture test if available
        const cultureResultOptions = cultureTest.dictionaryResults || [];

        // Determine if we should show antibiogram directly (for Neisseria gonorrhoeae POSITIVE)
        const showAntibiogramDirectly = (() => {
          if (!cultureResult) return false;

          // Check if the selected result is "Neisseria gonorrhoeae Positive"
          if (cultureResultOptions.length > 0) {
            const selectedOption = cultureResultOptions.find(
              (opt) => opt.id === cultureResult,
            );
            const value = selectedOption?.value?.toLowerCase() || "";
            // Must contain both "neisseria" AND "positive"
            return value.includes("neisseria") && value.includes("positive");
          }

          const value = cultureResult.toLowerCase();
          return value.includes("neisseria") && value.includes("positive");
        })();

        // Show organisms only for positive cultures (non-Neisseria)
        const isCulturePositive = (() => {
          if (!cultureResult || showAntibiogramDirectly) return false;

          // For dropdown options
          if (cultureResultOptions.length > 0) {
            const selectedOption = cultureResultOptions.find(
              (opt) => opt.id === cultureResult,
            );
            return (
              selectedOption?.value?.toLowerCase().includes("positive") || false
            );
          }

          // For radio buttons
          return cultureResult === "positive" || cultureResult === "Positive";
        })();

        // Render culture results based on test configuration
        const renderCultureResults = () => {
          // If we have dictionary results from the test, use Select dropdown
          if (cultureResultOptions.length > 0) {
            return (
              <Column lg={8} md={4} sm={4}>
                <Select
                  id={`cultureResult_${idPrefix}_${testId}`}
                  labelText={`${cultureTest.testName} - Résultat`}
                  value={cultureResult || ""}
                  onChange={(e) =>
                    handleCultureResultChange(testId, e.target.value)
                  }
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
                legendText={`${cultureTest.testName} - Résultat`}
                name={`cultureResult_${idPrefix}_${testId}`}
                valueSelected={cultureResult || ""}
                onChange={(value) => handleCultureResultChange(testId, value)}
                disabled={disabled}
              >
                <RadioButton
                  id={`culture_negative_${idPrefix}_${testId}`}
                  labelText="Négative"
                  value="negative"
                />
                <RadioButton
                  id={`culture_positive_${idPrefix}_${testId}`}
                  labelText="Positive"
                  value="positive"
                />
                <RadioButton
                  id={`culture_contaminated_${idPrefix}_${testId}`}
                  labelText="Contaminée"
                  value="contaminated"
                />
              </RadioButtonGroup>
            </Column>
          );
        };

        return (
          <div
            key={uniqueCultureKey}
            style={{
              marginBottom: index < cultureTests.length - 1 ? "3rem" : "0",
            }}
          >
            <Grid fullWidth>
              {/* Culture result selection */}
              {renderCultureResults()}

              {/* For NORMAL culture type: show organism list when culture is positive */}
              {isCulturePositive && !showAntibiogramDirectly && (
                <Column lg={16} md={8} sm={4}>
                  <div style={{ marginTop: "2rem" }}>
                    <OrganismList
                      accessionNumber={accessionNumber}
                      organisms={organisms}
                      onChange={(newOrganisms) =>
                        handleOrganismsChange(testId, newOrganisms)
                      }
                      disabled={disabled}
                    />
                  </div>
                </Column>
              )}

              {/* For NEISSERIA_GONORRHOEAE culture type: show antibiogram directly without organism identification */}
              {showAntibiogramDirectly && (
                <Column lg={16} md={8} sm={4}>
                  <div
                    style={{
                      marginTop: "2rem",
                      padding: "1rem",
                      backgroundColor: "#f4f4f4",
                      borderRadius: "4px",
                    }}
                  >
                    <p
                      style={{
                        fontStyle: "italic",
                        color: "#666",
                        marginBottom: "1rem",
                      }}
                    >
                      Pour les cultures Neisseria gonorrhoeae, l'antibiogramme
                      sera effectué directement sans identification de germe.
                    </p>
                    <OrganismList
                      accessionNumber={accessionNumber}
                      organisms={organisms}
                      onChange={(newOrganisms) =>
                        handleOrganismsChange(testId, newOrganisms)
                      }
                      disabled={disabled}
                      skipOrganismIdentification={true}
                    />
                  </div>
                </Column>
              )}
            </Grid>
          </div>
        );
      })}
    </Section>
  );
};

export default CultureSection;
