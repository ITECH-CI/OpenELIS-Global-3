package org.openelisglobal.patient.service;

import java.util.List;

/**
 * Recherche des patients par numéro CMU, pour reprendre un patient existant au
 * lieu d'en créer un second (demandes électroniques).
 */
public interface PatientCmuLookupService {

    /**
     * Identifiants internes des patients dont le numéro CMU (type d'identité "CMU"
     * ou colonne national_id), une fois normalisé, vaut {@code normalizedCmu}. Du
     * plus ancien au plus récent ; liste vide si aucun.
     */
    List<String> findPatientIdsByCmu(String normalizedCmu);
}
