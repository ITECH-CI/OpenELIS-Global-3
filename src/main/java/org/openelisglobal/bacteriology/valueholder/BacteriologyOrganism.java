package org.openelisglobal.bacteriology.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

public class BacteriologyOrganism extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;
    private Integer id;
    private Integer resultGroupId;
    private Integer organismNumber; // 1, 2, or 3
    private String organismType; // BACTERIA or YEAST
    private Integer organismNameDictId;
    private String organismNameText;
    private String gramType;
    private String groupingMode;
    private Boolean capsulePresence;
    private String otherCharacteristics;
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

    public Integer getResultGroupId() {
        return resultGroupId;
    }

    public void setResultGroupId(Integer resultGroupId) {
        this.resultGroupId = resultGroupId;
    }

    public Integer getOrganismNumber() {
        return organismNumber;
    }

    public void setOrganismNumber(Integer organismNumber) {
        this.organismNumber = organismNumber;
    }

    public String getOrganismType() {
        return organismType;
    }

    public void setOrganismType(String organismType) {
        this.organismType = organismType;
    }

    public Integer getOrganismNameDictId() {
        return organismNameDictId;
    }

    public void setOrganismNameDictId(Integer organismNameDictId) {
        this.organismNameDictId = organismNameDictId;
    }

    public String getOrganismNameText() {
        return organismNameText;
    }

    public void setOrganismNameText(String organismNameText) {
        this.organismNameText = organismNameText;
    }

    public String getGramType() {
        return gramType;
    }

    public void setGramType(String gramType) {
        this.gramType = gramType;
    }

    public String getGroupingMode() {
        return groupingMode;
    }

    public void setGroupingMode(String groupingMode) {
        this.groupingMode = groupingMode;
    }

    public Boolean getCapsulePresence() {
        return capsulePresence;
    }

    public void setCapsulePresence(Boolean capsulePresence) {
        this.capsulePresence = capsulePresence;
    }

    public String getOtherCharacteristics() {
        return otherCharacteristics;
    }

    public void setOtherCharacteristics(String otherCharacteristics) {
        this.otherCharacteristics = otherCharacteristics;
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
