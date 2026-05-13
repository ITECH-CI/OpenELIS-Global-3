import { Copy, Reset, Save } from "@carbon/icons-react";
import {
  Button,
  ButtonSet,
  Checkbox,
  Column,
  DataTable,
  Grid,
  InlineLoading,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TextArea,
} from "@carbon/react";
import { useContext, useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";
import {
  convertAlphaNumLabNumForDisplay,
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import { API_ENDPOINTS } from "./BacteriologyConstants";

const BacteriologyValidation = ({
  analysisId,
  accessionNumber,
  sampleId,
  testName,
  sampleType,
  onSave,
  disabled = false,
}) => {
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);
  const intl = useIntl();

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  // Validation state - track what's checked for validation
  const [macroscopyValidation, setMacroscopyValidation] = useState({});
  const [microscopyValidation, setMicroscopyValidation] = useState({});
  const [cultureValidation, setCultureValidation] = useState({});
  const [organismValidation, setOrganismValidation] = useState({});

  // Rejection flags
  const [macroscopyRejection, setMacroscopyRejection] = useState({});
  const [microscopyRejection, setMicroscopyRejection] = useState({});
  const [cultureRejection, setCultureRejection] = useState({});
  const [organismRejection, setOrganismRejection] = useState({});

  // Notes/interpretation state
  const [sampleInterpretation, setSampleInterpretation] = useState("");

  useEffect(() => {
    if (analysisId) {
      loadBacteriologyResults();
    }
  }, [analysisId]);

  const loadBacteriologyResults = () => {
    setLoading(true);
    getFromOpenElisServer(
      `${API_ENDPOINTS.GET_RESULTS}/${analysisId}`,
      (responseData) => {
        setData(responseData);
        if (responseData && responseData.sampleInterpretation != null) {
          setSampleInterpretation(responseData.sampleInterpretation);
        } else {
          setSampleInterpretation("");
        }
        setLoading(false);
      },
      () => {
        setLoading(false);
      },
    );
  };

  // Helper function to format test name with sample type
  const formatTestName = (testName, includeSampleType = true) => {
    if (!testName) return "";
    if (!includeSampleType || !data?.sampleTypeName) return testName;
    return `${testName} (${data.sampleTypeName})`;
  };

  const handleValidate = () => {
    setSaving(true);

    // Collect all validated items (no organisms - they are validated with their culture)
    const validatedItems = {
      macroscopy: Object.keys(macroscopyValidation).filter(
        (k) => macroscopyValidation[k],
      ),
      microscopy: Object.keys(microscopyValidation).filter(
        (k) => microscopyValidation[k],
      ),
      culture: Object.keys(cultureValidation).filter(
        (k) => cultureValidation[k],
      ),
    };

    // Collect rejected items (no organisms - they are rejected with their culture)
    const rejectedItems = {
      macroscopy: Object.keys(macroscopyRejection).filter(
        (k) => macroscopyRejection[k],
      ),
      microscopy: Object.keys(microscopyRejection).filter(
        (k) => microscopyRejection[k],
      ),
      culture: Object.keys(cultureRejection).filter((k) => cultureRejection[k]),
    };

    const formData = {
      analysisId: parseInt(analysisId),
      validated: validatedItems,
      rejected: rejectedItems,
      sampleInterpretation: sampleInterpretation || "",
    };

    postToOpenElisServerJsonResponse(
      API_ENDPOINTS.VALIDATE_RESULTS,
      JSON.stringify(formData),
      (response) => {
        setSaving(false);
        addNotification({
          kind: "success",
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({ id: "validation.save.success" }),
        });
        setNotificationVisible(true);

        // Clear all checkboxes after successful validation
        setMacroscopyValidation({});
        setMicroscopyValidation({});
        setCultureValidation({});
        setOrganismValidation({});
        setMacroscopyRejection({});
        setMicroscopyRejection({});
        setCultureRejection({});
        setOrganismRejection({});

        // Reload data
        loadBacteriologyResults();

        if (onSave) {
          onSave(formData);
        }
      },
      (error) => {
        console.error("[BacteriologyValidation] Validation failed:", error);
        setSaving(false);
        addNotification({
          kind: "error",
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            intl.formatMessage({ id: "validation.save.error" }) + ": " + error,
        });
        setNotificationVisible(true);
      },
    );
  };

  const handleReset = () => {
    setMacroscopyValidation({});
    setMicroscopyValidation({});
    setCultureValidation({});
    setOrganismValidation({});
    setMacroscopyRejection({});
    setMicroscopyRejection({});
    setCultureRejection({});
    setOrganismRejection({});
  };

  const handleAcceptAll = () => {
    const newMacroValidation = {};
    const newMicroValidation = {};
    const newCultureValidation = {};

    // Check all macroscopy
    if (data?.macroscopyResults && Array.isArray(data.macroscopyResults)) {
      data.macroscopyResults.forEach((testResult) => {
        newMacroValidation[testResult.testId] = true;
      });
    }

    // Check all microscopy
    if (data?.microscopyResults && Array.isArray(data.microscopyResults)) {
      data.microscopyResults.forEach((testResult) => {
        newMicroValidation[testResult.testId] = true;
      });
    }

    // Check all cultures (organisms are validated with their culture)
    if (data?.cultureResults && Array.isArray(data.cultureResults)) {
      data.cultureResults.forEach((testResult) => {
        newCultureValidation[testResult.testId] = true;
      });
    }

    setMacroscopyValidation(newMacroValidation);
    setMicroscopyValidation(newMicroValidation);
    setCultureValidation(newCultureValidation);

    // Clear rejections
    setMacroscopyRejection({});
    setMicroscopyRejection({});
    setCultureRejection({});
  };

  const handleRejectAll = () => {
    const newMacroRejection = {};
    const newMicroRejection = {};
    const newCultureRejection = {};

    // Check all macroscopy rejections
    if (data?.macroscopyResults && Array.isArray(data.macroscopyResults)) {
      data.macroscopyResults.forEach((testResult) => {
        newMacroRejection[testResult.testId] = true;
      });
    }

    // Check all microscopy rejections
    if (data?.microscopyResults && Array.isArray(data.microscopyResults)) {
      data.microscopyResults.forEach((testResult) => {
        newMicroRejection[testResult.testId] = true;
      });
    }

    // Check all culture rejections (organisms are rejected with their culture)
    if (data?.cultureResults && Array.isArray(data.cultureResults)) {
      data.cultureResults.forEach((testResult) => {
        newCultureRejection[testResult.testId] = true;
      });
    }

    setMacroscopyRejection(newMacroRejection);
    setMicroscopyRejection(newMicroRejection);
    setCultureRejection(newCultureRejection);

    // Clear validations
    setMacroscopyValidation({});
    setMicroscopyValidation({});
    setCultureValidation({});
  };

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <InlineLoading
          description={intl.formatMessage({ id: "loading.message" })}
        />
      </div>
    );
  }

  if (!data) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <FormattedMessage id="no.data.available" />
      </div>
    );
  }

  return (
    <div
      className="bacteriology-validation"
      style={{
        border: "2px solid #0f62fe",
        borderRadius: "4px",
        padding: "1.5rem",
        marginBottom: "2rem",
        backgroundColor: "#ffffff",
        boxShadow: "0 2px 4px rgba(0,0,0,0.1)",
      }}
    >
      <Stack gap={6}>
        {/* Header with Sample Information */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            paddingBottom: "1rem",
            borderBottom: "2px solid #e0e0e0",
          }}
        >
          <div style={{ display: "flex", alignItems: "center" }}>
            <Button
              onClick={async () => {
                if ("clipboard" in navigator) {
                  return await navigator.clipboard.writeText(accessionNumber);
                } else {
                  return document.execCommand("copy", true, accessionNumber);
                }
              }}
              kind="ghost"
              iconDescription={intl.formatMessage({
                id: "instructions.copy.labnum",
              })}
              hasIconOnly
              renderIcon={Copy}
            />
            <div style={{ marginLeft: "0.5rem" }}>
              <h4 style={{ margin: 0 }}>
                <FormattedMessage id="column.name.sampleInfo" />:{" "}
                {configurationProperties.AccessionFormat === "ALPHANUM"
                  ? convertAlphaNumLabNumForDisplay(accessionNumber)
                  : accessionNumber}
              </h4>
            </div>
            {data?.sampleTypeName && (
              <div
                style={{
                  marginLeft: "1.5rem",
                  color: "#525252",
                  fontSize: "0.875rem",
                }}
              >
                Type: {data.sampleTypeName}
              </div>
            )}
          </div>
          <div
            style={{
              fontSize: "0.875rem",
              color: "#525252",
              fontWeight: "500",
            }}
          >
            {(() => {
              const totalTests =
                (data.macroscopyResults?.length || 0) +
                (data.microscopyResults?.length || 0) +
                (data.cultureResults?.length || 0) +
                (data.organisms?.length || 0);
              return `${totalTests} test${totalTests > 1 ? "s" : ""}`;
            })()}
          </div>
        </div>

        {/* Bulk actions */}
        <Grid className="gridBoundary">
          <Column lg={8} md={4} sm={2}>
            <Checkbox
              id={`accept-all-${accessionNumber}`}
              labelText={intl.formatMessage({ id: "validation.accept.all" })}
              onChange={(e) => {
                if (e.target.checked) {
                  handleAcceptAll();
                }
              }}
              disabled={disabled || saving}
            />
          </Column>
          <Column lg={8} md={4} sm={2}>
            <Checkbox
              id={`reject-all-${accessionNumber}`}
              labelText={intl.formatMessage({ id: "validation.reject.all" })}
              onChange={(e) => {
                if (e.target.checked) {
                  handleRejectAll();
                }
              }}
              disabled={disabled || saving}
            />
          </Column>
        </Grid>

        {/* Macroscopy Section */}
        {data.macroscopyResults && data.macroscopyResults.length > 0 && (
          <div>
            <h4>
              <FormattedMessage id="bacteriology.macroscopy.title" />
            </h4>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      <FormattedMessage id="column.name.testName" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="column.name.result" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="column.name.save" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="column.name.retest" />
                    </TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {data.macroscopyResults.map((testResult) => (
                    <TableRow key={`macro-${testResult.testId}`}>
                      <TableCell>
                        {formatTestName(testResult.testName)}
                        {testResult.testDescription &&
                          testResult.testDescription !==
                            testResult.testName && (
                            <div
                              style={{ fontSize: "0.875rem", color: "#525252" }}
                            >
                              {testResult.testDescription}
                            </div>
                          )}
                      </TableCell>
                      <TableCell>
                        <span style={{ whiteSpace: "pre-line" }}>
                          {testResult.displayValue}
                          {testResult.unitOfMeasure &&
                            ` ${testResult.unitOfMeasure}`}
                        </span>
                      </TableCell>
                      <TableCell>
                        <Checkbox
                          id={`macro-validate-${testResult.testId}`}
                          labelText=""
                          checked={
                            macroscopyValidation[testResult.testId] || false
                          }
                          onChange={(e) => {
                            setMacroscopyValidation({
                              ...macroscopyValidation,
                              [testResult.testId]: e.target.checked,
                            });
                            if (e.target.checked) {
                              setMacroscopyRejection({
                                ...macroscopyRejection,
                                [testResult.testId]: false,
                              });
                            }
                          }}
                          disabled={disabled || saving}
                        />
                      </TableCell>
                      <TableCell>
                        <Checkbox
                          id={`macro-reject-${testResult.testId}`}
                          labelText=""
                          checked={
                            macroscopyRejection[testResult.testId] || false
                          }
                          onChange={(e) => {
                            setMacroscopyRejection({
                              ...macroscopyRejection,
                              [testResult.testId]: e.target.checked,
                            });
                            if (e.target.checked) {
                              setMacroscopyValidation({
                                ...macroscopyValidation,
                                [testResult.testId]: false,
                              });
                            }
                          }}
                          disabled={disabled || saving}
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </div>
        )}

        {/* Microscopy Section */}
        {data.microscopyResults && data.microscopyResults.length > 0 && (
          <div>
            <h4>
              <FormattedMessage id="bacteriology.microscopy.title" />
            </h4>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      <FormattedMessage id="column.name.testName" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="column.name.result" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="column.name.save" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="column.name.retest" />
                    </TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {data.microscopyResults.map((testResult) => (
                    <TableRow key={`micro-${testResult.testId}`}>
                      <TableCell>
                        {formatTestName(testResult.testName)}
                        {testResult.testDescription &&
                          testResult.testDescription !==
                            testResult.testName && (
                            <div
                              style={{ fontSize: "0.875rem", color: "#525252" }}
                            >
                              {testResult.testDescription}
                            </div>
                          )}
                      </TableCell>
                      <TableCell>
                        <span style={{ whiteSpace: "pre-line" }}>
                          {testResult.displayValue}
                          {testResult.unitOfMeasure &&
                            ` ${testResult.unitOfMeasure}`}
                        </span>
                      </TableCell>
                      <TableCell>
                        <Checkbox
                          id={`micro-validate-${testResult.testId}`}
                          labelText=""
                          checked={
                            microscopyValidation[testResult.testId] || false
                          }
                          onChange={(e) => {
                            setMicroscopyValidation({
                              ...microscopyValidation,
                              [testResult.testId]: e.target.checked,
                            });
                            if (e.target.checked) {
                              setMicroscopyRejection({
                                ...microscopyRejection,
                                [testResult.testId]: false,
                              });
                            }
                          }}
                          disabled={disabled || saving}
                        />
                      </TableCell>
                      <TableCell>
                        <Checkbox
                          id={`micro-reject-${testResult.testId}`}
                          labelText=""
                          checked={
                            microscopyRejection[testResult.testId] || false
                          }
                          onChange={(e) => {
                            setMicroscopyRejection({
                              ...microscopyRejection,
                              [testResult.testId]: e.target.checked,
                            });
                            if (e.target.checked) {
                              setMicroscopyValidation({
                                ...microscopyValidation,
                                [testResult.testId]: false,
                              });
                            }
                          }}
                          disabled={disabled || saving}
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </div>
        )}

        {/* Culture Section with Organisms */}
        {data.cultureResults && data.cultureResults.length > 0 && (
          <div>
            <h4>
              <FormattedMessage id="bacteriology.culture.title" />
            </h4>
            {data.cultureResults.map((cultureResult) => {
              // Find organisms for this culture - convert both to string for comparison
              const cultureTestIdStr = String(cultureResult.testId);
              const cultureOrganisms =
                data.organisms?.filter((orgData) => {
                  const organismTestIdStr = String(
                    orgData?.organismGroup?.testId,
                  );
                  const matches = organismTestIdStr === cultureTestIdStr;
                  return matches;
                }) || [];

              return (
                <div
                  key={`culture-${cultureResult.testId}`}
                  style={{
                    marginBottom: "2rem",
                    border: "1px solid #e0e0e0",
                    borderRadius: "4px",
                  }}
                >
                  {/* Culture Test Result */}
                  <TableContainer>
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableHeader>
                            <FormattedMessage id="column.name.testName" />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage id="column.name.result" />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage id="column.name.save" />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage id="column.name.retest" />
                          </TableHeader>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        <TableRow>
                          <TableCell>
                            <strong>
                              {formatTestName(cultureResult.testName)}
                            </strong>
                            {cultureResult.testDescription &&
                              cultureResult.testDescription !==
                                cultureResult.testName && (
                                <div
                                  style={{
                                    fontSize: "0.875rem",
                                    color: "#525252",
                                  }}
                                >
                                  {cultureResult.testDescription}
                                </div>
                              )}
                          </TableCell>
                          <TableCell>
                            <strong>{cultureResult.displayValue}</strong>
                            {cultureResult.unitOfMeasure &&
                              ` ${cultureResult.unitOfMeasure}`}
                          </TableCell>
                          <TableCell>
                            <Checkbox
                              id={`culture-validate-${cultureResult.testId}`}
                              labelText=""
                              checked={
                                cultureValidation[cultureResult.testId] || false
                              }
                              onChange={(e) => {
                                setCultureValidation({
                                  ...cultureValidation,
                                  [cultureResult.testId]: e.target.checked,
                                });
                                if (e.target.checked) {
                                  setCultureRejection({
                                    ...cultureRejection,
                                    [cultureResult.testId]: false,
                                  });
                                }
                              }}
                              disabled={disabled || saving}
                            />
                          </TableCell>
                          <TableCell>
                            <Checkbox
                              id={`culture-reject-${cultureResult.testId}`}
                              labelText=""
                              checked={
                                cultureRejection[cultureResult.testId] || false
                              }
                              onChange={(e) => {
                                setCultureRejection({
                                  ...cultureRejection,
                                  [cultureResult.testId]: e.target.checked,
                                });
                                if (e.target.checked) {
                                  setCultureValidation({
                                    ...cultureValidation,
                                    [cultureResult.testId]: false,
                                  });
                                }
                              }}
                              disabled={disabled || saving}
                            />
                          </TableCell>
                        </TableRow>
                      </TableBody>
                    </Table>
                  </TableContainer>

                  {/* Organisms for this culture */}
                  {cultureOrganisms.length > 0 && (
                    <div
                      style={{ padding: "1rem", backgroundColor: "#f9f9f9" }}
                    >
                      <h5 style={{ marginBottom: "1rem", fontSize: "0.95rem" }}>
                        <FormattedMessage id="bacteriology.organisms.title" /> (
                        {cultureOrganisms.length})
                      </h5>
                      {cultureOrganisms.map((orgData, orgIndex) => {
                        const organism = orgData?.organism;
                        const organismGroup = orgData?.organismGroup;
                        const antibiograms = orgData?.antibiograms || [];
                        const orgGroupId = organismGroup?.id;

                        if (!organism || !orgGroupId) {
                          return null;
                        }

                        return (
                          <div
                            key={`organism-${orgGroupId}`}
                            style={{
                              marginBottom:
                                orgIndex < cultureOrganisms.length - 1
                                  ? "1.5rem"
                                  : "0",
                              padding: "1rem",
                              backgroundColor: "#ffffff",
                              borderRadius: "4px",
                              border: "1px solid #e0e0e0",
                            }}
                          >
                            {/* Organism Info - Compact Display */}
                            <div style={{ marginBottom: "1rem" }}>
                              <div
                                style={{
                                  display: "grid",
                                  gridTemplateColumns: "auto 1fr",
                                  gap: "0.5rem 1rem",
                                  fontSize: "0.875rem",
                                }}
                              >
                                <strong>
                                  Organisme #{organism.organismNumber}:
                                </strong>
                                <span>
                                  {organism.resolvedOrganismName ||
                                    organism.organismNameText ||
                                    (organism.organismNameDictId
                                      ? `ID: ${organism.organismNameDictId}`
                                      : "N/A")}
                                </span>

                                <strong>Type Gram:</strong>
                                <span>{organism.gramType || "N/A"}</span>

                                <strong>Mode de regroupement:</strong>
                                <span>{organism.groupingMode || "N/A"}</span>

                                {organism.capsulePresence && (
                                  <>
                                    <strong>Présence de capsule:</strong>
                                    <span>Oui</span>
                                  </>
                                )}

                                {organism.otherCharacteristics &&
                                  organism.otherCharacteristics.trim() && (
                                    <>
                                      <strong>Autres caractéristiques:</strong>
                                      <span>
                                        {organism.otherCharacteristics}
                                      </span>
                                    </>
                                  )}
                              </div>
                            </div>

                            {/* Antibiogram Results */}
                            {antibiograms.length > 0 && (
                              <div>
                                <h6
                                  style={{
                                    fontSize: "0.85rem",
                                    marginBottom: "0.5rem",
                                    color: "#525252",
                                  }}
                                >
                                  Antibiogramme ({antibiograms.length}{" "}
                                  antibiotiques)
                                </h6>
                                <TableContainer>
                                  <Table size="sm">
                                    <TableHead>
                                      <TableRow>
                                        <TableHeader>Antibiotique</TableHeader>
                                        <TableHeader>Résultat</TableHeader>
                                        <TableHeader>CMI</TableHeader>
                                        <TableHeader>Diamètre (mm)</TableHeader>
                                      </TableRow>
                                    </TableHead>
                                    <TableBody>
                                      {antibiograms.map(
                                        (antibiogram, abIndex) => {
                                          return (
                                            <TableRow
                                              key={`antibiogram-${orgGroupId}-${abIndex}`}
                                            >
                                              <TableCell>
                                                {antibiogram.antibioticNameText ||
                                                  (antibiogram.antibioticDictId
                                                    ? `ID: ${antibiogram.antibioticDictId}`
                                                    : "N/A")}
                                              </TableCell>
                                              <TableCell>
                                                <span
                                                  style={{
                                                    padding: "0.25rem 0.5rem",
                                                    borderRadius: "4px",
                                                    fontWeight: "bold",
                                                    fontSize: "0.8rem",
                                                    backgroundColor:
                                                      antibiogram.interpretationText ===
                                                        "Sensible" ||
                                                      antibiogram.interpretationText ===
                                                        "S"
                                                        ? "#d4edda"
                                                        : antibiogram.interpretationText ===
                                                              "Résistant" ||
                                                            antibiogram.interpretationText ===
                                                              "R"
                                                          ? "#f8d7da"
                                                          : antibiogram.interpretationText ===
                                                                "Intermédiaire" ||
                                                              antibiogram.interpretationText ===
                                                                "I"
                                                            ? "#fff3cd"
                                                            : "#e2e3e5",
                                                    color:
                                                      antibiogram.interpretationText ===
                                                        "Sensible" ||
                                                      antibiogram.interpretationText ===
                                                        "S"
                                                        ? "#155724"
                                                        : antibiogram.interpretationText ===
                                                              "Résistant" ||
                                                            antibiogram.interpretationText ===
                                                              "R"
                                                          ? "#721c24"
                                                          : antibiogram.interpretationText ===
                                                                "Intermédiaire" ||
                                                              antibiogram.interpretationText ===
                                                                "I"
                                                            ? "#856404"
                                                            : "#383838",
                                                  }}
                                                >
                                                  {antibiogram.interpretationText ||
                                                    antibiogram.result ||
                                                    "N/A"}
                                                </span>
                                              </TableCell>
                                              <TableCell>
                                                {antibiogram.micValue || "-"}
                                              </TableCell>
                                              <TableCell>
                                                {antibiogram.diameterMm || "-"}
                                              </TableCell>
                                            </TableRow>
                                          );
                                        },
                                      )}
                                    </TableBody>
                                  </Table>
                                </TableContainer>
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {/* Orphaned Organisms Section - organisms without matching culture (fallback) */}
        {data.organisms &&
          data.organisms.length > 0 &&
          (() => {
            // Find organisms that weren't matched to any culture - use string comparison
            const matchedOrganismIds = new Set();
            data.cultureResults?.forEach((cultureResult) => {
              const cultureTestIdStr = String(cultureResult.testId);
              data.organisms?.forEach((orgData) => {
                const organismTestIdStr = String(
                  orgData?.organismGroup?.testId,
                );
                if (organismTestIdStr === cultureTestIdStr) {
                  matchedOrganismIds.add(orgData?.organismGroup?.id);
                }
              });
            });

            const orphanedOrganisms = data.organisms.filter(
              (orgData) => !matchedOrganismIds.has(orgData?.organismGroup?.id),
            );

            if (orphanedOrganisms.length === 0) {
              return null;
            }

            return (
              <div>
                <h4 style={{ color: "#d32f2f" }}>
                  Organismes identifiés (non associés à une culture)
                </h4>
                <div
                  style={{
                    padding: "1rem",
                    backgroundColor: "#fff3e0",
                    borderRadius: "4px",
                    border: "1px solid #ff9800",
                  }}
                >
                  <p
                    style={{
                      marginBottom: "1rem",
                      fontSize: "0.875rem",
                      color: "#e65100",
                    }}
                  >
                    Les organismes suivants n'ont pas pu être associés à une
                    culture. Cela peut indiquer un problème de données.
                  </p>
                  {orphanedOrganisms.map((orgData, index) => {
                    const organism = orgData?.organism;
                    const organismGroup = orgData?.organismGroup;
                    const antibiograms = orgData?.antibiograms || [];
                    const orgGroupId = organismGroup?.id;

                    if (!organism || !orgGroupId) {
                      return null;
                    }

                    return (
                      <div
                        key={`orphan-${orgGroupId}`}
                        style={{
                          marginBottom:
                            index < orphanedOrganisms.length - 1
                              ? "1.5rem"
                              : "0",
                          padding: "1rem",
                          backgroundColor: "#ffffff",
                          borderRadius: "4px",
                          border: "1px solid #e0e0e0",
                        }}
                      >
                        <div
                          style={{
                            marginBottom: "0.5rem",
                            fontSize: "0.875rem",
                            color: "#d32f2f",
                          }}
                        >
                          <strong>Test ID associé:</strong>{" "}
                          {organismGroup.testId || "N/A"}
                        </div>
                        <div style={{ marginBottom: "1rem" }}>
                          <div
                            style={{
                              display: "grid",
                              gridTemplateColumns: "auto 1fr",
                              gap: "0.5rem 1rem",
                              fontSize: "0.875rem",
                            }}
                          >
                            <strong>
                              Organisme #{organism.organismNumber}:
                            </strong>
                            <span>
                              {organism.resolvedOrganismName ||
                                organism.organismNameText ||
                                (organism.organismNameDictId
                                  ? `ID: ${organism.organismNameDictId}`
                                  : "N/A")}
                            </span>

                            <strong>Type Gram:</strong>
                            <span>{organism.gramType || "N/A"}</span>

                            <strong>Mode de regroupement:</strong>
                            <span>{organism.groupingMode || "N/A"}</span>

                            {organism.capsulePresence && (
                              <>
                                <strong>Présence de capsule:</strong>
                                <span>Oui</span>
                              </>
                            )}

                            {organism.otherCharacteristics &&
                              organism.otherCharacteristics.trim() && (
                                <>
                                  <strong>Autres caractéristiques:</strong>
                                  <span>{organism.otherCharacteristics}</span>
                                </>
                              )}
                          </div>
                        </div>

                        {antibiograms.length > 0 && (
                          <div>
                            <h6
                              style={{
                                fontSize: "0.85rem",
                                marginBottom: "0.5rem",
                                color: "#525252",
                              }}
                            >
                              Antibiogramme ({antibiograms.length}{" "}
                              antibiotiques)
                            </h6>
                            <TableContainer>
                              <Table size="sm">
                                <TableHead>
                                  <TableRow>
                                    <TableHeader>Antibiotique</TableHeader>
                                    <TableHeader>Résultat</TableHeader>
                                    <TableHeader>CMI</TableHeader>
                                    <TableHeader>Diamètre (mm)</TableHeader>
                                  </TableRow>
                                </TableHead>
                                <TableBody>
                                  {antibiograms.map((antibiogram, abIndex) => (
                                    <TableRow
                                      key={`antibiogram-orphan-${orgGroupId}-${abIndex}`}
                                    >
                                      <TableCell>
                                        {antibiogram.antibioticNameText ||
                                          (antibiogram.antibioticDictId
                                            ? `ID: ${antibiogram.antibioticDictId}`
                                            : "N/A")}
                                      </TableCell>
                                      <TableCell>
                                        <span
                                          style={{
                                            padding: "0.25rem 0.5rem",
                                            borderRadius: "4px",
                                            fontWeight: "bold",
                                            fontSize: "0.8rem",
                                            backgroundColor:
                                              antibiogram.interpretationText ===
                                                "Sensible" ||
                                              antibiogram.interpretationText ===
                                                "S"
                                                ? "#d4edda"
                                                : antibiogram.interpretationText ===
                                                      "Résistant" ||
                                                    antibiogram.interpretationText ===
                                                      "R"
                                                  ? "#f8d7da"
                                                  : antibiogram.interpretationText ===
                                                        "Intermédiaire" ||
                                                      antibiogram.interpretationText ===
                                                        "I"
                                                    ? "#fff3cd"
                                                    : "#e2e3e5",
                                            color:
                                              antibiogram.interpretationText ===
                                                "Sensible" ||
                                              antibiogram.interpretationText ===
                                                "S"
                                                ? "#155724"
                                                : antibiogram.interpretationText ===
                                                      "Résistant" ||
                                                    antibiogram.interpretationText ===
                                                      "R"
                                                  ? "#721c24"
                                                  : antibiogram.interpretationText ===
                                                        "Intermédiaire" ||
                                                      antibiogram.interpretationText ===
                                                        "I"
                                                    ? "#856404"
                                                    : "#383838",
                                          }}
                                        >
                                          {antibiogram.interpretationText ||
                                            antibiogram.result ||
                                            "N/A"}
                                        </span>
                                      </TableCell>
                                      <TableCell>
                                        {antibiogram.micValue || "-"}
                                      </TableCell>
                                      <TableCell>
                                        {antibiogram.diameterMm || "-"}
                                      </TableCell>
                                    </TableRow>
                                  ))}
                                </TableBody>
                              </Table>
                            </TableContainer>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            );
          })()}

        {/* Sample Interpretation / Notes Section */}
        <div style={{ marginTop: "1.5rem" }}>
          <h4 style={{ marginBottom: "0.75rem" }}>
            <FormattedMessage id="validation.sampleInterpretation.label" />
          </h4>
          <TextArea
            id={`bacteriology-interpretation-${sampleId || analysisId}`}
            labelText=""
            maxCount={200}
            placeholder={intl.formatMessage({
              id: "validation.sampleInterpretation.placeholder",
            })}
            value={sampleInterpretation}
            onChange={(e) => {
              const limitedValue = e.target.value.slice(0, 199);
              setSampleInterpretation(limitedValue);
            }}
            rows={3}
            style={{ width: "100%" }}
            disabled={disabled || saving}
          />
          {sampleInterpretation && (
            <div
              style={{
                marginTop: "0.5rem",
                fontSize: "0.875rem",
                color: "#525252",
              }}
            >
              {sampleInterpretation.length}/200 caractères
            </div>
          )}
        </div>

        {/* Action buttons */}
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
            onClick={handleValidate}
            disabled={disabled || saving}
          >
            {saving ? (
              <InlineLoading
                description={intl.formatMessage({ id: "label.saving" })}
              />
            ) : (
              <FormattedMessage id="label.button.validate" />
            )}
          </Button>
        </ButtonSet>
      </Stack>
    </div>
  );
};

export default BacteriologyValidation;
