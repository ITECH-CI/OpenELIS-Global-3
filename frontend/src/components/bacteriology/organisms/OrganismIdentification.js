import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  Select,
  SelectItem,
  RadioButtonGroup,
  RadioButton,
  TextInput,
  Checkbox,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import {
  API_ENDPOINTS,
  ORGANISM_TYPES,
  GRAM_TYPES,
} from "../BacteriologyConstants";
import AntibiogramTable from "./AntibiogramTable";

const OrganismIdentification = ({
  accessionNumber,
  organismNumber,
  organism,
  onChange,
  disabled = false,
}) => {
  const [organismNames, setOrganismNames] = useState([]);
  const [gramTypes, setGramTypes] = useState([]);
  const [groupingModes, setGroupingModes] = useState([]);
  const [loading, setLoading] = useState(true);

  // Use accessionNumber as prefix for unique IDs
  const idPrefix = accessionNumber ? accessionNumber.replace(/[^a-zA-Z0-9]/g, '_') : 'org';

  useEffect(() => {
    // Load organism names from backend
    getFromOpenElisServer(API_ENDPOINTS.ORGANISMS, (data) => {
      console.log("Organism Names Response:", data);
      setOrganismNames(data);
    });

    // Load gram types from dictionary
    getFromOpenElisServer("/rest/dictionary/category/Bacteriology Gram Type", (data) => {
      console.log("Gram Types Response:", data);
      console.log("First Gram Type:", data && data[0]);
      setGramTypes(data || []);
    });

    // Load grouping modes from dictionary
    getFromOpenElisServer("/rest/dictionary/category/Bacteriology Grouping Mode", (data) => {
      console.log("Grouping Modes Response:", data);
      console.log("First Grouping Mode:", data && data[0]);
      setGroupingModes(data || []);
      setLoading(false);
    });
  }, []);

  const handleFieldChange = (field, value) => {
    onChange({ ...organism, [field]: value });
  };

  const isBacteria = organism.organismType === ORGANISM_TYPES.BACTERIA;
  const showAntibiogram = isBacteria;

  return (
    <Accordion>
      <AccordionItem
        title={`Germe ${organismNumber}`}
        open={organism.organismNumber === organismNumber}
      >
        <Grid>
          <Column lg={4} md={4} sm={4}>
            <RadioButtonGroup
              legendText="Type d'organisme"
              name={`organismType_${idPrefix}_${organismNumber}`}
              valueSelected={organism.organismType || ""}
              onChange={(value) => handleFieldChange("organismType", value)}
              disabled={disabled}
            >
              <RadioButton
                id={`bacteria_${idPrefix}_${organismNumber}`}
                labelText="Bactérie"
                value={ORGANISM_TYPES.BACTERIA}
              />
              <RadioButton
                id={`yeast_${idPrefix}_${organismNumber}`}
                labelText="Levure"
                value={ORGANISM_TYPES.YEAST}
              />
            </RadioButtonGroup>
          </Column>

          <Column lg={8} md={8} sm={4}>
            <Select
              id={`organismName_${idPrefix}_${organismNumber}`}
              labelText="Nom de l'organisme"
              value={organism.organismNameDictId || ""}
              onChange={(e) =>
                handleFieldChange("organismNameDictId", parseInt(e.target.value))
              }
              disabled={disabled || loading}
            >
              <SelectItem value="" text="Sélectionner..." />
              {organismNames.map((name) => (
                <SelectItem
                  key={name.id}
                  value={name.id}
                  text={name.name}
                />
              ))}
            </Select>

            <TextInput
              id={`organismNameText_${idPrefix}_${organismNumber}`}
              labelText="Ou saisir un nom libre"
              value={organism.organismNameText || ""}
              onChange={(e) =>
                handleFieldChange("organismNameText", e.target.value)
              }
              disabled={disabled}
              placeholder="Si non trouvé dans la liste"
            />
          </Column>

          {isBacteria && (
            <>
              <Column lg={4} md={4} sm={4}>
                <Select
                  id={`gramType_${idPrefix}_${organismNumber}`}
                  labelText="Type de Gram"
                  value={organism.gramType || ""}
                  onChange={(e) => handleFieldChange("gramType", e.target.value)}
                  disabled={disabled || loading}
                >
                  <SelectItem value="" text="Sélectionner..." />
                  {gramTypes.map((type) => (
                    <SelectItem
                      key={type.id}
                      value={type.value}
                      text={type.value}
                    />
                  ))}
                </Select>
              </Column>

              <Column lg={4} md={4} sm={4}>
                <Select
                  id={`groupingMode_${idPrefix}_${organismNumber}`}
                  labelText="Mode de regroupement"
                  value={organism.groupingMode || ""}
                  onChange={(e) =>
                    handleFieldChange("groupingMode", e.target.value)
                  }
                  disabled={disabled || loading}
                >
                  <SelectItem value="" text="Sélectionner..." />
                  {groupingModes.map((mode) => (
                    <SelectItem
                      key={mode.id}
                      value={mode.value}
                      text={mode.value}
                    />
                  ))}
                </Select>
              </Column>

              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id={`capsule_${idPrefix}_${organismNumber}`}
                  labelText="Présence de capsule"
                  checked={organism.capsulePresence || false}
                  onChange={(checked) =>
                    handleFieldChange("capsulePresence", checked)
                  }
                  disabled={disabled}
                />
              </Column>

              <Column lg={12} md={8} sm={4}>
                <TextInput
                  id={`otherChar_${idPrefix}_${organismNumber}`}
                  labelText="Autres caractéristiques"
                  value={organism.otherCharacteristics || ""}
                  onChange={(e) =>
                    handleFieldChange("otherCharacteristics", e.target.value)
                  }
                  disabled={disabled}
                  placeholder="Autres observations..."
                  multiline
                />
              </Column>
            </>
          )}

          {showAntibiogram && (
            <Column lg={16} md={8} sm={4}>
              <h4 style={{ marginTop: "2rem", marginBottom: "1rem" }}>
                Antibiogramme
              </h4>
              <AntibiogramTable
                accessionNumber={accessionNumber}
                organismNumber={organismNumber}
                antibiograms={organism.antibiograms || []}
                onChange={(antibiograms) =>
                  handleFieldChange("antibiograms", antibiograms)
                }
                disabled={disabled}
              />
            </Column>
          )}
        </Grid>
      </AccordionItem>
    </Accordion>
  );
};

export default OrganismIdentification;
