import React, { useEffect, useState } from "react";
import { Modal, Select, SelectItem } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

// Demande l'échantillon de chaque examen qui en admet plusieurs. Un seul type
// possible : l'examen est déjà placé et n'apparaît pas ici.
const ExamChoiceModal = ({ choices, onConfirm, onClose }) => {
  const intl = useIntl();
  const [selected, setSelected] = useState([]);

  useEffect(() => {
    setSelected(choices.map(() => ""));
  }, [choices]);

  const complete = selected.length > 0 && selected.every((value) => value);

  return (
    <Modal
      open={choices.length > 0}
      modalHeading={intl.formatMessage({ id: "examChoice.heading" })}
      primaryButtonText={intl.formatMessage({ id: "label.button.confirm" })}
      secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
      primaryButtonDisabled={!complete}
      onRequestClose={onClose}
      onRequestSubmit={() =>
        onConfirm(
          choices.map((choice, index) =>
            choice.options.find(
              (option, optionIndex) => String(optionIndex) === selected[index],
            ),
          ),
        )
      }
    >
      <p style={{ marginBottom: "1rem" }}>
        <FormattedMessage id="examChoice.explanation" />
      </p>
      {choices.map((choice, index) => (
        <Select
          key={"examChoice" + index}
          id={"examChoice" + index}
          labelText={choice.name}
          value={selected[index] || ""}
          onChange={(e) => {
            const next = [...selected];
            next[index] = e.target.value;
            setSelected(next);
          }}
        >
          <SelectItem value="" text="" />
          {choice.options.map((option, optionIndex) => (
            <SelectItem
              key={optionIndex}
              value={String(optionIndex)}
              text={option.label + " — " + option.sampleTypeName}
            />
          ))}
        </Select>
      ))}
    </Modal>
  );
};

export default ExamChoiceModal;
