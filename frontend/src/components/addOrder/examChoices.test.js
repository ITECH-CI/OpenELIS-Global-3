import { applyExamChoices, normalizeExamChoices } from "./examChoices";

const sampleType = (id, name, index) => ({
  index,
  id: String(id),
  sampleTypeId: String(id),
  name,
  tests: [],
  panels: [],
  sampleXML: {},
});

describe("examens à placer (demande électronique)", () => {
  it("un seul choix reçu (objet) devient un tableau", () => {
    const choices = normalizeExamChoices({
      examChoices: {
        examChoice: {
          name: "Charge virale",
          option: {
            kind: "test",
            id: 12,
            label: "Charge virale VIH",
            sampleTypeId: 3,
            sampleTypeName: "Plasma",
          },
        },
      },
    });
    expect(choices).toHaveLength(1);
    expect(choices[0].options).toHaveLength(1);
    expect(choices[0].options[0].id).toBe("12");
  });

  it("aucun choix : tableau vide", () => {
    expect(normalizeExamChoices({ examChoices: "" })).toEqual([]);
    expect(normalizeExamChoices({})).toEqual([]);
  });

  it("deux types possibles : l'examen est placé dans l'échantillon choisi, créé si besoin", () => {
    const samples = [sampleType(1, "Sang total", 1)];
    const updated = applyExamChoices(
      samples,
      [
        {
          kind: "test",
          id: "12",
          label: "Charge virale VIH",
          sampleTypeId: "3",
          sampleTypeName: "Plasma",
        },
      ],
      sampleType,
    );
    expect(updated).toHaveLength(2);
    expect(updated[1].sampleTypeId).toBe("3");
    expect(updated[1].tests).toEqual([{ id: "12", name: "Charge virale VIH" }]);
    // l'original n'est pas modifié
    expect(samples).toHaveLength(1);
  });

  it("type déjà présent : ajouté une seule fois", () => {
    const samples = [sampleType(3, "Plasma", 1)];
    const option = {
      kind: "test",
      id: "12",
      label: "Charge virale VIH",
      sampleTypeId: "3",
      sampleTypeName: "Plasma",
    };
    const updated = applyExamChoices(samples, [option, option], sampleType);
    expect(updated).toHaveLength(1);
    expect(updated[0].tests).toHaveLength(1);
  });

  it("panel : ses tests suivent", () => {
    const updated = applyExamChoices(
      [],
      [
        {
          kind: "panel",
          id: "7",
          label: "Bilan hépatique",
          sampleTypeId: "4",
          sampleTypeName: "Sérum",
          testIds: "20,21",
          testNames: "ASAT,ALAT",
        },
      ],
      sampleType,
    );
    expect(updated[0].panels).toEqual([
      {
        id: "7",
        name: "Bilan hépatique",
        tests: "ASAT,ALAT",
        testIds: "20,21",
      },
    ]);
  });
});
