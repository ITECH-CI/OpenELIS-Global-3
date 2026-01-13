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
package org.openelisglobal.bacteriology.controller.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for individual flora detail information
 * Contains characteristics of a single bacterial flora
 */
public class FloraDetailDTO {

    @JsonProperty("floraNumber")
    private Integer floraNumber;

    @JsonProperty("gramTypeDictId")
    private Integer gramTypeDictId;

    @JsonProperty("groupingModeDictId")
    private Integer groupingModeDictId;

    @JsonProperty("capsulated")
    private Boolean capsulated;

    public FloraDetailDTO() {
        this.capsulated = false;
    }

    public FloraDetailDTO(Integer floraNumber, Integer gramTypeDictId,
                          Integer groupingModeDictId, Boolean capsulated) {
        this.floraNumber = floraNumber;
        this.gramTypeDictId = gramTypeDictId;
        this.groupingModeDictId = groupingModeDictId;
        this.capsulated = capsulated != null ? capsulated : false;
    }

    public Integer getFloraNumber() {
        return floraNumber;
    }

    public void setFloraNumber(Integer floraNumber) {
        this.floraNumber = floraNumber;
    }

    public Integer getGramTypeDictId() {
        return gramTypeDictId;
    }

    public void setGramTypeDictId(Integer gramTypeDictId) {
        this.gramTypeDictId = gramTypeDictId;
    }

    public Integer getGroupingModeDictId() {
        return groupingModeDictId;
    }

    public void setGroupingModeDictId(Integer groupingModeDictId) {
        this.groupingModeDictId = groupingModeDictId;
    }

    public Boolean getCapsulated() {
        return capsulated;
    }

    public void setCapsulated(Boolean capsulated) {
        this.capsulated = capsulated != null ? capsulated : false;
    }
}
