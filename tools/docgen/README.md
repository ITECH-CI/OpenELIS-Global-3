# Generateur Word des guides

Produit un `.docx` mis en forme a partir d'une source Markdown, en appliquant
les conventions de redaction du programme.

## Mise en route

```
cd tools/docgen
npm install
node build2.js ../../docs/civ/administration/index.md -o /chemin/Guide_administration.docx
```

## Ligne de parametrage

La premiere ligne de la source porte les metadonnees du document :

```
@@META: Titre | Sous-titre | Mois annee, version X.Y | sommaire:oui
```

Mettre `sommaire:non` pour supprimer le sommaire. Sans ligne `@@META`, le
fichier est converti sans page de garde ni sommaire : c'est le mode utilise pour
les pages de documentation publiees par MkDocs.

## Conventions appliquees

- Aucun cadratin ni demi-cadratin : **la generation echoue** et signale chaque
  ligne fautive. Remplacer par deux-points, point-virgule, virgule ou
  parentheses.
- Titres de la meme couleur que le corps de texte, sans filet.
- Numerotation I, A, 1, a selon la profondeur, avec retrait progressif.
- Niveaux hierarchiques portes par les titres, pour le volet de navigation de
  Word.
- Tableaux a largeurs proportionnelles au contenu.
- Figures legendees et numerotees automatiquement.
- Blocs de commandes encadres.

## Ce que le generateur comprend

Titres de niveau 1 a 4, paragraphes, listes a puces et numerotees sur deux
niveaux, tableaux au format GitHub, blocs de code delimites par trois accents
graves, images `![legende](chemin)`, et en ligne : `**gras**`, `*italique*`,
`` `code` `` et liens.
