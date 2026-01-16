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
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.test.valueholder.Test;

/**
 * Entity representing a dependency relationship between a derived formula and a
 * source test. Links a TestDerivedFormula to the tests whose values are used in
 * the formula expression.
 */
@Entity
@Table(name = "test_derived_dependency", schema = "clinlims")
@SequenceGenerator(name = "test_derived_dependency_seq", sequenceName = "test_derived_dependency_seq", allocationSize = 1)
public class TestDerivedDependency extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_derived_dependency_seq")
    @Column(name = "id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "derived_formula_id", nullable = false)
    private TestDerivedFormula derivedFormula;

    @ManyToOne
    @JoinColumn(name = "source_test_id", nullable = false)
    private Test sourceTest;

    public TestDerivedDependency() {
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

    public TestDerivedFormula getDerivedFormula() {
        return derivedFormula;
    }

    public void setDerivedFormula(TestDerivedFormula derivedFormula) {
        this.derivedFormula = derivedFormula;
    }

    public Test getSourceTest() {
        return sourceTest;
    }

    public void setSourceTest(Test sourceTest) {
        this.sourceTest = sourceTest;
    }
}
