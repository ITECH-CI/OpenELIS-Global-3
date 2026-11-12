// Examens d'une demande électronique à placer par l'opérateur : plusieurs
// types d'échantillon sont possibles et la demande n'en indique aucun
// (LabOrderSearchProvider, élément examChoices).

const asArray = (value) =>
  value == null || value === "" ? [] : Array.isArray(value) ? value : [value];

/** Choix reçus du serveur, sous forme de tableau [{ name, options: [...] }]. */
export const normalizeExamChoices = (order) => {
  if (!order || !order.examChoices) {
    return [];
  }
  return asArray(order.examChoices.examChoice)
    .map((choice) => ({
      name: choice.name != null ? String(choice.name) : "",
      options: asArray(choice.option).map((option) => ({
        kind: option.kind === "panel" ? "panel" : "test",
        id: String(option.id),
        label: option.label != null ? String(option.label) : "",
        sampleTypeId: String(option.sampleTypeId),
        sampleTypeName:
          option.sampleTypeName != null ? String(option.sampleTypeName) : "",
        testIds: option.testIds != null ? String(option.testIds) : "",
        testNames: option.testNames != null ? String(option.testNames) : "",
      })),
    }))
    .filter((choice) => choice.options.length > 0);
};

/**
 * Place les options choisies dans les échantillons du bon : l'examen est ajouté
 * à l'échantillon du type choisi, créé s'il n'existe pas encore.
 */
export const applyExamChoices = (samples, selectedOptions, newSampleType) => {
  const updated = samples.map((sample) => ({
    ...sample,
    tests: [...(sample.tests || [])],
    panels: [...(sample.panels || [])],
  }));
  selectedOptions.filter(Boolean).forEach((option) => {
    let sample = updated.find(
      (s) => String(s.sampleTypeId) === option.sampleTypeId,
    );
    if (!sample) {
      sample = newSampleType(
        option.sampleTypeId,
        option.sampleTypeName,
        updated.length + 1,
      );
      sample.rowid = updated.length + 1;
      sample.tests = [...(sample.tests || [])];
      sample.panels = [...(sample.panels || [])];
      updated.push(sample);
    }
    if (option.kind === "panel") {
      if (!sample.panels.some((p) => String(p.id) === option.id)) {
        sample.panels.push({
          id: option.id,
          name: option.label,
          tests: option.testNames,
          testIds: option.testIds,
        });
      }
    } else if (!sample.tests.some((t) => String(t.id) === option.id)) {
      sample.tests.push({ id: option.id, name: option.label });
    }
  });
  return updated;
};
