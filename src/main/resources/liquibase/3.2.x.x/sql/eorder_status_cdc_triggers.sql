-- ============================================================================
-- CDC TRIGGERS — push des statuts/résultats e-order (module d'échange unifié,
-- incrément 4d). Quand une analyse LIÉE à un e-order (via
-- sample.clinical_order_id) change de statut ou reçoit un résultat, alimente
-- `eorder_sync_status` (sync_flag = 2 = TO_UPDATE) pour que DataPushTask remonte
-- l'événement au serveur consolidé.
--
-- AMÉLIORATION vs oedatauploader : l'uploader devait réconcilier a posteriori
-- (jointure patient + test VL) car il n'avait pas le lien direct e-order↔sample.
-- OE POSE ce lien nativement (Accessioner : sample.clinical_order_id =
-- electronic_order.id). Le trigger l'exploite pour UPSERT directement la ligne de
-- suivi (request_uuid = electronic_order.external_id) — plus de réconciliation.
--
-- Idempotent : DROP TRIGGER/FUNCTION IF EXISTS + CREATE OR REPLACE. Anti-
-- régression de flag : ne repasse à 2 que depuis 1 (IMPORTED) ou 3 (SYNCED),
-- jamais depuis 4 (IN_PROGRESS) ni 6 (SEND_FAILED en cours). EXCEPTION WHEN OTHERS
-- non bloquant. Délimiteur : ~ (endDelimiter du changeSet).
-- ============================================================================

DROP TRIGGER IF EXISTS trg_eorder_analysis_update ON clinlims.analysis CASCADE~
DROP TRIGGER IF EXISTS trg_eorder_result_insert ON clinlims.result CASCADE~
DROP TRIGGER IF EXISTS trg_eorder_result_update ON clinlims.result CASCADE~
DROP FUNCTION IF EXISTS eorder_status_sync_on_analysis() CASCADE~
DROP FUNCTION IF EXISTS eorder_status_sync_on_result() CASCADE~
DROP FUNCTION IF EXISTS eorder_status_upsert(INTEGER) CASCADE~

-- Fonction partagée : pour une analyse donnée, si son sample est lié à un e-order,
-- upsert la ligne eorder_sync_status (crée avec analysis_id/electronic_order_id/
-- request_uuid si absente ; sinon repasse à sync_flag=2 si en 1 ou 3).
CREATE OR REPLACE FUNCTION eorder_status_upsert(p_analysis_id INTEGER)
RETURNS VOID
LANGUAGE PLPGSQL
AS $body$
DECLARE
    v_sample_id      INTEGER;
    v_eo_id          INTEGER;
    v_request_uuid   VARCHAR(200);
BEGIN
    -- Résout le sample de l'analyse et l'e-order lié (clinical_order_id).
    SELECT s.id, s.clinical_order_id
    INTO v_sample_id, v_eo_id
    FROM clinlims.analysis a
    JOIN clinlims.sample_item si ON si.id = a.sampitem_id
    JOIN clinlims.sample s ON s.id = si.samp_id
    WHERE a.id = p_analysis_id;

    -- Pas de sample lié à un e-order : rien à suivre (analyse non issue d'un e-order).
    IF v_eo_id IS NULL THEN
        RETURN;
    END IF;

    SELECT external_id INTO v_request_uuid
    FROM clinlims.electronic_order WHERE id = v_eo_id;

    IF v_request_uuid IS NULL THEN
        RETURN;
    END IF;

    -- Réarme aussi depuis 4 (IN_PROGRESS) : un changement survenant PENDANT un push
    -- ne doit pas être perdu. Le service (markSynced) est optimiste : il ne passe à 3
    -- QUE si la ligne est restée en 4 — sinon un trigger l'a repassée à 2 ici, on la
    -- laisse pour un nouveau push. Ne réarme JAMAIS depuis 6 (SEND_FAILED, déjà en file).
    INSERT INTO clinlims.eorder_sync_status
        (request_uuid, sample_id, analysis_id, electronic_order_id, sync_flag, created_at, updated_at)
    VALUES (v_request_uuid, v_sample_id, p_analysis_id, v_eo_id, 2, now(), now())
    ON CONFLICT (request_uuid) DO UPDATE
        SET sync_flag = 2,
            sample_id = EXCLUDED.sample_id,
            analysis_id = EXCLUDED.analysis_id,
            updated_at = now()
        WHERE clinlims.eorder_sync_status.sync_flag IN (1, 3, 4);
END;
$body$~

CREATE OR REPLACE FUNCTION eorder_status_sync_on_analysis()
RETURNS TRIGGER
LANGUAGE PLPGSQL
AS $body$
BEGIN
    PERFORM eorder_status_upsert(NEW.id);
    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'eorder_status_sync_on_analysis failed for analysis %: %', NEW.id, SQLERRM;
    RETURN NEW;
END;
$body$~

CREATE OR REPLACE FUNCTION eorder_status_sync_on_result()
RETURNS TRIGGER
LANGUAGE PLPGSQL
AS $body$
BEGIN
    PERFORM eorder_status_upsert(NEW.analysis_id);
    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'eorder_status_sync_on_result failed for result on analysis %: %', NEW.analysis_id, SQLERRM;
    RETURN NEW;
END;
$body$~

CREATE TRIGGER trg_eorder_analysis_update
AFTER UPDATE ON clinlims.analysis
FOR EACH ROW
WHEN (OLD.status_id IS DISTINCT FROM NEW.status_id
      OR OLD.completed_date IS DISTINCT FROM NEW.completed_date
      OR OLD.released_date IS DISTINCT FROM NEW.released_date)
EXECUTE PROCEDURE eorder_status_sync_on_analysis()~

CREATE TRIGGER trg_eorder_result_insert
AFTER INSERT ON clinlims.result
FOR EACH ROW
EXECUTE PROCEDURE eorder_status_sync_on_result()~

CREATE TRIGGER trg_eorder_result_update
AFTER UPDATE ON clinlims.result
FOR EACH ROW
WHEN (OLD.value IS DISTINCT FROM NEW.value
      OR OLD.result_type IS DISTINCT FROM NEW.result_type)
EXECUTE PROCEDURE eorder_status_sync_on_result()~
