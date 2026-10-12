-- Annule TOUT le travail du référentiel des établissements (synchronisations et
-- fusions, automatiques ou manuelles), à partir du suivi
-- (clinlims.ref_etablissement_rapprochement) et du journal des fusions
-- (clinlims.ref_fusion_reference) :
-- 1. chaque fusion est annulée, de la plus récente à la plus ancienne
--    (références remises sur l'organisation d'origine) ;
-- 2. les organisations modifiées retrouvent nom, code, statut actif et
--    organisation parente d'avant leur première décision ;
-- 3. les organisations CRÉÉES (établissements, régions, districts) sont
--    supprimées si rien ne les cite, sinon désactivées ;
-- 4. les colonnes ref_statut / ref_niveau / fusionne_dans sont vidées.
-- La table du référentiel et les fonctions restent en place. Ensuite :
-- /OEToFhir?checkAll=true.
--
-- Usage : docker exec -i openelisglobal-database psql -U clinlims -d clinlims -v ON_ERROR_STOP=1 < annuler_synchronisation.sql
BEGIN;

-- 1. Fusions
DO $$
DECLARE f record;
BEGIN
  FOR f IN SELECT id FROM clinlims.ref_etablissement_rapprochement
            WHERE decision = 'fusion_source' ORDER BY id DESC LOOP
    PERFORM clinlims.ref_annuler_fusion(f.id);
  END LOOP;
END $$;

-- 2. État d'origine (première décision tracée de chaque organisation)
CREATE TEMP TABLE _origine ON COMMIT DROP AS
  SELECT DISTINCT ON (org_id) org_id, ancien_nom, ancien_short_name, ancien_actif, ancien_org_parent
    FROM clinlims.ref_etablissement_rapprochement
   WHERE decision IN ('rattachee', 'mise_a_jour', 'code_retire', 'desactivee') AND org_id IS NOT NULL
   ORDER BY org_id, date_decision, id;
UPDATE clinlims.organization o
   SET name = coalesce(g.ancien_nom, o.name), short_name = g.ancien_short_name,
       is_active = g.ancien_actif, org_id = g.ancien_org_parent, lastupdated = now()
  FROM _origine g
 WHERE o.id = g.org_id
   AND NOT EXISTS (SELECT 1 FROM clinlims.ref_etablissement_rapprochement c
                    WHERE c.org_id = o.id AND c.decision = 'creee');

-- 3. Organisations créées : établissements d'abord, puis districts, puis régions
CREATE TEMP TABLE _creees ON COMMIT DROP AS
  SELECT DISTINCT x.org_id AS id, o.ref_niveau,
         clinlims.ref_historique(x.org_id) - (SELECT count(*) FROM clinlims.organization e
                                               WHERE e.org_id = x.org_id
                                                 AND EXISTS (SELECT 1 FROM clinlims.ref_etablissement_rapprochement y
                                                              WHERE y.org_id = e.id AND y.decision = 'creee')) AS cite
    FROM clinlims.ref_etablissement_rapprochement x JOIN clinlims.organization o ON o.id = x.org_id
   WHERE x.decision = 'creee';
UPDATE clinlims.organization o SET is_active = 'N', lastupdated = now() FROM _creees c WHERE o.id = c.id AND c.cite > 0;
DELETE FROM clinlims.organization_organization_type ot USING _creees c WHERE ot.org_id = c.id AND c.cite = 0;
DELETE FROM clinlims.organization o USING _creees c WHERE o.id = c.id AND c.cite = 0 AND c.ref_niveau = 'ETABLISSEMENT';
DELETE FROM clinlims.organization o USING _creees c WHERE o.id = c.id AND c.cite = 0 AND c.ref_niveau = 'DISTRICT';
DELETE FROM clinlims.organization o USING _creees c WHERE o.id = c.id AND c.cite = 0 AND c.ref_niveau = 'REGION';

-- 4. Colonnes du référentiel vidées ; suivi et journal vidés
UPDATE clinlims.organization SET ref_statut = NULL, ref_niveau = NULL, fusionne_dans = NULL
 WHERE ref_statut IS NOT NULL OR ref_niveau IS NOT NULL OR fusionne_dans IS NOT NULL;
DELETE FROM clinlims.ref_fusion_reference;
DELETE FROM clinlims.ref_etablissement_rapprochement;

SELECT 'organisations' AS bilan, count(*) AS total, count(*) FILTER (WHERE is_active = 'Y') AS actives
  FROM clinlims.organization;
COMMIT;
