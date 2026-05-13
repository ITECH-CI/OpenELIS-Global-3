import { Reset, Save } from "@carbon/icons-react";
import { Button, ButtonSet, Form, InlineLoading, Stack } from "@carbon/react";
import { useContext, useEffect, useState } from "react";
import { FormattedMessage } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import { API_ENDPOINTS } from "./BacteriologyConstants";
import CultureSection from "./sections/CultureSection";
import MacroscopySection from "./sections/MacroscopySection";
import MicroscopySection from "./sections/MicroscopySection";
import ResultHeader from "./sections/ResultHeader";

const BacteriologyResultEntry = ({
  analysisId,
  accessionNumber,
  testResults = [],
  sysUserId,
  onSave,
  disabled = false,
}) => {
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [macroscopyResults, setMacroscopyResults] = useState({});
  const [microscopyResults, setMicroscopyResults] = useState({});
  const [floraData, setFloraData] = useState({});
  // Map of testId -> { cultureResult, organisms }
  const [cultures, setCultures] = useState({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [initialData, setInitialData] = useState(null);

  useEffect(() => {
    if (analysisId) {
      loadBacteriologyResults();
    }
  }, [analysisId]);

  const loadBacteriologyResults = () => {
    setLoading(true);
    // includeFinalized=true so already-validated tests remain visible and editable on
    // the result-entry / modify-order pages (lab-number / accession search). Without
    // this flag the backend strips Finalized analyses and the fields appear empty.
    getFromOpenElisServer(
      `${API_ENDPOINTS.GET_RESULTS}/${analysisId}?includeFinalized=true`,
      (data) => {
        // Load macroscopy results from Map (testId -> value)
        if (data && data.macroscopyResultsMap) {
          setMacroscopyResults(data.macroscopyResultsMap);
        }

        // Load microscopy results from Map (testId -> value)
        if (data && data.microscopyResultsMap) {
          setMicroscopyResults(data.microscopyResultsMap);
        }

        // Load culture results and organisms
        const loadedCultures = {};

        if (data && data.cultureResultsMap) {
          // Reconstruct cultures map from cultureResultsMap (testId -> value)
          Object.entries(data.cultureResultsMap).forEach(
            ([testId, cultureResult]) => {
              loadedCultures[testId] = {
                cultureResult: cultureResult,
                organisms: [],
              };
            },
          );
        }

        // Add organisms to appropriate cultures using testId from organismGroup
        if (data && data.organisms && Array.isArray(data.organisms)) {
          // Group organisms by their testId (from organismGroup)
          const organismsByCulture = {}; // testId -> organisms array

          data.organisms.forEach((orgData) => {
            const organism = orgData?.organism;
            const organismGroup = orgData?.organismGroup;

            // Skip if organism is null or invalid
            if (!organism) {
              return;
            }

            // Ensure organism has required fields - map antibiograms if they exist
            const normalizedOrganism = {
              id: organism.id || null,
              organismGroupId: organismGroup?.id || null,
              organismNumber: organism.organismNumber || null, // Keep original, will be set later
              organismType: organism.organismType || "BACTERIA",
              organismNameDictId: organism.organismNameDictId || null,
              organismNameText: organism.organismNameText || "",
              gramType: organism.gramType || "",
              groupingMode: organism.groupingMode || "",
              capsulePresence: Boolean(organism.capsulePresence),
              otherCharacteristics: organism.otherCharacteristics || "",
              antibiograms: Array.isArray(orgData.antibiograms)
                ? orgData.antibiograms.map((ab) => ({
                    id: ab.id || null,
                    antibioticDictId: ab.antibioticDictId || null,
                    result: ab.result || "",
                    diameterMm: ab.diameterMm || null,
                    micValue: ab.micValue || "",
                    interpretationComment: ab.interpretationComment || "",
                  }))
                : [],
            };

            // Use testId from organismGroup to assign to correct culture
            const testId = organismGroup?.testId;
            let assignedCultureKey = testId ? String(testId) : null;

            // If no testId, try backward compatibility - assign to first culture
            if (!assignedCultureKey) {
              const cultureKeys = Object.keys(loadedCultures);
              if (cultureKeys.length > 0) {
                assignedCultureKey = cultureKeys[0];
              } else {
                assignedCultureKey = "default";
              }
            }

            // Group organisms by their assigned culture
            if (!organismsByCulture[assignedCultureKey]) {
              organismsByCulture[assignedCultureKey] = [];
            }
            organismsByCulture[assignedCultureKey].push(normalizedOrganism);
          });

          // Now assign organisms to cultures with proper numbering within each culture
          Object.entries(organismsByCulture).forEach(
            ([cultureKey, organisms]) => {
              if (cultureKey === "default") {
                loadedCultures["default"] = {
                  cultureResult: "positive",
                  organisms: organisms.map((org, index) => ({
                    ...org,
                    organismNumber: org.organismNumber || index + 1,
                  })),
                };
              } else {
                if (!loadedCultures[cultureKey]) {
                  // Culture result not saved yet, but we have organisms - create entry
                  loadedCultures[cultureKey] = {
                    cultureResult: "",
                    organisms: [],
                  };
                }
                // Renumber organisms within this culture (1, 2, 3)
                loadedCultures[cultureKey].organisms = organisms.map(
                  (org, index) => ({
                    ...org,
                    organismNumber: org.organismNumber || index + 1,
                  }),
                );
              }
            },
          );
        }

        setCultures(loadedCultures);
        setInitialData(data);
        setLoading(false);

        // Load flora data asynchronously (count + per-flora details for "Nombre de
        // flore" tests). The form is already usable; floraData is hydrated when the
        // response arrives.
        getFromOpenElisServer(
          `${API_ENDPOINTS.FLORA_BY_ANALYSIS}/${analysisId}`,
          (floraList) => {
            if (Array.isArray(floraList)) {
              const loadedFlora = {};
              floraList.forEach((entry) => {
                if (entry && entry.floraCountTestId != null) {
                  loadedFlora[String(entry.floraCountTestId)] = {
                    count: entry.count || 0,
                    details: Array.isArray(entry.details) ? entry.details : [],
                  };
                }
              });
              setFloraData(loadedFlora);
            }
          },
        );
      },
      () => {
        setLoading(false);
      },
    );
  };

  const handleSave = () => {
    setSaving(true);

    // Filter out "default" culture key (used for backward compatibility in loading)
    const validCultures = {};
    Object.entries(cultures).forEach(([testId, culture]) => {
      // Only include cultures with numeric testIds (exclude "default")
      if (testId !== "default" && !isNaN(parseInt(testId))) {
        validCultures[testId] = culture;
      }
    });

    // Collect all organisms from all cultures
    // Keep organism numbers as they are (1-3 per culture), don't renumber globally
    const allOrganisms = [];
    Object.values(validCultures).forEach((culture) => {
      if (culture.organisms && culture.organisms.length > 0) {
        // Ensure each organism in this culture has a valid organismNumber (1-3)
        culture.organisms.forEach((organism, cultureIndex) => {
          allOrganisms.push({
            ...organism,
            // Keep original organismNumber if valid, otherwise use position in culture (1-3)
            organismNumber: organism.organismNumber || cultureIndex + 1,
            // Filter out antibiograms that don't have an antibiotic selected
            antibiograms: (organism.antibiograms || []).filter(
              (ab) => ab.antibioticDictId && ab.antibioticDictId !== "",
            ),
          });
        });
      }
    });

    const formData = {
      analysisId: parseInt(analysisId),
      sysUserId: parseInt(sysUserId),
      macroscopyResults,
      microscopyResults,
      floraData,
      // For backward compatibility, use first culture's result or empty
      cultureResult: Object.values(validCultures)[0]?.cultureResult || "",
      organisms: allOrganisms,
      // Add cultures data for future use (only valid numeric testIds)
      cultures: validCultures,
    };

    // Validate JSON serializability before sending
    try {
      JSON.stringify(formData);
    } catch (err) {
      console.error("[BacteriologyResultEntry] Data serialization error:", err);
      setSaving(false);
      addNotification({
        kind: "error",
        title: "Error",
        message: "Data serialization error: " + err.message,
      });
      setNotificationVisible(true);
      return;
    }

    postToOpenElisServerJsonResponse(
      API_ENDPOINTS.SAVE_RESULTS,
      JSON.stringify(formData),
      () => {
        // After the main save succeeds, persist flora data per flora-count test.
        // The flora REST endpoint is separate (BacteriologyFloraRestController) so we
        // fire one POST per (analysisId, floraCountTestId) entry collected by FloraList.
        // IMPORTANT: target the analysisId of THIS flora-count test, not the page's
        // primary analysisId — otherwise the row is stored against an unrelated
        // analysis (e.g. Macroscopie - Turbidité) and never round-trips back.
        const floraEntries = Object.entries(floraData || {});
        floraEntries.forEach(([testId, data]) => {
          if (!testId || isNaN(parseInt(testId))) {
            return;
          }
          const floraTest = testResults.find(
            (t) => String(t.testId) === String(testId) && t.analysisId,
          );
          const floraAnalysisId = floraTest?.analysisId;
          if (!floraAnalysisId) {
            console.warn(
              "[BacteriologyResultEntry] No matching analysisId for flora test",
              testId,
              "- skipping save",
            );
            return;
          }
          const payload = {
            count: data?.count != null ? parseInt(data.count) : 0,
            details: (data?.details || []).map((d) => ({
              floraNumber: d.floraNumber,
              gramTypeDictId:
                d.gramTypeDictId && d.gramTypeDictId !== ""
                  ? parseInt(d.gramTypeDictId)
                  : null,
              groupingModeDictId:
                d.groupingModeDictId && d.groupingModeDictId !== ""
                  ? parseInt(d.groupingModeDictId)
                  : null,
              otherCharacteristicDictId:
                d.otherCharacteristicDictId &&
                d.otherCharacteristicDictId !== ""
                  ? parseInt(d.otherCharacteristicDictId)
                  : null,
            })),
          };
          postToOpenElisServerJsonResponse(
            `${API_ENDPOINTS.FLORA_BY_ANALYSIS}/${floraAnalysisId}/test/${testId}`,
            JSON.stringify(payload),
            () => {},
            (err) => {
              console.error(
                "[BacteriologyResultEntry] Flora save failed for test",
                testId,
                err,
              );
            },
          );
        });

        setSaving(false);
        addNotification({
          kind: "success",
          title: "Success",
          message: "Bacteriology results saved successfully",
        });
        setNotificationVisible(true);
        // Reload data to get updated values and prevent duplicates
        loadBacteriologyResults();
        if (onSave) {
          onSave(formData);
        }
      },
      (error) => {
        console.error("[BacteriologyResultEntry] Save failed! Error:", error);
        setSaving(false);
        addNotification({
          kind: "error",
          title: "Error",
          message: "Failed to save bacteriology results: " + error,
        });
        setNotificationVisible(true);
      },
    );
  };

  const handleReset = () => {
    setMacroscopyResults({});
    setMicroscopyResults({});
    setFloraData({});
    setCultures({});
    if (initialData) {
      loadBacteriologyResults();
    }
  };

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <InlineLoading description="Loading bacteriology results..." />
      </div>
    );
  }

  return (
    <Form className="bacteriology-result-entry">
      <Stack gap={6}>
        <ResultHeader
          testResult={testResults[0]}
          totalTests={testResults.length}
        />

        <MacroscopySection
          accessionNumber={accessionNumber}
          testResults={testResults}
          macroscopyResults={macroscopyResults}
          onChange={setMacroscopyResults}
          disabled={disabled || saving}
        />

        <MicroscopySection
          accessionNumber={accessionNumber}
          testResults={testResults}
          microscopyResults={microscopyResults}
          floraData={floraData}
          onChange={setMicroscopyResults}
          onFloraChange={setFloraData}
          disabled={disabled || saving}
        />

        <CultureSection
          accessionNumber={accessionNumber}
          testResults={testResults}
          cultures={cultures}
          onCulturesChange={setCultures}
          disabled={disabled || saving}
        />

        <ButtonSet>
          <Button
            kind="secondary"
            renderIcon={Reset}
            onClick={handleReset}
            disabled={disabled || saving}
          >
            <FormattedMessage id="label.button.reset" />
          </Button>
          <Button
            kind="primary"
            renderIcon={Save}
            onClick={handleSave}
            disabled={disabled || saving}
          >
            {saving ? (
              <InlineLoading description="Saving..." />
            ) : (
              <FormattedMessage id="label.button.save" />
            )}
          </Button>
        </ButtonSet>
      </Stack>
    </Form>
  );
};

export default BacteriologyResultEntry;
