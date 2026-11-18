import { describe, expect, test } from "vitest";
import {
  countCsvDataRows,
  fileNameFromContentDisposition,
} from "./ReportDownload";

describe("nom du fichier exporté", () => {
  test("forme simple, sans guillemets (en-tête des exports CSV)", () => {
    expect(
      fileNameFromContentDisposition(
        "attachment;filename=Charge_virale_2026.csv",
        "export.csv",
      ),
    ).toBe("Charge_virale_2026.csv");
  });

  test("forme entre guillemets et forme encodée", () => {
    expect(
      fileNameFromContentDisposition(
        'attachment; filename="export du jour.csv"',
        "x",
      ),
    ).toBe("export du jour.csv");
    expect(
      fileNameFromContentDisposition(
        "attachment; filename*=UTF-8''r%C3%A9sultats.csv",
        "x",
      ),
    ).toBe("résultats.csv");
  });

  test("en-tête absent : nom de repli", () => {
    expect(fileNameFromContentDisposition(null, "export.csv")).toBe(
      "export.csv",
    );
  });
});

describe("nombre de lignes exportées", () => {
  test("en-tête exclu, dernière ligne vide ignorée", () => {
    expect(countCsvDataRows("a,b\n1,2\n3,4\n")).toBe(2);
    expect(countCsvDataRows("a,b\r\n1,2\r\n3,4")).toBe(2);
  });

  test("retour à la ligne dans un champ entre guillemets", () => {
    expect(countCsvDataRows('a,b\n"ligne\nsur deux",2\n3,4\n')).toBe(2);
  });

  test("fichier vide ou en-tête seul", () => {
    expect(countCsvDataRows("")).toBe(0);
    expect(countCsvDataRows("a,b\n")).toBe(0);
  });
});
