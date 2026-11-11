package org.openelisglobal.patient.service;

import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;
import org.apache.commons.validator.GenericValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC direct, dans la transaction Spring en cours. La comparaison se fait sur
 * la valeur normalisée (espaces, tirets et points retirés), pour retrouver
 * aussi les numéros saisis avant la normalisation.
 */
@Service
public class PatientCmuLookupServiceImpl implements PatientCmuLookupService {

    private static final String SQL = "SELECT DISTINCT CAST(id AS varchar) AS id, id AS ordre FROM ("
            + " SELECT p.id FROM clinlims.patient p"
            + "  WHERE regexp_replace(p.national_id, '[[:space:].-]', '', 'g') = :cmu" + " UNION"
            + " SELECT pi.patient_id FROM clinlims.patient_identity pi"
            + "  JOIN clinlims.patient_identity_type t ON t.id = pi.identity_type_id"
            + "  WHERE t.identity_type = 'CMU'"
            + "  AND regexp_replace(pi.identity_data, '[[:space:].-]', '', 'g') = :cmu" + ") ids ORDER BY ordre";

    private final NamedParameterJdbcTemplate jdbc;

    @Autowired
    public PatientCmuLookupServiceImpl(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findPatientIdsByCmu(String normalizedCmu) {
        if (GenericValidator.isBlankOrNull(normalizedCmu)) {
            return Collections.emptyList();
        }
        return jdbc.query(SQL, new MapSqlParameterSource("cmu", normalizedCmu), (rs, i) -> rs.getString("id"));
    }
}
