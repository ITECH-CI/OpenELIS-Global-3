import * as Yup from "yup";

// Build the patient validation schema, making subjectNumber / nationalId
// conditionally required according to the site_information config flags
// (exposed by the backend as PATIENT_SUBJECT_NUMBER_REQUIRED /
// PATIENT_NATIONAL_ID_REQUIRED). Passing no config keeps every conditional
// field optional, which preserves the previous behavior for callers that have
// not yet wired the configuration in.
export const buildCreatePatientValidationSchema = (
  configurationProperties = {},
) => {
  const isRequired = (flag) => String(flag) === "true";

  let subjectNumber = Yup.string();
  if (isRequired(configurationProperties.PATIENT_SUBJECT_NUMBER_REQUIRED)) {
    subjectNumber = subjectNumber.required("Subject number required");
  }

  let nationalId = Yup.string();
  if (isRequired(configurationProperties.PATIENT_NATIONAL_ID_REQUIRED)) {
    nationalId = nationalId.required("National ID required");
  }

  return Yup.object().shape({
    subjectNumber,
    nationalId,
    birthDateForDisplay: Yup.string()
      .required("Patient Birth date Required")
      .test("valid-date", "Invalid date format", function (value) {
        const dateFormat = /^\d{2}\/\d{2}\/\d{4}$/;
        if (!value || !value.match(dateFormat)) {
          return false;
        }
        const [day, month, year] = value.split("/");
        const date = new Date(`${year}-${month}-${day}`);
        const date2 = new Date(`${year}-${day}-${month}`);

        const validDate1 = date instanceof Date && !isNaN(date);
        const validDate2 = date2 instanceof Date && !isNaN(date2);

        return validDate1 || validDate2;
      }),
    patientContact: Yup.object().shape({
      person: Yup.object().shape({
        email: Yup.string().email("Contact Email Must Be Valid"),
      }),
    }),
    gender: Yup.string().required("Gender is Required"),
  });
};

// Backward-compatible default export (no conditional required fields).
const CreatePatientValidationSchema = buildCreatePatientValidationSchema();

export default CreatePatientValidationSchema;
