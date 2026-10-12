-- Référentiel national des établissements (DHIS2 / MFL) : fonctions.
-- Chargé par referentiel_etablissements_dhis2.xml (runOnChange). Voir
-- docs/dev-notes/REFERENTIEL_ETABLISSEMENTS.md pour les règles.
--
-- Colonnes de clinlims.organization utilisées :
--   ref_statut    MFL (créée depuis la liste), FUSIONNE (organisation existante
--                 réconciliée avec un établissement, région ou district MFL),
--                 LEGACY (hors référentiel)
--   ref_niveau    REGION, DISTRICT, ETABLISSEMENT (NULL : laboratoire de
--                 référence, fournisseur, service…)
--   fusionne_dans organisation qui a reçu l'historique de celle-ci
--   org_id        organisation parente : district d'un établissement, région
--                 d'un district

-- ---------------------------------------------------------------- noms
-- Nom normalisé : majuscules, sans accents, mots séparés par un espace.
CREATE OR REPLACE FUNCTION clinlims.ref_nom_normalise(t text) RETURNS text
LANGUAGE sql IMMUTABLE AS $f$
  SELECT trim(regexp_replace(upper(translate(coalesce(t, ''),
         'ÀÂÄÁÃÉÈÊËÎÏÍÔÖÓÕÙÛÜÚÇŒàâäáãéèêëîïíôöóõùûüúçœ’',
         'AAAAAEEEEIIIOOOOUUUUCOAAAAAEEEEIIIOOOOUUUUCO''')), '[^A-Z0-9]+', ' ', 'g'));
$f$;

-- Deux noms désignent-ils le même établissement ? Vrai si un mot significatif
-- de l'un se retrouve dans l'autre : mot de 5 lettres ou plus par ses 6
-- premières lettres, n'importe où (tolère « MANZANOUA » / « MANZANOUAN »,
-- « CSUGABIADJI » / « GABIADJI ») ; mot de 4 lettres comme mot entier
-- (« SOKO »). Termes génériques et sigles fréquents du référentiel ignorés.
CREATE OR REPLACE FUNCTION clinlims.ref_nom_coherent(a text, b text) RETURNS boolean
LANGUAGE sql IMMUTABLE AS $f$
  WITH n AS (SELECT clinlims.ref_nom_normalise(a) AS na, clinlims.ref_nom_normalise(b) AS nb),
  mots AS (
    SELECT w, replace(n.nb, ' ', '') AS autre, string_to_array(n.nb, ' ') AS autres_mots
      FROM n, regexp_split_to_table(n.na, ' ') AS w
    UNION ALL
    SELECT w, replace(n.na, ' ', ''), string_to_array(n.na, ' ')
      FROM n, regexp_split_to_table(n.nb, ' ') AS w)
  SELECT coalesce(bool_or(CASE WHEN length(w) >= 5 THEN position(left(w, 6) IN autre) > 0
                               ELSE w = ANY (autres_mots) END), false)
    FROM mots
   WHERE length(w) >= 4 AND w !~ '[0-9]'
     AND w NOT IN ('PUBLIC', 'PRIVE', 'CENTRE', 'SANTE', 'URBAIN', 'URBAINE', 'RURAL', 'RURALE',
                   'DISPENSAIRE', 'MATERNITE', 'HOPITAL', 'GENERAL', 'GENERALE', 'COMMUNAUTAIRE',
                   'CLINIQUE', 'INFIRMERIE', 'AUTONOME', 'CONFESSIONNEL', 'CONFESSIONNELLE',
                   'MEDICAL', 'MEDICALE', 'SOCIAL', 'SOCIALE', 'REGIONAL', 'REGIONALE', 'SAINT', 'SAINTE',
                   'CSUS', 'DIST', 'SSSU', 'EPHD', 'EPHR', 'EPHN', 'INHP', 'NORD', 'PLUS', 'POUR', 'CITE',
                   'DAME', 'DIEU', 'TOUS', 'MERE', 'PORT', 'POST', 'GEND', 'GSPM', 'COTE', 'ZONE', 'GARE',
                   'CAMP', 'CARE', 'DISP', 'CENT', 'MATER');
$f$;

-- UUID v4 (identifiant FHIR) sans extension.
CREATE OR REPLACE FUNCTION clinlims.ref_uuid() RETURNS uuid
LANGUAGE sql VOLATILE AS $f$
  SELECT (substr(h, 1, 8) || '-' || substr(h, 9, 4) || '-4' || substr(h, 14, 3) || '-'
          || substr('89ab', (get_byte(decode(substr(h, 17, 2), 'hex'), 0) % 4) + 1, 1)
          || substr(h, 18, 3) || '-' || substr(h, 21, 12))::uuid
    FROM (SELECT md5(random()::text || clock_timestamp()::text) AS h) g;
$f$;

-- ---------------------------------------------------------- historique
-- Nombre de données qui citent une organisation : échantillons (site
-- demandeur), demandes électroniques, transferts, commandes, sous-organisations,
-- adresses de patients (région / district), etc. Sert à choisir l'organisation
-- qui survit à une fusion.
CREATE OR REPLACE FUNCTION clinlims.ref_historique(p_org numeric) RETURNS bigint
LANGUAGE sql STABLE AS $f$
  SELECT (SELECT count(*) FROM clinlims.sample_requester sr
           WHERE sr.requester_id = p_org
             AND sr.requester_type_id = (SELECT id FROM clinlims.requester_type WHERE requester_type = 'organization'))
       + (SELECT count(*) FROM clinlims.sample_organization WHERE org_id = p_org)
       + (SELECT count(*) FROM clinlims.referral WHERE organization_id = p_org)
       + (SELECT count(*) FROM clinlims.electronic_order WHERE organization_id = p_org)
       + (SELECT count(*) FROM clinlims.orders WHERE org_id = p_org)
       + (SELECT count(*) FROM clinlims.inventory_receipt WHERE org_id = p_org)
       + (SELECT count(*) FROM clinlims.test_section WHERE org_id = p_org)
       + (SELECT count(*) FROM clinlims.organization WHERE org_id = p_org)
       + (SELECT count(*) FROM clinlims.patient_identity pi
           JOIN clinlims.patient_identity_type pit ON pit.id = pi.identity_type_id
          WHERE pit.identity_type IN ('HEALTH REGION', 'HEALTH DISTRICT') AND pi.identity_data = p_org::text);
$f$;

-- --------------------------------------------------------------- fusion
-- Fusionne p_source DANS p_cible : toutes les données qui citaient la source
-- citent désormais la cible (chaque ligne modifiée est journalisée dans
-- ref_fusion_reference, ce qui permet d'annuler) ; tables à clé composée
-- (adresse, types, projets, encodage HL7) : complétées chez la cible, rien n'est
-- supprimé chez la source. La source est désactivée, fusionne_dans = cible.
-- Si la source porte un code du référentiel et pas la cible, la cible prend
-- l'identité de l'établissement (code, nom, parent, niveau). Retourne
-- l'identifiant de la fusion (ligne « fusion_source » du suivi).
CREATE OR REPLACE FUNCTION clinlims.ref_fusionner_organisations(p_source numeric, p_cible numeric,
                                                               p_motif text, p_version text)
RETURNS bigint LANGUAGE plpgsql AS $f$
DECLARE
  v_id bigint;
  v_req_type numeric;
  s clinlims.organization%ROWTYPE;
  c clinlims.organization%ROWTYPE;
  s_ref boolean;
  c_ref boolean;
  v_simple record;
  v_compose record;
BEGIN
  IF p_source = p_cible THEN
    RAISE EXCEPTION 'Fusion impossible : source et cible identiques (%)', p_source;
  END IF;
  SELECT * INTO s FROM clinlims.organization WHERE id = p_source;
  SELECT * INTO c FROM clinlims.organization WHERE id = p_cible;
  IF s.id IS NULL OR c.id IS NULL THEN
    RAISE EXCEPTION 'Fusion impossible : organisation % ou % introuvable', p_source, p_cible;
  END IF;
  s_ref := EXISTS (SELECT 1 FROM clinlims.ref_etablissement_dhis2 r WHERE r.facility_code = s.short_name);
  c_ref := EXISTS (SELECT 1 FROM clinlims.ref_etablissement_dhis2 r WHERE r.facility_code = c.short_name);
  IF s_ref AND c_ref AND s.short_name <> c.short_name THEN
    RAISE EXCEPTION 'Fusion impossible : % et % portent deux codes d''établissement différents (% / %)',
      p_source, p_cible, s.short_name, c.short_name;
  END IF;
  SELECT id INTO v_req_type FROM clinlims.requester_type WHERE requester_type = 'organization';

  INSERT INTO clinlims.ref_etablissement_rapprochement
         (org_id, cible_id, facility_code, decision, motif, ancien_nom, ancien_short_name, ancien_actif,
          ancien_org_parent, ancien_ref_statut, ancien_ref_niveau, version)
  VALUES (s.id, c.id, CASE WHEN s_ref THEN s.short_name END, 'fusion_source', p_motif, s.name, s.short_name,
          s.is_active, s.org_id, s.ref_statut, s.ref_niveau, p_version)
  RETURNING id INTO v_id;
  INSERT INTO clinlims.ref_etablissement_rapprochement
         (org_id, cible_id, facility_code, decision, motif, ancien_nom, ancien_short_name, ancien_actif,
          ancien_org_parent, ancien_ref_statut, ancien_ref_niveau, version)
  VALUES (c.id, v_id, CASE WHEN c_ref THEN c.short_name END, 'fusion_cible', p_motif, c.name, c.short_name,
          c.is_active, c.org_id, c.ref_statut, c.ref_niveau, p_version);

  -- Références simples (clé primaire unique) : déplacées et journalisées.
  FOR v_simple IN SELECT * FROM (VALUES
      ('sample_requester', 'id', 'requester_id', format('requester_type_id = %s', coalesce(v_req_type, -1))),
      ('sample_organization', 'id', 'org_id', 'true'),
      ('referral', 'id', 'organization_id', 'true'),
      ('electronic_order', 'id', 'organization_id', 'true'),
      ('orders', 'id', 'org_id', 'true'),
      ('inventory_receipt', 'id', 'org_id', 'true'),
      ('test_section', 'id', 'org_id', 'true'),
      ('organization_contact', 'id', 'organization_id', 'true'),
      ('org_mls_lab_type', 'org_mlt_id', 'org_id', 'true'),
      ('organization', 'id', 'org_id', format('id <> %s', p_cible))) AS t(tab, pk, col, cond)
  LOOP
    EXECUTE format('INSERT INTO clinlims.ref_fusion_reference (fusion_id, table_name, pk_colonnes, pk_valeurs, '
                   || 'colonne, ancienne_valeur, nouvelle_valeur, operation) '
                   || 'SELECT %s, %L, %L, %I::text, %L, %L, %L, ''maj'' FROM clinlims.%I WHERE %I = %s AND %s',
                   v_id, v_simple.tab, v_simple.pk, v_simple.pk, v_simple.col, p_source, p_cible,
                   v_simple.tab, v_simple.col, p_source, v_simple.cond);
    EXECUTE format('UPDATE clinlims.%I SET %I = %s WHERE %I = %s AND %s',
                   v_simple.tab, v_simple.col, p_cible, v_simple.col, p_source, v_simple.cond);
  END LOOP;

  -- Adresse des patients (région / district) et site du laboratoire : texte.
  INSERT INTO clinlims.ref_fusion_reference (fusion_id, table_name, pk_colonnes, pk_valeurs, colonne,
                                            ancienne_valeur, nouvelle_valeur, operation)
  SELECT v_id, 'patient_identity', 'id', pi.id::text, 'identity_data', p_source::text, p_cible::text, 'maj'
    FROM clinlims.patient_identity pi JOIN clinlims.patient_identity_type pit ON pit.id = pi.identity_type_id
   WHERE pit.identity_type IN ('HEALTH REGION', 'HEALTH DISTRICT') AND pi.identity_data = p_source::text;
  UPDATE clinlims.patient_identity pi SET identity_data = p_cible::text
    FROM clinlims.patient_identity_type pit
   WHERE pit.id = pi.identity_type_id AND pit.identity_type IN ('HEALTH REGION', 'HEALTH DISTRICT')
     AND pi.identity_data = p_source::text;
  INSERT INTO clinlims.ref_fusion_reference (fusion_id, table_name, pk_colonnes, pk_valeurs, colonne,
                                            ancienne_valeur, nouvelle_valeur, operation)
  SELECT v_id, 'site_information', 'id', id::text, 'value', p_source::text, p_cible::text, 'maj'
    FROM clinlims.site_information WHERE name = 'siteId' AND trim(value) = p_source::text;
  UPDATE clinlims.site_information SET value = p_cible::text, lastupdated = now()
   WHERE name = 'siteId' AND trim(value) = p_source::text;

  -- Tables à clé composée : lignes manquantes ajoutées chez la cible (journal
  -- « ajout » : supprimées à l'annulation) ; celles de la source restent.
  FOR v_compose IN SELECT * FROM (VALUES
      ('organization_organization_type', 'org_id', 'org_type_id'),
      ('organization_address', 'organization_id', 'address_part_id'),
      ('project_organization', 'org_id', 'project_id'),
      ('org_hl7_encoding_type', 'organization_id', 'encoding_type_id')) AS t(tab, col, autre)
  LOOP
    EXECUTE format('INSERT INTO clinlims.ref_fusion_reference (fusion_id, table_name, pk_colonnes, pk_valeurs, '
                   || 'colonne, ancienne_valeur, nouvelle_valeur, operation) '
                   || 'SELECT %s, %L, %L, %L || '','' || x.%I::text, %L, NULL, %L, ''ajout'' FROM clinlims.%I x '
                   || 'WHERE x.%I = %s AND NOT EXISTS (SELECT 1 FROM clinlims.%I y WHERE y.%I = %s AND y.%I = x.%I)',
                   v_id, v_compose.tab, v_compose.col || ',' || v_compose.autre, p_cible::text, v_compose.autre,
                   v_compose.col, p_cible, v_compose.tab, v_compose.col, p_source, v_compose.tab, v_compose.col,
                   p_cible, v_compose.autre, v_compose.autre);
  END LOOP;
  -- Copie effective des lignes composées.
  INSERT INTO clinlims.organization_organization_type (org_id, org_type_id)
  SELECT p_cible, x.org_type_id FROM clinlims.organization_organization_type x
   WHERE x.org_id = p_source AND NOT EXISTS (SELECT 1 FROM clinlims.organization_organization_type y
                                             WHERE y.org_id = p_cible AND y.org_type_id = x.org_type_id);
  INSERT INTO clinlims.organization_address (organization_id, address_part_id, type, value)
  SELECT p_cible, x.address_part_id, x.type, x.value FROM clinlims.organization_address x
   WHERE x.organization_id = p_source AND NOT EXISTS (SELECT 1 FROM clinlims.organization_address y
                                                      WHERE y.organization_id = p_cible
                                                        AND y.address_part_id = x.address_part_id);
  INSERT INTO clinlims.project_organization (project_id, org_id)
  SELECT x.project_id, p_cible FROM clinlims.project_organization x
   WHERE x.org_id = p_source AND NOT EXISTS (SELECT 1 FROM clinlims.project_organization y
                                             WHERE y.org_id = p_cible AND y.project_id = x.project_id);
  INSERT INTO clinlims.org_hl7_encoding_type (organization_id, encoding_type_id, lastupdated)
  SELECT p_cible, x.encoding_type_id, now() FROM clinlims.org_hl7_encoding_type x
   WHERE x.organization_id = p_source AND NOT EXISTS (SELECT 1 FROM clinlims.org_hl7_encoding_type y
                                                      WHERE y.organization_id = p_cible
                                                        AND y.encoding_type_id = x.encoding_type_id);

  -- Identité : la cible prend celle de l'établissement si c'est la source qui la
  -- portait ; statut FUSIONNE dès qu'un établissement du référentiel est en jeu.
  IF s_ref AND NOT c_ref THEN
    UPDATE clinlims.organization SET short_name = NULL WHERE id = s.id;
    UPDATE clinlims.organization
       SET short_name = s.short_name, name = s.name, org_id = s.org_id, ref_niveau = s.ref_niveau
     WHERE id = c.id;
  ELSIF s_ref THEN
    UPDATE clinlims.organization SET short_name = NULL WHERE id = s.id;
  END IF;
  UPDATE clinlims.organization
     SET is_active = 'Y',
         ref_statut = CASE WHEN s_ref OR c_ref OR s.ref_statut IN ('MFL', 'FUSIONNE')
                                OR c.ref_statut IN ('MFL', 'FUSIONNE') THEN 'FUSIONNE' ELSE c.ref_statut END,
         lastupdated = now()
   WHERE id = c.id;
  -- La source, désactivée, reste une organisation historique (LEGACY) qui pointe
  -- vers celle qui a reçu son historique.
  UPDATE clinlims.organization
     SET is_active = 'N', fusionne_dans = c.id, ref_statut = coalesce(s.ref_statut, 'LEGACY'),
         ref_niveau = coalesce(s.ref_niveau, c.ref_niveau), lastupdated = now()
   WHERE id = s.id;
  RETURN v_id;
END;
$f$;

-- Fusionne deux organisations en gardant celle qui a le plus d'historique (à
-- égalité : le site du laboratoire, puis l'active, puis le plus petit id), sauf
-- si p_survivante est précisée. Retourne l'identifiant de la fusion.
CREATE OR REPLACE FUNCTION clinlims.ref_fusionner(p_a numeric, p_b numeric, p_survivante numeric DEFAULT NULL,
                                                 p_motif text DEFAULT 'fusion manuelle',
                                                 p_version text DEFAULT 'manuelle')
RETURNS bigint LANGUAGE plpgsql AS $f$
DECLARE
  v_site text;
  v_cible numeric;
BEGIN
  IF p_survivante IS NOT NULL AND p_survivante NOT IN (p_a, p_b) THEN
    RAISE EXCEPTION 'L''organisation survivante (%) doit être l''une des deux fusionnées', p_survivante;
  END IF;
  SELECT nullif(trim(value), '') INTO v_site FROM clinlims.site_information WHERE name = 'siteId';
  v_cible := coalesce(p_survivante,
    (SELECT o.id FROM clinlims.organization o WHERE o.id IN (p_a, p_b)
      ORDER BY clinlims.ref_historique(o.id) DESC, (o.id::text = coalesce(v_site, '')) DESC,
               (o.is_active = 'Y') DESC, o.id
      LIMIT 1));
  RETURN clinlims.ref_fusionner_organisations(CASE WHEN v_cible = p_a THEN p_b ELSE p_a END, v_cible,
                                              p_motif, p_version);
END;
$f$;

-- Annule une fusion : références remises sur la source, lignes ajoutées chez la
-- cible supprimées, état des deux organisations restauré.
CREATE OR REPLACE FUNCTION clinlims.ref_annuler_fusion(p_fusion_id bigint) RETURNS void
LANGUAGE plpgsql AS $f$
DECLARE
  j record;
  v_where text;
  st record;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM clinlims.ref_etablissement_rapprochement
                  WHERE id = p_fusion_id AND decision = 'fusion_source') THEN
    RAISE EXCEPTION 'Fusion % introuvable ou déjà annulée', p_fusion_id;
  END IF;
  FOR j IN SELECT * FROM clinlims.ref_fusion_reference WHERE fusion_id = p_fusion_id ORDER BY id DESC LOOP
    SELECT string_agg(format('%I::text = %L', k, v), ' AND ')
      INTO v_where
      FROM unnest(string_to_array(j.pk_colonnes, ','), string_to_array(j.pk_valeurs, ',')) AS u(k, v);
    IF j.operation = 'maj' THEN
      EXECUTE format('UPDATE clinlims.%I SET %I = %L WHERE %s', j.table_name, j.colonne, j.ancienne_valeur, v_where);
    ELSE
      EXECUTE format('DELETE FROM clinlims.%I WHERE %s', j.table_name, v_where);
    END IF;
  END LOOP;
  FOR st IN SELECT * FROM clinlims.ref_etablissement_rapprochement
             WHERE (id = p_fusion_id AND decision = 'fusion_source')
                OR (cible_id = p_fusion_id AND decision = 'fusion_cible') LOOP
    UPDATE clinlims.organization
       SET name = coalesce(st.ancien_nom, name), short_name = st.ancien_short_name,
           is_active = st.ancien_actif, org_id = st.ancien_org_parent,
           ref_statut = st.ancien_ref_statut, ref_niveau = st.ancien_ref_niveau, fusionne_dans = NULL,
           lastupdated = now()
     WHERE id = st.org_id;
  END LOOP;
  DELETE FROM clinlims.ref_fusion_reference WHERE fusion_id = p_fusion_id;
  UPDATE clinlims.ref_etablissement_rapprochement SET decision = decision || '_annulee'
   WHERE (id = p_fusion_id AND decision = 'fusion_source') OR (cible_id = p_fusion_id AND decision = 'fusion_cible');
END;
$f$;

-- ------------------------------------------------------- synchronisation
-- Aligne les organisations sur le référentiel. Rejouable. Retourne le bilan des
-- décisions de cette exécution (détail : ref_etablissement_rapprochement).
CREATE OR REPLACE FUNCTION clinlims.sync_referentiel_etablissements(p_version text)
RETURNS TABLE(decision text, nombre bigint)
LANGUAGE plpgsql AS $f$
DECLARE
  -- now() = début de la transaction, comme la valeur par défaut de date_decision
  v_debut timestamp := now();
  v_type_site numeric;
  v_type_region numeric;
  v_type_district numeric;
  v_site text;
  v_perdant record;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM clinlims.ref_etablissement_dhis2) THEN
    RAISE NOTICE 'Référentiel des établissements vide : rien à synchroniser';
    RETURN;
  END IF;
  SELECT id INTO v_type_site FROM clinlims.organization_type WHERE short_name = 'referring clinic';
  IF v_type_site IS NULL THEN
    RAISE NOTICE 'Type d''organisation « referring clinic » absent : synchronisation ignorée';
    RETURN;
  END IF;
  SELECT id INTO v_type_region FROM clinlims.organization_type WHERE short_name = 'Health Region';
  IF v_type_region IS NULL THEN
    INSERT INTO clinlims.organization_type (id, short_name, description, lastupdated)
    VALUES (nextval('clinlims.organization_type_seq'), 'Health Region', 'Région sanitaire', now())
    RETURNING id INTO v_type_region;
  END IF;
  SELECT id INTO v_type_district FROM clinlims.organization_type WHERE short_name = 'Health District';
  IF v_type_district IS NULL THEN
    INSERT INTO clinlims.organization_type (id, short_name, description, lastupdated)
    VALUES (nextval('clinlims.organization_type_seq'), 'Health District', 'District sanitaire', now())
    RETURNING id INTO v_type_district;
  END IF;
  SELECT nullif(trim(value), '') INTO v_site FROM clinlims.site_information WHERE name = 'siteId';

  -- ======================================== régions et districts du MFL
  CREATE TEMP TABLE _region ON COMMIT DROP AS
    SELECT region_code AS code, min(region_libelle) AS nom
      FROM clinlims.ref_etablissement_dhis2 WHERE coalesce(region_code, '') <> '' GROUP BY region_code;
  CREATE TEMP TABLE _district ON COMMIT DROP AS
    SELECT district_code AS code, min(district_libelle) AS nom, min(region_code) AS region_code
      FROM clinlims.ref_etablissement_dhis2 WHERE coalesce(district_code, '') <> '' GROUP BY district_code;

  -- Régions : mise à jour du nom, création des absentes.
  INSERT INTO clinlims.ref_etablissement_rapprochement
         (org_id, facility_code, decision, motif, ancien_nom, ancien_short_name, ancien_actif, ancien_org_parent,
          ancien_ref_statut, ancien_ref_niveau, version)
  SELECT o.id, r.code, 'mise_a_jour', 'région du référentiel', o.name, o.short_name, o.is_active, o.org_id,
         o.ref_statut, o.ref_niveau, p_version
    FROM clinlims.organization o JOIN _region r ON r.code = o.short_name
   WHERE o.ref_niveau = 'REGION' AND o.ref_statut IN ('MFL', 'FUSIONNE') AND o.fusionne_dans IS NULL
     AND (o.name IS DISTINCT FROM left(r.nom, 80) OR o.is_active IS DISTINCT FROM 'Y');
  UPDATE clinlims.organization o SET name = left(r.nom, 80), is_active = 'Y', lastupdated = now()
    FROM _region r
   WHERE r.code = o.short_name AND o.ref_niveau = 'REGION' AND o.ref_statut IN ('MFL', 'FUSIONNE')
     AND o.fusionne_dans IS NULL AND (o.name IS DISTINCT FROM left(r.nom, 80) OR o.is_active IS DISTINCT FROM 'Y');
  CREATE TEMP TABLE _nouvelle_region ON COMMIT DROP AS
    SELECT nextval('clinlims.organization_seq') AS id, r.code, r.nom FROM _region r
     WHERE NOT EXISTS (SELECT 1 FROM clinlims.organization o WHERE o.short_name = r.code AND o.ref_niveau = 'REGION'
                         AND o.ref_statut IN ('MFL', 'FUSIONNE') AND o.fusionne_dans IS NULL);
  INSERT INTO clinlims.organization (id, name, short_name, is_active, mls_sentinel_lab_flag, mls_lab_flag,
                                     lastupdated, fhir_uuid, ref_statut, ref_niveau)
  SELECT id, left(nom, 80), code, 'Y', 'N', 'N', now(), clinlims.ref_uuid(), 'MFL', 'REGION' FROM _nouvelle_region;
  INSERT INTO clinlims.organization_organization_type (org_id, org_type_id) SELECT id, v_type_region FROM _nouvelle_region;
  INSERT INTO clinlims.ref_etablissement_rapprochement (org_id, facility_code, decision, motif, version)
  SELECT id, code, 'creee', 'région du référentiel', p_version FROM _nouvelle_region;

  -- Districts : nom et région parente à jour, création des absents.
  CREATE TEMP TABLE _region_org ON COMMIT DROP AS
    SELECT DISTINCT ON (o.short_name) o.short_name AS code, o.id FROM clinlims.organization o
     WHERE o.ref_niveau = 'REGION' AND o.ref_statut IN ('MFL', 'FUSIONNE') AND o.fusionne_dans IS NULL
     ORDER BY o.short_name, o.id;
  INSERT INTO clinlims.ref_etablissement_rapprochement
         (org_id, facility_code, decision, motif, ancien_nom, ancien_short_name, ancien_actif, ancien_org_parent,
          ancien_ref_statut, ancien_ref_niveau, version)
  SELECT o.id, d.code, 'mise_a_jour', 'district du référentiel', o.name, o.short_name, o.is_active, o.org_id,
         o.ref_statut, o.ref_niveau, p_version
    FROM clinlims.organization o JOIN _district d ON d.code = o.short_name JOIN _region_org ro ON ro.code = d.region_code
   WHERE o.ref_niveau = 'DISTRICT' AND o.ref_statut IN ('MFL', 'FUSIONNE') AND o.fusionne_dans IS NULL
     AND (o.name IS DISTINCT FROM left(d.nom, 80) OR o.org_id IS DISTINCT FROM ro.id OR o.is_active IS DISTINCT FROM 'Y');
  UPDATE clinlims.organization o SET name = left(d.nom, 80), org_id = ro.id, is_active = 'Y', lastupdated = now()
    FROM _district d JOIN _region_org ro ON ro.code = d.region_code
   WHERE d.code = o.short_name AND o.ref_niveau = 'DISTRICT' AND o.ref_statut IN ('MFL', 'FUSIONNE')
     AND o.fusionne_dans IS NULL
     AND (o.name IS DISTINCT FROM left(d.nom, 80) OR o.org_id IS DISTINCT FROM ro.id OR o.is_active IS DISTINCT FROM 'Y');
  CREATE TEMP TABLE _nouveau_district ON COMMIT DROP AS
    SELECT nextval('clinlims.organization_seq') AS id, d.code, d.nom, ro.id AS region_id
      FROM _district d LEFT JOIN _region_org ro ON ro.code = d.region_code
     WHERE NOT EXISTS (SELECT 1 FROM clinlims.organization o WHERE o.short_name = d.code AND o.ref_niveau = 'DISTRICT'
                         AND o.ref_statut IN ('MFL', 'FUSIONNE') AND o.fusionne_dans IS NULL);
  INSERT INTO clinlims.organization (id, name, short_name, is_active, mls_sentinel_lab_flag, mls_lab_flag,
                                     lastupdated, fhir_uuid, ref_statut, ref_niveau, org_id)
  SELECT id, left(nom, 80), code, 'Y', 'N', 'N', now(), clinlims.ref_uuid(), 'MFL', 'DISTRICT', region_id
    FROM _nouveau_district;
  INSERT INTO clinlims.organization_organization_type (org_id, org_type_id) SELECT id, v_type_district FROM _nouveau_district;
  INSERT INTO clinlims.ref_etablissement_rapprochement (org_id, facility_code, decision, motif, version)
  SELECT id, code, 'creee', 'district du référentiel', p_version FROM _nouveau_district;

  CREATE TEMP TABLE _district_org ON COMMIT DROP AS
    SELECT DISTINCT ON (o.short_name) o.short_name AS code, o.id FROM clinlims.organization o
     WHERE o.ref_niveau = 'DISTRICT' AND o.ref_statut IN ('MFL', 'FUSIONNE') AND o.fusionne_dans IS NULL
     ORDER BY o.short_name, o.id;

  -- ============================================== état des organisations
  CREATE TEMP TABLE _org ON COMMIT DROP AS
    SELECT o.id, o.name, o.short_name, o.is_active, o.org_id, o.ref_statut, o.ref_niveau,
           regexp_replace(coalesce(o.short_name, ''), '\s', '', 'g') AS code,
           coalesce(o.id::text = v_site, false) AS est_site,
           coalesce((SELECT array_agg(t.short_name::text)
                       FROM clinlims.organization_organization_type ot
                       JOIN clinlims.organization_type t ON t.id = ot.org_type_id
                      WHERE ot.org_id = o.id), '{}') AS types
      FROM clinlims.organization o
     WHERE o.fusionne_dans IS NULL;
  ALTER TABLE _org ADD COLUMN type_site boolean, ADD COLUMN type_region_district boolean,
                   ADD COLUMN type_fonctionnel boolean, ADD COLUMN eligible boolean;
  UPDATE _org SET
    type_site = EXISTS (SELECT 1 FROM unnest(types) ty
                         WHERE ty IN ('referring clinic', 'ARV Service Loc', 'patient referral')
                            OR ty LIKE 'EID %' OR ty LIKE 'RTN HIV %'),
    type_region_district = types && ARRAY['Health Region', 'Health District'],
    type_fonctionnel = EXISTS (SELECT 1 FROM unnest(types) ty
                                WHERE NOT (ty IN ('referring clinic', 'ARV Service Loc', 'patient referral',
                                                  'Health Region', 'Health District')
                                           OR ty LIKE 'EID %' OR ty LIKE 'RTN HIV %'));
  -- Établissements éligibles au rattachement : types de site uniquement (pas de
  -- type région, district, laboratoire de référence, fournisseur, service…) ;
  -- le site du laboratoire l'est toujours.
  UPDATE _org SET eligible = est_site OR (type_site AND NOT type_region_district AND NOT type_fonctionnel
                                          AND coalesce(ref_niveau, 'ETABLISSEMENT') = 'ETABLISSEMENT');

  -- Anciennes régions et districts (hors MFL) : LEGACY, désactivés.
  INSERT INTO clinlims.ref_etablissement_rapprochement
         (org_id, decision, motif, ancien_nom, ancien_short_name, ancien_actif, ancien_org_parent,
          ancien_ref_statut, ancien_ref_niveau, version)
  SELECT o.id, 'desactivee', 'région ou district hors référentiel', o.name, o.short_name, o.is_active,
         o.org_id, o.ref_statut, o.ref_niveau, p_version
    FROM _org o
   WHERE o.type_region_district AND NOT o.type_site AND NOT o.est_site AND o.is_active = 'Y'
     AND coalesce(o.ref_statut, 'LEGACY') = 'LEGACY';
  UPDATE clinlims.organization g
     SET ref_statut = 'LEGACY',
         ref_niveau = CASE WHEN o.types @> ARRAY['Health Region'] THEN 'REGION' ELSE 'DISTRICT' END,
         -- seules les actives sont désactivées (et tracées ci-dessus)
         is_active = CASE WHEN g.is_active = 'Y' AND NOT o.est_site THEN 'N' ELSE g.is_active END,
         lastupdated = now()
    FROM _org o
   WHERE g.id = o.id AND o.type_region_district AND NOT o.type_site
     AND coalesce(o.ref_statut, 'LEGACY') = 'LEGACY'
     AND (g.is_active = 'Y' OR g.ref_statut IS NULL OR g.ref_niveau IS NULL);

  -- ===================================================== établissements
  -- Nom de référence : nom DHIS2, sinon nom court ; 80 caractères au plus.
  CREATE TEMP TABLE _ref ON COMMIT DROP AS
    SELECT facility_code, district_code,
           CASE WHEN length(coalesce(nullif(nom_dhis2, ''), nom_court)) <= 80
                THEN coalesce(nullif(nom_dhis2, ''), nom_court)
                WHEN length(nom_court) <= 80 THEN nom_court
                ELSE left(coalesce(nullif(nom_dhis2, ''), nom_court), 80) END AS nom,
           concat_ws(' ', nom_dhis2, nom_court, nom_long) AS noms
    FROM clinlims.ref_etablissement_dhis2;
  -- Noms (normalisés) qui ne désignent qu'un seul établissement.
  CREATE TEMP TABLE _ref_nom ON COMMIT DROP AS
    SELECT nn AS nom_norm, min(facility_code) AS facility_code
      FROM (SELECT facility_code, clinlims.ref_nom_normalise(n) AS nn
              FROM clinlims.ref_etablissement_dhis2, LATERAL unnest(ARRAY[nom_dhis2, nom_court, nom_long]) AS n
             WHERE coalesce(n, '') <> '') s
     GROUP BY nn HAVING count(DISTINCT facility_code) = 1;

  -- 1. Revendications (organisations éligibles) : code (exact ou zéros
  --    rétablis) + nom cohérent ; titulaire précédent (statut MFL ou FUSIONNE) ;
  --    à défaut, nom identique à celui d'un seul établissement.
  CREATE TEMP TABLE _claim ON COMMIT DROP AS
    SELECT o.id, r.facility_code,
           CASE WHEN o.code = r.facility_code THEN 'code exact, nom cohérent'
                ELSE 'code ' || o.code || ' complété en ' || r.facility_code || ', nom cohérent' END AS motif
      FROM _org o
      JOIN _ref r ON r.facility_code = CASE WHEN o.code ~ '^[0-9]{1,5}$' THEN lpad(o.code, 5, '0') END
     WHERE o.eligible AND (clinlims.ref_nom_coherent(o.name, r.noms)
                           OR (o.code = r.facility_code AND o.ref_statut IN ('MFL', 'FUSIONNE')));
  INSERT INTO _claim
    SELECT o.id, n.facility_code, 'nom identique au référentiel'
      FROM _org o JOIN _ref_nom n ON n.nom_norm = clinlims.ref_nom_normalise(o.name)
     WHERE o.eligible AND clinlims.ref_nom_normalise(o.name) <> ''
       AND NOT EXISTS (SELECT 1 FROM _claim c WHERE c.id = o.id);

  -- 2. Un titulaire par code : site du laboratoire, puis le plus d'historique,
  --    puis le titulaire précédent, puis l'active, puis le plus petit id.
  CREATE TEMP TABLE _titulaire ON COMMIT DROP AS
    SELECT DISTINCT ON (c.facility_code) c.facility_code, c.id, c.motif
      FROM _claim c JOIN _org o ON o.id = c.id
     ORDER BY c.facility_code, o.est_site DESC, clinlims.ref_historique(o.id) DESC,
              (o.ref_statut IN ('MFL', 'FUSIONNE')) DESC NULLS LAST, (o.is_active = 'Y') DESC, o.id;

  -- 3. Titulaires : nom, code, actif, statut, niveau, district parent.
  INSERT INTO clinlims.ref_etablissement_rapprochement
         (org_id, facility_code, decision, motif, ancien_nom, ancien_short_name, ancien_actif, ancien_org_parent,
          ancien_ref_statut, ancien_ref_niveau, version)
  SELECT o.id, t.facility_code, CASE WHEN o.code = t.facility_code THEN 'mise_a_jour' ELSE 'rattachee' END,
         t.motif, o.name, o.short_name, o.is_active, o.org_id, o.ref_statut, o.ref_niveau, p_version
    FROM _titulaire t JOIN _org o ON o.id = t.id JOIN _ref r ON r.facility_code = t.facility_code
    LEFT JOIN _district_org d ON d.code = r.district_code
   WHERE o.code IS DISTINCT FROM t.facility_code OR o.name IS DISTINCT FROM r.nom
      OR o.is_active IS DISTINCT FROM 'Y' OR o.org_id IS DISTINCT FROM d.id
      OR o.ref_niveau IS DISTINCT FROM 'ETABLISSEMENT' OR o.ref_statut NOT IN ('MFL', 'FUSIONNE')
      OR o.ref_statut IS NULL;
  UPDATE clinlims.organization g
     SET name = r.nom, short_name = t.facility_code, is_active = 'Y', org_id = d.id,
         ref_niveau = 'ETABLISSEMENT',
         ref_statut = CASE WHEN g.ref_statut = 'MFL' THEN 'MFL' ELSE 'FUSIONNE' END, lastupdated = now()
    FROM _titulaire t JOIN _ref r ON r.facility_code = t.facility_code
    LEFT JOIN _district_org d ON d.code = r.district_code
   WHERE g.id = t.id
     AND (g.short_name IS DISTINCT FROM t.facility_code OR g.name IS DISTINCT FROM r.nom
          OR g.is_active IS DISTINCT FROM 'Y' OR g.org_id IS DISTINCT FROM d.id
          OR g.ref_niveau IS DISTINCT FROM 'ETABLISSEMENT' OR g.ref_statut IS NULL
          OR g.ref_statut NOT IN ('MFL', 'FUSIONNE'));
  INSERT INTO clinlims.organization_organization_type (org_id, org_type_id)
  SELECT t.id, v_type_site FROM _titulaire t
   WHERE NOT EXISTS (SELECT 1 FROM clinlims.organization_organization_type ot
                      WHERE ot.org_id = t.id AND ot.org_type_id = v_type_site);

  -- 4. Autres revendications cohérentes du même code : c'est le même
  --    établissement, l'historique est fusionné dans le titulaire.
  FOR v_perdant IN
    SELECT c.id, t.id AS cible, t.facility_code, o.code
      FROM _claim c JOIN _titulaire t ON t.facility_code = c.facility_code JOIN _org o ON o.id = c.id
     WHERE c.id <> t.id ORDER BY c.id
  LOOP
    IF v_perdant.code ~ '^[0-9]{5}$' AND v_perdant.code <> v_perdant.facility_code THEN
      -- code faux (rapprochement par le nom) : retiré avant la fusion
      UPDATE clinlims.organization SET short_name = NULL WHERE id = v_perdant.id;
    END IF;
    PERFORM clinlims.ref_fusionner_organisations(v_perdant.id, v_perdant.cible,
      'même établissement ' || v_perdant.facility_code || ' (code et nom cohérents)', p_version);
  END LOOP;

  -- 5. Toute autre organisation qui porte un code du référentiel sans en être
  --    titulaire (nom incohérent, non éligible) : le code lui est retiré.
  INSERT INTO clinlims.ref_etablissement_rapprochement
         (org_id, facility_code, decision, motif, ancien_nom, ancien_short_name, ancien_actif, ancien_org_parent,
          ancien_ref_statut, ancien_ref_niveau, version)
  SELECT g.id, g.short_name, 'code_retire',
         CASE WHEN o.eligible THEN 'nom incohérent avec l''établissement ' || g.short_name
              ELSE 'organisation non éligible (type région, district, laboratoire…)' END,
         g.name, g.short_name, g.is_active, g.org_id, g.ref_statut, g.ref_niveau, p_version
    FROM clinlims.organization g JOIN _org o ON o.id = g.id JOIN _ref r ON r.facility_code = g.short_name
   WHERE g.fusionne_dans IS NULL AND NOT EXISTS (SELECT 1 FROM _titulaire t WHERE t.id = g.id);
  UPDATE clinlims.organization g SET short_name = NULL, lastupdated = now()
    FROM _org o, _ref r
   WHERE g.id = o.id AND r.facility_code = g.short_name AND g.fusionne_dans IS NULL
     AND NOT EXISTS (SELECT 1 FROM _titulaire t WHERE t.id = g.id);

  -- 6. Établissements sans organisation : créés.
  CREATE TEMP TABLE _nouveau ON COMMIT DROP AS
    SELECT nextval('clinlims.organization_seq') AS id, r.facility_code, r.nom, d.id AS district_id
      FROM _ref r LEFT JOIN _district_org d ON d.code = r.district_code
     WHERE NOT EXISTS (SELECT 1 FROM _titulaire t WHERE t.facility_code = r.facility_code);
  INSERT INTO clinlims.organization (id, name, short_name, is_active, mls_sentinel_lab_flag, mls_lab_flag,
                                     lastupdated, fhir_uuid, ref_statut, ref_niveau, org_id)
  SELECT id, nom, facility_code, 'Y', 'N', 'N', now(), clinlims.ref_uuid(), 'MFL', 'ETABLISSEMENT', district_id
    FROM _nouveau;
  INSERT INTO clinlims.organization_organization_type (org_id, org_type_id) SELECT id, v_type_site FROM _nouveau;
  INSERT INTO clinlims.ref_etablissement_rapprochement (org_id, facility_code, decision, motif, version)
  SELECT id, facility_code, 'creee', 'établissement du référentiel', p_version FROM _nouveau;

  -- 7. Établissements éligibles non titulaires : LEGACY ; désactivés s'ils
  --    étaient actifs (jamais le site du laboratoire).
  INSERT INTO clinlims.ref_etablissement_rapprochement
         (org_id, decision, motif, ancien_nom, ancien_short_name, ancien_actif, ancien_org_parent,
          ancien_ref_statut, ancien_ref_niveau, version)
  SELECT g.id, 'desactivee', 'site hors référentiel', g.name, g.short_name, g.is_active, g.org_id,
         g.ref_statut, g.ref_niveau, p_version
    FROM clinlims.organization g JOIN _org o ON o.id = g.id
   WHERE o.eligible AND NOT o.est_site AND g.is_active = 'Y' AND g.fusionne_dans IS NULL
     AND NOT EXISTS (SELECT 1 FROM _titulaire t WHERE t.id = g.id);
  UPDATE clinlims.organization g
     SET ref_statut = 'LEGACY', ref_niveau = 'ETABLISSEMENT',
         -- seules les actives sont désactivées (et tracées ci-dessus)
         is_active = CASE WHEN g.is_active = 'Y' AND NOT o.est_site THEN 'N' ELSE g.is_active END,
         lastupdated = now()
    FROM _org o
   WHERE g.id = o.id AND o.eligible AND g.fusionne_dans IS NULL
     AND NOT EXISTS (SELECT 1 FROM _titulaire t WHERE t.id = g.id)
     AND (g.is_active = 'Y' OR g.ref_statut IS DISTINCT FROM 'LEGACY' OR g.ref_niveau IS DISTINCT FROM 'ETABLISSEMENT')
     AND NOT (o.est_site AND g.ref_statut = 'LEGACY' AND g.ref_niveau = 'ETABLISSEMENT');

  -- 8. Autres organisations (laboratoires de référence, fournisseurs, sites à
  --    types mêlés…) : LEGACY, inchangées par ailleurs.
  UPDATE clinlims.organization g SET ref_statut = 'LEGACY'
    FROM _org o
   WHERE g.id = o.id AND g.ref_statut IS NULL AND NOT o.eligible AND g.fusionne_dans IS NULL;

  -- 9. Site du laboratoire sans code d'établissement : signalé.
  IF v_site IS NOT NULL AND NOT EXISTS (SELECT 1 FROM _titulaire t WHERE t.id::text = v_site) THEN
    INSERT INTO clinlims.ref_etablissement_rapprochement (org_id, decision, motif, version)
    SELECT o.id, 'site_sans_code', 'renseigner son code d''établissement puis rejouer la synchronisation',
           p_version FROM _org o WHERE o.est_site;
    RAISE NOTICE 'Le site du laboratoire (siteId = %) n''est rattaché à aucun établissement du référentiel', v_site;
  END IF;

  RETURN QUERY
    SELECT x.decision::text, count(*) FROM clinlims.ref_etablissement_rapprochement x
     WHERE x.date_decision >= v_debut GROUP BY x.decision ORDER BY x.decision;
END;
$f$;
