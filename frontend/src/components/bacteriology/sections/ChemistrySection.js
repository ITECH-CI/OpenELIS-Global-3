import { Column, Grid, Section } from "@carbon/react";
import { useCallback, useMemo } from "react";
import { FormattedMessage } from "react-intl";
import BacteriologyResultField from "../common/BacteriologyResultField";

// Chemistry tests (Glucose, Protéine) belong to bacteriology samples (e.g. LCR)
// but are neither macroscopy, microscopy nor culture. They are identified BY
// NAME (normalized for case/accents) so the match stays correct across
// databases — never by id, which is sequence-generated and differs in prod.
// Test names are matched by substring (not equality) because the loaded test
// name often carries a sample-type suffix, e.g. "Glucose(LCR)".
const CHEMISTRY_TEST_NAMES = ["glucose", "proteine"];

const isChemistryTest = (testName) => {
  if (!testName) return false;
  const normalized = testName
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim();
  return CHEMISTRY_TEST_NAMES.some((name) => normalized.includes(name));
};

const ChemistrySection = ({
  accessionNumber,
  testResults = [],
  chemistryResults = {},
  onChange,
  disabled = false,
}) => {
  const handleFieldChange = useCallback(
    (testId, value) => {
      onChange({ ...chemistryResults, [testId]: value });
    },
    [onChange, chemistryResults],
  );

  // Clean test name by removing trailing parentheses (sample type)
  const cleanTestName = (testName) =>
    testName.replace(/\s*\([^)]*\)\s*$/, "").trim();

  const chemistryTests = useMemo(
    () => testResults.filter((test) => isChemistryTest(test.testName)),
    [testResults],
  );

  if (chemistryTests.length === 0) {
    return null;
  }

  return (
    <Section>
      <h3 style={{ marginBottom: "1rem" }}>
        <FormattedMessage
          id="bacteriology.section.chemistry"
          defaultMessage="Chimie"
        />
      </h3>

      <div className="bacteriology-regular-tests">
        <Grid fullWidth>
          {chemistryTests.map((test) => {
            const isDictionary = test.resultType === "D";
            const isMultiSelect = test.resultType === "M";
            const uniqueKey = `${test.analysisId}-${test.testId}`;
            const cleanedName = cleanTestName(test.testName);

            return (
              <Column
                key={uniqueKey}
                lg={8}
                md={4}
                sm={4}
                style={{ marginBottom: "1.5rem" }}
              >
                <BacteriologyResultField
                  id={`chem_${test.testId}`}
                  label={
                    test.unitsOfMeasure
                      ? `${cleanedName} (${test.unitsOfMeasure})`
                      : cleanedName
                  }
                  type={
                    isDictionary || isMultiSelect
                      ? "select"
                      : test.resultType === "N"
                        ? "number"
                        : "text"
                  }
                  value={chemistryResults[test.testId] ?? ""}
                  onChange={(value) => handleFieldChange(test.testId, value)}
                  options={test.dictionaryResults || []}
                  disabled={disabled}
                  required={false}
                />
              </Column>
            );
          })}
        </Grid>
      </div>
    </Section>
  );
};

export default ChemistrySection;
