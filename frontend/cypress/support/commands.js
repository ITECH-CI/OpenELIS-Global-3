// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add('login', (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })

Cypress.Commands.add("getElement", (selector) => {
  cy.wait(100)
    .get("body")
    .then(($body) => {
      if ($body.find(selector).length) {
        return cy.get(selector);
      } else {
        return null;
      }
    });
});

Cypress.Commands.add("enterText", (selector, value) => {
  return cy
    .get(selector)
    .should("exist")
    .and("be.visible")
    .clear()
    .type(value)
    .should("have.value", value);
});

// Résout une clé de traduction i18n vers le texte de la langue RÉELLEMENT active
// dans l'application (comme App.js : localStorage 'locale' sinon navigator.language,
// repli 'en'). Rend les assertions de texte indépendantes de la langue : au lieu de
// coder « Add Or Modify Patient » (qui casse en français), on écrit
// cy.t('patient.label.modify').then(txt => ...). Le fichier de langue est lu depuis
// les sources du frontend (src/languages/<lang>.json).
Cypress.Commands.add("t", (key) => {
  return cy.window({ log: false }).then((win) => {
    const lang = (
      win.localStorage.getItem("locale") ||
      (win.navigator.language || "en").split(/[-_]/)[0]
    ).toLowerCase();
    const load = (l) =>
      cy
        .readFile(`src/languages/${l}.json`, { log: false })
        .then((json) => (json && json[key] != null ? json[key] : null));
    return load(lang).then((txt) =>
      txt != null ? txt : lang === "en" ? key : load("en"),
    );
  });
});

// Sucre : vérifie qu'un élément contient le libellé traduit d'une clé i18n.
Cypress.Commands.add("containsKey", (selector, key) => {
  return cy.t(key).then((txt) => {
    if (selector) {
      return cy.get(selector).should("contain.text", txt);
    }
    return cy.contains(txt).should("be.visible");
  });
});
