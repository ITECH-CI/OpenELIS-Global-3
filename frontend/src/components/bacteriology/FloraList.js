import React, { useState, useEffect } from "react";
import { Grid, Column, Select, SelectItem, Checkbox } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

/**
 * FloraList - Manages dynamic flora count and details
 *
 * Allows users to select the number of floras (0, 1, 2, >=3)
 * and provide details for each flora (gram type, grouping mode, capsulated)
 *
 * @param {string} accessionNumber - Used for generating unique IDs
 * @param {string} testId - The flora count test ID
 * @param {number} floraCount - Current flora count (0-3+)
 * @param {array} floraDetails - Array of flora detail objects
 * @param {function} onFloraCountChange - Callback when count changes
 * @param {function} onFloraDetailsChange - Callback when details change
 * @param {boolean} disabled - Whether fields are disabled
 */
const FloraList = ({
  accessionNumber,
  testId,
  floraCount = 0,
  floraDetails = [],
  onFloraCountChange,
  onFloraDetailsChange,
  disabled = false,
}) => {
  const [gramTypes, setGramTypes] = useState([]);
  const [groupingModes, setGroupingModes] = useState([]);
  const [loading, setLoading] = useState(true);

  // Generate unique ID prefix
  const idPrefix = accessionNumber
    ? accessionNumber.replace(/[^a-zA-Z0-9]/g, "_")
    : "flora";

  useEffect(() => {
    // Load gram types and grouping modes from backend
    Promise.all([
      new Promise((resolve) => {
        getFromOpenElisServer(
          "/rest/dictionary/category/Bacteriology Flora Gram Type",
          (data) => resolve(data || []),
        );
      }),
      new Promise((resolve) => {
        getFromOpenElisServer(
          "/rest/dictionary/category/Bacteriology Flora Grouping Mode",
          (data) => resolve(data || []),
        );
      }),
    ]).then(([gramData, groupingData]) => {
      setGramTypes(gramData);
      setGroupingModes(groupingData);
      setLoading(false);
    });
  }, []);

  // Handle flora count change
  const handleCountChange = (e) => {
    const newCount = parseInt(e.target.value) || 0;
    onFloraCountChange(testId, newCount);

    // Initialize flora details array if count increased
    if (newCount > floraDetails.length) {
      const newDetails = [...floraDetails];
      for (let i = floraDetails.length; i < newCount; i++) {
        newDetails.push({
          floraNumber: i + 1,
          gramTypeId: "",
          groupingModeId: "",
          capsulated: false,
        });
      }
      onFloraDetailsChange(newDetails);
    } else if (newCount < floraDetails.length) {
      // Trim flora details if count decreased
      onFloraDetailsChange(floraDetails.slice(0, newCount));
    }
  };

  // Handle flora detail field change
  const handleDetailChange = (index, field, value) => {
    const updatedDetails = [...floraDetails];
    updatedDetails[index] = {
      ...updatedDetails[index],
      [field]: value,
    };
    onFloraDetailsChange(updatedDetails);
  };

  if (loading) {
    return <div>Chargement des options de flore...</div>;
  }

  return (
    <div className="flora-list">
      {/* Flora Count Selector */}
      <Grid fullWidth>
        <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
          <Select
            id={`flora_count_${idPrefix}_${testId}`}
            labelText="Nombre de flores"
            value={floraCount}
            onChange={handleCountChange}
            disabled={disabled}
          >
            <SelectItem value={0} text="0" />
            <SelectItem value={1} text="1" />
            <SelectItem value={2} text="2" />
            <SelectItem value={3} text="≥3" />
          </Select>
        </Column>
      </Grid>

      {/* Flora Details - Dynamic based on count */}
      {floraCount > 0 && (
        <div
          className="flora-details"
          style={{ marginLeft: "2rem", marginTop: "1rem" }}
        >
          {floraDetails.map((flora, index) => (
            <div
              key={index}
              className="flora-item"
              style={{ marginBottom: "2rem" }}
            >
              <h5 style={{ marginBottom: "1rem" }}>Flore {flora.floraNumber}</h5>
              <Grid fullWidth>
                {/* Gram Type */}
                <Column lg={8} md={4} sm={4} style={{ marginBottom: "1rem" }}>
                  <Select
                    id={`flora_gram_${idPrefix}_${testId}_${index}`}
                    labelText="Type de Gram"
                    value={flora.gramTypeId || ""}
                    onChange={(e) =>
                      handleDetailChange(index, "gramTypeId", e.target.value)
                    }
                    disabled={disabled}
                  >
                    <SelectItem value="" text="Sélectionner..." />
                    {gramTypes.map((type) => (
                      <SelectItem
                        key={type.id}
                        value={type.id}
                        text={type.value}
                      />
                    ))}
                  </Select>
                </Column>

                {/* Grouping Mode */}
                <Column lg={8} md={4} sm={4} style={{ marginBottom: "1rem" }}>
                  <Select
                    id={`flora_grouping_${idPrefix}_${testId}_${index}`}
                    labelText="Mode de regroupement"
                    value={flora.groupingModeId || ""}
                    onChange={(e) =>
                      handleDetailChange(
                        index,
                        "groupingModeId",
                        e.target.value,
                      )
                    }
                    disabled={disabled}
                  >
                    <SelectItem value="" text="Sélectionner..." />
                    {groupingModes.map((mode) => (
                      <SelectItem
                        key={mode.id}
                        value={mode.id}
                        text={mode.value}
                      />
                    ))}
                  </Select>
                </Column>

                {/* Capsulated */}
                <Column lg={8} md={4} sm={4} style={{ marginBottom: "1rem" }}>
                  <Checkbox
                    id={`flora_capsulated_${idPrefix}_${testId}_${index}`}
                    labelText="Capsulé"
                    checked={flora.capsulated || false}
                    onChange={(e) =>
                      handleDetailChange(index, "capsulated", e.target.checked)
                    }
                    disabled={disabled}
                  />
                </Column>
              </Grid>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default FloraList;
