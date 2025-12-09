package org.openelisglobal.patient.action.bean;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.validation.annotations.SafeHtml;

public class PatientRoutineBacterioInfo {

    @NotNull
    private Boolean currentHospitalization;

    // Numéro de chambre : alphanumérique simple
    @Pattern(regexp = ValidationHelper.ALPHA_NUM_REGEX)
    @Size(max = 20)
    private String roomNumber;

    // Renseignements cliniques (IDs du dictionnaire) : au moins 0, liste non nulle
    @NotNull
    private List<Integer> clinicalInformations = new ArrayList<>();

    // Autres renseignements cliniques : texte libre mais limité en taille
    @Size(max = 255)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String clinicalInformationOther;

    // ATB dans les 3 derniers mois ? (Oui/Non -> Boolean)
    @NotNull
    private Boolean recentAntibiotherapy;

    // Liste des ATB (dictionnaire) dans les 3 derniers mois
    @NotNull
    private List<Integer> recentAntibiotherapyList = new ArrayList<>();

    // ATB en cours ? (Oui/Non -> Boolean)
    @NotNull
    private Boolean currentAntibiotherapy;

    // Liste des ATB de l'antibiothérapie en cours
    @NotNull
    private List<Integer> currentAntibiotherapyList = new ArrayList<>();

    // Durée de l'ATB en cours (en jours) : 0–365 par exemple
    @Min(0)
    @Max(365)
    private Integer currentAntibiotherapyDuration;

    // Hospitalisation récente (3 derniers mois) ? (Oui/Non)
    @NotNull
    private Boolean recentHospitalization;

    @Min(0)
    @Max(100)
    private Integer recentHospitalizationCount;

    // Gestes invasifs récents (IDs du dictionnaire)
    @NotNull
    private List<Integer> recentInvasiveGestures = new ArrayList<>();

    // Dispositifs à demeure (IDs du dictionnaire)
    @NotNull
    private List<Integer> indwellingDevice = new ArrayList<>();

    public Boolean getCurrentHospitalization() {
        return currentHospitalization;
    }

    public void setCurrentHospitalization(Boolean currentHospitalization) {
        this.currentHospitalization = currentHospitalization;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public List<Integer> getClinicalInformations() {
        return clinicalInformations;
    }

    public void setClinicalInformations(List<Integer> clinicalInformations) {
        this.clinicalInformations = clinicalInformations;
    }

    public String getClinicalInformationOther() {
        return clinicalInformationOther;
    }

    public void setClinicalInformationOther(String clinicalInformationOther) {
        this.clinicalInformationOther = clinicalInformationOther;
    }

    public Boolean getRecentAntibiotherapy() {
        return recentAntibiotherapy;
    }

    public void setRecentAntibiotherapy(Boolean recentAntibiotherapy) {
        this.recentAntibiotherapy = recentAntibiotherapy;
    }

    public List<Integer> getRecentAntibiotherapyList() {
        return recentAntibiotherapyList;
    }

    public void setRecentAntibiotherapyList(List<Integer> recentAntibiotherapyList) {
        this.recentAntibiotherapyList = recentAntibiotherapyList;
    }

    public Boolean getCurrentAntibiotherapy() {
        return currentAntibiotherapy;
    }

    public void setCurrentAntibiotherapy(Boolean currentAntibiotherapy) {
        this.currentAntibiotherapy = currentAntibiotherapy;
    }

    public List<Integer> getCurrentAntibiotherapyList() {
        return currentAntibiotherapyList;
    }

    public void setCurrentAntibiotherapyList(List<Integer> currentAntibiotherapyList) {
        this.currentAntibiotherapyList = currentAntibiotherapyList;
    }

    public Integer getCurrentAntibiotherapyDuration() {
        return currentAntibiotherapyDuration;
    }

    public void setCurrentAntibiotherapyDuration(Integer currentAntibiotherapyDuration) {
        this.currentAntibiotherapyDuration = currentAntibiotherapyDuration;
    }

    public Boolean getRecentHospitalization() {
        return recentHospitalization;
    }

    public void setRecentHospitalization(Boolean recentHospitalization) {
        this.recentHospitalization = recentHospitalization;
    }

    public Integer getRecentHospitalizationCount() {
        return recentHospitalizationCount;
    }

    public void setRecentHospitalizationCount(Integer recentHospitalizationCount) {
        this.recentHospitalizationCount = recentHospitalizationCount;
    }

    public List<Integer> getRecentInvasiveGestures() {
        return recentInvasiveGestures;
    }

    public void setRecentInvasiveGestures(List<Integer> recentInvasiveGestures) {
        this.recentInvasiveGestures = recentInvasiveGestures;
    }

    public List<Integer> getIndwellingDevice() {
        return indwellingDevice;
    }

    public void setIndwellingDevice(List<Integer> indwellingDevice) {
        this.indwellingDevice = indwellingDevice;
    }

}
