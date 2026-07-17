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
package org.openelisglobal.dataexchange.sync.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Payload de remontée consolidée POSTé à {@code {sync.apiUrl}/syncOrders} du
 * serveur consolidé (module d'échange unifié, incrément 4b).
 *
 * <p>
 * Contrat aligné sur le SC legacy (ci.itech.oedatarepo.dto.DataSyncPayload) —
 * <b>ne pas renommer</b> les champs consommés ({@code labUuid}, {@code orders},
 * {@code orderMetadataDTOs}, {@code analysisDTOs}) : le SC ignore
 * silencieusement un champ inconnu, ce qui provoquerait une perte de données
 * sans erreur. Modernisation ADDITIVE seulement (le SC tolère les champs
 * inconnus) :
 *
 * <ul>
 * <li>{@code schemaVersion} : AJOUT. Ignoré par les SC actuels ; permettra à
 * une future version du SC de distinguer un émetteur modernisé d'un uploader
 * legacy (le versionnement absent de {@code /syncOrders}).</li>
 * <li>{@code checksum} : RETIRÉ (le checksum d'origine était trivial — hash des
 * seuls compteurs — et non bloquant côté SC).</li>
 * <li>Les listes imbriquées {@code orders[].orderMetadata/analyses} et {@code
 * analysisDTOs[].analysisMetadata} sont retirées des sous-DTO (jamais lues par
 * le SC).</li>
 * </ul>
 *
 * Cf. DESIGN_INTEROP_MODULE_CIV.md §6.2.
 */
public class DataSyncPayload {

    /**
     * Version de schéma de l'émetteur modernisé. Champ additif, ignoré par les SC
     * legacy. La valeur du legacy (implicite) est {@code null}.
     */
    public static final String CURRENT_SCHEMA_VERSION = "civ-oe-2";

    private String labUuid;
    private String schemaVersion = CURRENT_SCHEMA_VERSION;
    private List<OrderDTO> orders;
    private List<OrderMetadataDTO> orderMetadataDTOs;
    private List<AnalysisDTO> analysisDTOs;
    private LocalDateTime syncTimestamp;
    private Integer orderCount;
    private Integer metadataCount;
    private Integer analysisCount;

    public DataSyncPayload() {
    }

    public String getLabUuid() {
        return labUuid;
    }

    public void setLabUuid(String labUuid) {
        this.labUuid = labUuid;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public List<OrderDTO> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderDTO> orders) {
        this.orders = orders;
    }

    public List<OrderMetadataDTO> getOrderMetadataDTOs() {
        return orderMetadataDTOs;
    }

    public void setOrderMetadataDTOs(List<OrderMetadataDTO> orderMetadataDTOs) {
        this.orderMetadataDTOs = orderMetadataDTOs;
    }

    public List<AnalysisDTO> getAnalysisDTOs() {
        return analysisDTOs;
    }

    public void setAnalysisDTOs(List<AnalysisDTO> analysisDTOs) {
        this.analysisDTOs = analysisDTOs;
    }

    public LocalDateTime getSyncTimestamp() {
        return syncTimestamp;
    }

    public void setSyncTimestamp(LocalDateTime syncTimestamp) {
        this.syncTimestamp = syncTimestamp;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public Integer getMetadataCount() {
        return metadataCount;
    }

    public void setMetadataCount(Integer metadataCount) {
        this.metadataCount = metadataCount;
    }

    public Integer getAnalysisCount() {
        return analysisCount;
    }

    public void setAnalysisCount(Integer analysisCount) {
        this.analysisCount = analysisCount;
    }
}
