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

// When the user selects "Levure" (yeast) on a Sécrétions vaginales culture,
// we constrain the organism picker to the single yeast we currently track
// (Candida albicans) and pre-select it. For other cultures with yeast the
// picker stays free (full dictionary).
//
// Candida albicans is resolved by name from the loaded yeast dictionary
// rather than a hardcoded dictionary id: the id is instance-specific and
// breaks on reseed/other deployments. The numeric id is only a last-resort
// fallback when the name match finds nothing.
const CANDIDA_ALBICANS_FALLBACK_DICT_ID = 3245;
const CANDIDA_ALBICANS_NAME_NORMALIZED = "candida albicans";
const VAGINAL_CULTURE_TEST_NAME = "Culture - Sécrétions vaginales";

// Lowercase + strip accents so the match works regardless of locale/casing.
const normalizeOrganismName = (value) =>
  String(value || "")
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim();

// Find the Candida albicans entry in a loaded organism list by name.
const findCandidaAlbicans = (list) =>
  (list || []).find((o) =>
    normalizeOrganismName(o?.value || o?.name).includes(
      CANDIDA_ALBICANS_NAME_NORMALIZED,
    ),
  );

const OrganismIdentification = ({
  accessionNumber,
  organismNumber,
  organism,
  onChange,
  disabled = false,
  skipOrganismIdentification = false,
  cultureTestName = "",
  cultureTestId = "",
}) => {
  const [organismNames, setOrganismNames] = useState([]);
  const [yeastNames, setYeastNames] = useState([]);
  const [gramTypes, setGramTypes] = useState([]);
  const [groupingModes, setGroupingModes] = useState([]);
  const [loading, setLoading] = useState(true);

  // Build a unique id prefix that distinguishes (a) the order (accessionNumber)
  // and (b) the culture inside that order (cultureTestId). Without the latter
  // the radio buttons and selects of two parallel cultures share the same DOM
  // ids and clicks on one toggle the other.
  const accessionToken = accessionNumber
    ? accessionNumber.replace(/[^a-zA-Z0-9]/g, "_")
    : "org";
  const cultureToken = cultureTestId
    ? String(cultureTestId).replace(/[^a-zA-Z0-9]/g, "_")
    : "c";
  const idPrefix = `${accessionToken}_${cultureToken}`;

  useEffect(() => {
    // Load organism names from dictionary category "Bacteria"
    getFromOpenElisServer("/rest/dictionary/category/Bacteria", (data) => {
      setOrganismNames(data || []);
    });

    // Load yeast names from dictionary category "Yeasts"
    getFromOpenElisServer("/rest/dictionary/category/Yeasts", (data) => {
      setYeastNames(data || []);
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

  // Special handling for the "Culture - Sécrétions vaginales" culture: when
  // the user picks 'Levure' the picker is restricted to Candida albicans and
  // auto-filled. For any other culture the picker stays free.
  //
  // Match the TEST name strictly at the start of the string. The frontend may
  // suffix the test name with the sample type (e.g.
  // "Culture - Col(Sécrétions vaginales) - Résultat"), so a substring match on
  // "sécrétions vaginales" wrongly triggered for Col too. The actual
  // "Culture - Sécrétions vaginales" test always begins with that literal.
  const normalizedCultureName = (cultureTestName || "")
    .toLowerCase()
    .trimStart();
  const isVaginalCulture = normalizedCultureName.startsWith(
    VAGINAL_CULTURE_TEST_NAME.toLowerCase(),
  );
  const shouldForceCandidaAlbicans = isYeast && isVaginalCulture;

  // Picker source : Yeasts quand Levure, sinon Bacteria. Pour le cas particulier
  // 'Culture - Sécrétions vaginales' + Levure, on force Candida albicans (le
  // picker se réduit à une seule entrée et est figé).
  const baseOrganismList = isYeast ? yeastNames : organismNames;
  // Resolve the Candida albicans entry (and thus its real dict id) from the
  // loaded list; fall back to the known id only if the name match fails.
  const candidaEntry = findCandidaAlbicans(baseOrganismList);
  const candidaDictId = candidaEntry
    ? parseInt(candidaEntry.id)
    : CANDIDA_ALBICANS_FALLBACK_DICT_ID;
  const displayedOrganismNames = shouldForceCandidaAlbicans
    ? candidaEntry
      ? [candidaEntry]
      : (baseOrganismList || []).filter(
          (o) => parseInt(o?.id) === CANDIDA_ALBICANS_FALLBACK_DICT_ID,
        )
    : baseOrganismList;

  useEffect(() => {
    if (!shouldForceCandidaAlbicans) return;
    const expected = candidaDictId;
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
  }, [shouldForceCandidaAlbicans, candidaDictId]);

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
              disabled={disabled || loading || shouldForceCandidaAlbicans}
              placeholder={
                shouldForceCandidaAlbicans
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
