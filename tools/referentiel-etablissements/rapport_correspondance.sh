#!/bin/bash
#
# Rapport de correspondance entre les organisations d'OpenELIS et le
# référentiel national des établissements (DHIS2 / MFL), AVANT toute écriture.
# LECTURE SEULE : le référentiel est chargé dans une table temporaire de la
# session ; aucune table d'OpenELIS n'est modifiée.
#
# Usage : sudo ./rapport_correspondance.sh etablissements_mfl.csv [dossier_sortie]
# Produit dans le dossier de sortie (défaut : .) :
#   correspondance_detail.csv  une ligne par organisation : classement, types,
#                              nombre d'échantillons qui la citent, établissement
#                              du référentiel rapproché
#   correspondance_resume.csv  comptes par type d'organisation et par classement
#
# Classement :
#   exacte          short_name (sans espaces) = un facility_code à 5 chiffres
#   candidate_code  short_name de 1 à 4 chiffres qui, complété de zéros, existe
#   candidate_nom   nom normalisé (majuscules, sans accents ni ponctuation) égal
#                   au nom DHIS2, court ou long d'un seul établissement
#   sans            aucune des trois
# nom_coherent : oui si un mot significatif du nom du référentiel figure dans le
# nom OpenELIS (contrôle visuel des correspondances, y compris « exactes »).
# Les « candidates » sont informatives : rien n'est appliqué automatiquement.
#
# Variables : DB_CONTAINER (openelisglobal-database).
set -euo pipefail

CSV="${1:?Usage : $0 etablissements_mfl.csv [dossier_sortie]}"
OUT="${2:-.}"
DB_CONTAINER="${DB_CONTAINER:-openelisglobal-database}"
[ -r "$CSV" ] || { echo "Fichier introuvable : $CSV" >&2; exit 1; }
mkdir -p "$OUT"

# Chargement du référentiel (texte : facility_code garde ses zéros de tête) et
# classement, dans UNE session psql (table temporaire).
run_report() { # $1 = requête finale (COPY ... TO STDOUT)
    {
        cat <<'SQL'
\set ON_ERROR_STOP on
CREATE TEMP TABLE ref (facility_code text, nom_long text, nom_court text, statut text,
  region_code text, region_libelle text, district_code text, district_libelle text,
  code_etablissement text, ancien_code_dhis2 text, code_dhis2_lu text, nom_dhis2 text, origine text);
COPY ref FROM STDIN WITH (FORMAT csv, HEADER true);
SQL
        cat "$CSV"
        printf '\\.\n'
        cat <<'SQL'
CREATE TEMP TABLE ref_nom AS
  SELECT DISTINCT facility_code,
         regexp_replace(upper(translate(n, 'ÀÂÄÁÉÈÊËÎÏÍÔÖÓÙÛÜÚÇàâäáéèêëîïíôöóùûüúç', 'AAAAEEEEIIIOOOUUUUCAAAAEEEEIIIOOOUUUUC')), '[^A-Z0-9]', '', 'g') AS nom_norm
  FROM ref, LATERAL unnest(ARRAY[nom_dhis2, nom_court, nom_long]) AS n
  WHERE coalesce(n, '') <> '';
CREATE TEMP TABLE ref_nom_unique AS
  SELECT nom_norm, min(facility_code) AS facility_code FROM ref_nom GROUP BY nom_norm
  HAVING count(DISTINCT facility_code) = 1;
CREATE TEMP TABLE org AS
  SELECT o.id, o.name, o.short_name, o.is_active,
         regexp_replace(coalesce(o.short_name, ''), '\s', '', 'g') AS code,
         regexp_replace(upper(translate(o.name, 'ÀÂÄÁÉÈÊËÎÏÍÔÖÓÙÛÜÚÇàâäáéèêëîïíôöóùûüúç', 'AAAAEEEEIIIOOOUUUUCAAAAEEEEIIIOOOUUUUC')), '[^A-Z0-9]', '', 'g') AS nom_norm,
         coalesce((SELECT string_agg(t.short_name, '|' ORDER BY t.short_name)
                     FROM clinlims.organization_organization_type ot
                     JOIN clinlims.organization_type t ON t.id = ot.org_type_id
                    WHERE ot.org_id = o.id), '(aucun type)') AS types,
         (SELECT count(DISTINCT sr.sample_id) FROM clinlims.sample_requester sr
           WHERE sr.requester_id = o.id
             AND sr.requester_type_id = (SELECT id FROM clinlims.requester_type WHERE requester_type = 'organization')) AS echantillons
  FROM clinlims.organization o;
CREATE TEMP TABLE classement AS
  SELECT org.*,
    CASE WHEN org.code ~ '^[0-9]{5}$' AND EXISTS (SELECT 1 FROM ref WHERE ref.facility_code = org.code) THEN 'exacte'
         WHEN org.code ~ '^[0-9]{1,4}$' AND EXISTS (SELECT 1 FROM ref WHERE ref.facility_code = lpad(org.code, 5, '0')) THEN 'candidate_code'
         WHEN EXISTS (SELECT 1 FROM ref_nom_unique u WHERE u.nom_norm = org.nom_norm AND org.nom_norm <> '') THEN 'candidate_nom'
         ELSE 'sans' END AS classement
  FROM org;
CREATE TEMP TABLE detail AS
  SELECT c.id, c.name AS nom, c.short_name, c.is_active AS actif, c.types, c.echantillons, c.classement,
         r.facility_code AS code_ref, coalesce(nullif(r.nom_dhis2, ''), r.nom_court) AS nom_ref,
         r.district_libelle AS district_ref,
         (SELECT count(*) FROM org o2 WHERE o2.code = c.code AND c.classement = 'exacte') AS orgs_meme_code,
         -- nom cohérent : un mot significatif (4 lettres ou plus, hors sigles et
         -- termes génériques) du nom du référentiel figure dans le nom OpenELIS
         CASE WHEN r.facility_code IS NULL THEN ''
              WHEN EXISTS (SELECT 1 FROM regexp_split_to_table(regexp_replace(upper(translate(concat_ws(' ', r.nom_dhis2, r.nom_court),
                          'ÀÂÄÁÉÈÊËÎÏÍÔÖÓÙÛÜÚÇàâäáéèêëîïíôöóùûüúç', 'AAAAEEEEIIIOOOUUUUCAAAAEEEEIIIOOOUUUUC')), '[^A-Z0-9]+', ' ', 'g'), ' ') AS w
                           WHERE length(w) >= 4
                             AND w NOT IN ('PUBLIC','PRIVE','CENTRE','SANTE','URBAIN','URBAINE','RURAL','DISPENSAIRE','MATERNITE',
                                           'HOPITAL','GENERAL','COMMUNAUTAIRE','CLINIQUE','INFIRMERIE','AUTONOME','CONFESSIONNEL','CONFESSIONNELLE')
                             AND position(w IN c.nom_norm) > 0) THEN 'oui'
              ELSE 'non' END AS nom_coherent
  FROM classement c
  LEFT JOIN ref r ON r.facility_code = CASE c.classement
         WHEN 'exacte' THEN c.code
         WHEN 'candidate_code' THEN lpad(c.code, 5, '0')
         WHEN 'candidate_nom' THEN (SELECT u.facility_code FROM ref_nom_unique u WHERE u.nom_norm = c.nom_norm)
         END;
SQL
        printf '%s\n' "$1"
    } | docker exec -i "$DB_CONTAINER" psql -U clinlims -d clinlims -qAt
}

run_report "COPY (SELECT * FROM detail ORDER BY classement, echantillons DESC, id) TO STDOUT WITH (FORMAT csv, HEADER true);" \
    >"$OUT/correspondance_detail.csv"
run_report "COPY (SELECT t.type, d.classement, count(*) AS organisations,
                  count(*) FILTER (WHERE d.actif = 'Y') AS actives,
                  count(*) FILTER (WHERE d.echantillons > 0) AS citees_par_echantillons,
                  sum(d.echantillons) AS echantillons
             FROM detail d, LATERAL unnest(string_to_array(d.types, '|')) AS t(type)
            GROUP BY 1, 2 ORDER BY 1, 2) TO STDOUT WITH (FORMAT csv, HEADER true);" \
    >"$OUT/correspondance_resume.csv"

echo "Référentiel : $(($(wc -l <"$CSV") - 1)) établissements"
python3 - "$OUT/correspondance_detail.csv" <<'PY'
import csv, sys, collections
rows = list(csv.DictReader(open(sys.argv[1], encoding="utf-8")))
print("Organisations par classement (actives / citées par des échantillons) :")
for k in ("exacte", "candidate_code", "candidate_nom", "sans"):
    sel = [r for r in rows if r["classement"] == k]
    print(f"  {k:15} {len(sel):5}  ({sum(r['actif'] == 'Y' for r in sel)} actives, "
          f"{sum(int(r['echantillons']) > 0 for r in sel)} citées)")
ex = [r for r in rows if r["classement"] == "exacte"]
print("Correspondances exactes au nom NON cohérent (à vérifier) :", sum(r["nom_coherent"] == "non" for r in ex))
print("Codes exacts portés par plusieurs organisations :",
      len({r["code_ref"] for r in rows if r["classement"] == "exacte" and int(r["orgs_meme_code"]) > 1}))
PY
echo "Fichiers : $OUT/correspondance_detail.csv, $OUT/correspondance_resume.csv"
