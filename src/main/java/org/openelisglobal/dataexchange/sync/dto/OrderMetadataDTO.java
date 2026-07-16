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

/**
 * Renseignement clinique (observation_history) accompagnant une demande,
 * remonté au serveur consolidé. Noms de champs JSON alignés EXACTEMENT sur le
 * contrat SC (ci.itech.oedatarepo.dto.OrderMetadataDTO) — ne pas renommer (le
 * SC ignore silencieusement un champ inconnu = perte de données). Cf.
 * DESIGN_INTEROP_MODULE_CIV.md §6.2.
 */
public class OrderMetadataDTO {

    private Long sampleLocalId;
    private Long sampleItemLocalId;
    private Long observationLocalId;
    private String observationName;
    private String valueType;
    private String observationDisplayKey;
    private String observationValue;
    private LocalDateTime observationLastupdated;

    public Long getSampleLocalId() {
        return sampleLocalId;
    }

    public void setSampleLocalId(Long sampleLocalId) {
        this.sampleLocalId = sampleLocalId;
    }

    public Long getSampleItemLocalId() {
        return sampleItemLocalId;
    }

    public void setSampleItemLocalId(Long sampleItemLocalId) {
        this.sampleItemLocalId = sampleItemLocalId;
    }

    public Long getObservationLocalId() {
        return observationLocalId;
    }

    public void setObservationLocalId(Long observationLocalId) {
        this.observationLocalId = observationLocalId;
    }

    public String getObservationName() {
        return observationName;
    }

    public void setObservationName(String observationName) {
        this.observationName = observationName;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getObservationDisplayKey() {
        return observationDisplayKey;
    }

    public void setObservationDisplayKey(String observationDisplayKey) {
        this.observationDisplayKey = observationDisplayKey;
    }

    public String getObservationValue() {
        return observationValue;
    }

    public void setObservationValue(String observationValue) {
        this.observationValue = observationValue;
    }

    public LocalDateTime getObservationLastupdated() {
        return observationLastupdated;
    }

    public void setObservationLastupdated(LocalDateTime observationLastupdated) {
        this.observationLastupdated = observationLastupdated;
    }
}
