package org.openelisglobal.dataexchange.fhir;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.openelisglobal.patient.util.CmuNumber;

/**
 * Numéro CMU d'un patient FHIR reçu (demande électronique, recherche de
 * patient) : premier identifiant dont le système est accepté
 * ({@code org.openelisglobal.cmu.identifier.systems.accepted}) et dont la
 * valeur, normalisée, est conforme. Une valeur non conforme n'est jamais
 * retenue.
 */
public final class CmuIdentifiers {

    private CmuIdentifiers() {
    }

    public static String incomingCmu(Patient fhirPatient, FhirConfig fhirConfig) {
        if (fhirPatient == null || fhirConfig == null) {
            return null;
        }
        Set<String> accepted = acceptedSystems(fhirConfig);
        for (Identifier identifier : fhirPatient.getIdentifier()) {
            if (identifier.hasSystem() && accepted.contains(identifier.getSystem().trim())) {
                String cmu = CmuNumber.conformingOrNull(identifier.getValue(), fhirConfig.getCmuFormat());
                if (cmu != null) {
                    return cmu;
                }
            }
        }
        return null;
    }

    public static Set<String> acceptedSystems(FhirConfig fhirConfig) {
        String[] systems = fhirConfig.getCmuAcceptedSystems();
        if (systems == null) {
            return Collections.emptySet();
        }
        return Arrays.stream(systems).filter(s -> s != null && !s.trim().isEmpty()).map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
