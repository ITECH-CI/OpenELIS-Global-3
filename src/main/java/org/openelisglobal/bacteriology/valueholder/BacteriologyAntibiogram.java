package org.openelisglobal.bacteriology.valueholder;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

public class BacteriologyAntibiogram extends BaseObject<Integer> {

    private Integer id;
    private Integer organismId;
    private Integer antibioticDictId;
    private String result; // S (Sensible), I (Intermédiaire), R (Résistant)
    private BigDecimal diameterMm;
    private String micValue;
    private String interpretationComment;
    private Boolean isActive = true;
    private Timestamp lastupdated;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrganismId() {
        return organismId;
    }

    public void setOrganismId(Integer organismId) {
        this.organismId = organismId;
    }

    public Integer getAntibioticDictId() {
        return antibioticDictId;
    }

    public void setAntibioticDictId(Integer antibioticDictId) {
        this.antibioticDictId = antibioticDictId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public BigDecimal getDiameterMm() {
        return diameterMm;
    }

    public void setDiameterMm(BigDecimal diameterMm) {
        this.diameterMm = diameterMm;
    }

    public String getMicValue() {
        return micValue;
    }

    public void setMicValue(String micValue) {
        this.micValue = micValue;
    }

    public String getInterpretationComment() {
        return interpretationComment;
    }

    public void setInterpretationComment(String interpretationComment) {
        this.interpretationComment = interpretationComment;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Timestamp getLastupdated() {
        return lastupdated;
    }

    public void setLastupdated(Timestamp lastupdated) {
        this.lastupdated = lastupdated;
    }
}
