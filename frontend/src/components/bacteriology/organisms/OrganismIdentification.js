import {
  Accordion,
  AccordionItem,
  Column,
  Grid,
  RadioButton,
  RadioButtonGroup,
  TextInput,
} from "@carbon/react";
import { useEffect, useState } from "react";
import { getFromOpenElisServer } from "../../utils/Utils";
import { ORGANISM_TYPES } from "../BacteriologyConstants";
import AntibiogramTable from "./AntibiogramTable";
import SearchableSelect from "./SearchableSelect";

const OrganismIdentification = ({
  accessionNumber,
  organismNumber,
  organism,
  onChange,
  disabled = false,
  skipOrganismIdentification = false,
}) => {
  const [organismNames, setOrganismNames] = useState([]);
  const [gramTypes, setGramTypes] = useState([]);
  const [groupingModes, setGroupingModes] = useState([]);
  const [loading, setLoading] = useState(true);

  // Use accessionNumber as prefix for unique IDs
  const idPrefix = accessionNumber
    ? accessionNumber.replace(/[^a-zA-Z0-9]/g, "_")
    : "org";

  useEffect(() => {
    // Load organism names from dictionary category "Bacteria"
    getFromOpenElisServer("/rest/dictionary/category/Bacteria", (data) => {
      setOrganismNames(data);
    });

    // Load gram types from dictionary
    getFromOpenElisServer(
      "/rest/dictionary/category/Bacteriology Gram Type",
      (data) => {
        setGramTypes(data || []);
      },
    );

    // Load grouping modes from dictionary
    getFromOpenElisServer(
      "/rest/dictionary/category/Bacteriology Grouping Mode",
      (data) => {
        setGroupingModes(data || []);
        setLoading(false);
      },
    );
  }, []);

  const handleFieldChange = (field, value) => {
    onChange({ ...organism, [field]: value });
  };

  const isBacteria = organism.organismType === ORGANISM_TYPES.BACTERIA;
  const isYeast = organism.organismType === ORGANISM_TYPES.YEAST;
  const showAntibiogram = isBacteria;

  // For Neisseria gonorrhoeae: skip identification and show antibiogram directly
  if (skipOrganismIdentification) {
    return (
      <div style={{ marginTop: "1rem" }}>
        <h4 style={{ marginBottom: "1rem" }}>
          Antibiogramme pour Neisseria gonorrhoeae
        </h4>
        <AntibiogramTable
          accessionNumber={accessionNumber}
          organismNumber={organismNumber}
          antibiograms={organism.antibiograms || []}
          onChange={(antibiograms) =>
            handleFieldChange("antibiograms", antibiograms)
          }
          disabled={disabled}
          uniqueId={`org${organismNumber}_neiss`}
        />
      </div>
    );
  }

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
            <SearchableSelect
              id={`organismName_${idPrefix}_${organismNumber}`}
              labelText="Nom de l'organisme (recherchez ou sélectionnez)"
              items={organismNames}
              selectedValue={organism.organismNameDictId}
              onChange={(value) =>
                handleFieldChange(
                  "organismNameDictId",
                  value ? parseInt(value) : null,
                )
              }
              returnType="id"
              disabled={disabled || loading}
              placeholder="Rechercher un organisme..."
            />

            <TextInput
              id={`organismNameText_${idPrefix}_${organismNumber}`}
              labelText="Ou saisir un nom libre"
              value={organism.organismNameText || ""}
              onChange={(e) =>
                handleFieldChange("organismNameText", e.target.value)
              }
              disabled={disabled}
              placeholder="Si non trouvé dans la liste"
              style={{ marginTop: "1rem" }}
            />
          </Column>

          {isBacteria && (
            <>
              <Column lg={4} md={4} sm={4}>
                <SearchableSelect
                  id={`gramType_${idPrefix}_${organismNumber}`}
                  labelText="Type de Gram"
                  items={gramTypes}
                  selectedValue={organism.gramType}
                  onChange={(value) => handleFieldChange("gramType", value)}
                  disabled={disabled || loading}
                  placeholder="Rechercher..."
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <SearchableSelect
                  id={`groupingMode_${idPrefix}_${organismNumber}`}
                  labelText="Mode de regroupement"
                  items={groupingModes}
                  selectedValue={organism.groupingMode}
                  onChange={(value) => handleFieldChange("groupingMode", value)}
                  disabled={disabled || loading}
                  placeholder="Rechercher..."
                />
              </Column>

              {/* Capsule presence removed - not needed in result entry */}

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
                  rows={4}
                />
              </Column>
            </>
          )}

          {/* Show info message for yeast */}
          {isYeast && (
            <Column lg={16} md={8} sm={4}>
              <div
                style={{
                  marginTop: "1rem",
                  padding: "1rem",
                  backgroundColor: "#f4f4f4",
                  borderRadius: "4px",
                  borderLeft: "4px solid #0f62fe",
                }}
              >
                <p style={{ margin: 0, color: "#161616", fontStyle: "italic" }}>
                  Pour les levures, l'antibiogramme n'est pas disponible. Seule
                  l'identification est requise.
                </p>
              </div>
            </Column>
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
                uniqueId={`org${organismNumber}`}
              />
            </Column>
          )}
        </Grid>
      </AccordionItem>
    </Accordion>
  );
};

export default OrganismIdentification;
