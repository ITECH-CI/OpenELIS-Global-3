/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH-CI. All Rights Reserved.
 */
package org.openelisglobal.dataexchange.sync.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.dataexchange.sync.dto.AnalysisDTO;
import org.openelisglobal.dataexchange.sync.dto.DataSyncPayload;
import org.openelisglobal.dataexchange.sync.dto.OrderDTO;
import org.openelisglobal.dataexchange.sync.dto.OrderMetadataDTO;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Construit le payload de remontée consolidée depuis les SERVICES MÉTIER OE
 * (incrément 4b) — pas de SQL en dur. Les noms de champs des DTO sont alignés
 * sur le contrat du serveur consolidé (cf. {@link DataSyncPayload}).
 *
 * <p>
 * {@code @Transactional(readOnly=true)} : le parcours des relations
 * ({@code getSampleItem()}, {@code getSample()}, {@code getTest()}…) traverse
 * des ValueHolder lazy — la session Hibernate doit rester ouverte pendant toute
 * la construction, sinon LazyInitializationException.
 */
@Service
public class DataSyncPayloadBuilderImpl implements DataSyncPayloadBuilder {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private PatientService patientService;

    /**
     * Identifiant du labo émetteur, requis par le SC pour attribuer la remontée
     * (contrat legacy). Sans lui, le SC ne peut rattacher les données au bon labo.
     */
    @Value("${org.openelisglobal.consolidated.sync.labUuid:}")
    private String labUuid;

    /** Type d'organisation "demandeur/référent" à remonter (referring org). */
    private static final String REFERRING_ORG_TYPE_ID = "1";

    @Override
    @Transactional(readOnly = true)
    public DataSyncPayload buildForAnalysisIds(List<String> analysisIds) {
        DataSyncPayload payload = new DataSyncPayload();
        List<OrderDTO> orders = new ArrayList<>();
        List<AnalysisDTO> analyses = new ArrayList<>();
        List<OrderMetadataDTO> metadata = new ArrayList<>();
        // Un seul OrderDTO / OrderMetadataDTO par sample, même si plusieurs analyses.
        Map<String, OrderDTO> ordersBySample = new LinkedHashMap<>();

        if (analysisIds != null) {
            for (String analysisId : analysisIds) {
                try {
                    Analysis analysis = analysisService.get(analysisId);
                    if (analysis == null) {
                        continue;
                    }
                    // Garde-fou : analysisLocalId est la CLÉ d'upsert côté SC. Un id non
                    // numérique produirait une clé null -> collision/écrasement au SC. Les
                    // id OE sont numériques (LIMSStringNumberUserType), mais on saute par
                    // sécurité plutôt que d'émettre un DTO à clé nulle.
                    if (toLong(analysis.getId()) == null) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "buildForAnalysisIds",
                                "analyse " + analysisId + " ignorée : id non numérique (clé d'upsert invalide)");
                        continue;
                    }
                    analyses.add(toAnalysisDTO(analysis));

                    SampleItem sampleItem = analysis.getSampleItem();
                    Sample sample = sampleItem != null ? sampleItem.getSample() : null;
                    if (sample != null && !ordersBySample.containsKey(sample.getId())) {
                        ordersBySample.put(sample.getId(), toOrderDTO(sample));
                        metadata.addAll(toMetadata(sample));
                    }
                } catch (RuntimeException e) {
                    // Une analyse défaillante ne doit pas casser tout le lot.
                    LogEvent.logError(this.getClass().getSimpleName(), "buildForAnalysisIds",
                            "analyse " + analysisId + " ignorée : " + e);
                }
            }
        }
        orders.addAll(ordersBySample.values());

        payload.setLabUuid(GenericValidator.isBlankOrNull(labUuid) ? null : labUuid);
        payload.setSyncTimestamp(LocalDateTime.now());
        payload.setOrders(orders);
        payload.setAnalysisDTOs(analyses);
        payload.setOrderMetadataDTOs(metadata);
        payload.setOrderCount(orders.size());
        payload.setAnalysisCount(analyses.size());
        payload.setMetadataCount(metadata.size());
        return payload;
    }

    private AnalysisDTO toAnalysisDTO(Analysis a) {
        AnalysisDTO dto = new AnalysisDTO();
        dto.setAnalysisLocalId(toLong(a.getId()));
        SampleItem sampleItem = a.getSampleItem();
        if (sampleItem != null) {
            dto.setSampleItemLocalId(toLong(sampleItem.getId()));
            dto.setSampleItemSortOrder(toInteger(sampleItem.getSortOrder()));
            if (sampleItem.getSample() != null) {
                dto.setSampleLocalId(toLong(sampleItem.getSample().getId()));
            }
        }
        dto.setAnalysisUuid(GenericValidator.isBlankOrNull(a.getFhirUuidAsString()) ? null : a.getFhirUuidAsString());
        dto.setAnalysisStatusId(a.getStatusId());
        dto.setAnalysisStatus(statusName(a.getStatusId()));
        dto.setAnalysisStartedDate(toLdt(a.getStartedDate()));
        dto.setAnalysisCompletedDate(toLdt(a.getCompletedDate()));
        dto.setAnalysisReleasedDate(toLdt(a.getReleasedDate()));
        dto.setSampleTypeName(a.getSampleTypeName());
        if (a.getTestSection() != null) {
            dto.setTestSection(a.getTestSection().getLocalizedName());
        }
        if (a.getTest() != null) {
            dto.setTest(a.getTest().getLocalizedName());
            dto.setTestLoinc(a.getTest().getLoinc());
        }
        dto.setAnalysisLastupdated(toLdt(a.getLastupdated()));

        // LIMITATION DE CONTRAT : le SC ne stocke qu'UN résultat par analyse (upsert
        // par analysisLocalId+labId, un seul resultLocalId/resultValue par ligne).
        // L'uploader legacy avait la même limite (dédup du LEFT JOIN result). On
        // remonte donc UN résultat, mais on privilégie un résultat REPORTABLE (plus
        // pertinent que le plus petit id arbitraire) ; fallback sur tous les résultats.
        // À revoir si le SC évolue pour porter plusieurs résultats (cf. bactériologie
        // multi-résultats : groupes/ATB/flore).
        List<Result> results = resultService.getReportableResultsByAnalysis(a);
        if (results == null || results.isEmpty()) {
            results = resultService.getResultsByAnalysis(a);
        }
        if (results != null && !results.isEmpty()) {
            Result r = results.get(0);
            dto.setResultLocalId(toLong(r.getId()));
            dto.setResultSortOrder(r.getSortOrder());
            dto.setResultType(r.getResultType());
            dto.setResultValue(safeResultValue(r));
            dto.setResultMinLimit(r.getMinNormal() == null ? null : String.valueOf(r.getMinNormal()));
            dto.setResultMaxLimit(r.getMaxNormal() == null ? null : String.valueOf(r.getMaxNormal()));
        }
        return dto;
    }

    private OrderDTO toOrderDTO(Sample sample) {
        OrderDTO dto = new OrderDTO();
        dto.setSampleLocalId(toLong(sample.getId()));
        dto.setLabno(sample.getAccessionNumber());
        dto.setSampleEnteredDate(toLdt(sample.getEnteredDate()));
        dto.setSampleReceivedDate(toLdt(sample.getReceivedTimestamp()));
        dto.setSampleCollectionDate(toLdt(sample.getCollectionDate()));
        dto.setSampleStatus(statusName(sample.getStatusId()));
        dto.setSampleReferringId(sample.getReferringId());
        dto.setSampleLastupdated(toLdt(sample.getLastupdated()));

        // Patient
        Patient patient = sampleService.getPatient(sample);
        if (patient != null) {
            dto.setPatientLocalId(toLong(patient.getId()));
            dto.setPatientNationalId(patient.getNationalId());
            dto.setPatientUpidCode(patient.getUpidCode());
            dto.setPatientExternalId(patient.getExternalId());
            dto.setPatientGuid(patientService.getGUID(patient));
            dto.setPatientGender(patient.getGender());
            dto.setPatientBirthdate(toLocalDate(patient.getBirthDate()));
        }

        // Organisation demandeuse (referring)
        Organization org = sampleService.getOrganizationRequester(sample, REFERRING_ORG_TYPE_ID);
        if (org != null) {
            dto.setOrganizationName(org.getOrganizationName());
            dto.setOrganizationDhis2Code(org.getShortName());
            dto.setOrganizationDatimCode(org.getDatimOrgCode());
            dto.setOrganizationDatimName(org.getDatimOrgName());
        }

        // Prestataire (provider)
        Person requester = sampleService.getPersonRequester(sample);
        if (requester != null) {
            dto.setSampleProvider(fullName(requester));
        }
        return dto;
    }

    private List<OrderMetadataDTO> toMetadata(Sample sample) {
        List<OrderMetadataDTO> list = new ArrayList<>();
        List<ObservationHistory> histories = SpringContext
                .getBean(org.openelisglobal.observationhistory.service.ObservationHistoryService.class)
                .getObservationHistoriesBySampleId(sample.getId());
        if (histories == null) {
            return list;
        }
        ObservationHistoryTypeService typeService = SpringContext.getBean(ObservationHistoryTypeService.class);
        for (ObservationHistory oh : histories) {
            OrderMetadataDTO dto = new OrderMetadataDTO();
            dto.setObservationLocalId(toLong(oh.getId()));
            dto.setSampleLocalId(toLong(oh.getSampleId()));
            dto.setSampleItemLocalId(toLong(oh.getSampleItemId()));
            dto.setValueType(oh.getValueType());
            dto.setObservationValue(oh.getValue());
            if (!GenericValidator.isBlankOrNull(oh.getObservationHistoryTypeId())) {
                ObservationHistoryType type = typeService.get(oh.getObservationHistoryTypeId());
                if (type != null) {
                    dto.setObservationName(type.getTypeName());
                }
            }
            dto.setObservationLastupdated(toLdt(oh.getLastupdated()));
            list.add(dto);
        }
        return list;
    }

    private String statusName(String statusId) {
        if (GenericValidator.isBlankOrNull(statusId)) {
            return null;
        }
        try {
            return SpringContext.getBean(IStatusService.class).getStatusNameFromId(statusId);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String safeResultValue(Result r) {
        try {
            return resultService.getResultValue(r, false);
        } catch (RuntimeException e) {
            return r.getValue();
        }
    }

    private static String fullName(Person p) {
        String first = p.getFirstName() == null ? "" : p.getFirstName().trim();
        String last = p.getLastName() == null ? "" : p.getLastName().trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? null : full;
    }

    // --- Conversions de types ---------------------------------------------------

    /**
     * {@code getId()} renvoie une String numérique ; le contrat SC attend un Long.
     */
    private static Long toLong(String s) {
        if (GenericValidator.isBlankOrNull(s)) {
            return null;
        }
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer toInteger(String s) {
        if (GenericValidator.isBlankOrNull(s)) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** java.util.Date (sql.Date / Timestamp) -> LocalDateTime (fuseau système). */
    private static LocalDateTime toLdt(Date d) {
        if (d == null) {
            return null;
        }
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static LocalDate toLocalDate(Date d) {
        if (d == null) {
            return null;
        }
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
