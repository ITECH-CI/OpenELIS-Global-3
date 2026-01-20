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
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.test.valueholder.Test;

/**
 * Entity representing an alias for a test. Used to resolve test tokens in
 * derived formulas to the actual test. For example, "Hb" might be an alias for
 * the "Hemoglobin" test.
 */
@Entity
@Table(name = "test_alias", schema = "clinlims")
@SequenceGenerator(name = "test_alias_seq", sequenceName = "test_alias_seq", allocationSize = 1)
public class TestAlias extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_alias_seq")
    @Column(name = "id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(name = "alias", nullable = false)
    private String alias;

    @Column(name = "lastupdated")
    private Timestamp lastupdated;

    public TestAlias() {
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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Timestamp getLastupdated() {
        return lastupdated;
    }

    public void setLastupdated(Timestamp lastupdated) {
        this.lastupdated = lastupdated;
    }
}
