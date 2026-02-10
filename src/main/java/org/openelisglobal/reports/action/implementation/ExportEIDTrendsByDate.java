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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.validator.GenericValidator;
import org.jfree.util.Log;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.project.dao.ProjectDAO;
import org.openelisglobal.project.daoimpl.ProjectDAOImpl;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder;
import org.openelisglobal.reports.action.implementation.reportBeans.StudyEIDColumnBuilder;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;

public class ExportEIDTrendsByDate extends CSVSampleExportReport implements IReportParameterSetter, IReportCreator {
    protected final ProjectDAO projectDAO = new ProjectDAOImpl();
    static String EIDProjectName = "Early Infant Diagnosis for HIV Study";
    private String projectStr;
    private Project project;
    private String indicStr;
    protected static final SimpleDateFormat postgresDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // @Override
    @Override
    protected String reportFileName() {
        return "ExportEIDTrendsByDate";
    }

    @Override
    public void setRequestParameters(ReportForm form) {
        form.setReportName(getReportNameForParameterPage());
        form.setUseLowerDateRange(Boolean.TRUE);
        form.setUseUpperDateRange(Boolean.TRUE);
        form.setUseDashboard(Boolean.TRUE);
        form.setProjectCodeList(getProjectList());
    }

    protected String getReportNameForParameterPage() {
        return MessageUtil.getMessage("reports.label.project.export") + " "
                + MessageUtil.getMessage("sample.export.releasedDate");
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

        indicStr = form.getVlStudyType();

        lowDateStr = form.getLowerDateRange();
        highDateStr = form.getUpperDateRange();
        projectStr = form.getVlStudyType();
        dateRange = new DateRange(lowDateStr, highDateStr);
        String[] splitline = form.getVlStudyType().split(":");

        projectStr = splitline[0];

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
            add1LineErrorMessage("report.error.message.project.missing");
            return false;
        }
        project = SpringContext.getBean(ProjectService.class).getProjectById(projectStr);
        if (project == null) {
            add1LineErrorMessage("report.error.message.project.missing");
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

    @Override
    protected void writeResultsToBuffer(ByteArrayOutputStream buffer) {
        try {
            String currentAccessionNumber = null;
            String[] splitBase = null;
            while (csvColumnBuilder.next()) {
                String line = csvColumnBuilder.nextLine();
                String[] splitLine = line.split(",");

                if (splitLine[0].equals(currentAccessionNumber)) {
                    merge(splitBase, splitLine);
                } else {
                    if (currentAccessionNumber != null && writeAble(splitBase[16].trim())) {

                        writeConsolidatedBaseToBuffer(buffer, splitBase);
                    }
                    splitBase = splitLine;
                    currentAccessionNumber = splitBase[0];
                }
            }
            if (ObjectUtils.isNotEmpty(splitBase)) {
                if (writeAble(splitBase[16].trim())) {
                    writeConsolidatedBaseToBuffer(buffer, splitBase);
                }
            }
        } catch (IOException | SQLException | ParseException e) {
            Log.error("Error in " + this.getClass().getSimpleName() + " writeResultsToBuffer: ", e);
        }
    }

    private boolean writeAble(String result) throws ParseException {

        String[] splitLine = indicStr.split(":");
        String indic = splitLine[1];
        if (indic.equals("EID Positive")) {
            try {
                return result.toLowerCase().contains("pos");
            } catch (Exception e) {
                return false;
            }
        } else if (indic.equals("EID Negative")) {
            try {
                return result.toLowerCase().contains("neg") || result.contains("gative") || result.contains("gatif");
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    private void merge(String[] base, String[] line) {
        for (int i = 0; i < base.length; ++i) {
            if (GenericValidator.isBlankOrNull(base[i])) {
                base[i] = line[i];
            }
        }
    }

    protected void writeConsolidatedBaseToBuffer(ByteArrayOutputStream buffer, String[] splitBase)
            throws IOException, UnsupportedEncodingException {

        if (splitBase != null) {
            StringBuffer consolidatedLine = new StringBuffer();
            for (String value : splitBase) {
                consolidatedLine.append(value);
                consolidatedLine.append(",");
            }

            consolidatedLine.deleteCharAt(consolidatedLine.lastIndexOf(","));
            buffer.write(consolidatedLine.toString().getBytes("utf-8"));
        }
    }

    private CSVColumnBuilder getColumnBuilder(String projectId) {
        return new StudyEIDColumnBuilder(dateRange, projectStr);

    }

    /*
     *
     * /**
     *
     * @return a list of the correct projects for display
     */
    protected List<Project> getProjectList() {
        List<Project> projects = new ArrayList<>();

        try {
            Project eidProject = new Project();
            eidProject.setProjectName(EIDProjectName);
            eidProject = SpringContext.getBean(ProjectService.class).getProjectByName(eidProject, true, true);
            Project project = new Project();
            project.setId(eidProject.getId() + ":EID Positive");
            project.setProjectName("EID Positive");
            projects.add(project);
            project = new Project();
            project.setId(eidProject.getId() + ":EID Negative");
            project.setProjectName("EID Negative");
            projects.add(project);
        } catch (Exception e) {
            Project project = new Project();
            project.setId("25:EID Positive");
            project.setProjectName("EID Positive");
            projects.add(project);
            project = new Project();
            project.setId("25:EID Negative");
            project.setProjectName("EID Negative");
            projects.add(project);
        }

        return projects;
    }

}
