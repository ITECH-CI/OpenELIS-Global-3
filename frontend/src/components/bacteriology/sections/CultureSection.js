import React, { useState } from "react";
import { Grid, Column, Section, RadioButtonGroup, RadioButton, Toggle } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import OrganismList from "../organisms/OrganismList";

const CultureSection = ({
  accessionNumber,
  cultureResult,
  onCultureResultChange,
  organisms = [],
  onOrganismsChange,
  disabled = false,
}) => {
  const [showCulture, setShowCulture] = useState(false);

  // Show organisms only if culture is positive
  const isCulturePositive = cultureResult === "positive";

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

        {showCulture && (
          <>
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

            {isCulturePositive && (
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
          </>
        )}
      </Grid>
    </Section>
  );
};

export default CultureSection;
