import LoginPage from "../pages/LoginPage";
import AdminPage from "../pages/AdminPage";

let homePage = null;
let loginPage = null;
let adminPage = new AdminPage();
let orderEntityPage = null;
let patientEntryPage = null;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

// NB : l'assistant de création d'ordonnance du fork CIV enchaîne les étapes dans
// l'ordre Programme → Ordonnance (site/prescripteur/n° labo/dates) → Échantillon →
// Patient, puis Soumettre. Le test suit donc cet ordre (et non patient d'abord).
describe("Order Entity", function () {
  it("Navigate to Home Page then to Order entity Page ", function () {
    homePage = loginPage.goToHomePage();
    orderEntityPage = homePage.goToOrderPage();
    patientEntryPage = orderEntityPage.getPatientPage();
  });

  it("Step 1 - select program", function () {
    cy.wait(1000);
    orderEntityPage.selectProgram();
    cy.wait(200);
    orderEntityPage.clickNextButton();
  });

  it("Step 2 - order: site, requester, lab number and dates", function () {
    cy.wait(1000);
    cy.fixture("Order").then((order) => {
      orderEntityPage.enterSiteName(order.siteName);
      orderEntityPage.enterRequesterLastAndFirstName(
        order.requester.fullName,
        order.requester.firstName,
        order.requester.lastName,
      );
      orderEntityPage.rememberSiteAndRequester();
      order.samples.forEach((sample) => {
        orderEntityPage.requestDate(sample.receivedDate);
        orderEntityPage.receivedDate(sample.receivedDate);
      });
      orderEntityPage.generateLabOrderNumber();
    });
    orderEntityPage.clickNextButton();
  });

  it("Step 3 - sample type, panels, collection date and referral", function () {
    cy.wait(1000);
    cy.fixture("Order").then((order) => {
      order.samples.forEach((sample) => {
        orderEntityPage.selectSampleTypeOption(sample.sampleType);
        orderEntityPage.checkPanelCheckBoxField();
        orderEntityPage.collectionDate(sample.collectionDate);
      });
    });
    orderEntityPage.clickNextButton();
  });

  it("Step 4 - search and select the patient", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      patientEntryPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.lastName,
      );
      patientEntryPage.clickSearchPatientButton();
      patientEntryPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
      patientEntryPage.selectPatientFromSearchResults();
      cy.wait(300);
      patientEntryPage.getFirstName().should("have.value", patient.firstName);
      patientEntryPage.getLastName().should("have.value", patient.lastName);
    });
  });

  it("Step 5 - submit the order", function () {
    orderEntityPage.clickSubmitOrderButton();
    cy.wait(8000);
  });
});
