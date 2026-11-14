package org.openelisglobal.dataexchange.fhir.service;

import java.util.Collection;
import org.hl7.fhir.r4.model.Task;

/**
 * Annulation d'une demande électronique par le demandeur (Task de la plateforme
 * passée à cancelled).
 */
public interface RequesterCancellationService {

    enum Outcome {
        /** demande pas encore prise en charge : annulée, hors de la liste à traiter */
        CANCELLED,
        /** demande déjà prise en charge : mention seulement, le laboratoire décide */
        FLAGGED_TAKEN,
        /** déjà traitée, ou aucune demande correspondante : aucun effet */
        NO_EFFECT,
        /** annulation faite par le laboratoire lui-même : ignorée */
        LABORATORY_CANCELLATION
    }

    /**
     * Applique l'annulation aux demandes électroniques des numéros de demande
     * donnés (identifiants des ServiceRequest de la Task). Ne crée ni ne réactive
     * jamais une demande ; rejouer l'appel est sans effet.
     */
    Outcome applyRequesterCancellation(Task cancelledTask, Collection<String> orderNumbers);
}
