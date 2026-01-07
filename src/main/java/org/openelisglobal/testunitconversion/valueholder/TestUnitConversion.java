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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.testunitconversion.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;

/**
 * Entity representing a unit conversion rule for a test. Converts test results
 * from traditional units to SI (International System) units. Uses linear
 * conversion formula: valueSi = (value × factor) + offsetValue
 */
@Entity
@Table(name = "test_unit_conversion", schema = "clinlims")
@SequenceGenerator(name = "test_unit_conversion_seq", sequenceName = "test_unit_conversion_seq", allocationSize = 1)
public class TestUnitConversion extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_unit_conversion_seq")
    @Column(name = "id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @ManyToOne
    @JoinColumn(name = "from_uom_id", nullable = false)
    private UnitOfMeasure fromUom;

    @ManyToOne
    @JoinColumn(name = "to_uom_id", nullable = false)
    private UnitOfMeasure toUom;

    @Column(name = "factor", precision = 18, scale = 8)
    private BigDecimal factor = BigDecimal.ONE;

    @Column(name = "offset_value", precision = 18, scale = 8)
    private BigDecimal offsetValue = BigDecimal.ZERO;

    @Column(name = "decimals")
    private Integer decimals;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "lastupdated")
    private Timestamp lastupdated;

    public TestUnitConversion() {
        super();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public UnitOfMeasure getFromUom() {
        return fromUom;
    }

    public void setFromUom(UnitOfMeasure fromUom) {
        this.fromUom = fromUom;
    }

    public UnitOfMeasure getToUom() {
        return toUom;
    }

    public void setToUom(UnitOfMeasure toUom) {
        this.toUom = toUom;
    }

    public BigDecimal getFactor() {
        return factor;
    }

    public void setFactor(BigDecimal factor) {
        this.factor = factor;
    }

    public BigDecimal getOffsetValue() {
        return offsetValue;
    }

    public void setOffsetValue(BigDecimal offsetValue) {
        this.offsetValue = offsetValue;
    }

    public Integer getDecimals() {
        return decimals;
    }

    public void setDecimals(Integer decimals) {
        this.decimals = decimals;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Timestamp getLastupdated() {
        return lastupdated;
    }

    public void setLastupdated(Timestamp lastupdated) {
        this.lastupdated = lastupdated;
    }

    /**
     * Converts a value from the traditional unit to SI unit using this rule.
     * Formula: valueSi = (value × factor) + offsetValue
     *
     * @param value the value in traditional units
     * @return the value in SI units, rounded to specified decimals
     */
    public BigDecimal convertToSi(BigDecimal value) {
        if (value == null) {
            return null;
        }

        BigDecimal siValue = value.multiply(factor).add(offsetValue);

        if (decimals != null && decimals >= 0) {
            siValue = siValue.setScale(decimals, BigDecimal.ROUND_HALF_UP);
        }

        return siValue;
    }
}
