/*
* The contents of this file are subject to the Mozilla Public License
* Version 1.1 (the "License"); you may not use this file except in
* compliance with the License. You may obtain a copy of the License at
* http://www.mozilla.org/MPL/
*
* Software distributed under the License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific language governing rights and limitations under
* the License.
*
* The Original Code is OpenELIS code.
*
* Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
*/
package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.AGE_MONTHS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.AGE_WEEKS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.AGE_YEARS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.DATE;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.DATE_TIME;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.NONE;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.SAMPLE_STATUS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.TEST_RESULT;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.SQLConstant;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.valueholder.TestResult;

public class BacteriologyColumnBuilder extends RoutineColumnBuilder {

    protected static final String FROM_SAMPLE_PATIENT_ORGANIZATION = " FROM sample as s "
            + "\n JOIN sample_human as sh ON sh.samp_id = s.id " + "\n JOIN patient as pat ON pat.id = sh.patient_id "
            + "\n JOIN person as per ON pat.person_id = per.id "
            + "\n LEFT JOIN sample_requester sr ON sr.sample_id = s.id AND sr.requester_type_id = 2 "
            + "\n LEFT JOIN organization AS o ON o.id = sr.requester_id \n ";

    protected static final String SELECT_SAMPLE_PATIENT_ORGANIZATION = "SELECT DISTINCT s.id as sample_id, s.accession_number, s.entered_date, s.received_date,"
            + " s.collection_date, s.status_id,demo.type_of_sample_name,demo.released_date "
            + "\n, COALESCE(pat.national_id, pat.external_id) national_id, pat.birth_date, per.first_name, per.last_name, pat.gender "
            + "\n, o.short_name as organization_code, o.name AS organization_name "
            + "\n, organism_data.organism_1_name, organism_data.organism_1_type, organism_data.organism_1_gram, organism_data.organism_1_grouping, organism_data.organism_1_capsule, organism_data.organism_1_antibiogram "
            + "\n, organism_data.organism_2_name, organism_data.organism_2_type, organism_data.organism_2_gram, organism_data.organism_2_grouping, organism_data.organism_2_capsule, organism_data.organism_2_antibiogram "
            + "\n, organism_data.organism_3_name, organism_data.organism_3_type, organism_data.organism_3_gram, organism_data.organism_3_grouping, organism_data.organism_3_capsule, organism_data.organism_3_antibiogram "
            + "\n, flora_data.flora_count, flora_data.flora_details "
            + "\n, demo.\"Bacterial Count\", demo.\"Bacteria\", demo.\"BacterioTypeExamens\", demo.\"Culture\", demo.\"Germ Identification\" "
            + "\n ";

    /**
     * @param dateRange
     */
    public BacteriologyColumnBuilder(DateRange dateRange) {
        super(dateRange);
    }

    /**
     * Define all report columns for bacteriology export
     */
    @Override
    protected void defineAllReportColumns() {
        add("accession_number", "LABNO", NONE);
        add("national_id", "IDENTIFIER", NONE);
        add("gender", "SEX", NONE);
        add("birth_date", "BIRTHDATE", DATE);
        add("collection_date", "AGEYEARS", AGE_YEARS);
        add("collection_date", "AGEMONTHS", AGE_MONTHS);
        add("collection_date", "AGEWEEKS", AGE_WEEKS);
        add("received_date", "DATERECPT", DATE_TIME); // reception date
        add("entered_date", "DATEENTERED", DATE_TIME); // interview date
        add("collection_date", "DATECOLLECT", DATE_TIME); // collection date
        add("released_date", "DATEVALIDATION", DATE_TIME); // validation date
        add("organization_code", "CODEREFERER", NONE);
        add("organization_name", "REFERER", NONE);
        add("status_id", "STATUS", SAMPLE_STATUS);
        add("type_of_sample_name", "TYPE_OF_SAMPLE", Strategy.NONE);

        // Add organism data columns (up to 3 organisms per sample)
        for (int i = 1; i <= 3; i++) {
            add("organism_" + i + "_name", "ORGANISM_" + i + "_NAME", NONE);
            add("organism_" + i + "_type", "ORGANISM_" + i + "_TYPE", NONE);
            add("organism_" + i + "_gram", "ORGANISM_" + i + "_GRAM", NONE);
            add("organism_" + i + "_grouping", "ORGANISM_" + i + "_GROUPING", NONE);
            add("organism_" + i + "_capsule", "ORGANISM_" + i + "_CAPSULE", NONE);
            add("organism_" + i + "_antibiogram", "ORGANISM_" + i + "_ANTIBIOGRAM", NONE);
        }

        // Add flora data columns
        add("flora_count", "FLORA_COUNT", NONE);
        add("flora_details", "FLORA_DETAILS", NONE);

        // Add bacteriology observation history types (alphabetical order to match
        // crosstab)
        // Column names must match exactly the SQL crosstab column definitions (with
        // spaces and capitals)
        add("Bacterial Count", "BACTERIAL_COUNT", NONE);
        add("Bacteria", "BACTERIA", NONE);
        add("BacterioTypeExamens", "BACTERIO_TYPE_EXAMENS", NONE);
        add("Culture", "CULTURE", NONE);
        add("Germ Identification", "GERM_IDENTIFICATION", NONE);

        addAllResultsColumns();
    }

    @Override
    protected void addAllResultsColumns() {
        // Map SQL column names (test_XXX) to full test names for CSV export
        for (Test test : allTests) {
            String testId = test.getId();
            String sqlColumnName = "test_" + testId; // Matches the column name in SQL crosstab
            String fullTestName = TestServiceImpl.getLocalizedTestNameWithType(test); // Full name for CSV header
            add(sqlColumnName, fullTestName, TEST_RESULT);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void defineAllTestsAndResults() {
        if (allTests == null) {
            // Get all bacteriology tests using stream filter similar to TB
            // Filter all active tests by test section name
            allTests = getBacteriologyTests();
        }
        if (testResultsByTestName == null) {
            testResultsByTestName = new HashMap<>();
            List<TestResult> allTestResults = testResultService.getAllTestResults();
            for (TestResult testResult : allTestResults) {
                String key = TestServiceImpl.getLocalizedTestNameWithType(testResult.getTest());
                testResultsByTestName.put(key, testResult);
            }
        }
    }

    /**
     * Get all bacteriology tests from Routine Bacteriology test section Similar to
     * getTbTest() but filters by Routine Bacteriology section
     */
    private List<Test> getBacteriologyTests() {
        // First, get the test section ID for "Routine Bacteriology"
        org.openelisglobal.test.service.TestSectionService testSectionService = org.openelisglobal.spring.util.SpringContext
                .getBean(org.openelisglobal.test.service.TestSectionService.class);

        org.openelisglobal.test.valueholder.TestSection bacterioSection = testSectionService
                .getTestSectionByName("Routine Bacteriology");

        if (bacterioSection == null) {
            org.openelisglobal.common.log.LogEvent.logWarn(this.getClass().getName(), "getBacteriologyTests",
                    "Test section 'Routine Bacteriology' not found");
            return new java.util.ArrayList<>();
        }

        String sectionId = bacterioSection.getId();
        org.openelisglobal.common.log.LogEvent.logInfo(this.getClass().getName(), "getBacteriologyTests",
                "Found test section 'Routine Bacteriology' with ID: " + sectionId);

        // Get all active tests and filter by test section ID
        List<Test> bacterioTests = testService.getAllActiveTests(false).stream().filter(test -> {
            String testSectionId = test.getTestSection() != null ? test.getTestSection().getId() : null;
            return sectionId.equals(testSectionId);
        }).sorted((t1, t2) -> {
            // Sort by test ID (numeric) to match SQL crosstab ORDER BY 1::integer
            Integer id1 = Integer.parseInt(t1.getId());
            Integer id2 = Integer.parseInt(t2.getId());
            return id1.compareTo(id2);
        }).collect(java.util.stream.Collectors.toList());

        org.openelisglobal.common.log.LogEvent.logInfo(this.getClass().getName(), "getBacteriologyTests",
                "Found " + bacterioTests.size() + " bacteriology tests");

        return bacterioTests;
    }

    @Override
    protected void appendResultCrosstab(java.sql.Date lowDate, java.sql.Date highDate) {
        SQLConstant listName = SQLConstant.RESULT;
        query.append(", \n\n ( SELECT si.samp_id, si.id AS sampleItem_id, si.sort_order AS sampleItemNo, " + listName
                + ".* " + " FROM sample_item AS si JOIN \n ");

        // Begin cross tab / pivot table - use test ID to avoid PostgreSQL 63-char limit
        // on column names
        query.append(" crosstab( "
                + "\n 'SELECT si.id, t.id::text, replace(replace(replace(replace(r.value ,E''\\n'', '' ''), E''\\t'', '' ''), E''\\r'', '' ''),'','',''.'') "
                + "\n FROM clinlims.analysis AS a join clinlims.test AS t on a.test_id = t.id  \n "
                + " JOIN test_section ts ON t.test_section_id = ts.id \n "
                + " join clinlims.test_result AS tr on t.id = tr.test_id  \n"
                + " join clinlims.sample_item AS si on si.id = a.sampitem_id \n"
                + " join clinlims.sample AS s on s.id = si.samp_id  \n"
                + " left join clinlims.result AS r on a.id = r.analysis_id  \n"
                + "\n WHERE ts.name = ''Routine Bacteriology'' AND s.entered_date >= date(''"
                + formatDateForDatabaseSql(lowDate) + "'') AND s.entered_date <= date(''"
                + formatDateForDatabaseSql(highDate) + "'')" + "\n " + "\n ORDER BY 1, 2 "
                + "\n ', 'SELECT t.id::text FROM test t JOIN test_section ts ON t.test_section_id = ts.id where t.is_active = ''Y'' AND ts.name = ''Routine Bacteriology'' ORDER BY 1::integer' ) ");
        query.append("\n as " + listName + " ( " // inner use of the list name
                + "\"si_id\" numeric(10) ");
        for (Test col : allTests) {
            // Use test ID as column alias in SQL to avoid PostgreSQL 63-character limit
            // Column will be renamed to full test name in CSV export headers via
            // defineAllReportColumns()
            String testId = col.getId();
            query.append("\n, \"test_" + testId + "\" varchar(200) ");
        }
        query.append(" ) \n");
        // left join all sample Items from the right sample range to the results table.
        query.append("\n ON si.id = " + listName + ".si_id " // the inner use a few lines above
                + "\n ORDER BY si.samp_id, si.id " + "\n) AS " + listName + "\n "); // outer re-use the list name to
                                                                                    // name this sparse matrix of
                                                                                    // results.
    }

    @Override
    protected String buildWhereSamplePatienOrgSQL(Date lowDate, Date highDate) {
        String WHERE_SAMPLE_PATIENT_ORG = " WHERE " + "\n s.entered_date >= '" + formatDateForDatabaseSql(lowDate) + "'"
                + "\n AND s.entered_date <= '" + formatDateForDatabaseSql(highDate) + "'";
        return WHERE_SAMPLE_PATIENT_ORG;
    }

    @Override
    protected void appendObservationHistoryCrosstab(java.sql.Date lowDate, java.sql.Date highDate) {
        appendCrosstabPreamble(SQLConstant.DEMO);

        // Add bacteriology observation history types
        query.append("\n crosstab( " + "\n 'SELECT s.id::numeric as samp_id, oht.type_name, oh.value "
                + "\n FROM clinlims.sample AS s "
                + "\n LEFT JOIN clinlims.observation_history oh ON oh.sample_id = s.id "
                + "\n LEFT JOIN clinlims.observation_history_type oht ON oht.id = oh.observation_history_type_id "
                + "\n WHERE s.entered_date >= date(''" + formatDateForDatabaseSql(lowDate) + "'') "
                + "\n AND s.entered_date <= date(''" + formatDateForDatabaseSql(highDate) + "'') "
                + "\n AND oht.type_name IN (''BacterioTypeExamens'', ''Germ Identification'', ''Bacteria'', ''Culture'', ''Bacterial Count'') "
                + "\n ORDER BY 1, 2', " + "\n 'SELECT type_name FROM clinlims.observation_history_type "
                + "\n WHERE type_name IN (''BacterioTypeExamens'', ''Germ Identification'', ''Bacteria'', ''Culture'', ''Bacterial Count'') "
                + "\n ORDER BY 1' " + "\n ) \n ");

        // Define structure with bacteriology observation history types
        query.append(" as demo ( " + " \"s_id\" numeric(10) " + "\n, \"Bacterial Count\" varchar(100) "
                + "\n, \"Bacteria\" varchar(100) " + "\n, \"BacterioTypeExamens\" varchar(100) "
                + "\n, \"Culture\" varchar(100) " + "\n, \"Germ Identification\" varchar(100) " + " ) \n");
        appendCrosstabPostfix(lowDate, highDate, SQLConstant.DEMO);
    }

    @Override
    protected void appendCrosstabPreamble(SQLConstant listName) {
        query.append(", \n\n ( SELECT s.id AS samp_id, " + listName + ".*, a.released_date,a.type_of_sample_name "
                + " FROM sample AS s  LEFT JOIN sample_item si on si.samp_id = s.id \n"
                + " LEFT JOIN analysis a on a.sampitem_id = si.id LEFT JOIN \n ");
    }

    /**
     * Append organism data with up to 3 organisms per sample, including antibiogram
     * results
     */
    protected void appendOrganismDataQuery(java.sql.Date lowDate, java.sql.Date highDate) {
        query.append(", \n\n ( SELECT samp_id, ");
        query.append("\n MAX(CASE WHEN org_rank = 1 THEN organism_name END) as organism_1_name,");
        query.append("\n MAX(CASE WHEN org_rank = 1 THEN organism_type END) as organism_1_type,");
        query.append("\n MAX(CASE WHEN org_rank = 1 THEN gram_type END) as organism_1_gram,");
        query.append("\n MAX(CASE WHEN org_rank = 1 THEN grouping_mode END) as organism_1_grouping,");
        query.append("\n MAX(CASE WHEN org_rank = 1 THEN capsule END) as organism_1_capsule,");
        query.append("\n MAX(CASE WHEN org_rank = 1 THEN antibiogram END) as organism_1_antibiogram,");
        query.append("\n MAX(CASE WHEN org_rank = 2 THEN organism_name END) as organism_2_name,");
        query.append("\n MAX(CASE WHEN org_rank = 2 THEN organism_type END) as organism_2_type,");
        query.append("\n MAX(CASE WHEN org_rank = 2 THEN gram_type END) as organism_2_gram,");
        query.append("\n MAX(CASE WHEN org_rank = 2 THEN grouping_mode END) as organism_2_grouping,");
        query.append("\n MAX(CASE WHEN org_rank = 2 THEN capsule END) as organism_2_capsule,");
        query.append("\n MAX(CASE WHEN org_rank = 2 THEN antibiogram END) as organism_2_antibiogram,");
        query.append("\n MAX(CASE WHEN org_rank = 3 THEN organism_name END) as organism_3_name,");
        query.append("\n MAX(CASE WHEN org_rank = 3 THEN organism_type END) as organism_3_type,");
        query.append("\n MAX(CASE WHEN org_rank = 3 THEN gram_type END) as organism_3_gram,");
        query.append("\n MAX(CASE WHEN org_rank = 3 THEN grouping_mode END) as organism_3_grouping,");
        query.append("\n MAX(CASE WHEN org_rank = 3 THEN capsule END) as organism_3_capsule,");
        query.append("\n MAX(CASE WHEN org_rank = 3 THEN antibiogram END) as organism_3_antibiogram");
        query.append("\n FROM ( ");
        query.append("\n   SELECT s.id as samp_id, ");
        query.append("\n     ROW_NUMBER() OVER (PARTITION BY s.id ORDER BY bo.id) as org_rank,");
        query.append("\n     COALESCE(d_org.dict_entry, bo.organism_name_text) as organism_name,");
        query.append("\n     bo.organism_type as organism_type,");
        query.append("\n     bo.gram_type as gram_type,");
        query.append("\n     bo.grouping_mode as grouping_mode,");
        query.append(
                "\n     CASE WHEN bo.capsule_presence = true THEN 'Yes' WHEN bo.capsule_presence = false THEN 'No' ELSE '' END as capsule,");
        query.append("\n     ( SELECT string_agg(d_antibiotic.dict_entry || ':' || ");
        query.append("\n       COALESCE(CASE WHEN ba.result = 'S' THEN 'Sensitive' ");
        query.append("\n                     WHEN ba.result = 'I' THEN 'Intermediate' ");
        query.append("\n                     WHEN ba.result = 'R' THEN 'Resistant' ");
        query.append("\n                     ELSE '' END, ''), '; ' ORDER BY d_antibiotic.dict_entry)");
        query.append("\n       FROM clinlims.bacteriology_antibiogram ba");
        query.append("\n       LEFT JOIN clinlims.dictionary d_antibiotic ON d_antibiotic.id = ba.antibiotic_dict_id");
        query.append("\n       WHERE ba.organism_id = bo.id");
        query.append("\n     ) as antibiogram");
        query.append("\n   FROM clinlims.sample s");
        query.append("\n   LEFT JOIN clinlims.sample_item si ON si.samp_id = s.id");
        query.append("\n   LEFT JOIN clinlims.analysis a ON a.sampitem_id = si.id");
        query.append(
                "\n   LEFT JOIN clinlims.bacteriology_result_group brg ON brg.analysis_id = a.id AND brg.group_type = 'ORGANISM'");
        query.append("\n   LEFT JOIN clinlims.bacteriology_organism bo ON bo.result_group_id = brg.id");
        query.append("\n   LEFT JOIN clinlims.dictionary d_org ON d_org.id = bo.organism_name_dict_id");
        query.append("\n   WHERE s.entered_date >= '" + formatDateForDatabaseSql(lowDate) + "'");
        query.append("\n   AND s.entered_date <= '" + formatDateForDatabaseSql(highDate) + "'");
        query.append("\n ) ranked_organisms");
        query.append("\n GROUP BY samp_id");
        query.append("\n ) AS organism_data \n");
    }

    /**
     * Append flora data for the sample
     */
    protected void appendFloraDataQuery(java.sql.Date lowDate, java.sql.Date highDate) {
        query.append(", \n\n ( SELECT inner_s.id AS samp_id, ");
        query.append("\n MAX(inner_bf.flora_count) as flora_count,");
        query.append("\n MAX(( SELECT string_agg(");
        query.append("\n     d_gram.dict_entry || ':' || ");
        query.append("\n     COALESCE(d_grouping.dict_entry, '') || ");
        query.append(
                "\n     CASE WHEN d_other.dict_entry IS NOT NULL THEN ' (' || d_other.dict_entry || ')' ELSE '' END,");
        query.append("\n     '; ' ORDER BY bfd.id)");
        query.append("\n   FROM clinlims.bacteriology_flora_detail bfd");
        query.append("\n   LEFT JOIN clinlims.dictionary d_gram ON d_gram.id = bfd.gram_type_dict_id");
        query.append("\n   LEFT JOIN clinlims.dictionary d_grouping ON d_grouping.id = bfd.grouping_mode_dict_id");
        query.append("\n   LEFT JOIN clinlims.dictionary d_other ON d_other.id = bfd.other_characteristic_dict_id");
        query.append("\n   WHERE bfd.flora_id = inner_bf.id");
        query.append("\n )) as flora_details");
        query.append("\n FROM clinlims.sample inner_s");
        query.append("\n LEFT JOIN clinlims.sample_item inner_si ON inner_si.samp_id = inner_s.id");
        query.append("\n LEFT JOIN clinlims.analysis inner_a ON inner_a.sampitem_id = inner_si.id");
        query.append("\n LEFT JOIN clinlims.bacteriology_flora inner_bf ON inner_bf.analysis_id = inner_a.id");
        query.append("\n WHERE inner_s.entered_date >= '" + formatDateForDatabaseSql(lowDate) + "'");
        query.append("\n AND inner_s.entered_date <= '" + formatDateForDatabaseSql(highDate) + "'");
        query.append("\n GROUP BY inner_s.id");
        query.append("\n ) AS flora_data \n");
    }

    /**
     * Build the SQL query for bacteriology export
     */
    @Override
    public void makeSQL() {
        // Initialize tests and results before building SQL
        defineAllTestsAndResults();

        query = new StringBuilder();
        Date lowDate = dateRange.getLowDate();
        Date highDate = dateRange.getHighDate();
        query.append(SELECT_SAMPLE_PATIENT_ORGANIZATION);
        // more cross tabulation of other columns goes where
        query.append(SELECT_ALL_DEMOGRAPHIC_AND_RESULTS);

        // FROM clause for ordinary lab (sample and patient) tables
        query.append(FROM_SAMPLE_PATIENT_ORGANIZATION);

        // all observation history from expressions
        appendObservationHistoryCrosstab(lowDate, highDate);

        appendResultCrosstab(lowDate, highDate);

        // append organism data
        appendOrganismDataQuery(lowDate, highDate);

        // append flora data
        appendFloraDataQuery(lowDate, highDate);

        // and finally the join that puts these all together. Each cross table should be
        // listed here otherwise it's not in the result and you'll get a full join
        query.append(buildWhereSamplePatienOrgSQL(lowDate, highDate)
                // insert joining of any other crosstab here.
                + "\n AND s.id = demo.samp_id " + "\n AND s.id = result.samp_id "
                + "\n AND s.id = organism_data.samp_id " + "\n AND s.id = flora_data.samp_id "
                + "\n ORDER BY s.accession_number ");
        // no don't insert another crosstab or table here, go up before the main WHERE
        // clause
        return;
    }

}
