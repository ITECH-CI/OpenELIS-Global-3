package org.openelisglobal.common.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.SQLException;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseCleanServiceImpl implements DatabaseCleanService {

    private static final String CLEAN_SQL = "truncate sample_projects, " + "sample_human, " + "result_inventory, "
            + "result_signature, " + "result, " + "analysis, " + "analyzer_results, " + "sample_item, "
            + "observation_history, " + "sample, " + "provider, " + "patient_identity, " + "patient_contact, "
            + "patient_patient_type, " + "note, " + "sample_requester, " + "sample_qaevent, " + "referral, "
            + "patient, " + "person, " + "person_address, " + "report_external_export, " + "report_external_import, "
            + "document_track, " + "qa_observation," + "electronic_order," + "referral_result,non_conformity, "
            + "nce_specimen, nce_action_log, nc_event, "
            + "bacterio_sample_panel, bacteriology_antibiogram, bacteriology_conditional_test,bacteriology_flora, "
            + "bacteriology_flora_detail, bacteriology_organism, bacteriology_result_group,bacteriology_test_group_mapping, "
            + "hfj_resource," + "history CASCADE; " + "ALTER SEQUENCE IF EXISTS note_seq restart 1; "
            + "ALTER SEQUENCE IF EXISTS sample_human_seq restart 1; "
            + "ALTER SEQUENCE IF EXISTS result_inventory_seq restart 1; "
            + "ALTER SEQUENCE IF EXISTS result_signature_seq restart 1; "
            + "ALTER SEQUENCE IF EXISTS result_seq restart 1; " + "ALTER SEQUENCE IF EXISTS analysis_seq restart 1; "
            + "ALTER SEQUENCE IF EXISTS sample_item_seq restart 1; " + "ALTER SEQUENCE IF EXISTS sample_seq restart 1; "
            + "ALTER SEQUENCE IF EXISTS provider_seq restart 1; "
            + "ALTER SEQUENCE IF EXISTS patient_identity_seq restart 1; "
            + "ALTER SEQUENCE IF EXISTS patient_patient_type_seq restart 1; "
            + "ALTER SEQUENCE IF EXISTS patient_seq restart 1; " + "ALTER SEQUENCE IF EXISTS person_seq restart 1; "
            + "ALTER SEQUENCE IF EXISTS report_external_import_seq restart 1; "
            + "ALTER SEQUENCE IF EXISTS report_queue_seq restart 1; "
            + "ALTER SEQUENCE IF EXISTS sample_qaevent_seq restart 1; "
            + "ALTER SEQUENCE IF EXISTS bacterio_sample_panel_seq restart 1; ALTER SEQUENCE IF EXISTS bacteriology_antibiogram_seq restart 1;"
            + "ALTER SEQUENCE IF EXISTS bacteriology_conditional_test_seq restart 1; ALTER SEQUENCE IF EXISTS bacteriology_flora_detail_id_seq restart 1;"
            + "ALTER SEQUENCE IF EXISTS bacteriology_flora_id_seq restart 1; ALTER SEQUENCE IF EXISTS bacteriology_organism_seq restart 1;"
            + "ALTER SEQUENCE IF EXISTS bacteriology_result_group_seq restart 1; ALTER SEQUENCE IF EXISTS bacteriology_test_group_mapping_seq restart 1;"
            + "ALTER SEQUENCE IF EXISTS history_seq restart 1;"
            + " UPDATE accession_number_info SET cur_val=0 WHERE 1=1;";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void cleanDatabase() {
        entityManager.unwrap(Session.class).doWork(new Work() {

            @Override
            public void execute(Connection connection) throws SQLException {
                connection.prepareStatement(CLEAN_SQL).execute();
            }
        });
    }
}
