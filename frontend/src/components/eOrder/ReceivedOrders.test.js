import React from "react";
import { render, wait, within } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import ReceivedOrders, { formatAge } from "./ReceivedOrders";
import { NotificationContext } from "../layout/Layout";
import messages from "../../languages/fr.json";
import { getFromOpenElisServer } from "../utils/Utils";

jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServer: jest.fn(),
}));
jest.mock("../common/PageBreadCrumb", () => () => null);
jest.mock("../common/CustomDatePicker", () => () => null);

// Patients fictifs uniquement (DEMO, numéros CMU 9992611…).
const row = (id, extra = {}) => ({
  id: String(id),
  taskId: "bdms-" + id,
  requestNumber: "bdms-" + id,
  requestDate: "12/11/2026 08:00",
  receivedAtMillis: 0,
  ageMinutes: 90,
  priority: "ROUTINE",
  patientFirstName: "Patient",
  patientLastName: "DEMO",
  cmuNumber: "9992611000001",
  provisionalIdentity: false,
  requestingFacility: "CSU DEMO",
  prescriber: "DEMO Prescripteur",
  origine: "PLATEFORME",
  etape: "A_TRAITER",
  cancelledByRequester: false,
  labNumber: null,
  readOnly: false,
  exams: [
    {
      electronicOrderId: String(id),
      externalOrderId: "bdms-sr-" + id,
      testName: "Charge virale VIH",
    },
  ],
  ...extra,
});

const serve = (page) => {
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.startsWith("/rest/ReceivedOrders")) {
      callback(page);
    } else {
      callback([]);
    }
  });
};

const renderPage = () =>
  render(
    <IntlProvider locale="fr" messages={messages}>
      <NotificationContext.Provider
        value={{
          notificationVisible: false,
          setNotificationVisible: () => {},
          addNotification: () => {},
        }}
      >
        <ReceivedOrders refreshMs={3600000} />
      </NotificationContext.Provider>
    </IntlProvider>,
  );

describe("fenêtre « Demandes reçues »", () => {
  beforeEach(() => getFromOpenElisServer.mockReset());

  it("liste vide : message, aucune ligne", async () => {
    serve({ rows: [], total: 0, page: 1, pageSize: 25 });
    const { getByTestId, queryAllByTestId } = renderPage();
    await wait(() => expect(getByTestId("receivedOrdersEmpty")).toBeTruthy());
    expect(queryAllByTestId("receivedOrderRow")).toHaveLength(0);
  });

  it("ouverte d'office sur les demandes à traiter", async () => {
    serve({ rows: [], total: 0, page: 1, pageSize: 25 });
    renderPage();
    await wait(() =>
      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        expect.stringContaining("etape=A_TRAITER"),
        expect.any(Function),
      ),
    );
  });

  it("liste paginée : la page reçue et la pagination", async () => {
    const rows = Array.from({ length: 25 }, (_, i) => row(i + 1));
    serve({ rows, total: 30, page: 1, pageSize: 25 });
    const { queryAllByTestId, getByText } = renderPage();
    await wait(() =>
      expect(queryAllByTestId("receivedOrderRow")).toHaveLength(25),
    );
    expect(getByText(/1–25 sur 30|1-25 of 30|30/)).toBeTruthy();
  });

  it("patient sans numéro CMU : badge « identité provisoire »", async () => {
    serve({
      rows: [row(1, { cmuNumber: null, provisionalIdentity: true })],
      total: 1,
      page: 1,
      pageSize: 25,
    });
    const { getByText } = renderPage();
    await wait(() => expect(getByText("Identité provisoire")).toBeTruthy());
  });

  it("demande annulée par le demandeur : étape affichée, sans action de prise en charge", async () => {
    serve({
      rows: [
        row(1, {
          etape: "ANNULEE_DEMANDEUR",
          cancelledByRequester: true,
          readOnly: true,
        }),
      ],
      total: 1,
      page: 1,
      pageSize: 25,
    });
    const { getByTestId, queryByText } = renderPage();
    await wait(() =>
      expect(
        within(getByTestId("receivedOrderRow")).getByText(
          "Annulée par le demandeur",
        ),
      ).toBeTruthy(),
    );
    expect(queryByText("Prendre en charge")).toBeNull();
  });

  it("demande prise en charge : lecture seule, numéro de laboratoire affiché", async () => {
    serve({
      rows: [
        row(1, {
          etape: "PRISE_EN_CHARGE",
          labNumber: "DEMO26000001",
          readOnly: true,
        }),
      ],
      total: 1,
      page: 1,
      pageSize: 25,
    });
    const { getByText, queryByText } = renderPage();
    await wait(() => expect(getByText("DEMO26000001")).toBeTruthy());
    expect(queryByText("Refuser")).toBeNull();
  });

  it("ancienneté lisible", () => {
    const intl = {
      formatMessage: ({ id }, { value }) => id.split(".").pop() + ":" + value,
    };
    expect(formatAge(5, intl)).toBe("minutes:5");
    expect(formatAge(125, intl)).toBe("hours:2");
    expect(formatAge(3000, intl)).toBe("days:2");
  });
});
