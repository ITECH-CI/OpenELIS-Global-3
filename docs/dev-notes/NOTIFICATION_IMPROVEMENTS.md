# Améliorations des Notifications d'Erreur

## Problèmes Résolus

### 1. Notifications qui s'empilent

**Problème**: À chaque nouvelle erreur, les notifications s'empilaient sans
effacer les anciennes.

**Solution**: Modification de `Layout.js` pour effacer les notifications
précédentes avant d'ajouter une nouvelle.

```javascript
// Avant
const addNotification = (notificationBody) => {
  setNotifications([...notifications, notificationBody]);
};

// Après
const addNotification = (notificationBody) => {
  // Clear previous notifications before adding new one to prevent stacking
  setNotifications([notificationBody]);
};
```

### 2. Messages d'erreur génériques

**Problème**: Lors d'une erreur de sauvegarde d'ordonnance, seul un message
générique était affiché sans détails sur la cause de l'erreur.

**Solution**: Modifications apportées pour afficher les messages d'erreur
détaillés provenant du backend.

## Fichiers Modifiés

### 1. [frontend/src/components/utils/Utils.js](frontend/src/components/utils/Utils.js)

**Modification de `postToOpenElisServer`** pour retourner le corps de la réponse
en plus du code HTTP.

**Avant**:

```javascript
.then((response) => response.status)
.then((status) => {
  callback(status, extraParams);
})
```

**Après**:

```javascript
.then(async (response) => {
  const status = response.status;
  let body = null;

  // Try to parse response body as JSON if available
  const contentType = response.headers.get("content-type");
  if (contentType && contentType.indexOf("application/json") !== -1) {
    try {
      body = await response.json();
    } catch (e) {
      console.error("Failed to parse JSON response:", e);
    }
  } else {
    // Try to get text response for non-JSON responses
    try {
      body = await response.text();
    } catch (e) {
      console.error("Failed to get text response:", e);
    }
  }

  return { status, body };
})
.then(({ status, body }) => {
  callback(status, body, extraParams);
})
```

### 2. [frontend/src/components/layout/Layout.js](frontend/src/components/layout/Layout.js)

**Modification de `addNotification`** pour effacer les anciennes notifications.

```javascript
const addNotification = (notificationBody) => {
  // Clear previous notifications before adding new one to prevent stacking
  setNotifications([notificationBody]);
};
```

### 3. [frontend/src/components/addOrder/Index.js](frontend/src/components/addOrder/Index.js)

**Modification de `handlePost`** pour afficher les erreurs détaillées lors de la
sauvegarde d'ordonnance.

**Signature modifiée**:

```javascript
const handlePost = (status, responseBody) => {
  // ... extraction et affichage des erreurs détaillées
};
```

**Extraction des erreurs**:

- Vérifie si `responseBody.error` existe
- Vérifie si `responseBody.message` existe
- Vérifie si `responseBody.errors` est un tableau de validations
- Affiche le texte brut si la réponse est une chaîne
- Inclut le code HTTP dans le message

### 4. [frontend/src/components/patient/SearchPatientForm.js](frontend/src/components/patient/SearchPatientForm.js)

**Modification de `handlePost`** pour afficher les erreurs détaillées lors de
l'importation de patient.

### 5. [frontend/src/components/patient/CreatePatientForm.js](frontend/src/components/patient/CreatePatientForm.js)

**Modification de `handlePost`** pour afficher les erreurs détaillées lors de la
création de patient.

## Types d'Erreurs Supportés

Les modifications supportent maintenant plusieurs formats de réponse d'erreur du
backend:

### 1. JSON avec propriété `error`

```json
{
  "error": "Le numéro national existe déjà"
}
```

### 2. JSON avec propriété `message`

```json
{
  "message": "Validation failed for field 'nationalId'"
}
```

### 3. JSON avec tableau d'erreurs

```json
{
  "errors": [
    { "message": "National ID is required" },
    { "message": "Phone number is invalid" }
  ]
}
```

### 4. Texte brut

```
Internal server error: Database connection failed
```

## Format d'Affichage

Les erreurs sont maintenant affichées avec le format suivant:

```
Erreur HTTP 400
Le numéro national existe déjà
```

ou pour les erreurs multiples:

```
Erreur HTTP 400
National ID is required, Phone number is invalid
```

## Avantages

1. **Meilleure expérience utilisateur**: Les utilisateurs voient exactement ce
   qui ne va pas au lieu d'un message générique
2. **Débogage facilité**: Les développeurs et administrateurs peuvent identifier
   rapidement les problèmes
3. **Pas d'empilement**: Une seule notification visible à la fois, évitant la
   confusion
4. **Compatibilité**: Fonctionne avec tous les formats de réponse d'erreur du
   backend

## Tests Recommandés

1. Tester avec une erreur de validation (ex: champ requis manquant)
2. Tester avec une erreur de duplicate (ex: ID national déjà existant)
3. Tester avec une erreur serveur (ex: problème de base de données)
4. Tester avec plusieurs erreurs simultanées
5. Vérifier que les notifications ne s'empilent pas après plusieurs erreurs
   consécutives
