import React from "react";
import { render, wait } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import ReceivedOrdersPage from "./ReceivedOrdersPage";
import { NotificationContext } from "../layout/Layout";
import messages from "../../languages/fr.json";
import { getFromOpenElisServer } from "../utils/Utils";

jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServer: jest.fn(),
}));
jest.mock("../common/PageBreadCrumb", () => () => null);
jest.mock("../common/CustomDatePicker", () => () => null);

const serve = (settings) =>
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.startsWith("/rest/LocalOrderEntrySettings")) {
      callback(settings);
    } else if (url.startsWith("/rest/ReceivedOrders")) {
      callback({ rows: [], total: 0, page: 1, pageSize: 25 });
    } else {
      callback([]);
    }
  });

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
        <ReceivedOrdersPage />
      </NotificationContext.Provider>
    </IntlProvider>,
  );

describe("« Nouvelle demande »", () => {
  beforeEach(() => getFromOpenElisServer.mockReset());

  it("réglage à false : bouton absent", async () => {
    serve({ enabled: false, permitted: false });
    const { queryByTestId, getByTestId } = renderPage();
    await wait(() => expect(getByTestId("receivedOrdersEmpty")).toBeTruthy());
    expect(queryByTestId("newLocalOrder")).toBeNull();
  });

  it("réglage activé mais utilisateur sans le droit : bouton absent", async () => {
    serve({ enabled: true, permitted: false });
    const { queryByTestId, getByTestId } = renderPage();
    await wait(() => expect(getByTestId("receivedOrdersEmpty")).toBeTruthy());
    expect(queryByTestId("newLocalOrder")).toBeNull();
  });

  it("réglage activé et droit « Enregistrer des demandes » : bouton présent", async () => {
    serve({ enabled: true, permitted: true });
    const { getByTestId } = renderPage();
    await wait(() => expect(getByTestId("newLocalOrder")).toBeTruthy());
  });
});
