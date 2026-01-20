package org.openelisglobal.bacteriology.action.bean;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Bean for antibiogram result data
 */
public class AntibiogramResultBean {

    private Integer id;

    @NotNull(message = "Antibiotic must be selected")
    private Integer antibioticDictId;

    // Result can be empty during entry, but if provided must be S, I, or R
    @Pattern(regexp = "^[SIR]?$", message = "Result must be S (Sensible), I (Intermédiaire), or R (Résistant)")
    private String result;

    private BigDecimal diameterMm;

    @Size(max = 50)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String micValue;

    @Size(max = 1000)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String interpretationComment;

    public AntibiogramResultBean() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
}
