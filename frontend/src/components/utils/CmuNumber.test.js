import { cmuForSubmit, isCmuAccepted, normalizeCmu } from "./CmuNumber";
import CreatePatientValidationSchema, {
  buildCreatePatientValidationSchema,
} from "../formModel/validationSchema/CreatePatientValidationShema";

// Numéros fictifs uniquement (préfixe 9992611).
const patient = (nationalId, extra = {}) => ({
  nationalId,
  birthDateForDisplay: "17/05/1990",
  gender: "F",
  ...extra,
});

const cmuError = (schema, values) =>
  schema
    .validate(values, { abortEarly: false })
    .then(() => null)
    .catch((e) => e.inner.find((i) => i.path === "nationalId") || null);

describe("numéro CMU", () => {
  it("normalise espaces, tirets et points", () => {
    expect(normalizeCmu(" 999 2611-000.001 ")).toBe("9992611000001");
    expect(cmuForSubmit("999-2611-000-001")).toBe("9992611000001");
    expect(cmuForSubmit("DEMO")).toBe("DEMO");
  });

  it("accepte vide ou 13 chiffres", () => {
    expect(isCmuAccepted("")).toBe(true);
    expect(isCmuAccepted(undefined)).toBe(true);
    expect(isCmuAccepted("9992611000001")).toBe(true);
    expect(isCmuAccepted("999261100000")).toBe(false);
    expect(isCmuAccepted("999261100000A")).toBe(false);
  });
});

describe("formulaire patient : contrôle du numéro CMU", () => {
  const schema = buildCreatePatientValidationSchema(
    {},
    { cmuInvalid: "CMU invalide" },
  );

  it("valeur conforme : acceptée", async () => {
    expect(await cmuError(schema, patient("999 2611 000 001"))).toBeNull();
  });

  it("valeur non conforme : refusée avec un message clair", async () => {
    const error = await cmuError(schema, patient("999261100000"));
    expect(error).not.toBeNull();
    expect(error.message).toBe("CMU invalide");
  });

  it("champ vide : accepté", async () => {
    expect(await cmuError(schema, patient(""))).toBeNull();
  });

  it("patient déjà enregistré : contrôlé dans le formulaire patient", async () => {
    expect(
      await cmuError(schema, patient("999261100000", { patientPK: "12" })),
    ).not.toBeNull();
  });

  it("saisie de demande : un patient existant n'est pas bloqué", async () => {
    expect(
      await cmuError(
        CreatePatientValidationSchema,
        patient("999261100000", { patientPK: "12" }),
      ),
    ).toBeNull();
    expect(
      await cmuError(CreatePatientValidationSchema, patient("999261100000")),
    ).not.toBeNull();
  });
});
