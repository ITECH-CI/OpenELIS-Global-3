package org.openelisglobal.bacteriology.action.bean;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Bean for organism identification data
 */
public class BacteriologyOrganismBean {

    private Integer id;
    private Integer organismGroupId;

    @NotNull
    @Min(1)
    @Max(3)
    private Integer organismNumber;

    @NotNull
    @Pattern(regexp = "^(BACTERIA|YEAST)$", message = "Organism type must be BACTERIA or YEAST")
    private String organismType;

    private Integer organismNameDictId;

    @Size(max = 255)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String organismNameText;

    @Pattern(regexp = "^(POSITIVE|NEGATIVE)?$", message = "Gram type must be POSITIVE or NEGATIVE")
    private String gramType;

    @Size(max = 100)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String groupingMode;

    private Boolean capsulePresence;

    @Size(max = 500)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String otherCharacteristics;

    @Valid
    private List<AntibiogramResultBean> antibiograms = new ArrayList<>();

    public BacteriologyOrganismBean() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrganismGroupId() {
        return organismGroupId;
    }

    public void setOrganismGroupId(Integer organismGroupId) {
        this.organismGroupId = organismGroupId;
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

    public List<AntibiogramResultBean> getAntibiograms() {
        return antibiograms;
    }

    public void setAntibiograms(List<AntibiogramResultBean> antibiograms) {
        this.antibiograms = antibiograms;
    }
}
