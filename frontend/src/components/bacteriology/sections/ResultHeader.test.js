import React from "react";
import { render } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import fr from "../../../languages/fr.json";
import en from "../../../languages/en.json";
import ResultHeader from "./ResultHeader";
import CustomSelect from "../../common/CustomSelect";

// Patient fictif uniquement.
const testResult = {
  accessionNumber: "DEMO26000001",
  patientInfo: "DEMO Patient",
  sampleType: "Sang",
  priority: "Normal",
  testSectionName: "Routine Bacteriology",
};

const renderIn = (locale, messages, node) =>
  render(
    <IntlProvider locale={locale} messages={messages}>
      {node}
    </IntlProvider>,
  );

describe("carte d'échantillon de bactériologie", () => {
  it("en français : « Patient : », unité traduite, deux-points espacés", () => {
    const { container } = renderIn(
      "fr",
      fr,
      <ResultHeader testResult={testResult} totalTests={1} />,
    );
    const text = container.textContent;
    expect(text).toContain("Patient :");
    expect(text).not.toContain("Patiente");
    expect(text).toContain("Unité d'analyse : Bactériologie de routine");
    expect(text).not.toContain("Routine Bacteriology");
  });

  it("en anglais : deux-points collés", () => {
    const { container } = renderIn(
      "en",
      en,
      <ResultHeader testResult={testResult} totalTests={1} />,
    );
    expect(container.textContent).toContain("Patient:");
  });

  it("liste déroulante sans libellé de repli fourni : « Sélectionner... »", () => {
    const { getByText } = renderIn(
      "fr",
      fr,
      <CustomSelect id="demo" onChange={() => {}} options={[]} />,
    );
    expect(getByText("Sélectionner...")).toBeTruthy();
  });
});
