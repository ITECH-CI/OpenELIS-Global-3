import { Column, Grid, Section, Select, SelectItem } from "@carbon/react";
import { useMemo, useState } from "react";
import { FormattedMessage } from "react-intl";
import BacteriologyResultField from "../common/BacteriologyResultField";
import ConditionalTestGroup from "../ConditionalTestGroup";
import FloraList from "../FloraList";

// Tests for which the user may switch the unit of measure between mm³ and
// num/champ at result entry time are identified BY NAME, not by id: test ids
// are sequence-generated and differ between databases (a hardcoded id list
// silently attached the unit picker to the wrong tests in prod). These are the
// "Microscopie - Etat frais Quantitatif - Hématies/Leucocytes" tests (with or
// without a sample-type suffix like "Sécrétion vaginale").
const isUomSelectableTest = (testName) => {
  if (!testName) return false;
  const normalized = testName
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");
  return (
    normalized.includes("etat frais quantitatif") &&
    (normalized.includes("hematies") || normalized.includes("leucocytes"))
  );
};

const MicroscopySection = ({
  accessionNumber,
  testResults = [],
  microscopyResults = {},
  microscopyUoms = {},
  microscopyUomOptions = [],
  floraData = {},
  onChange,
  onUomChange,
  onFloraChange,
  disabled = false,
}) => {
  const handleFieldChange = (testId, value) => {
    onChange({ ...microscopyResults, [testId]: value });
  };

  // Local fallback if the parent doesn't yet handle UoM state (Phase 1).
  const [localUoms, setLocalUoms] = useState({});
  const effectiveUoms =
    Object.keys(microscopyUoms || {}).length > 0 ? microscopyUoms : localUoms;

  const handleUomChange = (testId, uomId) => {
    if (onUomChange) {
      onUomChange({ ...(microscopyUoms || {}), [testId]: uomId });
    } else {
      setLocalUoms((prev) => ({ ...prev, [testId]: uomId }));
    }
  };

  // IMPORTANT: both callbacks below MUST use the functional updater form because
  // FloraList fires onFloraCountChange and onFloraDetailsChange back-to-back when
  // the user changes the count, and React batches state updates within an event.
  // Without the functional form the second call would overwrite the first.
  const handleFloraCountChange = (testId, count) => {
    onFloraChange((prev) => ({
      ...(prev || {}),
      [testId]: {
        ...((prev || {})[testId] || {}),
        count: count,
      },
    }));
  };

  const handleFloraDetailsChange = (testId, details) => {
    onFloraChange((prev) => ({
      ...(prev || {}),
      [testId]: {
        ...((prev || {})[testId] || {}),
        details: details,
      },
    }));
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
    const childTests = microscopyTests.filter(
      (test) => test.parentTestId != null,
    );
    const floraCountTests = microscopyTests.filter(
      (test) => test.isFloraCountTest === true,
    );

    // Find tests that are actually parents (have children pointing to them).
    // A flora-count test is a parent too, but it owns its own rendering (FloraList)
    // so we must NOT render it again in a ConditionalTestGroup.
    const parentTestIds = new Set(
      childTests.map((child) => child.parentTestId),
    );
    const parentTests = microscopyTests.filter(
      (test) => parentTestIds.has(test.testId) && !test.isFloraCountTest,
    );

    // Group children by parent
    const groups = parentTests.map((parent) => {
      const children = childTests.filter(
        (child) => child.parentTestId === parent.testId,
      );
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
          {floraTests.map((test) => {
            const uniqueFloraKey = `${test.analysisId}-${test.testId}`;
            return (
              <div key={uniqueFloraKey} style={{ marginBottom: "2rem" }}>
                <FloraList
                  accessionNumber={accessionNumber}
                  testId={test.testId}
                  testName={cleanTestName(test.testName)}
                  floraCount={floraData[test.testId]?.count || 0}
                  floraDetails={floraData[test.testId]?.details || []}
                  onFloraCountChange={handleFloraCountChange}
                  onFloraDetailsChange={(details) =>
                    handleFloraDetailsChange(test.testId, details)
                  }
                  disabled={disabled}
                />
              </div>
            );
          })}
        </div>
      )}

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
              const uomSelectable =
                isUomSelectableTest(test.testName) &&
                microscopyUomOptions.length > 0;
              const selectedUomId =
                effectiveUoms[test.testId] != null
                  ? String(effectiveUoms[test.testId])
                  : "";
              const labelWithUom =
                !uomSelectable && test.unitsOfMeasure
                  ? `${cleanedName} (${test.unitsOfMeasure})`
                  : cleanedName;

              const field = (
                <BacteriologyResultField
                  id={`micro_${test.testId}`}
                  label={labelWithUom}
                  type={
                    isDictionary || isMultiSelect
                      ? "select"
                      : test.resultType === "N"
                        ? "number"
                        : "text"
                  }
                  value={microscopyResults[test.testId] ?? ""}
                  onChange={(value) => handleFieldChange(test.testId, value)}
                  options={test.dictionaryResults || []}
                  disabled={disabled}
                  required={false}
                />
              );

              return (
                <Column
                  key={uniqueKey}
                  lg={8}
                  md={4}
                  sm={4}
                  style={{ marginBottom: "1.5rem" }}
                >
                  {uomSelectable ? (
                    <div
                      style={{
                        display: "flex",
                        gap: "0.5rem",
                        alignItems: "flex-end",
                      }}
                    >
                      <div style={{ flex: 2 }}>{field}</div>
                      <div style={{ flex: 1, minWidth: "9rem" }}>
                        <Select
                          id={`micro_uom_${test.testId}`}
                          labelText="Unité"
                          value={selectedUomId}
                          onChange={(e) =>
                            handleUomChange(test.testId, e.target.value)
                          }
                          disabled={disabled}
                        >
                          <SelectItem value="" text="Choisir..." />
                          {microscopyUomOptions.map((opt) => (
                            <SelectItem
                              key={opt.id}
                              value={String(opt.id)}
                              text={opt.label}
                            />
                          ))}
                        </Select>
                      </div>
                    </div>
                  ) : (
                    field
                  )}
                </Column>
              );
            })}
          </Grid>
        </div>
      )}
    </Section>
  );
};

export default MicroscopySection;
