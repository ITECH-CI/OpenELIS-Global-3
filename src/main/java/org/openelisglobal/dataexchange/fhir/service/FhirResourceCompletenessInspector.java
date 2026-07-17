/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH-CI. All Rights Reserved.
 */
package org.openelisglobal.dataexchange.fhir.service;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;

/**
 * Inspecteur léger de complétude des ressources FHIR produites par la
 * transformation métier→FHIR. Signale les dégradations connues (subject/for
 * absent, {@code dataAbsentReason} posé, code manquant) SANS bloquer la
 * transformation : une ressource dégradée est persistée mais son statut de sync
 * passe en {@code SUCCESS_INCOMPLETE} pour être vérifiée avant
 * exposition/interop.
 *
 * <p>
 * Purement fonctionnel (pas de dépendance Spring) : on inspecte les objets FHIR
 * HAPI déjà en mémoire, on ne relit pas le store.
 */
public final class FhirResourceCompletenessInspector {

    private FhirResourceCompletenessInspector() {
    }

    /**
     * Inspecte un Patient. Renvoie la liste (éventuellement vide) des manques
     * détectés, préfixés par l'id de la ressource.
     */
    public static List<String> inspectPatient(Patient patient) {
        List<String> issues = new ArrayList<>();
        if (patient == null) {
            return issues;
        }
        String ref = "Patient/" + idOf(patient.getIdElement().getIdPart());
        if (!patient.hasName() || patient.getNameFirstRep().isEmpty()) {
            issues.add(ref + " : nom absent");
        }
        if (!patient.hasBirthDate()) {
            issues.add(ref + " : date de naissance absente");
        }
        return issues;
    }

    /** Inspecte un ServiceRequest (subject + code requis pour l'interop). */
    public static List<String> inspectServiceRequest(ServiceRequest sr) {
        List<String> issues = new ArrayList<>();
        if (sr == null) {
            return issues;
        }
        String ref = "ServiceRequest/" + idOf(sr.getIdElement().getIdPart());
        if (!sr.hasSubject()) {
            issues.add(ref + " : subject (patient) absent");
        }
        if (!sr.hasCode() || sr.getCode().isEmpty()) {
            issues.add(ref + " : code absent");
        }
        return issues;
    }

    /**
     * Inspecte une Observation : subject, basedOn, code, et surtout une valeur
     * réelle (une Observation avec {@code dataAbsentReason} est "réussie" mais ne
     * porte pas de résultat exploitable).
     */
    public static List<String> inspectObservation(Observation obs) {
        List<String> issues = new ArrayList<>();
        if (obs == null) {
            return issues;
        }
        String ref = "Observation/" + idOf(obs.getIdElement().getIdPart());
        if (!obs.hasSubject()) {
            issues.add(ref + " : subject (patient) absent");
        }
        if (!obs.hasCode() || obs.getCode().isEmpty()) {
            issues.add(ref + " : code absent");
        }
        if (!obs.hasValue() && obs.hasDataAbsentReason()) {
            issues.add(ref + " : valeur absente (dataAbsentReason)");
        }
        return issues;
    }

    /** Inspecte un DiagnosticReport : subject + au moins un result. */
    public static List<String> inspectDiagnosticReport(DiagnosticReport dr) {
        List<String> issues = new ArrayList<>();
        if (dr == null) {
            return issues;
        }
        String ref = "DiagnosticReport/" + idOf(dr.getIdElement().getIdPart());
        if (!dr.hasSubject()) {
            issues.add(ref + " : subject (patient) absent");
        }
        if (!dr.hasResult()) {
            issues.add(ref + " : aucune Observation liée (result)");
        }
        return issues;
    }

    private static String idOf(String idPart) {
        return idPart == null ? "?" : idPart;
    }
}
