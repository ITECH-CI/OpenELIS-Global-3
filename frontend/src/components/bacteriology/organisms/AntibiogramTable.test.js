import React from "react";
import { render } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import fr from "../../../languages/fr.json";
import AntibiogramTable from "./AntibiogramTable";

jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: (url, callback) => callback([]),
}));

describe("tableau d'antibiogramme", () => {
  it("en français : colonne « Antibiotique »", () => {
    const { getByText, queryByText } = render(
      <IntlProvider locale="fr" messages={fr}>
        <AntibiogramTable accessionNumber="DEMO26000001" onChange={() => {}} />
      </IntlProvider>,
    );
    expect(getByText("Antibiotique")).toBeTruthy();
    expect(queryByText("Antibiotic")).toBeNull();
  });
});
