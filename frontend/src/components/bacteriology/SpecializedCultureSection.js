import React, { useState, useEffect, useMemo } from "react";
import {
  Grid,
  Column,
  Toggle,
  RadioButtonGroup,
  RadioButton,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";
import OrganismList from "./organisms/OrganismList";

/**
 * SpecializedCultureSection - Handles different types of bacterial cultures
 *
 * Supports three culture types:
 * - NORMAL: Standard culture (negative or identification)
 * - NEISSERIA_GONORRHOEAE: Specialized culture for N. gonorrhoeae
 *
 * @param {string} accessionNumber - Used for generating unique IDs
 * @param {object} cultureTest - Culture test object with testId, cultureType, etc.
 * @param {string} cultureResult - Current culture result value
 * @param {array} organisms - Array of identified organisms
 * @param {function} onCultureResultChange - Callback when culture result changes
 * @param {function} onOrganismsChange - Callback when organisms change
 * @param {boolean} disabled - Whether fields are disabled
 */
const SpecializedCultureSection = ({
  accessionNumber,
  cultureTest,
  cultureResult = "",
  organisms = [],
  onCultureResultChange,
  onOrganismsChange,
  disabled = false,
}) => {
  const [showCulture, setShowCulture] = useState(false);

  // Generate unique ID prefix
  const idPrefix = accessionNumber
    ? accessionNumber.replace(/[^a-zA-Z0-9]/g, "_")
    : "culture";

  // Determine culture type
  const cultureType = cultureTest?.cultureType || "NORMAL";

  // Initialize showCulture based on existing data
  useEffect(() => {
    if (cultureResult || organisms.length > 0) {
      setShowCulture(true);
    }
  }, []);

  // Handle toggle change
  const handleToggleChange = (checked) => {
    setShowCulture(checked);
    if (!checked) {
      // Reset culture fields when toggled off
      onCultureResultChange("");
      onOrganismsChange([]);
    }
  };

  // Handle culture result change
  const handleCultureResultChange = (value) => {
    onCultureResultChange(value);

    // Clear organisms if result is negative
    if (value === "negative") {
      onOrganismsChange([]);
    }
    // Auto-create Neisseria gonorrhoeae organism if selected
    else if (value === "neisseria_gonorrhoeae") {
      const neisseriaOrganism = {
        organismNumber: 1,
        organismId: null, // Will be set by backend based on dictionary
        organismName: "Neisseria gonorrhoeae",
        type: "BACTERIA",
        gramType: "Gram négatif",
        morphology: "Diplococcus",
        capsulated: false,
        antibiograms: [],
      };
      onOrganismsChange([neisseriaOrganism]);
    }
  };

  // Determine if organisms section should be shown
  const shouldShowOrganisms = useMemo(() => {
    if (cultureType === "NORMAL") {
      return cultureResult === "identification";
    } else if (cultureType === "NEISSERIA_GONORRHOEAE") {
      return (
        cultureResult === "neisseria_gonorrhoeae_identification" ||
        cultureResult === "identification"
      );
    }
    return false;
  }, [cultureResult, cultureType]);

  if (!cultureTest) {
    return null;
  }

  return (
    <div className="specialized-culture-section">
      <h4 style={{ marginBottom: "1rem" }}>{cultureTest.testName}</h4>

      {/* Toggle for culture */}
      <Grid fullWidth>
        <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
          <Toggle
            id={`toggle_culture_${idPrefix}_${cultureTest.testId}`}
            labelText="Effectuer une culture ?"
            labelA="Non"
            labelB="Oui"
            toggled={showCulture}
            onToggle={handleToggleChange}
            disabled={disabled}
          />
        </Column>
      </Grid>

      {showCulture && (
        <>
          {/* Culture Result Selection */}
          <Grid fullWidth>
            <Column lg={16} md={8} sm={4} style={{ marginBottom: "1.5rem" }}>
              <fieldset className="cds--fieldset">
                <legend className="cds--label">Résultat de la culture</legend>
                <RadioButtonGroup
                  name={`culture_result_${idPrefix}_${cultureTest.testId}`}
                  valueSelected={cultureResult}
                  onChange={handleCultureResultChange}
                  orientation="vertical"
                >
                  {/* Negative option - always available */}
                  <RadioButton
                    id={`culture_negative_${idPrefix}_${cultureTest.testId}`}
                    labelText="Négative"
                    value="negative"
                    disabled={disabled}
                  />

                  {/* Culture type specific options */}
                  {cultureType === "NORMAL" && (
                    <RadioButton
                      id={`culture_identification_${idPrefix}_${cultureTest.testId}`}
                      labelText="Identification et antibiogramme"
                      value="identification"
                      disabled={disabled}
                    />
                  )}

                  {cultureType === "NEISSERIA_GONORRHOEAE" && (
                    <>
                      <RadioButton
                        id={`culture_neisseria_${idPrefix}_${cultureTest.testId}`}
                        labelText="Neisseria gonorrhoeae (sans antibiogramme)"
                        value="neisseria_gonorrhoeae"
                        disabled={disabled}
                      />
                      <RadioButton
                        id={`culture_neisseria_id_${idPrefix}_${cultureTest.testId}`}
                        labelText="Neisseria gonorrhoeae (avec antibiogramme)"
                        value="neisseria_gonorrhoeae_identification"
                        disabled={disabled}
                      />
                      <RadioButton
                        id={`culture_other_identification_${idPrefix}_${cultureTest.testId}`}
                        labelText="Autre identification et antibiogramme"
                        value="identification"
                        disabled={disabled}
                      />
                    </>
                  )}
                </RadioButtonGroup>
              </fieldset>
            </Column>
          </Grid>

          {/* Organisms Section */}
          {shouldShowOrganisms && (
            <div style={{ marginTop: "2rem" }}>
              <h5 style={{ marginBottom: "1rem" }}>
                <FormattedMessage id="bacteriology.organisms.title" />
              </h5>
              <OrganismList
                accessionNumber={accessionNumber}
                organisms={organisms}
                onChange={onOrganismsChange}
                disabled={disabled}
              />
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default SpecializedCultureSection;
