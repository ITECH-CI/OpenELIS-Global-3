-- ============================================================================
-- CDC TRIGGERS — remontée consolidée (module d'échange unifié, incrément 4b)
-- ============================================================================
-- Projette tout changement des tables métier (analysis, result, sample,
-- sample_item, patient, patient_identity, observation_history, organization,
-- sample_organization, sample_requester) dans la file `analysis_sync_status`
-- (upload_flag : 1=TO_INSERT, 2=TO_UPDATE, 3=UP_TO_DATE, 4=IN_PROGRESS), que le
-- service DataSync draine pour remonter vers le serveur consolidé — SANS toucher
-- au code métier OE (découplage CDC). Rapatrié fidèlement de oedatauploader
-- (optimized_sync_triggers.sql v3), qui l'exécute en prod.
--
-- Idempotent : DROP TRIGGER/FUNCTION IF EXISTS + CREATE OR REPLACE → rejouable.
-- Anti-régression : les UPDATE ne passent 3→2 que si upload_flag = 3 (jamais de
-- rétrogradation d'une ligne 1/2/4 en cours). WHEN (OLD.x IS DISTINCT FROM NEW.x)
-- borne les déclenchements aux changements significatifs. EXCEPTION WHEN OTHERS
-- non bloquant : un échec de trigger ne casse jamais la transaction métier.
--
-- Délimiteur d'instruction : ~ (le changeSet Liquibase déclare endDelimiter="~").
-- ============================================================================

-- ============================================
-- DROP EXISTING TRIGGERS AND FUNCTIONS
-- ============================================
DROP TRIGGER IF EXISTS analysis_insert_trigger ON analysis CASCADE~
DROP TRIGGER IF EXISTS sample_update_trigger ON sample CASCADE~
DROP TRIGGER IF EXISTS sampleitem_update_trigger ON sample_item CASCADE~
DROP TRIGGER IF EXISTS analysis_update_trigger ON analysis CASCADE~
DROP TRIGGER IF EXISTS result_update_trigger ON result CASCADE~
DROP TRIGGER IF EXISTS result_insert_trigger ON result CASCADE~
DROP TRIGGER IF EXISTS patient_update_trigger ON patient CASCADE~
DROP TRIGGER IF EXISTS patient_identity_trigger ON patient_identity CASCADE~
DROP TRIGGER IF EXISTS patient_identity_insert_trigger ON patient_identity CASCADE~
DROP TRIGGER IF EXISTS patient_identity_update_trigger ON patient_identity CASCADE~
DROP TRIGGER IF EXISTS patient_identity_delete_trigger ON patient_identity CASCADE~
DROP TRIGGER IF EXISTS observation_history_trigger ON observation_history CASCADE~
DROP TRIGGER IF EXISTS observation_history_insert_trigger ON observation_history CASCADE~
DROP TRIGGER IF EXISTS observation_history_update_trigger ON observation_history CASCADE~
DROP TRIGGER IF EXISTS observation_history_delete_trigger ON observation_history CASCADE~
DROP TRIGGER IF EXISTS organization_update_trigger ON organization CASCADE~
DROP TRIGGER IF EXISTS sample_organization_insert_trigger ON sample_organization CASCADE~
DROP TRIGGER IF EXISTS sample_organization_update_trigger ON sample_organization CASCADE~
DROP TRIGGER IF EXISTS sample_organization_delete_trigger ON sample_organization CASCADE~
DROP TRIGGER IF EXISTS sample_requester_insert_trigger ON sample_requester CASCADE~
DROP TRIGGER IF EXISTS sample_requester_update_trigger ON sample_requester CASCADE~
DROP TRIGGER IF EXISTS sample_requester_delete_trigger ON sample_requester CASCADE~

DROP FUNCTION IF EXISTS insert_analysis_sync_on_analysis_insert() CASCADE~
DROP FUNCTION IF EXISTS update_analysis_sync_on_sample_update() CASCADE~
DROP FUNCTION IF EXISTS update_analysis_sync_on_sampleitem_update() CASCADE~
DROP FUNCTION IF EXISTS update_analysis_sync_on_analysis_update() CASCADE~
DROP FUNCTION IF EXISTS update_analysis_sync_on_result_update() CASCADE~
DROP FUNCTION IF EXISTS update_analysis_sync_on_result_insert() CASCADE~
DROP FUNCTION IF EXISTS update_analysis_sync_on_patient_update() CASCADE~
DROP FUNCTION IF EXISTS update_analysis_sync_on_patient_identity_change() CASCADE~
DROP FUNCTION IF EXISTS update_analysis_sync_on_observation_history_change() CASCADE~
DROP FUNCTION IF EXISTS update_analysis_sync_on_organization_update() CASCADE~
DROP FUNCTION IF EXISTS update_analysis_sync_on_sample_organization_change() CASCADE~
DROP FUNCTION IF EXISTS update_analysis_sync_on_sample_requester_change() CASCADE~

-- ============================================
-- 1. ANALYSIS INSERT — nouvelle analyse entre en file à flag=1 (TO_INSERT)
-- ============================================
CREATE OR REPLACE FUNCTION insert_analysis_sync_on_analysis_insert()
RETURNS TRIGGER
LANGUAGE PLPGSQL
AS $body$
DECLARE
    v_sample_id INTEGER;
BEGIN
    SELECT samp_id INTO v_sample_id
    FROM sample_item
    WHERE id = NEW.sampitem_id;

    IF v_sample_id IS NOT NULL THEN
        INSERT INTO analysis_sync_status(sample_id, analysis_id, upload_flag, last_updated)
        VALUES (v_sample_id, NEW.id, 1, now())
        ON CONFLICT (analysis_id) DO NOTHING;
    END IF;

    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'insert_analysis_sync failed for analysis %: %', NEW.id, SQLERRM;
    RETURN NEW;
END;
$body$~

-- ============================================
-- 2. SAMPLE UPDATE
-- ============================================
CREATE OR REPLACE FUNCTION update_analysis_sync_on_sample_update()
RETURNS TRIGGER
LANGUAGE PLPGSQL
AS $body$
BEGIN
    UPDATE analysis_sync_status
    SET upload_flag = 2, last_updated = now()
    WHERE sample_id = NEW.id
      AND upload_flag = 3;

    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'update_analysis_sync_on_sample_update failed: %', SQLERRM;
    RETURN NEW;
END;
$body$~

-- ============================================
-- 3. SAMPLE_ITEM UPDATE
-- ============================================
CREATE OR REPLACE FUNCTION update_analysis_sync_on_sampleitem_update()
RETURNS TRIGGER
LANGUAGE PLPGSQL
AS $body$
BEGIN
    UPDATE analysis_sync_status
    SET upload_flag = 2, last_updated = now()
    WHERE sample_id = NEW.samp_id
      AND upload_flag = 3;

    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'update_analysis_sync_on_sampleitem_update failed: %', SQLERRM;
    RETURN NEW;
END;
$body$~

-- ============================================
-- 4. ANALYSIS UPDATE
-- ============================================
CREATE OR REPLACE FUNCTION update_analysis_sync_on_analysis_update()
RETURNS TRIGGER
LANGUAGE PLPGSQL
AS $body$
BEGIN
    UPDATE analysis_sync_status
    SET upload_flag = 2, last_updated = now()
    WHERE analysis_id = NEW.id
      AND upload_flag = 3;

    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'update_analysis_sync_on_analysis_update failed: %', SQLERRM;
    RETURN NEW;
END;
$body$~

-- ============================================
-- 5. RESULT UPDATE
-- ============================================
CREATE OR REPLACE FUNCTION update_analysis_sync_on_result_update()
RETURNS TRIGGER
LANGUAGE PLPGSQL
AS $body$
BEGIN
    UPDATE analysis_sync_status
    SET upload_flag = 2, last_updated = now()
    WHERE analysis_id = NEW.analysis_id
      AND upload_flag = 3;

    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'update_analysis_sync_on_result_update failed: %', SQLERRM;
    RETURN NEW;
END;
$body$~

-- ============================================
-- 6. RESULT INSERT
-- ============================================
CREATE OR REPLACE FUNCTION update_analysis_sync_on_result_insert()
RETURNS TRIGGER
LANGUAGE PLPGSQL
AS $body$
BEGIN
    UPDATE analysis_sync_status
    SET upload_flag = 2, last_updated = now()
    WHERE analysis_id = NEW.analysis_id
      AND upload_flag = 3;

    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'update_analysis_sync_on_result_insert failed: %', SQLERRM;
    RETURN NEW;
END;
$body$~

-- ============================================
-- 7. PATIENT UPDATE
-- ============================================
CREATE OR REPLACE FUNCTION update_analysis_sync_on_patient_update()
RETURNS TRIGGER
LANGUAGE PLPGSQL
AS $body$
BEGIN
    UPDATE analysis_sync_status ass
    SET upload_flag = 2, last_updated = now()
    FROM sample_human sh
    WHERE sh.patient_id = NEW.id
      AND ass.sample_id = sh.samp_id
      AND ass.upload_flag = 3;

    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'update_analysis_sync_on_patient_update failed for patient %: %', NEW.id, SQLERRM;
    RETURN NEW;
END;
$body$~

-- ============================================
-- 8. PATIENT_IDENTITY (insert/update/delete)
-- ============================================
CREATE OR REPLACE FUNCTION update_analysis_sync_on_patient_identity_change()
RETURNS TRIGGER
LANGUAGE PLPGSQL
AS $body$
DECLARE
    v_patient_id INTEGER;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_patient_id := OLD.patient_id;
    ELSE
        v_patient_id := NEW.patient_id;
    END IF;

    UPDATE analysis_sync_status ass
    SET upload_flag = 2, last_updated = now()
    FROM sample_human sh
    WHERE sh.patient_id = v_patient_id
      AND ass.sample_id = sh.samp_id
      AND ass.upload_flag = 3;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'update_analysis_sync_on_patient_identity_change failed: %', SQLERRM;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$body$~

-- ============================================
-- 9. OBSERVATION_HISTORY (insert/update/delete)
-- ============================================
CREATE OR REPLACE FUNCTION update_analysis_sync_on_observation_history_change()
RETURNS TRIGGER
LANGUAGE PLPGSQL
AS $body$
DECLARE
    v_sample_id INTEGER;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_sample_id := OLD.sample_id;
    ELSE
        v_sample_id := NEW.sample_id;
    END IF;

    UPDATE analysis_sync_status
    SET upload_flag = 2, last_updated = now()
    WHERE sample_id = v_sample_id
      AND upload_flag = 3;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'update_analysis_sync_on_observation_history_change failed: %', SQLERRM;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$body$~

-- ============================================
-- 10. ORGANIZATION UPDATE (via sample_organization ET sample_requester type org)
-- ============================================
CREATE OR REPLACE FUNCTION update_analysis_sync_on_organization_update()
RETURNS TRIGGER
LANGUAGE PLPGSQL
AS $body$
BEGIN
    UPDATE analysis_sync_status ass
    SET upload_flag = 2, last_updated = now()
    FROM sample_organization so
    WHERE so.org_id = NEW.id
      AND ass.sample_id = so.samp_id
      AND ass.upload_flag = 3;

    UPDATE analysis_sync_status ass
    SET upload_flag = 2, last_updated = now()
    FROM sample_requester sr
    WHERE sr.requester_id = NEW.id
      AND sr.requester_type_id = 1
      AND ass.sample_id = sr.sample_id
      AND ass.upload_flag = 3;

    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'update_analysis_sync_on_organization_update failed for org %: %', NEW.id, SQLERRM;
    RETURN NEW;
END;
$body$~

-- ============================================
-- 11. SAMPLE_ORGANIZATION (insert/update/delete)
-- ============================================
CREATE OR REPLACE FUNCTION update_analysis_sync_on_sample_organization_change()
RETURNS TRIGGER
LANGUAGE PLPGSQL
AS $body$
DECLARE
    v_sample_id INTEGER;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_sample_id := OLD.samp_id;
    ELSE
        v_sample_id := NEW.samp_id;
    END IF;

    UPDATE analysis_sync_status
    SET upload_flag = 2, last_updated = now()
    WHERE sample_id = v_sample_id
      AND upload_flag = 3;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'update_analysis_sync_on_sample_organization_change failed: %', SQLERRM;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$body$~

-- ============================================
-- 12. SAMPLE_REQUESTER (insert/update/delete)
-- ============================================
CREATE OR REPLACE FUNCTION update_analysis_sync_on_sample_requester_change()
RETURNS TRIGGER
LANGUAGE PLPGSQL
AS $body$
DECLARE
    v_sample_id INTEGER;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_sample_id := OLD.sample_id;
    ELSE
        v_sample_id := NEW.sample_id;
    END IF;

    UPDATE analysis_sync_status
    SET upload_flag = 2, last_updated = now()
    WHERE sample_id = v_sample_id
      AND upload_flag = 3;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'update_analysis_sync_on_sample_requester_change failed: %', SQLERRM;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$body$~

-- ============================================
-- CREATE TRIGGERS (WHEN clauses limitent aux changements significatifs)
-- ============================================

CREATE TRIGGER analysis_insert_trigger
AFTER INSERT ON analysis
FOR EACH ROW
EXECUTE PROCEDURE insert_analysis_sync_on_analysis_insert()~

CREATE TRIGGER sample_update_trigger
AFTER UPDATE ON sample
FOR EACH ROW
WHEN (
    OLD.status_id IS DISTINCT FROM NEW.status_id OR
    OLD.accession_number IS DISTINCT FROM NEW.accession_number OR
    OLD.received_date IS DISTINCT FROM NEW.received_date OR
    OLD.collection_date IS DISTINCT FROM NEW.collection_date OR
    OLD.entered_date IS DISTINCT FROM NEW.entered_date OR
    OLD.referring_id IS DISTINCT FROM NEW.referring_id
)
EXECUTE PROCEDURE update_analysis_sync_on_sample_update()~

CREATE TRIGGER sampleitem_update_trigger
AFTER UPDATE ON sample_item
FOR EACH ROW
WHEN (
    OLD.typeosamp_id IS DISTINCT FROM NEW.typeosamp_id OR
    OLD.collection_date IS DISTINCT FROM NEW.collection_date
)
EXECUTE PROCEDURE update_analysis_sync_on_sampleitem_update()~

CREATE TRIGGER analysis_update_trigger
AFTER UPDATE ON analysis
FOR EACH ROW
WHEN (
    OLD.status_id IS DISTINCT FROM NEW.status_id OR
    OLD.started_date IS DISTINCT FROM NEW.started_date OR
    OLD.completed_date IS DISTINCT FROM NEW.completed_date OR
    OLD.released_date IS DISTINCT FROM NEW.released_date OR
    OLD.test_id IS DISTINCT FROM NEW.test_id
)
EXECUTE PROCEDURE update_analysis_sync_on_analysis_update()~

CREATE TRIGGER result_update_trigger
AFTER UPDATE ON result
FOR EACH ROW
WHEN (
    OLD.value IS DISTINCT FROM NEW.value OR
    OLD.result_type IS DISTINCT FROM NEW.result_type OR
    OLD.min_normal IS DISTINCT FROM NEW.min_normal OR
    OLD.max_normal IS DISTINCT FROM NEW.max_normal
)
EXECUTE PROCEDURE update_analysis_sync_on_result_update()~

CREATE TRIGGER result_insert_trigger
AFTER INSERT ON result
FOR EACH ROW
EXECUTE PROCEDURE update_analysis_sync_on_result_insert()~

CREATE TRIGGER patient_update_trigger
AFTER UPDATE ON patient
FOR EACH ROW
WHEN (
    OLD.national_id IS DISTINCT FROM NEW.national_id OR
    OLD.external_id IS DISTINCT FROM NEW.external_id OR
    OLD.gender IS DISTINCT FROM NEW.gender OR
    OLD.birth_date IS DISTINCT FROM NEW.birth_date OR
    OLD.upid_code IS DISTINCT FROM NEW.upid_code
)
EXECUTE PROCEDURE update_analysis_sync_on_patient_update()~

CREATE TRIGGER patient_identity_insert_trigger
AFTER INSERT ON patient_identity
FOR EACH ROW
EXECUTE PROCEDURE update_analysis_sync_on_patient_identity_change()~

CREATE TRIGGER patient_identity_update_trigger
AFTER UPDATE ON patient_identity
FOR EACH ROW
WHEN (OLD.identity_data IS DISTINCT FROM NEW.identity_data)
EXECUTE PROCEDURE update_analysis_sync_on_patient_identity_change()~

CREATE TRIGGER patient_identity_delete_trigger
AFTER DELETE ON patient_identity
FOR EACH ROW
EXECUTE PROCEDURE update_analysis_sync_on_patient_identity_change()~

CREATE TRIGGER observation_history_insert_trigger
AFTER INSERT ON observation_history
FOR EACH ROW
EXECUTE PROCEDURE update_analysis_sync_on_observation_history_change()~

CREATE TRIGGER observation_history_update_trigger
AFTER UPDATE ON observation_history
FOR EACH ROW
WHEN (OLD.value IS DISTINCT FROM NEW.value)
EXECUTE PROCEDURE update_analysis_sync_on_observation_history_change()~

CREATE TRIGGER observation_history_delete_trigger
AFTER DELETE ON observation_history
FOR EACH ROW
EXECUTE PROCEDURE update_analysis_sync_on_observation_history_change()~

CREATE TRIGGER organization_update_trigger
AFTER UPDATE ON organization
FOR EACH ROW
WHEN (
    OLD.name IS DISTINCT FROM NEW.name OR
    OLD.short_name IS DISTINCT FROM NEW.short_name OR
    OLD.datim_org_code IS DISTINCT FROM NEW.datim_org_code OR
    OLD.datim_org_name IS DISTINCT FROM NEW.datim_org_name
)
EXECUTE PROCEDURE update_analysis_sync_on_organization_update()~

CREATE TRIGGER sample_organization_insert_trigger
AFTER INSERT ON sample_organization
FOR EACH ROW
EXECUTE PROCEDURE update_analysis_sync_on_sample_organization_change()~

CREATE TRIGGER sample_organization_update_trigger
AFTER UPDATE ON sample_organization
FOR EACH ROW
WHEN (OLD.org_id IS DISTINCT FROM NEW.org_id)
EXECUTE PROCEDURE update_analysis_sync_on_sample_organization_change()~

CREATE TRIGGER sample_organization_delete_trigger
AFTER DELETE ON sample_organization
FOR EACH ROW
EXECUTE PROCEDURE update_analysis_sync_on_sample_organization_change()~

CREATE TRIGGER sample_requester_insert_trigger
AFTER INSERT ON sample_requester
FOR EACH ROW
EXECUTE PROCEDURE update_analysis_sync_on_sample_requester_change()~

CREATE TRIGGER sample_requester_update_trigger
AFTER UPDATE ON sample_requester
FOR EACH ROW
WHEN (OLD.requester_id IS DISTINCT FROM NEW.requester_id)
EXECUTE PROCEDURE update_analysis_sync_on_sample_requester_change()~

CREATE TRIGGER sample_requester_delete_trigger
AFTER DELETE ON sample_requester
FOR EACH ROW
EXECUTE PROCEDURE update_analysis_sync_on_sample_requester_change()~
