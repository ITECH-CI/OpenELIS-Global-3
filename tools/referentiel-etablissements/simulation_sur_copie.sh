#!/bin/bash
#
# Simule la synchronisation du référentiel des établissements sur une COPIE de
# la base (base temporaire clinlims_simulation dans le même conteneur), affiche
# le bilan, puis supprime la copie. La base en service n'est jamais modifiée.
#
# Usage : sudo ./simulation_sur_copie.sh etablissements_mfl.csv referentiel_etablissements.sql [dossier_sortie]
#   (referentiel_etablissements.sql : src/main/resources/liquibase/3.2.x.x/sql/)
# Variables : DB_CONTAINER (openelisglobal-database), DB_SUPERUSER (postgres),
#             SITE_CODE (code à 5 chiffres à donner au site du laboratoire avant
#             la simulation, comme on le ferait avant le déploiement).
set -euo pipefail
CSV="${1:?Usage : $0 etablissements_mfl.csv referentiel_etablissements.sql [dossier_sortie]}"
SQL="${2:?fichier referentiel_etablissements.sql requis}"
OUT="${3:-.}"
C="${DB_CONTAINER:-openelisglobal-database}"
SU="${DB_SUPERUSER:-postgres}"
SIM=clinlims_simulation
mkdir -p "$OUT"
q() { docker exec "$C" psql -U clinlims -d $SIM -tA -F' | ' -c "$1"; }
nettoyer() { docker exec "$C" psql -U "$SU" -d postgres -qc "DROP DATABASE IF EXISTS $SIM" >/dev/null 2>&1 || true; }
trap nettoyer EXIT

echo "Copie de la base…"
nettoyer
docker exec "$C" psql -U "$SU" -d postgres -qc "CREATE DATABASE $SIM OWNER clinlims"
docker exec "$C" sh -c "pg_dump -U clinlims -d clinlims -Fc | pg_restore -U $SU -d $SIM --no-owner --role=clinlims >/dev/null 2>&1" || true

echo "Fonctions et référentiel…"
docker exec -i "$C" psql -U clinlims -d $SIM -v ON_ERROR_STOP=1 -q <<'DDL'
CREATE TABLE IF NOT EXISTS clinlims.ref_etablissement_dhis2 (facility_code varchar(5) primary key,
  nom_dhis2 varchar(255), nom_court varchar(255), nom_long varchar(255), statut varchar(10), region_code varchar(10),
  region_libelle varchar(100), district_code varchar(10), district_libelle varchar(100), origine varchar(50));
CREATE TABLE IF NOT EXISTS clinlims.ref_etablissement_rapprochement (id bigserial primary key, org_id numeric(10,0),
  facility_code varchar(5), decision varchar(30), motif varchar(255), ancien_nom varchar(80), ancien_short_name varchar(15),
  ancien_actif varchar(1), version varchar(100), date_decision timestamp default now(), cible_id bigint,
  ancien_org_parent numeric(10,0), ancien_ref_statut varchar(10), ancien_ref_niveau varchar(15));
CREATE TABLE IF NOT EXISTS clinlims.ref_fusion_reference (id bigserial primary key, fusion_id bigint, table_name varchar(60),
  pk_colonnes varchar(100), pk_valeurs varchar(100), colonne varchar(60), ancienne_valeur varchar(100),
  nouvelle_valeur varchar(100), operation varchar(10));
ALTER TABLE clinlims.organization ADD COLUMN IF NOT EXISTS ref_statut varchar(10),
  ADD COLUMN IF NOT EXISTS ref_niveau varchar(15), ADD COLUMN IF NOT EXISTS fusionne_dans numeric(10,0);
TRUNCATE clinlims.ref_etablissement_dhis2;
DDL
docker exec -i "$C" psql -U clinlims -d $SIM -v ON_ERROR_STOP=1 -q < "$SQL"
python3 - "$CSV" <<'PY' | docker exec -i "$C" psql -U clinlims -d $SIM -qc "\copy clinlims.ref_etablissement_dhis2 FROM STDIN WITH (FORMAT csv)"
import csv, sys
w = csv.writer(sys.stdout)
for r in csv.DictReader(open(sys.argv[1], encoding="utf-8")):
    w.writerow([r[k] for k in ("facility_code", "nom_dhis2", "nom_court", "nom_long", "statut", "region_code",
                               "region_libelle", "district_code", "district_libelle", "origine")])
PY

SITE=$(q "select nullif(trim(value),'') from clinlims.site_information where name='siteId'")
if [ -n "${SITE_CODE:-}" ] && [ -n "$SITE" ]; then
    q "update clinlims.organization set short_name='$SITE_CODE' where id=$SITE" >/dev/null
    echo "Site du laboratoire ($SITE) : code $SITE_CODE appliqué avant la simulation"
fi
echo "AVANT : $(q "select count(*)||' organisations, '||count(*) filter (where is_active='Y')||' actives' from clinlims.organization")"
echo "== Synchronisation (simulation) =="
q "SELECT * FROM clinlims.sync_referentiel_etablissements('simulation')"
echo "APRÈS : $(q "select count(*)||' organisations, '||count(*) filter (where is_active='Y')||' actives' from clinlims.organization")"
echo "Site du laboratoire : $(q "select coalesce(o.short_name,'(sans code)')||' '||o.name||' actif='||o.is_active from clinlims.organization o where o.id::text='$SITE'")"
echo "== Statut et niveau des organisations après simulation =="
q "select coalesce(ref_niveau,'-'), coalesce(ref_statut,'-'), case when is_active='Y' then 'active' else 'inactive' end, count(*) from clinlims.organization group by 1,2,3 order by 1,2,3"
echo "Échantillons dont le site demandeur a changé d'organisation (fusions) : $(q "select count(*) from clinlims.ref_fusion_reference where table_name='sample_requester'")"
echo "== Décisions par type d'organisation (une organisation peut avoir plusieurs types) =="
q "select coalesce(t.short_name,'(aucun type)') type, x.decision, count(*) from clinlims.ref_etablissement_rapprochement x
     left join clinlims.organization_organization_type ot on ot.org_id=x.org_id
     left join clinlims.organization_type t on t.id=ot.org_type_id
    where x.version='simulation' and x.decision <> 'creee' group by 1,2 order by 1,2"
docker exec "$C" psql -U clinlims -d $SIM -qc "\copy (select x.org_id, x.decision, x.facility_code, x.motif, x.ancien_nom, x.ancien_short_name, x.ancien_actif, o.name nouveau_nom, o.short_name nouveau_code, o.is_active nouvel_actif, (select count(distinct sr.sample_id) from clinlims.sample_requester sr where sr.requester_id=x.org_id) echantillons from clinlims.ref_etablissement_rapprochement x join clinlims.organization o on o.id=x.org_id where x.version='simulation' and x.decision <> 'creee' order by x.decision, x.org_id) TO STDOUT WITH (FORMAT csv, HEADER true)" > "$OUT/simulation_decisions.csv"
echo "Détail (hors créations) : $OUT/simulation_decisions.csv"
echo "Rejeu (doit n'afficher que site_sans_code éventuel) :"; q "SELECT * FROM clinlims.sync_referentiel_etablissements('simulation-rejeu')"
