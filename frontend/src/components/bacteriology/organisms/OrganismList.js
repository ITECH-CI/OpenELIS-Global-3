import React from "react";
import { Button, Stack } from "@carbon/react";
import { Add, TrashCan } from "@carbon/icons-react";
import { FormattedMessage } from "react-intl";
import { MAX_ORGANISMS, ORGANISM_TYPES } from "../BacteriologyConstants";
import OrganismIdentification from "./OrganismIdentification";

const OrganismList = ({
  accessionNumber,
  organisms = [],
  onChange,
  disabled = false,
  skipOrganismIdentification = false,
}) => {
  // For Neisseria cultures: auto-create a single organism if none exists
  React.useEffect(() => {
    if (skipOrganismIdentification && organisms.length === 0) {
      const neiszeriaOrganism = {
        id: null,
        organismGroupId: null,
        organismNumber: 1,
        organismType: ORGANISM_TYPES.BACTERIA,
        organismNameDictId: null,
        organismNameText: "Neisseria gonorrhoeae",
        gramType: "",
        groupingMode: "",
        capsulePresence: false,
        otherCharacteristics: "",
        antibiograms: [],
      };
      onChange([neiszeriaOrganism]);
    }
  }, [skipOrganismIdentification, organisms.length, onChange]);

  const handleAddOrganism = () => {
    if (organisms.length >= MAX_ORGANISMS) {
      return;
    }

    const newOrganism = {
      id: null,
      organismGroupId: null,
      organismNumber: organisms.length + 1,
      organismType: ORGANISM_TYPES.BACTERIA,
      organismNameDictId: null,
      organismNameText: "",
      gramType: "",
      groupingMode: "",
      capsulePresence: false,
      otherCharacteristics: "",
      antibiograms: [],
    };

    onChange([...organisms, newOrganism]);
  };

  const handleRemoveOrganism = (index) => {
    const updated = organisms.filter((_, i) => i !== index);
    // Renumber remaining organisms
    const renumbered = updated.map((org, i) => ({
      ...org,
      organismNumber: i + 1,
    }));
    onChange(renumbered);
  };

  const handleOrganismChange = (index, updatedOrganism) => {
    const updated = [...organisms];
    updated[index] = updatedOrganism;
    onChange(updated);
  };

  // For Neisseria: max 1 organism, otherwise MAX_ORGANISMS
  const maxOrganisms = skipOrganismIdentification ? 1 : MAX_ORGANISMS;
  const canAddMore = organisms.length < maxOrganisms;

  return (
    <div className="organism-list">
      <Stack gap={4}>
        {/* Only show header with add button if NOT skipping identification */}
        {!skipOrganismIdentification && (
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <h3>
              <FormattedMessage id="bacteriology.organisms.title" />
            </h3>
            <Button
              size="sm"
              kind="tertiary"
              renderIcon={Add}
              onClick={handleAddOrganism}
              disabled={disabled || !canAddMore}
            >
              <FormattedMessage id="bacteriology.organism.add" />
              {` (${organisms.length}/${maxOrganisms})`}
            </Button>
          </div>
        )}

        {organisms.length === 0 && !skipOrganismIdentification ? (
          <div style={{ padding: "2rem", textAlign: "center", color: "#888" }}>
            <FormattedMessage id="bacteriology.organisms.empty" />
          </div>
        ) : (
          organisms.map((organism, index) => (
            <div key={index} style={{ position: "relative" }}>
              {/* Only show delete button if NOT skipping identification */}
              {!skipOrganismIdentification && (
                <Button
                  size="sm"
                  kind="ghost"
                  hasIconOnly
                  renderIcon={TrashCan}
                  iconDescription="Supprimer ce germe"
                  onClick={() => handleRemoveOrganism(index)}
                  disabled={disabled}
                  style={{ position: "absolute", top: "1rem", right: "1rem" }}
                />
              )}
              <OrganismIdentification
                accessionNumber={accessionNumber}
                organismNumber={organism.organismNumber}
                organism={organism}
                onChange={(updated) => handleOrganismChange(index, updated)}
                disabled={disabled}
                skipOrganismIdentification={skipOrganismIdentification}
              />
            </div>
          ))
        )}
      </Stack>
    </div>
  );
};

export default OrganismList;
