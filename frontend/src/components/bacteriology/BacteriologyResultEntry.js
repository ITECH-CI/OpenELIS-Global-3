import React, { useState, useEffect, useContext } from "react";
import {
  Form,
  Stack,
  Button,
  InlineLoading,
  ButtonSet,
} from "@carbon/react";
import { Save, Reset } from "@carbon/icons-react";
import { FormattedMessage } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { API_ENDPOINTS } from "./BacteriologyConstants";
import MacroscopySection from "./sections/MacroscopySection";
import MicroscopySection from "./sections/MicroscopySection";
import CultureSection from "./sections/CultureSection";
import ResultHeader from "./sections/ResultHeader";

const BacteriologyResultEntry = ({
  analysisId,
  accessionNumber,
  testResults = [],
  sysUserId,
  onSave,
  disabled = false,
}) => {
  const { addNotification } = useContext(NotificationContext);

  const [macroscopyResults, setMacroscopyResults] = useState({});
  const [microscopyResults, setMicroscopyResults] = useState({});
  const [floraData, setFloraData] = useState({});
  const [cultureResult, setCultureResult] = useState("");
  const [organisms, setOrganisms] = useState([]);
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
    getFromOpenElisServer(
      `${API_ENDPOINTS.GET_RESULTS}/${analysisId}`,
      (data) => {
        // Load existing bacteriology results
        if (data && data.organisms) {
          setOrganisms(data.organisms.map(o => o.organism));
          if (data.organisms.length > 0) {
            setCultureResult("positive");
          }
        }
        if (data && data.cultureGroup) {
          // Culture result would be stored in the standard result table
          // This is just for loading the state
        }
        setInitialData(data);
        setLoading(false);
      },
      () => {
        setLoading(false);
      }
    );
  };

  const handleSave = () => {
    setSaving(true);

    const formData = {
      analysisId,
      sysUserId,
      macroscopyResults,
      microscopyResults,
      floraData,
      cultureResult,
      organisms: organisms.map((organism, index) => ({
        ...organism,
        organismNumber: index + 1,
      })),
    };

    postToOpenElisServerJsonResponse(
      API_ENDPOINTS.SAVE_RESULTS,
      JSON.stringify(formData),
      (response) => {
        setSaving(false);
        addNotification({
          kind: "success",
          title: "Success",
          message: "Bacteriology results saved successfully",
        });
        if (onSave) {
          onSave(formData);
        }
      },
      (error) => {
        setSaving(false);
        addNotification({
          kind: "error",
          title: "Error",
          message: "Failed to save bacteriology results: " + error,
        });
      }
    );
  };

  const handleReset = () => {
    setMacroscopyResults({});
    setMicroscopyResults({});
    setFloraData({});
    setCultureResult("");
    setOrganisms([]);
    if (initialData) {
      loadBacteriologyResults();
    }
  };

  const handleCultureResultChange = (value) => {
    setCultureResult(value);
    if (value === "negative") {
      setOrganisms([]);
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
        <ResultHeader testResult={testResults[0]} />

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
          cultureResult={cultureResult}
          onCultureResultChange={handleCultureResultChange}
          organisms={organisms}
          onOrganismsChange={setOrganisms}
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
