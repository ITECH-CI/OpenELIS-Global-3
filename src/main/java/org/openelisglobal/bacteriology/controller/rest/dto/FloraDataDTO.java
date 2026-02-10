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
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for bacterial flora data Contains the flora count and detailed
 * information about each flora
 */
public class FloraDataDTO {

    @JsonProperty("count")
    private Integer count;

    @JsonProperty("details")
    private List<FloraDetailDTO> details;

    public FloraDataDTO() {
        this.details = new ArrayList<>();
    }

    public FloraDataDTO(Integer count, List<FloraDetailDTO> details) {
        this.count = count;
        this.details = details != null ? details : new ArrayList<>();
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<FloraDetailDTO> getDetails() {
        return details;
    }

    public void setDetails(List<FloraDetailDTO> details) {
        this.details = details != null ? details : new ArrayList<>();
    }
}
