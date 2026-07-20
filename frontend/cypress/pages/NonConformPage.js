class NonConform {
  constructor() {
    this.selectors = {
      //centralized selectors
      title: "h2",
      searchType: "#type",
      searchField: "[data-cy='fieldName']",
      searchButton: "[data-testid='nce-search-button']",
      searchResult: "[data-testid='nce-search-result']",
      nceNumberResult: "[data-testid='nce-number-result']",
      sampleCheckbox: "[data-testid='nce-sample-checkbox']",
      goToFormButton: "[data-testid='nce-goto-form-button']",
      startDate: "input#startDate",
      reportingUnits: "#reportingUnits",
      description: "#text-area-1",
      suspectedCause: "#text-area-2",
      correctiveActionText: "#text-area-3",
      descriptionAndComments: "#text-area-10",
      nceCategory: "#nceCategory",
      nceType: "#nceType",
      consequences: "#consequences",
      recurrence: "#recurrence",
      labComponent: "#labComponent",
      discussionDate: "#tdiscussionDate",
      proposedCorrectiveAction: "#text-area-corrective",
      dateCompleted: "#dateCompleted",
      actionTypeCheckbox: "#correctiveAction",
      resolutionYes: "span:contains('Yes')",
      dateCompleted0: ".cds--date-picker-input__wrapper > #dateCompleted-0",
      submitButton: "[data-testid='nce-submit-button']",
      radioTable: "table",
      radioButton: 'input[type="radio"][name="radio-group"]',
      successToast: ".cds--toast-notification--success",
      errorToast: ".cds--toast-notification--error",
    };
  }

  getReportNonConformTitle() {
    return cy.get(this.selectors.title);
  }

  getViewNonConformTitle() {
    return cy.get(this.selectors.title);
  }

  selectSearchType(type) {
    cy.get(this.selectors.searchType, { timeout: 15000 })
      .should("be.visible")
      .select(type);
  }

  enterSearchField(value) {
    cy.get(this.selectors.searchField).type(value);
  }

  clickSearchButton() {
    cy.get(this.selectors.searchButton, { timeout: 15000 })
      .should("be.visible")
      .click();
  }

  validateSearchResult(expectedValue) {
    cy.get(this.selectors.searchResult)
      .first()
      .invoke("text")
      .should("eq", expectedValue);
  }

  validateLabNoSearchResult(labNo) {
    cy.get(this.selectors.searchResult).invoke("text").should("eq", labNo);
  }

  // Vérifie simplement qu'AU MOINS un résultat de recherche est présent (sans
  // coder en dur un numéro de laboratoire qui varie d'un environnement à l'autre).
  validateHasSearchResult() {
    cy.get(this.selectors.searchResult, { timeout: 15000 })
      .first()
      .should("be.visible")
      .invoke("text")
      .should("have.length.greaterThan", 0);
  }

  // Écrans View/Corrective : quand la recherche par n° labo renvoie PLUSIEURS
  // événements (cas fréquent en local), ils s'affichent en tableau avec des radios ;
  // avec un seul résultat, le formulaire se charge directement. On sélectionne le
  // 1er radio s'il est présent pour couvrir les deux cas.
  selectFirstResultRadioIfPresent() {
    cy.get("body").then(($body) => {
      if ($body.find(this.selectors.radioButton).length) {
        cy.get(this.selectors.radioButton).first().click({ force: true });
        // Le clic déclenche le chargement du formulaire de suivi (GET) : on attend
        // que le champ catégorie apparaisse avant de poursuivre.
        cy.get(this.selectors.nceCategory, { timeout: 15000 }).should("exist");
      }
    });
  }

  // Capture le numéro de laboratoire réel du 1er résultat et le sauvegarde dans la
  // fixture Patient (clé labNo), pour que les recherches ultérieures par n° labo
  // utilisent une valeur qui existe vraiment dans l'environnement courant.
  captureLabNoFromSearch() {
    cy.get(this.selectors.searchResult, { timeout: 15000 })
      .first()
      .invoke("text")
      .then((text) => {
        const labNo = text.trim().split(/\s+/)[0];
        cy.readFile("cypress/fixtures/Patient.json").then((existing) => {
          cy.writeFile("cypress/fixtures/Patient.json", {
            ...existing,
            labNo,
          });
        });
      });
  }

  validateNCESearchResult(NCENo) {
    cy.get(this.selectors.nceNumberResult).invoke("text").should("eq", NCENo);
  }

  clickCheckbox() {
    cy.get(this.selectors.sampleCheckbox)
      .should("be.visible")
      .check({ force: true });
  }

  clickGoToNceFormButton() {
    cy.get(this.selectors.goToFormButton).should("be.visible").click();
  }

  enterStartDate(date) {
    cy.get(this.selectors.startDate).type(date);
  }

  selectReportingUnit(unit) {
    cy.get(this.selectors.reportingUnits).select(unit);
  }

  enterDescription(description) {
    cy.get(this.selectors.description).type(description);
  }

  enterSuspectedCause(suspectedCause) {
    cy.get(this.selectors.suspectedCause).type(suspectedCause);
  }

  enterCorrectiveAction(correctiveAction) {
    cy.get(this.selectors.correctiveActionText).type(correctiveAction);
  }

  enterNceCategory(nceCategory) {
    cy.get(this.selectors.nceCategory).select(nceCategory);
  }

  enterNceType(nceType) {
    cy.get(this.selectors.nceType).select(nceType);
  }

  enterConsequences(consequences) {
    cy.get(this.selectors.consequences).select(consequences);
  }

  enterRecurrence(recurrence) {
    cy.get(this.selectors.recurrence).select(recurrence);
  }

  enterLabComponent(labComponent) {
    cy.get(this.selectors.labComponent).select(labComponent);
  }

  enterDescriptionAndComments(testText) {
    cy.get(this.selectors.descriptionAndComments).type(testText);
    cy.get(this.selectors.correctiveActionText).type(testText);
    cy.get(this.selectors.suspectedCause).type(testText);
  }

  enterDiscussionDate(date) {
    cy.get(this.selectors.discussionDate).type(date);
  }

  enterProposedCorrectiveAction(action) {
    cy.get(this.selectors.proposedCorrectiveAction)
      .should("not.be.disabled")
      .type(action, { force: true });
  }

  enterDateCompleted(date) {
    cy.get(this.selectors.dateCompleted).type(date);
  }

  selectActionType() {
    cy.get(this.selectors.actionTypeCheckbox).check({ force: true });
  }

  checkResolution() {
    // Radio « Oui » de la résolution (libellé selon la langue active).
    cy.t("yes.option").then((label) =>
      cy.contains("span", new RegExp("^\\s*" + label + "\\s*$")).click(),
    );
  }

  enterDateCompleted0(date) {
    cy.get(this.selectors.dateCompleted0).type(date);
  }

  submitForm() {
    cy.get(this.selectors.submitButton).click();
  }

  clickSubmitButton() {
    cy.get(this.selectors.submitButton).should("be.visible").click();
  }

  // Vérifie qu'un enregistrement (action corrective / résolution) a bien abouti :
  // un toast de succès apparaît et AUCUN toast d'erreur. Régression ciblée : la
  // résolution renvoyait un HTTP 500 (currentUserId absent) et l'événement restait
  // bloqué en CAPA sans que l'UI ne le signale.
  assertSaveSuccess() {
    cy.get(this.selectors.successToast, { timeout: 15000 }).should(
      "be.visible",
    );
    cy.get(this.selectors.errorToast).should("not.exist");
  }

  checkRadioButton() {
    cy.get(this.selectors.radioTable).should("be.visible");
    cy.get(this.selectors.radioButton).should("exist");
    return cy
      .get("tbody tr")
      .first()
      .within(() => {
        cy.get(this.selectors.radioButton)
          .should("exist")
          .click({ force: true });
      });
  }

  getAndSaveNceNumber() {
    cy.get(this.selectors.nceNumberResult)
      .invoke("text")
      .then((text) => {
        cy.readFile("cypress/fixtures/NonConform.json").then((existingData) => {
          const newData = { ...existingData, NceNumber: text.trim() };
          cy.writeFile("cypress/fixtures/NonConform.json", newData);
        });
      });
  }
}

export default NonConform;
