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

import jakarta.persistence.AttributeOverride;
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
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * BacteriologyFloraDetail - Represents detailed information about a single
 * bacterial flora.
 *
 * Stores the characteristics of each identified flora: Gram type, grouping mode
 * and an "other characteristic" dictionary value (e.g. Capsulé / Non Capsulé,
 * category "Bacteriology Capsule").
 */
@Entity
@Table(name = "bacteriology_flora_detail", schema = "clinlims")
// Override the parent BaseObject mapping: the actual column in this table is
// "lastupdated" (no underscore), unlike the convention used elsewhere.
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
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

    @Column(name = "other_characteristic_dict_id")
    private Integer otherCharacteristicDictId;

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

    public Integer getOtherCharacteristicDictId() {
        return otherCharacteristicDictId;
    }

    public void setOtherCharacteristicDictId(Integer otherCharacteristicDictId) {
        this.otherCharacteristicDictId = otherCharacteristicDictId;
    }
}
