import {
  Accordion,
  AccordionItem,
  Column,
  Grid,
  RadioButton,
  RadioButtonGroup,
} from "@carbon/react";
import { useEffect, useState } from "react";
import { getFromOpenElisServer } from "../../utils/Utils";
import { ORGANISM_TYPES } from "../BacteriologyConstants";
import AntibiogramTable from "./AntibiogramTable";
import SearchableSelect from "./SearchableSelect";

// When the user selects "Levure" (yeast), we constrain the organism picker
// to the single yeast we currently track (Candida albicans). Until a yeast/
// bacteria flag is attached to the dictionary entries we keep this id hard-coded.
const CANDIDA_ALBICANS_DICT_ID = 3245;
const CANDIDA_ALBICANS_NAME_LOWERCASE = "candida albicans";

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

  // When yeast is selected, restrict the picker to Candida albicans and
  // pre-select it. Auto-clear any bacteria-only fields that may have been set
  // before the switch (gramType, groupingMode).
  const displayedOrganismNames = isYeast
    ? (organismNames || []).filter((o) => {
        const dictId = parseInt(o?.id);
        const name = String(o?.value || o?.name || "").toLowerCase();
        return (
          dictId === CANDIDA_ALBICANS_DICT_ID
          || name.includes(CANDIDA_ALBICANS_NAME_LOWERCASE)
        );
      })
    : organismNames;

  useEffect(() => {
    if (!isYeast) return;
    const expected = CANDIDA_ALBICANS_DICT_ID;
    if (organism.organismNameDictId !== expected) {
      onChange({
        ...organism,
        organismNameDictId: expected,
        organismNameText: "",
        gramType: "",
        groupingMode: "",
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isYeast]);

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
              items={displayedOrganismNames}
              selectedValue={organism.organismNameDictId}
              onChange={(value) =>
                handleFieldChange(
                  "organismNameDictId",
                  value ? parseInt(value) : null,
                )
              }
              returnType="id"
              disabled={disabled || loading || isYeast}
              placeholder={
                isYeast
                  ? "Candida albicans"
                  : "Rechercher un organisme..."
              }
            />

            {/* "Ou saisir un nom libre" hidden from the result-entry/modification
                screen on request — the dictionary picker above is the only entry
                point. Any pre-existing organismNameText value is preserved in the
                data model and still rendered in reports/validation. */}
          </Column>

          {/* "Type de Gram" et "Mode de regroupement" sont masqués ici : ces
              informations sont déjà saisies dans la zone "Nombre de flore"
              (FloraList) pour chaque flore. Les champs sont conservés dans
              le data model côté backend pour les rapports/validation. */}

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
