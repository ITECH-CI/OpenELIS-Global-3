import { Column, Grid, Section } from "@carbon/react";
import { useMemo } from "react";
import { FormattedMessage } from "react-intl";
import BacteriologyResultField from "../common/BacteriologyResultField";
import ConditionalTestGroup from "../ConditionalTestGroup";
import FloraList from "../FloraList";

const MicroscopySection = ({
  accessionNumber,
  testResults = [],
  microscopyResults = {},
  floraData = {},
  onChange,
  onFloraChange,
  disabled = false,
}) => {
  const handleFieldChange = (testId, value) => {
    onChange({ ...microscopyResults, [testId]: value });
  };

  const handleFloraCountChange = (testId, count) => {
    onFloraChange({
      ...floraData,
      [testId]: {
        ...floraData[testId],
        count: count,
      },
    });
  };

  const handleFloraDetailsChange = (testId, details) => {
    onFloraChange({
      ...floraData,
      [testId]: {
        ...floraData[testId],
        details: details,
      },
    });
  };

  // Clean test name by removing trailing parentheses (sample type)
  const cleanTestName = (testName) => {
    // Remove trailing parentheses content like "(Sécrétions vaginales)"
    return testName.replace(/\s*\([^)]*\)\s*$/, "").trim();
  };

  // Filter tests that belong to microscopy
  const microscopyTests = testResults.filter((test) =>
    test.testName?.toLowerCase().includes("microscopie"),
  );

  // Separate conditional tests, flora tests, and regular tests
  const { conditionalGroups, floraTests, regularTests } = useMemo(() => {
    const childTests = microscopyTests.filter((test) => test.parentTestId != null);
    const floraCountTests = microscopyTests.filter((test) => test.isFloraCountTest === true);

    // Find tests that are actually parents (have children pointing to them)
    const parentTestIds = new Set(childTests.map(child => child.parentTestId));
    const parentTests = microscopyTests.filter((test) => parentTestIds.has(test.testId));

    // Group children by parent
    const groups = parentTests.map((parent) => {
      const children = childTests.filter((child) => child.parentTestId === parent.testId);
      return {
        parent: {
          ...parent,
          resultValue: microscopyResults[parent.testId] || "",
        },
        children: children.map((child) => ({
          ...child,
          resultValue: microscopyResults[child.testId] || "",
        })),
      };
    });

    // Regular tests are those that are neither parents nor children nor flora tests
    const regular = microscopyTests.filter(
      (test) =>
        !parentTestIds.has(test.testId) &&
        test.parentTestId == null &&
        !test.isFloraCountTest,
    );

    return {
      conditionalGroups: groups,
      floraTests: floraCountTests,
      regularTests: regular,
    };
  }, [microscopyTests, microscopyResults]);

  if (microscopyTests.length === 0) {
    return null;
  }

  return (
    <Section>
      <h3 style={{ marginBottom: "1rem" }}>
        <FormattedMessage id="bacteriology.section.microscopy" />
      </h3>

      {/* Flora Count Tests */}
      {floraTests.length > 0 && (
        <div style={{ marginBottom: "2rem" }}>
          {floraTests.map((test) => (
            <div key={test.testId} style={{ marginBottom: "2rem" }}>
              <FloraList
                accessionNumber={accessionNumber}
                testId={test.testId}
                floraCount={floraData[test.testId]?.count || 0}
                floraDetails={floraData[test.testId]?.details || []}
                onFloraCountChange={handleFloraCountChange}
                onFloraDetailsChange={(details) =>
                  handleFloraDetailsChange(test.testId, details)
                }
                disabled={disabled}
              />
            </div>
          ))}
        </div>
      )}

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
              <Column
                key={uniqueKey}
                lg={8}
                md={4}
                sm={4}
                style={{ marginBottom: "1.5rem" }}
              >
                <BacteriologyResultField
                  id={`micro_${test.testId}`}
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
                  value={microscopyResults[test.testId] || ""}
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

export default MicroscopySection;
