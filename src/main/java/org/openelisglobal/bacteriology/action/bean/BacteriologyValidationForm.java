package org.openelisglobal.bacteriology.action.bean;

import java.util.List;

public class BacteriologyValidationForm {

    private Integer analysisId;
    private String sysUserId;
    private ValidatedItems validated;
    private RejectedItems rejected;

    public Integer getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Integer analysisId) {
        this.analysisId = analysisId;
    }

    public String getSysUserId() {
        return sysUserId;
    }

    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    public ValidatedItems getValidated() {
        return validated;
    }

    public void setValidated(ValidatedItems validated) {
        this.validated = validated;
    }

    public RejectedItems getRejected() {
        return rejected;
    }

    public void setRejected(RejectedItems rejected) {
        this.rejected = rejected;
    }

    public static class ValidatedItems {
        private List<String> macroscopy;
        private List<String> microscopy;
        private List<String> culture;
        private List<String> organisms;

        public List<String> getMacroscopy() {
            return macroscopy;
        }

        public void setMacroscopy(List<String> macroscopy) {
            this.macroscopy = macroscopy;
        }

        public List<String> getMicroscopy() {
            return microscopy;
        }

        public void setMicroscopy(List<String> microscopy) {
            this.microscopy = microscopy;
        }

        public List<String> getCulture() {
            return culture;
        }

        public void setCulture(List<String> culture) {
            this.culture = culture;
        }

        public List<String> getOrganisms() {
            return organisms;
        }

        public void setOrganisms(List<String> organisms) {
            this.organisms = organisms;
        }
    }

    public static class RejectedItems {
        private List<String> macroscopy;
        private List<String> microscopy;
        private List<String> culture;
        private List<String> organisms;

        public List<String> getMacroscopy() {
            return macroscopy;
        }

        public void setMacroscopy(List<String> macroscopy) {
            this.macroscopy = macroscopy;
        }

        public List<String> getMicroscopy() {
            return microscopy;
        }

        public void setMicroscopy(List<String> microscopy) {
            this.microscopy = microscopy;
        }

        public List<String> getCulture() {
            return culture;
        }

        public void setCulture(List<String> culture) {
            this.culture = culture;
        }

        public List<String> getOrganisms() {
            return organisms;
        }

        public void setOrganisms(List<String> organisms) {
            this.organisms = organisms;
        }
    }
}
