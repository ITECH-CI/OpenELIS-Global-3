import React, { useState, useEffect } from "react";
import {
  DataTable,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  RadioButtonGroup,
  RadioButton,
  TextInput,
  Button,
} from "@carbon/react";
import { Add, TrashCan } from "@carbon/icons-react";
import { FormattedMessage } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import { API_ENDPOINTS, ANTIBIOGRAM_RESULTS } from "../BacteriologyConstants";

const AntibiogramTable = ({
  accessionNumber,
  organismNumber,
  antibiograms = [],
  onChange,
  disabled = false,
}) => {
  const [antibioticList, setAntibioticList] = useState([]);
  const [loading, setLoading] = useState(true);

  // Use accessionNumber as prefix for unique IDs
  const idPrefix = accessionNumber ? accessionNumber.replace(/[^a-zA-Z0-9]/g, '_') : 'atb';

  useEffect(() => {
    // Load antibiotics from backend
    getFromOpenElisServer(API_ENDPOINTS.ANTIBIOTICS, (data) => {
      setAntibioticList(data);
      setLoading(false);
    });
  }, []);

  const handleAddAntibiotic = () => {
    const newAntibiogram = {
      id: null,
      antibioticDictId: "",
      result: "",
      diameterMm: "",
      micValue: "",
      interpretationComment: "",
    };
    onChange([...antibiograms, newAntibiogram]);
  };

  const handleRemoveAntibiotic = (index) => {
    const updated = antibiograms.filter((_, i) => i !== index);
    onChange(updated);
  };

  const handleFieldChange = (index, field, value) => {
    const updated = [...antibiograms];
    updated[index] = { ...updated[index], [field]: value };
    onChange(updated);
  };

  const headers = [
    { key: "antibiotic", header: "Antibiotic" },
    { key: "result", header: "Résultat (S/I/R)" },
    { key: "diameter", header: "Diamètre (mm)" },
    { key: "mic", header: "MIC" },
    { key: "comment", header: "Commentaire" },
    { key: "actions", header: "" },
  ];

  if (loading) {
    return <div>Loading antibiotics...</div>;
  }

  return (
    <div className="antibiogram-table">
      <div style={{ marginBottom: "1rem" }}>
        <Button
          size="sm"
          kind="tertiary"
          renderIcon={Add}
          onClick={handleAddAntibiotic}
          disabled={disabled}
        >
          <FormattedMessage id="bacteriology.antibiogram.add" />
        </Button>
      </div>

      <TableContainer>
        <Table size="sm">
          <TableHead>
            <TableRow>
              {headers.map((header) => (
                <TableHeader key={header.key}>{header.header}</TableHeader>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {antibiograms.length === 0 ? (
              <TableRow>
                <TableCell colSpan={headers.length}>
                  <FormattedMessage id="bacteriology.antibiogram.empty" />
                </TableCell>
              </TableRow>
            ) : (
              antibiograms.map((antibiogram, index) => (
                <TableRow key={index}>
                  <TableCell>
                    <select
                      value={antibiogram.antibioticDictId || ""}
                      onChange={(e) =>
                        handleFieldChange(
                          index,
                          "antibioticDictId",
                          parseInt(e.target.value),
                        )
                      }
                      disabled={disabled}
                      style={{ width: "100%" }}
                    >
                      <option value="">Select...</option>
                      {antibioticList.map((ab) => (
                        <option key={ab.id} value={ab.id}>
                          {ab.localAbbreviation || ab.name}
                        </option>
                      ))}
                    </select>
                  </TableCell>
                  <TableCell>
                    <RadioButtonGroup
                      name={`result_${idPrefix}_${organismNumber}_${index}`}
                      valueSelected={antibiogram.result || ""}
                      onChange={(value) =>
                        handleFieldChange(index, "result", value)
                      }
                      orientation="horizontal"
                    >
                      <RadioButton
                        id={`s_${idPrefix}_${organismNumber}_${index}`}
                        labelText="S"
                        value={ANTIBIOGRAM_RESULTS.S}
                        disabled={disabled}
                      />
                      <RadioButton
                        id={`i_${idPrefix}_${organismNumber}_${index}`}
                        labelText="I"
                        value={ANTIBIOGRAM_RESULTS.I}
                        disabled={disabled}
                      />
                      <RadioButton
                        id={`r_${idPrefix}_${organismNumber}_${index}`}
                        labelText="R"
                        value={ANTIBIOGRAM_RESULTS.R}
                        disabled={disabled}
                      />
                    </RadioButtonGroup>
                  </TableCell>
                  <TableCell>
                    <TextInput
                      id={`diameter_${idPrefix}_${organismNumber}_${index}`}
                      type="number"
                      value={antibiogram.diameterMm || ""}
                      onChange={(e) =>
                        handleFieldChange(index, "diameterMm", e.target.value)
                      }
                      disabled={disabled}
                      size="sm"
                      hideLabel
                    />
                  </TableCell>
                  <TableCell>
                    <TextInput
                      id={`mic_${idPrefix}_${organismNumber}_${index}`}
                      value={antibiogram.micValue || ""}
                      onChange={(e) =>
                        handleFieldChange(index, "micValue", e.target.value)
                      }
                      disabled={disabled}
                      size="sm"
                      hideLabel
                    />
                  </TableCell>
                  <TableCell>
                    <TextInput
                      id={`comment_${idPrefix}_${organismNumber}_${index}`}
                      value={antibiogram.interpretationComment || ""}
                      onChange={(e) =>
                        handleFieldChange(
                          index,
                          "interpretationComment",
                          e.target.value,
                        )
                      }
                      disabled={disabled}
                      size="sm"
                      hideLabel
                    />
                  </TableCell>
                  <TableCell>
                    <Button
                      size="sm"
                      kind="ghost"
                      hasIconOnly
                      renderIcon={TrashCan}
                      iconDescription="Remove"
                      onClick={() => handleRemoveAntibiotic(index)}
                      disabled={disabled}
                    />
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </div>
  );
};

export default AntibiogramTable;
