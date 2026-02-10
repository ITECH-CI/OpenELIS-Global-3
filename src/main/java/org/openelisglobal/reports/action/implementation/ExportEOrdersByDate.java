/**
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
 * Copyright (C) CIRG, University of Washington, Seattle WA.  All Rights Reserved.
 *
 */
package org.openelisglobal.reports.action.implementation;

import static org.apache.commons.validator.GenericValidator.isBlankOrNull;

import org.jfree.util.Log;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder;
import org.openelisglobal.reports.action.implementation.reportBeans.EOrderColumnBuilder;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;

public class ExportEOrdersByDate extends CSVSampleExportReport implements IReportParameterSetter, IReportCreator {
    private String projectStr;
    private Project project;

    // @Override
    @Override
    protected String reportFileName() {
        return "ExportEOrderByDate";
    }

    @Override
    public void setRequestParameters(ReportForm form) {
        form.setReportName(getReportNameForParameterPage());
        form.setUseLowerDateRange(Boolean.TRUE);
        form.setUseUpperDateRange(Boolean.TRUE);
        form.setUseExportDateType(Boolean.FALSE);
    }

    protected String getReportNameForParameterPage() {
        return MessageUtil.getMessage("reports.label.project.export") + " "
                + MessageUtil.getMessage("sample.entry.project.receivedDate");
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();
        reportParameters.put("studyName", (project == null) ? null : project.getLocalizedName());
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        errorFound = false;

        lowDateStr = form.getLowerDateRange();
        highDateStr = form.getUpperDateRange();
        projectStr = form.getProjectCode();
        dateRange = new DateRange(lowDateStr, highDateStr);

        createReportParameters();

        errorFound = !validateSubmitParameters();
        if (errorFound) {
            return;
        }

        createReportItems();
    }

    /**
     * check everything
     */
    private boolean validateSubmitParameters() {
        return dateRange.validateHighLowDate("report.error.message.date.received.missing") && validateProject();
    }

    /**
     * @return true, if location is not blank or "0" is is found in the DB; false
     *         otherwise
     */
    private boolean validateProject() {
        if (isBlankOrNull(projectStr) || "0".equals(Integer.getInteger(projectStr))) {
            Log.error("Error in " + this.getClass().getSimpleName() + " validateProject: Project String not valid ");
            add1LineErrorMessage("report.error.message.project.missing");
            return false;
        }
        project = SpringContext.getBean(ProjectService.class).getProjectById(projectStr);
        if (project == null) {
            add1LineErrorMessage("report.error.message.project.missing");
            Log.error("Error in " + this.getClass().getSimpleName() + " validateProject: Project is null");
            return false;
        }
        return true;
    }

    /**
     * creating the list for generation to the report
     */
    private void createReportItems() {
        try {
            csvColumnBuilder = getColumnBuilder(projectStr);
            csvColumnBuilder.buildDataSource();
        } catch (Exception e) {
            Log.error("Error in " + this.getClass().getSimpleName() + ".createReportItems: ", e);
            add1LineErrorMessage("report.error.message.general.error");
        }
    }

    private CSVColumnBuilder getColumnBuilder(String projectStr) {
        return new EOrderColumnBuilder(dateRange);

    }

}
