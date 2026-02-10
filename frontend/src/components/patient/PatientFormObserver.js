import { useFormikContext } from "formik";
import { useEffect } from "react";

const bacterioFields = [
  "currentHospitalization",
  "roomNumber",
  "clinicalInformations",
  "clinicalInformationOther",
  "recentAntibiotherapy",
  "recentAntibiotherapyList",
  "currentAntibiotherapy",
  "currentAntibiotherapyList",
  "currentAntibiotherapyDuration",
  "recentHospitalization",
  "recentHospitalizationCount",
  "recentInvasiveGestures",
  "indwellingDevice",
];

const PatientFormObserver = (props) => {
  const { values } = useFormikContext();
  const { orderFormValues, setOrderFormValues, formAction } = props;

  // Split bacteriology routine fields into a dedicated object to avoid persisting
  // them as patient core properties.
  const extractBacterioInfo = (formValues) => {
    const patientRoutineBacterioInfo = {};
    const patientProps = { ...formValues };
    bacterioFields.forEach((field) => {
      patientRoutineBacterioInfo[field] = formValues[field];
      delete patientProps[field];
      // Also delete the -input variant created by FilterableMultiSelect
      delete patientProps[field + "-input"];
    });
    return { patientRoutineBacterioInfo, patientProps };
  };

  useEffect(() => {
    const { patientRoutineBacterioInfo, patientProps } =
      extractBacterioInfo(values);
    setOrderFormValues({
      ...orderFormValues,
      patientUpdateStatus: formAction,
      patientProperties: patientProps,
      patientRoutineBacterioInfo: {
        ...orderFormValues.patientRoutineBacterioInfo,
        ...patientRoutineBacterioInfo,
      },
    });
  }, [values]);
  return null;
};

export default PatientFormObserver;
