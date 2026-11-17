import React from "react";
import { fireEvent, render } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../languages/fr.json";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";
import Index from "./Index";

jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServer: jest.fn(),
}));
jest.mock("../common/PageBreadCrumb", () => () => null);
jest.mock("./OrderEntryAdditionalQuestions", () => () => (
  <div data-testid="stepProgram" />
));
jest.mock("./AddSample", () => () => <div data-testid="stepSample" />);
jest.mock("./PatientInfo", () => () => <div data-testid="stepPatient" />);
// L'étape « bulletin d'analyse » expose les champs marqués et sait se remplir.
jest.mock("./AddOrder", () => (props) => (
  <div data-testid="stepOrder">
    <span data-testid="marked">
      {Object.entries(props.changed)
        .filter(([, value]) => value)
        .map(([key]) => key)
        .sort()
        .join(",")}
    </span>
    <button
      onClick={() =>
        props.setOrderFormValues({
          ...props.orderFormValues,
          sampleOrderItems: {
            ...props.orderFormValues.sampleOrderItems,
            labNo: "DEMO26000001",
            referringSiteName: "CSU DEMO",
            providerLastName: "DEMO",
            providerFirstName: "Prescripteur",
          },
        })
      }
    >
      remplir
    </button>
  </div>
));

const renderWizard = () =>
  render(
    <IntlProvider locale="fr" messages={messages}>
      <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: () => {},
            addNotification: () => {},
          }}
        >
          <Index />
        </NotificationContext.Provider>
      </ConfigurationContext.Provider>
    </IntlProvider>,
  );

const next = (getByText) =>
  fireEvent.click(getByText(messages["next.action.button"]));

describe("assistant d'ordonnance", () => {
  it("étape « bulletin d'analyse » vide : « Suivante » reste sur l'étape et marque les quatre champs", () => {
    const { getByText, getByTestId, queryByTestId } = renderWizard();
    next(getByText);
    expect(getByTestId("stepOrder")).toBeTruthy();
    next(getByText);
    expect(getByTestId("stepOrder")).toBeTruthy();
    expect(queryByTestId("stepSample")).toBeNull();
    expect(getByTestId("marked").textContent).toBe(
      [
        "sampleOrderItems.labNo",
        "sampleOrderItems.providerFirstName",
        "sampleOrderItems.providerLastName",
        "sampleOrderItems.referringSiteName",
      ].join(","),
    );
  });

  it("étape remplie : « Suivante » passe à l'échantillon", () => {
    const { getByText, getByTestId } = renderWizard();
    next(getByText);
    fireEvent.click(getByText("remplir"));
    next(getByText);
    expect(getByTestId("stepSample")).toBeTruthy();
  });
});
