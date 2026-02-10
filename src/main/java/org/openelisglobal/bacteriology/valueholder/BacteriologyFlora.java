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
package org.openelisglobal.bacteriology.valueholder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * BacteriologyFlora - Represents bacterial flora information for bacteriology
 * tests
 *
 * This entity stores flora count data for microscopy results, with details
 * about each identified flora stored in the related BacteriologyFloraDetail
 * entities.
 */
@Entity
@Table(name = "bacteriology_flora", schema = "clinlims")
public class BacteriologyFlora extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bacteriology_flora_seq")
    @SequenceGenerator(name = "bacteriology_flora_seq", sequenceName = "bacteriology_flora_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "analysis_id", nullable = false)
    private Integer analysisId;

    @Column(name = "flora_count_test_id", nullable = false)
    private Integer floraCountTestId;

    @Column(name = "flora_count", nullable = false, length = 10)
    private String floraCount;

    @OneToMany(mappedBy = "flora", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BacteriologyFloraDetail> details = new ArrayList<>();

    @Column(name = "lastupdated", nullable = false)
    private Timestamp lastUpdated;

    public BacteriologyFlora() {
        super();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Integer getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Integer analysisId) {
        this.analysisId = analysisId;
    }

    public Integer getFloraCountTestId() {
        return floraCountTestId;
    }

    public void setFloraCountTestId(Integer floraCountTestId) {
        this.floraCountTestId = floraCountTestId;
    }

    public String getFloraCount() {
        return floraCount;
    }

    public void setFloraCount(String floraCount) {
        this.floraCount = floraCount;
    }

    public List<BacteriologyFloraDetail> getDetails() {
        return details;
    }

    public void setDetails(List<BacteriologyFloraDetail> details) {
        this.details = details;
        // Ensure bidirectional relationship
        if (details != null) {
            details.forEach(detail -> detail.setFlora(this));
        }
    }

    public void addDetail(BacteriologyFloraDetail detail) {
        details.add(detail);
        detail.setFlora(this);
    }

    public void removeDetail(BacteriologyFloraDetail detail) {
        details.remove(detail);
        detail.setFlora(null);
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
