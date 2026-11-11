// Numéro CMU de l'assuré (= matricule CNAM = n° de sécurité sociale).
// Normalisation : espaces, tirets et points retirés ; format : 13 chiffres.
// Même règle que le serveur (org.openelisglobal.patient.util.CmuNumber).
const CMU_FORMAT = /^[0-9]{13}$/;

export const normalizeCmu = (value) =>
  value == null ? "" : String(value).replace(/[\s.-]/g, "");

// Vide accepté ; sinon 13 chiffres après normalisation.
export const isCmuAccepted = (value) => {
  const normalized = normalizeCmu(value);
  return normalized === "" || CMU_FORMAT.test(normalized);
};

// Valeur à envoyer : normalisée si conforme, inchangée sinon.
export const cmuForSubmit = (value) => {
  const normalized = normalizeCmu(value);
  return CMU_FORMAT.test(normalized) ? normalized : value;
};
