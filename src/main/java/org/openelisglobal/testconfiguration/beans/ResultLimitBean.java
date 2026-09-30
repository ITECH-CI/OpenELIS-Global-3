/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.testconfiguration.beans;

import org.openelisglobal.common.util.validator.GenericValidator;

public class ResultLimitBean {
    private String gender;
    private String ageRange;
    private String normalRange;
    private String validRange;
    private String reportingRange;
    private String criticalRange;
    // valeurs BRUTES (âges en jours, bornes non arrondies) : les *Range ci-dessus
    // sont des libellés arrondis aux chiffres significatifs, inutilisables pour
    // pré-remplir la modification d'un test (0.7-1.1 affiché « 1-1 »)
    private String minAge;
    private String maxAge;
    private String lowNormal;
    private String highNormal;
    private String lowValid;
    private String highValid;
    private String lowCritical;
    private String highCritical;
    private String lowReportingRange;
    private String highReportingRange;

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = GenericValidator.isBlankOrNull(gender) ? "n/a" : gender;
    }

    public String getAgeRange() {
        return ageRange;
    }

    public void setAgeRange(String ageRange) {
        this.ageRange = ageRange;
    }

    public String getNormalRange() {
        return normalRange;
    }

    public void setNormalRange(String normalRange) {
        this.normalRange = normalRange;
    }

    public String getValidRange() {
        return validRange;
    }

    public void setValidRange(String validRange) {
        this.validRange = validRange;
    }

    public String getReportingRange() {
        return reportingRange;
    }

    public void setReportingRange(String reportingRange) {
        this.reportingRange = reportingRange;
    }

    public String getCriticalRange() {
        return criticalRange;
    }

    public void setCriticalRange(String criticalRange) {
        this.criticalRange = criticalRange;
    }

    public String getMinAge() {
        return minAge;
    }

    public void setMinAge(String minAge) {
        this.minAge = minAge;
    }

    public String getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(String maxAge) {
        this.maxAge = maxAge;
    }

    public String getLowNormal() {
        return lowNormal;
    }

    public void setLowNormal(String lowNormal) {
        this.lowNormal = lowNormal;
    }

    public String getHighNormal() {
        return highNormal;
    }

    public void setHighNormal(String highNormal) {
        this.highNormal = highNormal;
    }

    public String getLowValid() {
        return lowValid;
    }

    public void setLowValid(String lowValid) {
        this.lowValid = lowValid;
    }

    public String getHighValid() {
        return highValid;
    }

    public void setHighValid(String highValid) {
        this.highValid = highValid;
    }

    public String getLowCritical() {
        return lowCritical;
    }

    public void setLowCritical(String lowCritical) {
        this.lowCritical = lowCritical;
    }

    public String getHighCritical() {
        return highCritical;
    }

    public void setHighCritical(String highCritical) {
        this.highCritical = highCritical;
    }

    public String getLowReportingRange() {
        return lowReportingRange;
    }

    public void setLowReportingRange(String lowReportingRange) {
        this.lowReportingRange = lowReportingRange;
    }

    public String getHighReportingRange() {
        return highReportingRange;
    }

    public void setHighReportingRange(String highReportingRange) {
        this.highReportingRange = highReportingRange;
    }
}
