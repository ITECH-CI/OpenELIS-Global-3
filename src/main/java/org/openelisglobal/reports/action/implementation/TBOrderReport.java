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

package org.openelisglobal.reports.action.implementation;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.ErrorMessages;
import org.openelisglobal.reports.action.implementation.reportBeans.TbCategoryReportData;
import org.openelisglobal.reports.action.implementation.reportBeans.TbMicroscopyReportData;
import org.openelisglobal.reports.action.implementation.reportBeans.TbTestReportData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;

/**
 */
public class TBOrderReport extends Report implements IReportCreator, IReportParameterSetter {

    protected String lowerDateRange;
    protected String upperDateRange;
    protected Date lowDate;
    protected Date highDate;

    protected String reportType;
    private List<TbTestReportData> testReportItems;
    private List<TbCategoryReportData> categoryReportItems;
    private List<TbMicroscopyReportData> microscopyReportItems;

    private static Map<String, String> testResultMap;
    private static Map<String, String> tbCatetoryMap;
    private static Map<String, String> microscopyReportItemMap;

    static {
        testResultMap = new LinkedHashMap<String, String>();
        testResultMap.put("MTB détecté RIF Résistant", "MTB détecté, RIF résistante");
        testResultMap.put("MTB détecté RIF sensible", "MTB détecté, RIF sensible");
        testResultMap.put("MTB détecté RIF indéterminé", "MTB détecté, RIF indéterminé");
        testResultMap.put("MTB non détecté", "MTB non détecté");
        testResultMap.put("Erreur", "Erreurs");
        testResultMap.put("Invalide", "Invalides");
        testResultMap.put("Pas de résultat", "Pas de résultat");
        testResultMap.put("Total Received", "Nombre d’échantillons reçus");
        testResultMap.put("Total Realized", "Nombre de tests réalisés");
    }

    static {
        tbCatetoryMap = new LinkedHashMap<String, String>();
        tbCatetoryMap.put("Cas présumé jamais traité", "Cas présumés jamais traité");
        tbCatetoryMap.put("Rechute", "Rechutes");
        tbCatetoryMap.put("Echec", "Echecs");
        tbCatetoryMap.put("Reprise", "Reprises");
        tbCatetoryMap.put("Frottis positif à M2", "Frottis positif à M2*");

    }

    static {
        microscopyReportItemMap = new LinkedHashMap<String, String>();
        microscopyReportItemMap.put("Cas présumé jamais traité", "Frottis de cas présumés");
        microscopyReportItemMap.put("M2", "Frottis de Suivi M2");
        microscopyReportItemMap.put("M3_M9", "Frottis de Suivi M3 à M9");
    }

    private Integer presumedCase = 0;
    private Integer positiveCase = 0;

    private TestService testService = SpringContext.getBean(TestService.class);

    @Override
    public void setRequestParameters(ReportForm form) {
        new ReportSpecificationParameters(ReportSpecificationParameters.Parameter.DATE_RANGE,
                MessageUtil.getMessage("report.activity.report.base") + " " + MessageUtil.getMessage("test_section.TB"),
                MessageUtil.getMessage("report.instruction.all.fields")).setRequestParameters(form);
        new ReportSpecificationList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.TB_ACTIVITY_REPORT),
                MessageUtil.getMessage("tb.activity_report.type")).setRequestParameters(form);
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        errorFound = false;
        reportType = form.getSelectList().getSelection();

        lowerDateRange = form.getLowerDateRange();
        upperDateRange = form.getUpperDateRange();

        if (GenericValidator.isBlankOrNull(lowerDateRange)) {
            errorFound = true;
            ErrorMessages msgs = new ErrorMessages();
            msgs.setMsgLine1(MessageUtil.getMessage("report.error.message.noPrintableItems"));
            errorMsgs.add(msgs);
        }

        if (GenericValidator.isBlankOrNull(upperDateRange)) {
            upperDateRange = lowerDateRange;
        }

        try {
            lowDate = DateUtil.convertStringDateToSqlDate(lowerDateRange);
            highDate = DateUtil.convertStringDateToSqlDate(upperDateRange);
        } catch (LIMSRuntimeException e) {
            errorFound = true;
            ErrorMessages msgs = new ErrorMessages();
            msgs.setMsgLine1(MessageUtil.getMessage("report.error.message.date.format"));
            errorMsgs.add(msgs);
        }

        initializeReportItems();
        createReportParameters();

    }

    protected void initializeReportItems() {
        testReportItems = new ArrayList<>();
        categoryReportItems = new ArrayList<>();
        microscopyReportItems = new ArrayList<>();

        if (reportType.equals("GENEXPERT_MTB_REPORT")) {
            setTestReportItems(lowDate, highDate);
            setCategoryReportItems(lowDate, highDate);
        } else if (reportType.equals("MICROSCOPY_REPORT")) {
            setMicroscopyReportItems(lowDate, highDate);
        }

    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();

        reportParameters.put("siteName", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName));
        reportParameters.put("startDate", lowerDateRange);
        reportParameters.put("endDate", upperDateRange);

        reportParameters.put("testReportDataSource", new JRBeanCollectionDataSource(testReportItems));
        reportParameters.put("categoryReportDataSource", new JRBeanCollectionDataSource(categoryReportItems));
        reportParameters.put("microscopyReportDataSource", new JRBeanCollectionDataSource(microscopyReportItems));

        lowDate = DateUtil.convertStringDateToSqlDate(lowerDateRange);
        highDate = DateUtil.convertStringDateToSqlDate(upperDateRange);

        Integer receivedTbPresumedMicroscopyTestCountInteger = testService
                .getReceivedTbPresumedMicroscopyTestCount(lowDate, highDate);

        Integer positiveTbMicroscopyTestCountInteger = testService.getPositiveTbMicroscopyTestCount(lowDate, highDate);

        reportParameters.put("presumedCase",
                ObjectUtils.isNotEmpty(receivedTbPresumedMicroscopyTestCountInteger)
                        ? receivedTbPresumedMicroscopyTestCountInteger.intValue()
                        : 0);
        reportParameters.put("positiveCase",
                ObjectUtils.isNotEmpty(positiveTbMicroscopyTestCountInteger)
                        ? positiveTbMicroscopyTestCountInteger.intValue()
                        : 0);

    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JREmptyDataSource();
    }

    @Override
    protected String reportFileName() {
        if (reportType.equals("GENEXPERT_MTB_REPORT")) {
            return "TBGeneXpertReport";
        } else if (reportType.equals("MICROSCOPY_REPORT")) {
            return "TBMicroscopyReport";
        }
        return "";
    }

    protected void setTestReportItems(Date startDate, Date endDate) {
        List<Map<String, Object>> results = testService.getTbGXTestCountByResult(startDate, endDate);
        Set<Integer> months = results.stream().map(res -> Integer.parseInt(res.get("month").toString()))
                .collect(Collectors.toSet());

        LinkedList<Integer> quarterMonths = getSortedMonths(months, 1);

        testResultMap.forEach((k, v) -> {
            TbTestReportData data = new TbTestReportData();
            Integer m1 = 0, m2 = 0, m3 = 0, total = 0;
            Integer m1TotalNonRealized = 0, m2TotalNonRealized = 0, m3TotalNonRealized = 0, totalNonRealized = 0;
            Integer m1TotalReceived = 0, m2TotalReceived = 0, m3TotalReceived = 0, totalReceived = 0;

            for (Map<String, Object> element : results) {

                if (ObjectUtils.isNotEmpty(element.get("resultValue"))) {
                    if (element.get("resultValue").equals(k)) {
                        total += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());

                        if (quarterMonths.get(0) == Integer.parseInt(element.get("month").toString())) {
                            m1 += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                        } else if (quarterMonths.get(1) == Integer.parseInt(element.get("month").toString())) {
                            m2 += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                        }
                        if (quarterMonths.get(2) == Integer.parseInt(element.get("month").toString())) {
                            m3 += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                        }
                    }
                } else {
                    // for analysis that have test result null
                    totalNonRealized += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());

                    if (quarterMonths.get(0) == Integer.parseInt(element.get("month").toString())) {
                        m1TotalNonRealized += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    } else if (quarterMonths.get(1) == Integer.parseInt(element.get("month").toString())) {
                        m2TotalNonRealized += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    }
                    if (quarterMonths.get(2) == Integer.parseInt(element.get("month").toString())) {
                        m3TotalNonRealized += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    }
                }

                // for all
                totalReceived += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());

                if (quarterMonths.get(0) == Integer.parseInt(element.get("month").toString())) {
                    m1TotalReceived += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                } else if (quarterMonths.get(1) == Integer.parseInt(element.get("month").toString())) {
                    m2TotalReceived += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                }
                if (quarterMonths.get(2) == Integer.parseInt(element.get("month").toString())) {
                    m3TotalReceived += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                }

            }
            data.setResultLabel(v);
            if (k.equals("Total Received")) {
                data.setTestInMonth1(m1TotalReceived);
                data.setTestInMonth2(m2TotalReceived);
                data.setTestInMonth3(m3TotalReceived);
                data.setTotalTest(totalReceived);
            } else if (k.equals("Total Realized")) {
                data.setTestInMonth1(m1TotalReceived - m1TotalNonRealized);
                data.setTestInMonth2(m2TotalReceived - m2TotalNonRealized);
                data.setTestInMonth3(m3TotalReceived - m3TotalNonRealized);
                data.setTotalTest(totalReceived - totalNonRealized);
            } else {
                data.setTestInMonth1(m1);
                data.setTestInMonth2(m2);
                data.setTestInMonth3(m3);
                data.setTotalTest(total);
            }
            testReportItems.add(data);

        });

    }

    protected void setCategoryReportItems(Date startDate, Date endDate) {

        List<Map<String, Object>> results = testService.getTbGXTestCountByCategory(startDate, endDate);

        tbCatetoryMap.forEach((k, v) -> {
            Integer testResultResistant = 0, testResultSensitive = 0, testResultIndeterminate = 0,
                    testResultMTBNonDetected = 0;
            TbCategoryReportData data = new TbCategoryReportData();

            for (Map<String, Object> element : results) {
                if (ObjectUtils.isEmpty(element.get("diagnosticReason")))
                    continue;
                if (element.get("diagnosticReason").equals(k)) {
                    if (ObjectUtils.isEmpty(element.get("resultValue"))) {
                        continue;
                    }
                    if (element.get("resultValue").equals("MTB détecté RIF Résistant")) {
                        testResultResistant += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    }
                    if (element.get("resultValue").equals("MTB détecté RIF sensible")) {
                        testResultSensitive += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    }
                    if (element.get("resultValue").equals("MTB détecté RIF indéterminé")) {
                        testResultIndeterminate += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    }
                    if (element.get("resultValue").equals("MTB non détecté")) {
                        testResultMTBNonDetected += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    }
                }
            }

            data.setCategoryName(v);
            data.setTestResultResistant(testResultResistant);
            data.setTestResultIndeterminate(testResultIndeterminate);
            data.setTestResultMTBNonDetected(testResultMTBNonDetected);
            data.setTestResultSensitive(testResultSensitive);
            data.setTotalTest(
                    testResultResistant + testResultSensitive + testResultIndeterminate + testResultMTBNonDetected);
            categoryReportItems.add(data);
        });
    }

    protected void setMicroscopyReportItems(Date startDate, Date endDate) {
        List<Map<String, Object>> results = testService.getTbMicroscopyTestCountByResult(startDate, endDate);

        microscopyReportItemMap.forEach((k, v) -> {
            Integer positivePlusResult = 0;
            Integer positiveRBResult = 0;
            Integer negativeResult = 0;
            TbMicroscopyReportData data = new TbMicroscopyReportData();
            List<String> m3_9 = Arrays.asList("M3", "M4", "M5", "M6", "M7", "M8", "M9");

            for (Map<String, Object> element : results) {
                if (ObjectUtils.isEmpty(element.get("dataLabel")))
                    continue;
                if (element.get("dataLabel").equals("Cas présumé jamais traité")
                        && k.equals("Cas présumé jamais traité")) {
                    if (ObjectUtils.isEmpty(element.get("resultValue"))) {
                        continue;
                    }
                    if (element.get("resultValue").equals("Positif +++")
                            || element.get("resultValue").equals("Positif ++")
                            || element.get("resultValue").equals("Positif +")) {
                        positivePlusResult += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                        positiveCase += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                        presumedCase += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    }
                    if (element.get("resultValue").equals("Positif Rare BAAR")) {
                        positiveRBResult += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                        positiveCase += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                        presumedCase += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    }
                    if (element.get("resultValue").equals("Négatif")) {
                        negativeResult += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                        presumedCase += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    }

                }
                if (element.get("dataLabel").equals("M2") && k.equals("M2")) {
                    if (ObjectUtils.isEmpty(element.get("resultValue"))) {
                        continue;
                    }
                    if (element.get("resultValue").equals("Positif +++")
                            || element.get("resultValue").equals("Positif ++")
                            || element.get("resultValue").equals("Positif +")) {
                        positivePlusResult += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    }
                    if (element.get("resultValue").equals("Positif Rare BAAR")) {
                        positiveRBResult += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    }
                    if (element.get("resultValue").equals("Négatif")) {
                        negativeResult += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    }
                }
                if (m3_9.contains(element.get("dataLabel").toString()) && k.equals("M3_M9")) {
                    if (ObjectUtils.isEmpty(element.get("resultValue"))) {
                        continue;
                    }
                    if (element.get("resultValue").equals("Positif +++")
                            || element.get("resultValue").equals("Positif ++")
                            || element.get("resultValue").equals("Positif +")) {
                        positivePlusResult += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    }
                    if (element.get("resultValue").equals("Positif Rare BAAR")) {
                        positiveRBResult += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    }
                    if (element.get("resultValue").equals("Négatif")) {
                        negativeResult += Integer.parseInt(element.getOrDefault("resultCount", 0).toString());
                    }
                }
            }

            data.setReportItem(v);
            data.setPositivePlusResult(positivePlusResult);
            data.setPositiveRBResult(positiveRBResult);
            data.setNegativeResult(negativeResult);
            data.setTotalResult(positivePlusResult + positiveRBResult + negativeResult);
            microscopyReportItems.add(data);

        });
    }

    private static LinkedList<Integer> getSortedMonths(Set<Integer> months, int startingMonth) {
        LinkedList<Integer> sortedMonths = new LinkedList<>();

        List<Integer> sortedInput = new ArrayList<>(months);
        sortedInput.sort((m1, m2) -> {
            int pos1 = (m1 - startingMonth + 12) % 12;
            int pos2 = (m2 - startingMonth + 12) % 12;
            return Integer.compare(pos1, pos2);
        });

        sortedMonths.addAll(sortedInput);

        while (sortedMonths.size() < 3) {
            int lastMonth = sortedMonths.isEmpty() ? startingMonth : sortedMonths.getLast();
            int nextMonth = (lastMonth % 12) + 1;
            if (!sortedMonths.contains(nextMonth)) {
                sortedMonths.add(nextMonth);
            }
        }
        while (sortedMonths.size() > 3) {
            sortedMonths.removeLast();
        }

        return sortedMonths;
    }

}
