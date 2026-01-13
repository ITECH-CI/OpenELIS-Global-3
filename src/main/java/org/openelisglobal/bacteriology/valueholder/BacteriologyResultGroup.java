package org.openelisglobal.bacteriology.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

public class BacteriologyResultGroup extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;
	private Integer id;
    private Integer analysisId;
    private String groupType; // MACROSCOPY, MICROSCOPY, CULTURE, ORGANISM, ANTIBIOGRAM
    private Integer groupNumber; // For ORGANISM (1, 2, 3) or ANTIBIOGRAM (1, 2, 3)
    private Integer parentGroupId;
    private Integer displayOrder;
    private Boolean isActive = true;
    private Timestamp createdDate;
    private Timestamp lastupdated;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Integer analysisId) {
        this.analysisId = analysisId;
    }

    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    public Integer getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(Integer groupNumber) {
        this.groupNumber = groupNumber;
    }

    public Integer getParentGroupId() {
        return parentGroupId;
    }

    public void setParentGroupId(Integer parentGroupId) {
        this.parentGroupId = parentGroupId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public Timestamp getLastupdated() {
        return lastupdated;
    }

    public void setLastupdated(Timestamp lastupdated) {
        this.lastupdated = lastupdated;
    }
}
