package org.openelisglobal.sample.form;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import org.openelisglobal.common.validator.ValidationHelper;

public class TbSampleTest {
	
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String tbSpecimenNature;
    
	@Pattern(regexp = ValidationHelper.ID_REGEX)
    private String sampleId;
	
	@Pattern(regexp = ValidationHelper.ID_REGEX)
    private String sampleItemId;
	
	private String sysUserId;
	
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String tbAspect;
    
    @NotEmpty()
    private List<String> newSelectedTests;
    
    private List<String> selectedTests;
    
    @NotBlank()
    private String selectedTbMethod;
    
    private Integer order = 1;

	public String getTbSpecimenNature() {
		return tbSpecimenNature;
	}

	public void setTbSpecimenNature(String tbSpecimenNature) {
		this.tbSpecimenNature = tbSpecimenNature;
	}

	public String getTbAspect() {
		return tbAspect;
	}

	public void setTbAspect(String tbAspect) {
		this.tbAspect = tbAspect;
	}

	public List<String> getNewSelectedTests() {
		return newSelectedTests;
	}

	public void setNewSelectedTests(List<String> newSelectedTests) {
		this.newSelectedTests = newSelectedTests;
	}

	public List<String> getSelectedTests() {
		return selectedTests;
	}

	public void setSelectedTests(List<String> selectedTests) {
		this.selectedTests = selectedTests;
	}

	public String getSelectedTbMethod() {
		return selectedTbMethod;
	}

	public void setSelectedTbMethod(String selectedTbMethod) {
		this.selectedTbMethod = selectedTbMethod;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public String getSysUserId() {
		return sysUserId;
	}

	public void setSysUserId(String sysUserId) {
		this.sysUserId = sysUserId;
	}

	public String getSampleId() {
		return sampleId;
	}

	public void setSampleId(String sampleId) {
		this.sampleId = sampleId;
	}

	public String getSampleItemId() {
		return sampleItemId;
	}

	public void setSampleItemId(String sampleItemId) {
		this.sampleItemId = sampleItemId;
	}
	
}
