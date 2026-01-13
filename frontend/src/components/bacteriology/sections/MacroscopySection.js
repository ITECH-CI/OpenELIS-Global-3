import { Column, Grid, Section } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { useMemo } from "react";
import BacteriologyResultField from "../common/BacteriologyResultField";
import ConditionalTestGroup from "../ConditionalTestGroup";

const MacroscopySection = ({
  accessionNumber,
  testResults = [],
  macroscopyResults = {},
  onChange,
  disabled = false,
}) => {
  const handleFieldChange = (testId, value) => {
    onChange({ ...macroscopyResults, [testId]: value });
  };

  // Clean test name by removing trailing parentheses (sample type)
  const cleanTestName = (testName) => {
    // Remove trailing parentheses content like "(Secrétions vaginales)"
    return testName.replace(/\s*\([^)]*\)\s*$/, '').trim();
  };

  // Filter tests that belong to macroscopy
  const macroscopyTests = testResults.filter((test) =>
    test.testName?.toLowerCase().includes("macroscopie"),
  );

  // Separate conditional and non-conditional tests
  const { conditionalGroups, regularTests } = useMemo(() => {
    const childTests = macroscopyTests.filter((test) => test.parentTestId != null);

    // Find tests that are actually parents (have children pointing to them)
    const parentTestIds = new Set(childTests.map(child => child.parentTestId));
    const parentTests = macroscopyTests.filter((test) => parentTestIds.has(test.testId));

    // Group children by parent
    const groups = parentTests.map((parent) => {
      const children = childTests.filter((child) => child.parentTestId === parent.testId);
      return {
        parent: {
          ...parent,
          resultValue: macroscopyResults[parent.testId] || "",
        },
        children: children.map((child) => ({
          ...child,
          resultValue: macroscopyResults[child.testId] || "",
        })),
      };
    });

    // Regular tests are those that are neither parents nor children
    const regular = macroscopyTests.filter(
      (test) =>
        !parentTestIds.has(test.testId) &&
        test.parentTestId == null,
    );

    return { conditionalGroups: groups, regularTests: regular };
  }, [macroscopyTests, macroscopyResults]);

  if (macroscopyTests.length === 0) {
    return null;
  }

  return (
    <Section>
      <h3 style={{ marginBottom: "1rem" }}>
        <FormattedMessage id="bacteriology.section.macroscopy" />
      </h3>

      {/* Conditional Test Groups */}
      {conditionalGroups.length > 0 && (
        <div style={{ marginBottom: "2rem" }}>
          {conditionalGroups.map((group) => (
            <div key={group.parent.testId} style={{ marginBottom: "2rem" }}>
              <ConditionalTestGroup
                accessionNumber={accessionNumber}
                parentTest={group.parent}
                childTests={group.children}
                onResultChange={handleFieldChange}
                disabled={disabled}
              />
            </div>
          ))}
        </div>
      )}

      {/* Regular Tests */}
      {regularTests.length > 0 && (
        <Grid>
          {regularTests.map((test) => {
            const isDictionary = test.resultType === "D";
            const isMultiSelect = test.resultType === "M";
            const uniqueKey = `${test.analysisId}-${test.testId}`;
            const cleanedName = cleanTestName(test.testName);

            return (
              <Column key={uniqueKey} lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
                <BacteriologyResultField
                  id={`macro_${test.testId}`}
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
                  value={macroscopyResults[test.testId] || ""}
                  onChange={(value) => handleFieldChange(test.testId, value)}
                  options={test.dictionaryResults || []}
                  disabled={disabled}
                  required={false}
                />
              </Column>
            );
          })}
        </Grid>
      )}
    </Section>
  );
};

export default MacroscopySection;
