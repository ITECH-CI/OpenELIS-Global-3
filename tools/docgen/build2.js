#!/usr/bin/env node
/**
 * build2.js : Markdown vers Word, conventions de redaction LABQUASI II.
 *
 * Usage : node build2.js source.md [-o sortie.docx]
 *
 * Conventions appliquees (arretees le 23/08/2026) :
 *   - aucun cadratin ni demi-cadratin : la generation echoue si la source en contient ;
 *   - titres de la meme couleur que le corps de texte, sans filet ;
 *   - numerotation I, A, 1, a selon la profondeur, avec retrait progressif ;
 *   - niveaux hierarchiques portes par les titres (volet de navigation Word) ;
 *   - tableaux a largeurs proportionnelles au contenu ;
 *   - figures legendees, blocs de commandes encadres ;
 *   - sommaire optionnel construit a partir des titres de niveau 1.
 *
 * Ligne de parametrage, premiere ligne de la source :
 *   @@META: Titre | Sous-titre | Mois annee, version X.Y | sommaire:non
 */

const fs = require("fs");
const path = require("path");
const D = require("docx");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  WidthType, ShadingType, BorderStyle, AlignmentType, HeadingLevel,
  PageNumber, Footer, PageBreak, ImageRun, ExternalHyperlink,
} = D;

// ---------------------------------------------------------------- constantes
const POLICE = "Calibri";
const MONO = "Consolas";
const CORPS = 22;              // 11 pt, en demi-points
const NOIR = "000000";
const GRIS_FILET = "BFBFBF";
const GRIS_FOND = "F2F2F2";
const FOND_CODE = "F5F5F5";
const PAGE_LARGEUR = 9026;     // DXA utiles, A4 marges 2,5 cm
const PAS_RETRAIT = 240;       // retrait par niveau, en DXA

const TAILLE_TITRE = { 1: 32, 2: 26, 3: 24, 4: 22 };
const NIVEAU_WORD = {
  1: HeadingLevel.HEADING_1, 2: HeadingLevel.HEADING_2,
  3: HeadingLevel.HEADING_3, 4: HeadingLevel.HEADING_4,
};

// ------------------------------------------------------------- numerotation
const ROMAINS = ["I","II","III","IV","V","VI","VII","VIII","IX","X","XI","XII",
                 "XIII","XIV","XV","XVI","XVII","XVIII","XIX","XX","XXI","XXII",
                 "XXIII","XXIV","XXV","XXVI","XXVII","XXVIII","XXIX","XXX"];
const LETTRES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");

function etiquette(niveau, compteurs) {
  const n = compteurs[niveau];
  if (niveau === 1) return (ROMAINS[n - 1] || String(n)) + ".";
  if (niveau === 2) return (LETTRES[n - 1] || String(n)) + ".";
  if (niveau === 3) return String(n) + ".";
  return String.fromCharCode(96 + ((n - 1) % 26) + 1) + ")";
}

// ------------------------------------------------------- controle typographie
function controleTypographie(source, chemin) {
  const interdits = [
    [String.fromCharCode(0x2014), "cadratin"],
    [String.fromCharCode(0x2013), "demi-cadratin"],
  ];
  const fautes = [];
  source.split("\n").forEach((ligne, i) => {
    for (const [car, nom] of interdits) {
      if (ligne.includes(car)) fautes.push({ ligne: i + 1, nom, texte: ligne.trim() });
    }
  });
  if (fautes.length) {
    console.error(`\nGeneration refusee : ${fautes.length} caractere(s) interdit(s) dans ${chemin}\n`);
    for (const f of fautes.slice(0, 40)) {
      console.error(`  ligne ${f.ligne} (${f.nom}) : ${f.texte.slice(0, 110)}`);
    }
    if (fautes.length > 40) console.error(`  ... et ${fautes.length - 40} autre(s)`);
    console.error("\nRemplacer par le signe adequat : deux-points, point-virgule, virgule ou parentheses.\n");
    process.exit(2);
  }
}

// ------------------------------------------------------------ parsing inline
// Gere **gras**, *italique*, `code`, [texte](url). Retourne des TextRun.
function inline(texte, opts = {}) {
  const base = { font: POLICE, size: opts.size || CORPS, color: opts.color || NOIR };
  const runs = [];
  const motif = /(\*\*[^*]+\*\*|\*[^*]+\*|`[^`]+`|\[[^\]]+\]\([^)]+\))/g;
  let reste = 0, m;
  const pousse = (t, extra = {}) => {
    if (t) runs.push(new TextRun({ ...base, ...extra, text: t }));
  };
  while ((m = motif.exec(texte)) !== null) {
    pousse(texte.slice(reste, m.index), { bold: opts.bold, italics: opts.italics });
    const j = m[0];
    if (j.startsWith("**")) pousse(j.slice(2, -2), { bold: true, italics: opts.italics });
    else if (j.startsWith("`")) runs.push(new TextRun({ ...base, font: MONO, size: base.size - 2, text: j.slice(1, -1) }));
    else if (j.startsWith("[")) {
      const t = j.slice(1, j.indexOf("]"));
      const u = j.slice(j.indexOf("](") + 2, -1);
      runs.push(new ExternalHyperlink({
        link: u,
        children: [new TextRun({ ...base, text: t, style: "Hyperlink" })],
      }));
    } else pousse(j.slice(1, -1), { italics: true, bold: opts.bold });
    reste = m.index + j.length;
  }
  pousse(texte.slice(reste), { bold: opts.bold, italics: opts.italics });
  return runs.length ? runs : [new TextRun({ ...base, text: "" })];
}

// --------------------------------------------------------------- composants
function paragraphe(texte, retrait, opts = {}) {
  return new Paragraph({
    children: inline(texte, opts),
    spacing: { before: opts.before || 0, after: opts.after === undefined ? 120 : opts.after, line: 264 },
    indent: { left: retrait },
    alignment: opts.align,
  });
}

function titre(niveau, texte, etiq, retrait) {
  return new Paragraph({
    heading: NIVEAU_WORD[niveau],
    spacing: { before: niveau === 1 ? 360 : 260, after: 140 },
    indent: { left: retrait },
    keepNext: true,
    children: [
      new TextRun({ text: etiq + " ", font: POLICE, size: TAILLE_TITRE[niveau], bold: true, color: NOIR }),
      ...inline(texte, { size: TAILLE_TITRE[niveau], bold: true }),
    ],
  });
}

function puce(texte, niveauListe, retrait) {
  return new Paragraph({
    children: inline(texte),
    numbering: { reference: niveauListe === 0 ? "puces" : "puces", level: niveauListe },
    spacing: { after: 60, line: 264 },
    indent: { left: retrait + 400 + niveauListe * 360, hanging: 200 },
  });
}

function numero(texte, niveauListe, retrait, ref) {
  return new Paragraph({
    children: inline(texte),
    numbering: { reference: ref, level: niveauListe },
    spacing: { after: 60, line: 264 },
    indent: { left: retrait + 400 + niveauListe * 360, hanging: 260 },
  });
}

// Bloc de commandes encadre
function blocCode(lignes, retrait) {
  const largeur = PAGE_LARGEUR - retrait;
  return new Table({
    columnWidths: [largeur],
    width: { size: largeur, type: WidthType.DXA },
    indent: { size: retrait, type: WidthType.DXA },
    rows: [new TableRow({
      children: [new TableCell({
        width: { size: largeur, type: WidthType.DXA },
        shading: { type: ShadingType.CLEAR, fill: FOND_CODE, color: "auto" },
        margins: { top: 100, bottom: 100, left: 140, right: 140 },
        children: lignes.map(l => new Paragraph({
          children: [new TextRun({ text: l || " ", font: MONO, size: 19, color: NOIR })],
          spacing: { after: 0, line: 240 },
        })),
      })],
    })],
    borders: {
      top: { style: BorderStyle.SINGLE, size: 4, color: GRIS_FILET },
      bottom: { style: BorderStyle.SINGLE, size: 4, color: GRIS_FILET },
      left: { style: BorderStyle.SINGLE, size: 4, color: GRIS_FILET },
      right: { style: BorderStyle.SINGLE, size: 4, color: GRIS_FILET },
      insideHorizontal: { style: BorderStyle.NONE },
      insideVertical: { style: BorderStyle.NONE },
    },
  });
}

// Tableau a largeurs proportionnelles au contenu
function tableau(entetes, lignes, retrait) {
  const nbCol = entetes.length;
  const poids = entetes.map((h, i) => {
    const max = Math.max(
      h.length,
      ...lignes.map(l => (l[i] || "").length)
    );
    return Math.max(6, Math.min(max, 60));
  });
  const total = poids.reduce((a, b) => a + b, 0);
  const dispo = PAGE_LARGEUR - retrait;
  const larg = poids.map(p => Math.round(dispo * p / total));
  larg[nbCol - 1] = dispo - larg.slice(0, -1).reduce((a, b) => a + b, 0);

  const cellule = (txt, i, opts = {}) => new TableCell({
    width: { size: larg[i], type: WidthType.DXA },
    shading: opts.fill ? { type: ShadingType.CLEAR, fill: opts.fill, color: "auto" } : undefined,
    margins: { top: 70, bottom: 70, left: 100, right: 100 },
    verticalAlign: D.VerticalAlign.TOP,
    children: [new Paragraph({
      children: inline(txt, { size: 20, bold: opts.bold }),
      spacing: { after: 0, line: 240 },
    })],
  });

  const rows = [new TableRow({
    tableHeader: true,
    children: entetes.map((h, i) => cellule(h, i, { bold: true, fill: GRIS_FOND })),
  })];
  lignes.forEach(l => {
    rows.push(new TableRow({ children: entetes.map((_, i) => cellule(l[i] || "", i)) }));
  });

  return new Table({
    columnWidths: larg,
    width: { size: dispo, type: WidthType.DXA },
    indent: { size: retrait, type: WidthType.DXA },
    rows,
    borders: {
      top: { style: BorderStyle.SINGLE, size: 4, color: GRIS_FILET },
      bottom: { style: BorderStyle.SINGLE, size: 4, color: GRIS_FILET },
      left: { style: BorderStyle.SINGLE, size: 4, color: GRIS_FILET },
      right: { style: BorderStyle.SINGLE, size: 4, color: GRIS_FILET },
      insideHorizontal: { style: BorderStyle.SINGLE, size: 2, color: "D9D9D9" },
      insideVertical: { style: BorderStyle.SINGLE, size: 2, color: "D9D9D9" },
    },
  });
}

// Figure : image centree et legende numerotee
let compteurFigure = 0;
function figure(cheminImage, legende, racine) {
  const abs = path.resolve(racine, cheminImage);
  if (!fs.existsSync(abs)) {
    console.error(`  avertissement : image introuvable, ignoree : ${cheminImage}`);
    return [];
  }
  const buf = fs.readFileSync(abs);
  let dim = { width: 1200, height: 800 };
  try {
    const is = require("image-size");
    dim = (is.imageSize || is.default || is)(buf);
  } catch (e) { /* dimensions par defaut */ }
  const largeur = Math.min(600, dim.width);
  const hauteur = Math.round(largeur * dim.height / dim.width);
  compteurFigure += 1;
  const ext = path.extname(abs).slice(1).toLowerCase();
  return [
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 160, after: 60, line: 240, lineRule: D.LineRuleType.AUTO },
      keepNext: true,
      children: [new ImageRun({
        data: buf,
        type: ext === "jpg" ? "jpeg" : ext,
        transformation: { width: largeur, height: hauteur },
      })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 220 },
      children: [
        new TextRun({ text: `Figure ${compteurFigure} : `, font: POLICE, size: 19, bold: true, color: NOIR }),
        new TextRun({ text: legende, font: POLICE, size: 19, italics: true, color: NOIR }),
      ],
    }),
  ];
}

// ------------------------------------------------------------------ parsing
function construire(source, racine) {
  const lignes = source.split("\n");
  const blocs = [];
  const sommaireH1 = [];
  const compteurs = { 1: 0, 2: 0, 3: 0, 4: 0 };
  let retrait = 0;
  let seqNum = 0;
  let i = 0;

  while (i < lignes.length) {
    const l = lignes[i];

    // ligne de parametrage et filets : ignores
    if (l.startsWith("@@META:") || /^\s*(-{3,}|\*{3,}|_{3,})\s*$/.test(l)) { i++; continue; }

    // titre
    const mt = l.match(/^(#{1,4})\s+(.*)$/);
    if (mt) {
      const niveau = mt[1].length;
      const texte = mt[2].replace(/\s*#+\s*$/, "").trim();
      compteurs[niveau] += 1;
      for (let n = niveau + 1; n <= 4; n++) compteurs[n] = 0;
      retrait = (niveau - 1) * PAS_RETRAIT;
      const etiq = etiquette(niveau, compteurs);
      if (niveau === 1) sommaireH1.push(`${etiq} ${texte}`);
      blocs.push(titre(niveau, texte, etiq, retrait));
      i++; continue;
    }

    // bloc de code
    if (/^\s*```/.test(l)) {
      const corps = [];
      i++;
      while (i < lignes.length && !/^\s*```/.test(lignes[i])) { corps.push(lignes[i]); i++; }
      i++;
      blocs.push(blocCode(corps, retrait));
      blocs.push(new Paragraph({ children: [new TextRun({ text: "", size: 8 })], spacing: { after: 120 } }));
      continue;
    }

    // figure : ![legende](chemin)
    const mf = l.match(/^\s*!\[([^\]]*)\]\(([^)]+)\)\s*$/);
    if (mf) { blocs.push(...figure(mf[2], mf[1], racine)); i++; continue; }

    // tableau GFM
    if (/^\s*\|/.test(l) && i + 1 < lignes.length && /^\s*\|[\s:|-]+\|\s*$/.test(lignes[i + 1])) {
      const cellules = s => s.trim().replace(/^\||\|$/g, "").split("|").map(c => c.trim());
      const entetes = cellules(l);
      i += 2;
      const corps = [];
      while (i < lignes.length && /^\s*\|/.test(lignes[i])) { corps.push(cellules(lignes[i])); i++; }
      blocs.push(tableau(entetes, corps, retrait));
      blocs.push(new Paragraph({ children: [new TextRun({ text: "", size: 8 })], spacing: { after: 160 } }));
      continue;
    }

    // liste numerotee
    if (/^(\s*)\d+[.)]\s+/.test(l)) {
      const ref = "num" + (++seqNum);
      while (i < lignes.length && /^(\s*)\d+[.)]\s+/.test(lignes[i])) {
        const m = lignes[i].match(/^(\s*)\d+[.)]\s+(.*)$/);
        blocs.push(numero(m[2], Math.min(1, Math.floor(m[1].length / 2)), retrait, ref));
        i++;
      }
      continue;
    }

    // liste a puces
    if (/^(\s*)[-*+]\s+/.test(l)) {
      while (i < lignes.length && /^(\s*)[-*+]\s+/.test(lignes[i])) {
        const m = lignes[i].match(/^(\s*)[-*+]\s+(.*)$/);
        blocs.push(puce(m[2], Math.min(1, Math.floor(m[1].length / 2)), retrait));
        i++;
      }
      continue;
    }

    // ligne vide
    if (!l.trim()) { i++; continue; }

    // paragraphe : agrege les lignes jusqu'a la prochaine rupture
    const morceaux = [];
    while (i < lignes.length && lignes[i].trim()
           && !/^(#{1,4})\s/.test(lignes[i]) && !/^\s*```/.test(lignes[i])
           && !/^\s*\|/.test(lignes[i]) && !/^(\s*)[-*+]\s+/.test(lignes[i])
           && !/^(\s*)\d+[.)]\s+/.test(lignes[i]) && !/^\s*!\[/.test(lignes[i])) {
      morceaux.push(lignes[i].trim()); i++;
    }
    blocs.push(paragraphe(morceaux.join(" "), retrait));
  }

  return { blocs, sommaireH1 };
}

// ----------------------------------------------------------------- document
function pageDeGarde(meta) {
  const kids = [
    new Paragraph({ children: [new TextRun({ text: "", size: 40 })], spacing: { after: 2400 } }),
    new Paragraph({
      children: [new TextRun({ text: meta.titre, font: POLICE, size: 56, bold: true, color: NOIR })],
      spacing: { after: 200 },
    }),
  ];
  if (meta.sousTitre) {
    kids.push(new Paragraph({
      children: [new TextRun({ text: meta.sousTitre, font: POLICE, size: 30, color: NOIR })],
      spacing: { after: 160 },
    }));
  }
  if (meta.version) {
    kids.push(new Paragraph({
      children: [new TextRun({ text: meta.version, font: POLICE, size: 22, italics: true, color: NOIR })],
      spacing: { after: 0 },
    }));
  }
  kids.push(new Paragraph({ children: [new PageBreak()] }));
  return kids;
}

function sommaire(entrees) {
  const kids = [new Paragraph({
    children: [new TextRun({ text: "Sommaire", font: POLICE, size: 32, bold: true, color: NOIR })],
    spacing: { after: 220 },
  })];
  entrees.forEach(e => kids.push(new Paragraph({
    children: [new TextRun({ text: e, font: POLICE, size: CORPS, color: NOIR })],
    spacing: { after: 90, line: 264 },
  })));
  kids.push(new Paragraph({ children: [new PageBreak()] }));
  return kids;
}

function lireMeta(source) {
  const premiere = source.split("\n").find(l => l.trim()) || "";
  if (!premiere.startsWith("@@META:")) {
    return { titre: "Document", sousTitre: "", version: "", sommaire: false, aMeta: false };
  }
  const champs = premiere.slice(7).split("|").map(s => s.trim());
  const opt = (champs[3] || "").toLowerCase();
  return {
    titre: champs[0] || "Document",
    sousTitre: champs[1] || "",
    version: champs[2] || "",
    sommaire: !/sommaire\s*:\s*non/.test(opt),
    aMeta: true,
  };
}

const NUMEROTATION = {
  config: [
    { reference: "puces", levels: [
      { level: 0, format: D.LevelFormat.BULLET, text: "\u2022", alignment: AlignmentType.LEFT,
        style: { run: { font: POLICE, size: CORPS } } },
      { level: 1, format: D.LevelFormat.BULLET, text: "\u25E6", alignment: AlignmentType.LEFT,
        style: { run: { font: POLICE, size: CORPS } } },
    ] },
    ...Array.from({ length: 60 }, (_, k) => ({
      reference: "num" + (k + 1), levels: [
        { level: 0, format: D.LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT, start: 1,
          style: { run: { font: POLICE, size: CORPS } } },
        { level: 1, format: D.LevelFormat.LOWER_LETTER, text: "%2)", alignment: AlignmentType.LEFT, start: 1,
          style: { run: { font: POLICE, size: CORPS } } },
      ],
    })),
  ],
};

function styles() {
  const t = n => ({
    id: "Heading" + n, name: "Heading " + n, basedOn: "Normal", next: "Normal", quickFormat: true,
    run: { font: POLICE, size: TAILLE_TITRE[n], bold: true, color: NOIR },
    paragraph: { spacing: { before: n === 1 ? 360 : 260, after: 140 }, outlineLevel: n - 1 },
  });
  return {
    default: { document: { run: { font: POLICE, size: CORPS, color: NOIR },
                           paragraph: { spacing: { line: 264, after: 120 } } } },
    paragraphStyles: [t(1), t(2), t(3), t(4)],
  };
}

function pied(titreCourt) {
  return new Footer({ children: [new Paragraph({
    alignment: AlignmentType.CENTER,
    children: [
      new TextRun({ text: titreCourt + "   ", font: POLICE, size: 16, color: "595959" }),
      new TextRun({ children: [PageNumber.CURRENT], font: POLICE, size: 16, color: "595959" }),
      new TextRun({ text: " / ", font: POLICE, size: 16, color: "595959" }),
      new TextRun({ children: [PageNumber.TOTAL_PAGES], font: POLICE, size: 16, color: "595959" }),
    ],
  })] });
}

// --------------------------------------------------------------------- main
async function principal() {
  const args = process.argv.slice(2);
  if (!args.length) {
    console.error("Usage : node build2.js source.md [-o sortie.docx]");
    process.exit(1);
  }
  const entree = args[0];
  const io = args.indexOf("-o");
  const sortie = io !== -1 ? args[io + 1] : entree.replace(/\.md$/i, "") + ".docx";

  const source = fs.readFileSync(entree, "utf8");
  controleTypographie(source, entree);

  const meta = lireMeta(source);
  const racine = path.dirname(path.resolve(entree));
  const { blocs, sommaireH1 } = construire(source, racine);

  const enfants = [];
  if (meta.aMeta) enfants.push(...pageDeGarde(meta));
  if (meta.aMeta && meta.sommaire && sommaireH1.length) enfants.push(...sommaire(sommaireH1));
  enfants.push(...blocs);

  const doc = new Document({
    styles: styles(),
    numbering: NUMEROTATION,
    sections: [{
      properties: { page: { margin: { top: 1418, bottom: 1418, left: 1418, right: 1418 } } },
      footers: { default: pied(meta.titre) },
      children: enfants,
    }],
  });

  const buf = await Packer.toBuffer(doc);
  fs.writeFileSync(sortie, buf);
  console.log(`ecrit : ${sortie}`);
  console.log(`  titres de niveau 1 : ${sommaireH1.length} | figures : ${compteurFigure} | blocs : ${blocs.length}`);
}

principal().catch(e => { console.error(e); process.exit(1); });
