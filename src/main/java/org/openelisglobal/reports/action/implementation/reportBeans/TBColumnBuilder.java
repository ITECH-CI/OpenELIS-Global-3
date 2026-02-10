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
* Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
*
* Contributor(s): CIRG, University of Washington, Seattle WA.
*/
package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.AGE_MONTHS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.AGE_WEEKS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.AGE_YEARS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.DATE;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.DATE_TIME;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.NONE;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.SAMPLE_STATUS;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.SQLConstant;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.valueholder.TestResult;

public class TBColumnBuilder extends RoutineColumnBuilder {

    protected static final String FROM_SAMPLE_PATIENT_ORGANIZATION = " FROM sample as s "
            + "\n JOIN sample_human as sh ON sh.samp_id = s.id " + "\n JOIN patient as pat ON pat.id = sh.patient_id "
            + "\n JOIN person as per ON pat.person_id = per.id "
            + "\n LEFT JOIN sample_requester sr ON sr.sample_id = s.id AND sr.requester_type_id = 2 "
            + "\n LEFT JOIN organization AS o ON o.id = sr.requester_id \n ";

    protected static final String SELECT_SAMPLE_PATIENT_ORGANIZATION = "SELECT DISTINCT s.id as sample_id, s.accession_number, s.entered_date, s.received_date,"
            + " s.collection_date, s.status_id,demo.type_of_sample_name,demo.released_date "
            + "\n, COALESCE(pat.national_id, pat.external_id) national_id, pat.birth_date, per.first_name, per.last_name, pat.gender "
            + "\n, o.short_name as organization_code, o.name AS organization_name " + "\n ";

    /**
     * @param dateRange
     * @param projectStr
     */
    public TBColumnBuilder(DateRange dateRange) {
        super(dateRange);
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.reportBeans.CIRoutineColumnBuilder#defineAllReportColumns()
     */
    @Override
    protected void defineAllReportColumns() {
        // defineBasicColumns();
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
        // add("program", "PROGRAM", PROGRAM);
        add("status_id", "STATUS", SAMPLE_STATUS);
        add("type_of_sample_name", "TYPE_OF_SAMPLE", Strategy.NONE);
        add("tborderreason", "ORDER_REASON", Strategy.DICT);
        add("tbanalysismethod", "ANALYSIS_METHOD", Strategy.DICT);
        add("tbdiagnosticreason", "DIAGNOSTIC_REASON", Strategy.DICT);
        add("tbfollowupreason", "FOLLOW_UP_REASON", Strategy.DICT);
        add("tbfollowupreasonperiodline1", "FOLLOW_UP_REASON_LINE1", Strategy.NONE);
        add("tbfollowupreasonperiodline2", "FOLLOW_UP_REASON_LINE2", Strategy.NONE);
        add("tbsampleaspects", "SAMPLE_ASPECT", Strategy.DICT);
        addAllResultsColumns();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void defineAllTestsAndResults() {
        if (allTests == null) {
            allTests = testService.getTbTest();
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

    @Override
    protected void appendResultCrosstab(java.sql.Date lowDate, java.sql.Date highDate) {
        // A list of analytes which should not show up in the regular results,
        // String excludeAnalytes = getExcludedAnalytesSet();
        SQLConstant listName = SQLConstant.RESULT;
        query.append(", \n\n ( SELECT si.samp_id, si.id AS sampleItem_id, si.sort_order AS sampleItemNo, " + listName
                + ".* " + " FROM sample_item AS si JOIN \n ");

        // Begin cross tab / pivot table
        query.append(" crosstab( "
                + "\n 'SELECT si.id, t.description, replace(replace(replace(replace(r.value ,E''\\n'', '' ''), E''\\t'', '' ''), E''\\r'', '' ''),'','',''.'') "
                + "\n FROM clinlims.analysis AS a join clinlims.test AS t on a.test_id = t.id  \n "
                + " JOIN test_section ts ON t.test_section_id = ts.id \n "
                + " join clinlims.test_result AS tr on t.id = tr.test_id  \n"
                + " join clinlims.sample_item AS si on si.id = a.sampitem_id \n"
                + " join clinlims.sample AS s on s.id = si.samp_id  \n"
                + " left join clinlims.result AS r on a.id = r.analysis_id  \n"
                // + " left join sample_projects sp on si.samp_id = sp.samp_id \n"
                // + "\n WHERE sp.id IS NULL AND ts.name = ''TB'' AND s.entered_date >= date(''"
                + "\n WHERE ts.name = ''TB'' AND s.entered_date >= date(''" + formatDateForDatabaseSql(lowDate)
                + "'') AND s.entered_date <= date(''" + formatDateForDatabaseSql(highDate) + "'')" + "\n "
                // sql injection safe as user cannot overwrite validStatusId in database
                // + ((validStatusId == null) ? "" : " AND a.status_id = " + validStatusId)
                // + (( excludeAnalytes == null)?"":
                // " AND r.analyte_id NOT IN ( " + excludeAnalytes) + ")"
                // + " AND a.test_id = t.id "
                + "\n ORDER BY 1, 2 "
                + "\n ', 'SELECT t.description FROM test t JOIN test_section ts ON t.test_section_id = ts.id where t.is_active = ''Y'' AND ts.name = ''TB'' ORDER BY 1' ) ");
        query.append("\n as " + listName + " ( " // inner use of the list name
                + "\"si_id\" numeric(10) ");
        for (Test col : allTests) {
            // Use TestServiceImpl.getLocalizedTestNameWithType as in parent class
            // CSVRoutineColumnBuilder
            String testName = TestServiceImpl.getLocalizedTestNameWithType(col);
            // Escape double quotes by doubling them for PostgreSQL
            String escapedTestName = testName.replace("\"", "\"\"");
            query.append("\n, \"" + escapedTestName + "\" varchar(200) ");
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
                + "\n AND s.entered_date <= '" + formatDateForDatabaseSql(highDate) + "'"
        // projectStr)
        ;
        return WHERE_SAMPLE_PATIENT_ORG;
    }

    @Override
    protected void appendObservationHistoryCrosstab(java.sql.Date lowDate, java.sql.Date highDate) {
        appendCrosstabPreamble(SQLConstant.DEMO);

        // Only select TB-specific observation history types to optimize the query
        // Use doubled single quotes for PostgreSQL string literals inside the crosstab
        // SQL
        String tbObservationTypes = "''TbOrderReason'', ''TbAnalysisMethod'', ''TbDiagnosticReason'', ''TbFollowupReason'', "
                + "''TbFollowupReasonPeriodLine1'', ''TbFollowupReasonPeriodLine2'', ''TbSampleAspects''";

        query.append( // only TB-relevant Observation History items
                "\n crosstab( " + "\n 'SELECT DISTINCT oh.sample_id as samp_id, oht.type_name, value "
                        + "\n FROM observation_history AS oh, sample AS s, observation_history_type AS oht "
                        + "\n WHERE s.entered_date >= date(''" + formatDateForDatabaseSql(lowDate) + "'') "
                        + "\n AND s.entered_date <= date(''" + formatDateForDatabaseSql(highDate) + "'')"
                        + "\n AND s.id = oh.sample_id AND oh.observation_history_type_id = oht.id "
                        + "\n AND oht.type_name IN (" + tbObservationTypes + ") " + "\n order by 1;' " + "\n , "
                        + "\n 'SELECT DISTINCT oht.type_name FROM observation_history_type AS oht "
                        + "\n WHERE oht.type_name IN (" + tbObservationTypes + ") ORDER BY 1' " + "\n ) \n ");

        // Explicitly define the 7 TB observation history columns to match crosstab
        // query results
        // The crosstab query returns columns in alphabetical order: TbAnalysisMethod,
        // TbDiagnosticReason,
        // TbFollowupReason, TbFollowupReasonPeriodLine1, TbFollowupReasonPeriodLine2,
        // TbOrderReason, TbSampleAspects
        query.append(" as demo ( " + " \"s_id\" numeric(10) " + "\n, \"TbAnalysisMethod\" varchar(100) "
                + "\n, \"TbDiagnosticReason\" varchar(100) " + "\n, \"TbFollowupReason\" varchar(100) "
                + "\n, \"TbFollowupReasonPeriodLine1\" varchar(100) "
                + "\n, \"TbFollowupReasonPeriodLine2\" varchar(100) " + "\n, \"TbOrderReason\" varchar(100) "
                + "\n, \"TbSampleAspects\" varchar(100) " + " ) \n");
        appendCrosstabPostfix(lowDate, highDate, SQLConstant.DEMO);
    }

    @Override
    protected void appendCrosstabPreamble(SQLConstant listName) {
        query.append(", \n\n ( SELECT s.id AS samp_id, " + listName + ".*, a.released_date,a.type_of_sample_name "
                + " FROM sample AS s  LEFT JOIN sample_item si on si.samp_id = s.id \n"
                + " LEFT JOIN analysis a on a.sampitem_id = si.id LEFT JOIN \n ");
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.reportBeans.CIRoutineColumnBuilder#makeSQL()
     */
    @Override
    public void makeSQL() {
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

        // and finally the join that puts these all together. Each cross table should be
        // listed here otherwise it's not in the result and you'll get a full join
        query.append(buildWhereSamplePatienOrgSQL(lowDate, highDate)
                // insert joining of any other crosstab here.
                + "\n AND s.id = demo.samp_id " + "\n AND s.id = result.samp_id " + "\n ORDER BY s.accession_number ");
        // no don't insert another crosstab or table here, go up before the main WHERE
        // clause
        return;
    }

}
