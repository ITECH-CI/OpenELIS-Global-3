import { Add, TrashCan } from "@carbon/icons-react";
import {
  Button,
  ComboBox,
  RadioButton,
  RadioButtonGroup,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TextInput,
} from "@carbon/react";
import { useEffect, useState } from "react";
import { FormattedMessage } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import {
  ANTIBIOGRAM_RESULTS,
  ANTIBIOTICS_CATEGORY,
} from "../BacteriologyConstants";

const AntibiogramTable = ({
  accessionNumber,
  organismNumber,
  antibiograms = [],
  onChange,
  disabled = false,
  uniqueId = "", // Optional unique identifier for multiple antibiogram tables
}) => {
  const [antibioticList, setAntibioticList] = useState([]);
  const [loading, setLoading] = useState(true);

  // Use accessionNumber + uniqueId as prefix for unique IDs
  const idPrefix = accessionNumber
    ? `${accessionNumber.replace(/[^a-zA-Z0-9]/g, "_")}${uniqueId ? `_${uniqueId}` : ""}`
    : `atb${uniqueId ? `_${uniqueId}` : ""}`;

  useEffect(() => {
    // Load antibiotics from DisplayListService
    getFromOpenElisServer(
      `/rest/displayList/${encodeURIComponent(ANTIBIOTICS_CATEGORY)}`,
      (data) => {
        setAntibioticList(data || []);
        setLoading(false);
      },
    );
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

  // Transform antibiotics list for ComboBox
  const antibioticItems = antibioticList.map((ab) => ({
    id: String(ab.id),
    label: ab.value || ab.localAbbreviation || ab.name || String(ab.id),
  }));

  const headers = [
    { key: "antibiotic", header: "Antibiotic" },
    { key: "result", header: "Résultat (S/I/R)" },
    { key: "diameter", header: "Diamètre (mm)" },
    { key: "mic", header: "CMI" },
    { key: "comment", header: "Commentaire" },
    { key: "actions", header: "" },
  ];

  if (loading) {
    return <div>Loading antibiotics...</div>;
  }

  return (
    <div className="antibiogram-table" style={{ marginBottom: "3rem" }}>
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
              antibiograms.map((antibiogram, index) => {
                // Find selected antibiotic item
                const selectedItem = antibioticItems.find(
                  (item) => item.id === String(antibiogram.antibioticDictId),
                );

                return (
                  <TableRow key={index}>
                    <TableCell>
                      <ComboBox
                        id={`antibiotic_${idPrefix}_${organismNumber}_${index}`}
                        items={antibioticItems}
                        selectedItem={selectedItem || null}
                        onChange={({ selectedItem: newSelectedItem }) => {
                          if (newSelectedItem) {
                            // Only store the ID, not the entire selectedItem object
                            handleFieldChange(
                              index,
                              "antibioticDictId",
                              parseInt(newSelectedItem.id),
                            );
                          } else {
                            handleFieldChange(index, "antibioticDictId", "");
                          }
                        }}
                        itemToString={(item) => (item ? item.label : "")}
                        placeholder="Rechercher un antibiotique..."
                        disabled={disabled}
                        size="sm"
                        titleText=""
                        shouldFilterItem={({ item, inputValue }) => {
                          if (!inputValue) return true;
                          return item.label
                            .toLowerCase()
                            .includes(inputValue.toLowerCase());
                        }}
                      />
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
                        labelText="Diamètre"
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
                        labelText="CMI"
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
                        labelText="Commentaire"
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
                );
              })
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </div>
  );
};

export default AntibiogramTable;
