/**
 * Téléchargement d'un export (CSV) suivi par l'écran : le fichier est récupéré
 * par fetch, enregistré par le navigateur, et l'écran connaît son nom et son
 * nombre de lignes pour confirmer la génération.
 */

/** Nom du fichier annoncé par l'en-tête Content-Disposition, sinon le repli. */
export function fileNameFromContentDisposition(header, fallback) {
  if (!header) {
    return fallback;
  }
  const encoded = /filename\*\s*=\s*[^']*''([^;]+)/i.exec(header);
  if (encoded) {
    try {
      return decodeURIComponent(encoded[1].trim());
    } catch {
      // valeur mal encodée : on tente la forme simple
    }
  }
  const plain = /filename\s*=\s*"?([^";]+)"?/i.exec(header);
  return plain ? plain[1].trim() : fallback;
}

/**
 * Nombre de lignes de données d'un CSV, en-tête exclu. Un retour à la ligne
 * à l'intérieur d'un champ entre guillemets ne compte pas.
 */
export function countCsvDataRows(text) {
  if (!text) {
    return 0;
  }
  let records = 0;
  let inQuotes = false;
  let recordHasContent = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (c === '"') {
      inQuotes = !inQuotes;
      recordHasContent = true;
    } else if ((c === "\n" || c === "\r") && !inQuotes) {
      if (recordHasContent) {
        records++;
      }
      recordHasContent = false;
    } else {
      recordHasContent = true;
    }
  }
  if (recordHasContent) {
    records++;
  }
  return Math.max(records - 1, 0);
}

/**
 * Récupère l'export, le fait enregistrer par le navigateur et renvoie
 * { fileName, rowCount }. Une réponse en erreur lève une Error portant le
 * texte renvoyé par le serveur.
 */
export async function downloadReportFile(url, fallbackName) {
  const response = await fetch(url, { credentials: "include" });
  if (!response.ok) {
    const detail = await response.text().catch(() => "");
    const error = new Error(detail || response.statusText);
    error.status = response.status;
    throw error;
  }
  const blob = await response.blob();
  const fileName = fileNameFromContentDisposition(
    response.headers.get("Content-Disposition"),
    fallbackName,
  );
  const rowCount = countCsvDataRows(await blob.text());

  const href = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = href;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(href), 1000);

  return { fileName, rowCount };
}
