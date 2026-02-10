import { Column, Grid, Section } from "@carbon/react";
import { useCallback, useMemo } from "react";
import { FormattedMessage } from "react-intl";
import BacteriologyResultField from "../common/BacteriologyResultField";
import ConditionalTestGroup from "../ConditionalTestGroup";

const MacroscopySection = ({
  accessionNumber,
  testResults = [],
  macroscopyResults = {},
  onChange,
  disabled = false,
}) => {
  const handleFieldChange = useCallback(
    (testId, value) => {
      onChange({ ...macroscopyResults, [testId]: value });
    },
    [onChange, macroscopyResults],
  );

  // Clean test name by removing trailing parentheses (sample type)
  const cleanTestName = (testName) => {
    // Remove trailing parentheses content like "(Sécrétions vaginales)"
    return testName.replace(/\s*\([^)]*\)\s*$/, "").trim();
  };

  // Filter tests that belong to macroscopy - memoized to avoid re-filtering
  const macroscopyTests = useMemo(
    () =>
      testResults.filter((test) =>
        test.testName?.toLowerCase().includes("macroscopie"),
      ),
    [testResults],
  );

  // Separate conditional and non-conditional tests - ONLY depends on testResults, NOT on macroscopyResults
  const { conditionalGroupsStructure, regularTests } = useMemo(() => {
    const childTests = macroscopyTests.filter(
      (test) => test.parentTestId != null,
    );

    // Find tests that are actually parents (have children pointing to them)
    const parentTestIds = new Set(
      childTests.map((child) => child.parentTestId),
    );
    const parentTests = macroscopyTests.filter((test) =>
      parentTestIds.has(test.testId),
    );

    // Group children by parent - store structure only, not values
    const groupsStructure = parentTests.map((parent) => {
      const children = childTests.filter(
        (child) => child.parentTestId === parent.testId,
      );
      return {
        parent: parent,
        children: children,
      };
    });

    // Regular tests are those that are neither parents nor children
    const regular = macroscopyTests.filter(
      (test) => !parentTestIds.has(test.testId) && test.parentTestId == null,
    );

    return {
      conditionalGroupsStructure: groupsStructure,
      regularTests: regular,
    };
  }, [macroscopyTests]); // Only recompute when tests change, NOT when results change

  // Add result values to the structure (fast operation, done on every render)
  const conditionalGroups = conditionalGroupsStructure.map((group) => ({
    parent: {
      ...group.parent,
      resultValue: macroscopyResults[group.parent.testId] || "",
    },
    children: group.children.map((child) => ({
      ...child,
      resultValue: macroscopyResults[child.testId] || "",
    })),
  }));

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
        <div style={{ marginBottom: "1.5rem" }}>
          {conditionalGroups.map((group) => {
            const uniqueGroupKey = `${group.parent.analysisId}-${group.parent.testId}`;
            return (
              <div key={uniqueGroupKey} style={{ marginBottom: "1.5rem" }}>
                <ConditionalTestGroup
                  accessionNumber={accessionNumber}
                  parentTest={group.parent}
                  childTests={group.children}
                  onResultChange={handleFieldChange}
                  disabled={disabled}
                />
              </div>
            );
          })}
        </div>
      )}

      {/* Regular Tests */}
      {regularTests.length > 0 && (
        <div className="bacteriology-regular-tests">
          <Grid fullWidth>
            {regularTests.map((test) => {
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
                    value={macroscopyResults[test.testId] ?? ""}
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
      )}
    </Section>
  );
};

export default MacroscopySection;
