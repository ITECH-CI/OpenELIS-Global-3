import PatientEntryPage from "./PatientEntryPage";

class OrderEntityPage {
  sampleTypeOptionDropDown = "";

  constructor() {}

  visit() {
    cy.visit("/AddOrder");
  }

  getPatientPage() {
    return new PatientEntryPage();
  }

  clickNextButton() {
    // Libellé selon la langue active (« Next » / « Suivant »...).
    cy.t("next.action.button").then((label) =>
      cy.contains("button", label).click(),
    );
  }

  // Sélectionne un programme par son ID d'option (langue-agnostique : la valeur de
  // l'option est l'id du programme, pas son libellé traduit). Par défaut le
  // programme « Analyses de routine » (id 2), toujours disponible et sans
  // contrainte d'échantillon particulière.
  selectProgram(programId = "2") {
    cy.get("#additionalQuestionsSelect", { timeout: 15000 })
      .find("option")
      .should("have.length.greaterThan", 1);
    cy.get("#additionalQuestionsSelect").select(programId);
  }

  // Le type d'échantillon est un ComboBox Carbon (input[role=combobox] +
  // .cds--list-box__menu[role=listbox]), pas un <select> natif. Un clic sur l'input
  // ouvre la liste complète ; on choisit ensuite l'item par son libellé exact (NE
  // PAS saisir de texte : le filtre peut vider la liste selon le libellé localisé).
  selectSampleTypeOption(sampleType) {
    cy.get("#sampleId_0", { timeout: 15000 }).should("be.visible").click();
    // Match EXACT du libellé (regex ancrée) pour éviter qu'un item plus long ne
    // soit choisi (ex. « Serum » vs « Serum ... »).
    cy.contains(
      ".cds--list-box__menu-item",
      new RegExp("^\\s*" + sampleType + "\\s*$"),
      { timeout: 10000 },
    ).click();
  }

  collectionDate(value) {
    cy.get("input#collectionDate_0").type(value);
  }

  requestDate(value) {
    cy.get("input#order_requestDate").type(value);
  }
  receivedDate(value) {
    cy.get("input#order_receivedDate").type(value);
  }
  // Les panneaux disponibles pour le type d'échantillon s'affichent en cases à
  // cocher (libellés « Bilan Biochimique », « Serologie VIH »... déjà en français
  // dans le catalogue CIV). On clique le libellé de la case.
  checkPanelCheckBoxField() {
    cy.contains("Bilan Biochimique", { timeout: 15000 })
      .should("be.visible")
      .click();
    cy.contains("Serologie VIH").should("be.visible").click();
  }

  referTest() {
    cy.t("label.refertest.referencelab").then((label) =>
      cy.contains("span", label).click(),
    );
  }

  selectInstitute() {
    cy.get("#referredInstituteId_0_1").select("CEDRES");
  }

  selectReferralReason() {
    cy.get("#referralReasonId_0_1").select("Test not performed");
  }
  generateLabOrderNumber() {
    cy.get("[data-cy='generate-labNumber']").click();
  }

  validateAcessionNumber(order) {
    cy.intercept("GET", `**/rest/SampleEntryAccessionNumberValidation**`).as(
      "accessionNoValidation",
    );
    cy.get("#labNo").type(order, { delay: 300 });

    cy.wait("@accessionNoValidation").then((interception) => {
      const responseBody = interception.response.body;

      console.log(responseBody);

      expect(responseBody.status).to.be.false;
    });
  }
  enterSiteName(siteName) {
    cy.get("input#siteName").clear().type(siteName);
    cy.contains(".suggestion-active", siteName).should("be.visible").click();
  }
  enterRequesterLastAndFirstName(
    fullName,
    requesterFirstName,
    requesterLastName,
  ) {
    cy.get("#requesterId").clear().type(fullName);
    cy.contains(".suggestion-active", fullName).click();
    cy.get("input#requesterFirstName").clear().type(requesterFirstName);
    cy.get("input#requesterLastName").clear().type(requesterLastName);
  }
  rememberSiteAndRequester() {
    cy.t("order.remember.site.and.requester.label").then((label) =>
      cy.contains("span", label).click(),
    );
  }
  clickSubmitOrderButton() {
    cy.t("label.button.submit").then((label) =>
      cy.contains("button", label).click(),
    );
  }
}

export default OrderEntityPage;
