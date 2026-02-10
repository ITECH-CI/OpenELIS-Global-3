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

import java.sql.Date;
import org.openelisglobal.reports.action.implementation.Report.DateRange;

public class EOrderColumnBuilder extends CIStudyColumnBuilder {

    /**
     * @param dateRange
     * 
     */
    public EOrderColumnBuilder(DateRange dateRange) {
        super(dateRange, null);
    }

    /**
     * @see org.openelisglobal.reports.action.implementation.reportBeans.CIRoutineColumnBuilder#defineAllReportColumns()
     */
    @Override
    protected void defineAllReportColumns() {
        add("site_name", "SITE_NAME", Strategy.NONE);
        add("site_code", "SITE_CODE", Strategy.NONE);
        add("site_datim_code", "SITE_CODE_DATIM", Strategy.NONE);
        add("patient_subject_number", "PATIENT_CODE", Strategy.NONE);
        add("hiv_status", "HIV_STATUS", Strategy.NONE);
        add("regimen", "REGIMEN", Strategy.NONE);
        add("gender", "SEX", Strategy.NONE);
        add("birth_date", "BIRTH_DATE", Strategy.NONE);
        add("test_name", "TEST_NAME", Strategy.NONE);
        add("eorder_status", "EORDER_STATUS", Strategy.EORDER_STATUS);
        add("analysis_status", "ANALYSIS_STATUS", Strategy.ANALYSIS_STATUS);
        add("reject_reason", "EORDER_REJECTED_REASON", Strategy.NONE);
        add("order_reason", "ORDER_REASON", Strategy.DICT_PLUS);
        add("specimen_type", "SAMPLE_TYPE", Strategy.NONE);
        add("accession_number", "LABNO", Strategy.NONE);
        add("collection_date", "COLLECTION_DATE", Strategy.DATE_TIME);
        add("order_timestamp", "EORDER_RECEPTION_DATE", Strategy.DATE_TIME);
        add("reception_date", "SAMPLE_RECEPTION_DATE", Strategy.DATE_TIME);
        add("entry_date", "ENTRY_DATE", Strategy.DATE_TIME);
        add("completed_date", "COMPLETED_DATE", Strategy.DATE_TIME);
        add("released_date", "RELEASED_DATE", Strategy.DATE_TIME);
        add("test_result", "RESULT (CP/ML)", Strategy.NONE);
        add("test_result", "RESULT LOG", Strategy.LOG);

    }

    /**
     * @see org.openelisglobal.reports.action.implementation.reportBeans.CIRoutineColumnBuilder#makeSQL()
     */
    @Override
    public void makeSQL() {
        query = new StringBuilder();
        Date lowDate = dateRange.getLowDate();
        Date highDate = dateRange.getHighDate();

        query.append("SELECT\n" + "  coalesce(vl_sample.site_code,org.short_name) as site_code,\n"
                + "  coalesce(vl_sample.site_datim_code,org.datim_org_code) as site_datim_code,\n"
                + "  coalesce(vl_sample.site_name,org.name) as site_name,\n"
                + "  pat.national_id as patient_subject_number,\n"
                // + " pat.external_id as patient_site_subject_number,\n"
                + "  inputs.hiv_status,\n" + "  inputs.regimen,\n" + "  inputs.order_reason,\n"
                + "  inputs.specimen_type,\n" + "  pat.gender,\n" + "  pat.birth_date,\n" + "  pat.upid_code,\n"
                + "  eo.status_id AS eorder_status,\n" + "  qe.description reject_reason,\n"
                + "  to_timestamp((data::jsonb)->>'authoredOn', 'YYYY-MM-DD\"T\"HH24:MI') AS creation_date,\n"
                + "   vl_sample.test_name as test_name,\n" + "  vl_sample.analysis_status,\n"
                + "  vl_sample.accession_number,\n" + "  vl_sample.collection_date,\n" + "  eo.order_timestamp,\n"
                + "  vl_sample.reception_date,\n" + "  vl_sample.entry_date,\n" + "  vl_sample.completed_date,\n"
                + "  vl_sample.released_date,\n" + "  vl_sample.test_result\n" + "FROM\n"
                + " (select * from electronic_order where order_timestamp between '" + lowDate + "'" + " and '"
                + highDate + "' ) eo\n" + "LEFT JOIN patient pat ON pat.id = eo.patient_id\n"
                + "LEFT JOIN organization org on org.id = eo.organization_id \n"
                + "LEFT JOIN qa_event qe on qe.id = eo.reject_reason_id \n" + "LEFT JOIN LATERAL (\n" + "  SELECT\n"
                + "    MAX(CASE WHEN input->'type'->'coding' @> '[{\"code\": \"CI0030001AAAAAAAAAAAAAAAAAAAAAAAAAAA\"}]' \n"
                + "             THEN input->>'valueString' END) AS hiv_status,\n"
                + "    MAX(CASE WHEN input->'type'->'coding' @> '[{\"code\": \"162240AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"}]'\n"
                + "             THEN input->>'valueString' END) AS regimen,\n"
                + "    MAX(CASE WHEN input->'type'->'coding' @> '[{\"code\": \"CI0050002AAAAAAAAAAAAAAAAAAAAAAAAAAA\"}]'\n"
                + "             THEN input->>'valueString' END) AS order_reason,\n"
                + "    MAX(CASE WHEN input->'type'->'coding' @> '[{\"code\": \"CI0050007AAAAAAAAAAAAAAAAAAAAAAAAAAA\"}]'\n"
                + "             THEN input->>'valueString' END) AS specimen_type\n"
                + "  FROM jsonb_array_elements((data::jsonb)->'input') AS input\n" + ") inputs ON true\n"
                + "LEFT JOIN (\n"
                + "  select o.short_name site_code,o.datim_org_code site_datim_code,o.name site_name,t.name test_name,\n"
                + "a.status_id analysis_status,s.accession_number accession_number,si.collection_date collection_date,\n"
                + "s.received_date reception_date,s.entered_date entry_date,a.completed_date completed_date,\n"
                + "a.released_date released_date,res.value test_result, s.referring_id referring_id\n"
                + "from sample s join sample_item si ON s.id = si.samp_id \n"
                + "join analysis a on a.sampitem_id = si.id \n"
                + "join type_of_sample tos on tos.id = si.typeosamp_id \n"
                + "join test t on t.id = a.test_id left join result res on res.analysis_id = a.id\n"
                + "left join sample_organization so on so.samp_id = s.id\n"
                + "left join organization o on o.id = so.org_id\n"
                + "left join (select  oh.sample_id, oht.type_name,oh.value \n"
                + "from observation_history oh join observation_history_type oht on oh.observation_history_type_id = oht.id \n"
                + "where oht.type_name = 'nameOfRequestor') requester on s.id = requester.sample_id where s.referring_id is not null \n"
                + ") vl_sample ON eo.external_id = vl_sample.referring_id and eo.order_timestamp between '" + lowDate
                + "'  and  '" + highDate + "' order by eo.order_timestamp DESC ");

        return;
    }

}
