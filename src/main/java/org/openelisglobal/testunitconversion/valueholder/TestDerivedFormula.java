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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;

/**
 * Entity representing a derived formula for calculating SI values. Used for
 * complex tests whose SI value is calculated from multiple source tests using a
 * mathematical expression (e.g., "Hb x 100 / HCT" for MCHC).
 */
@Entity
@Table(name = "test_derived_formula", schema = "clinlims")
@SequenceGenerator(name = "test_derived_formula_seq", sequenceName = "test_derived_formula_seq", allocationSize = 1)
public class TestDerivedFormula extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_derived_formula_seq")
    @Column(name = "id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(name = "expression", nullable = false)
    private String expression;

    @ManyToOne
    @JoinColumn(name = "from_uom_id")
    private UnitOfMeasure fromUom;

    @ManyToOne
    @JoinColumn(name = "to_uom_si_id")
    private UnitOfMeasure toUomSi;

    @Column(name = "decimals")
    private Integer decimals;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "lastupdated")
    private Timestamp lastupdated;

    @OneToMany(mappedBy = "derivedFormula", fetch = FetchType.LAZY)
    private List<TestDerivedDependency> dependencies = new ArrayList<>();

    public TestDerivedFormula() {
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

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public UnitOfMeasure getFromUom() {
        return fromUom;
    }

    public void setFromUom(UnitOfMeasure fromUom) {
        this.fromUom = fromUom;
    }

    public UnitOfMeasure getToUomSi() {
        return toUomSi;
    }

    public void setToUomSi(UnitOfMeasure toUomSi) {
        this.toUomSi = toUomSi;
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

    public List<TestDerivedDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<TestDerivedDependency> dependencies) {
        this.dependencies = dependencies;
    }
}
