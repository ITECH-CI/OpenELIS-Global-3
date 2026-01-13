package org.openelisglobal.bacteriology.action.bean;

/**
 * DTO for antibiotic dictionary entry
 */
public class AntibioticDTO {

    private Integer id;
    private String name;
    private String localAbbreviation;
    private Integer sortOrder;

    public AntibioticDTO() {
    }

    public AntibioticDTO(Integer id, String name, String localAbbreviation, Integer sortOrder) {
        this.id = id;
        this.name = name;
        this.localAbbreviation = localAbbreviation;
        this.sortOrder = sortOrder;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocalAbbreviation() {
        return localAbbreviation;
    }

    public void setLocalAbbreviation(String localAbbreviation) {
        this.localAbbreviation = localAbbreviation;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
