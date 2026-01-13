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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * BacteriologyFloraDetail - Represents detailed information about a single bacterial flora
 *
 * This entity stores the characteristics of each identified flora including:
 * - Gram type (Gram positive/negative, cocci/bacilli, etc.)
 * - Grouping mode (isolated, clusters, chains, etc.)
 * - Capsulation status
 */
@Entity
@Table(name = "bacteriology_flora_detail", schema = "clinlims")
public class BacteriologyFloraDetail extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bacteriology_flora_detail_seq")
    @SequenceGenerator(name = "bacteriology_flora_detail_seq", sequenceName = "bacteriology_flora_detail_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flora_id", nullable = false)
    private BacteriologyFlora flora;

    @Column(name = "flora_number", nullable = false)
    private Integer floraNumber;

    @Column(name = "gram_type_dict_id")
    private Integer gramTypeDictId;

    @Column(name = "grouping_mode_dict_id")
    private Integer groupingModeDictId;

    @Column(name = "capsulated", nullable = false)
    private Boolean capsulated = false;

    @Column(name = "lastupdated", nullable = false)
    private Timestamp lastUpdated;

    public BacteriologyFloraDetail() {
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

    public BacteriologyFlora getFlora() {
        return flora;
    }

    public void setFlora(BacteriologyFlora flora) {
        this.flora = flora;
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
        this.capsulated = capsulated;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
