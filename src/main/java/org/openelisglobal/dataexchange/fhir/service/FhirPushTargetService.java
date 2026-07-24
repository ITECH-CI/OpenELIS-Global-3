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

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dataexchange.fhir.valueholder.FhirPushTarget;

/**
 * Gestion administrable des cibles de push FHIR distant (mini-HIE). L'écran
 * admin écrit cette table ; le {@code FhirPushEngineServiceImpl} (moteur natif)
 * la lit à chaque tick et pousse le delta du store FHIR local vers chaque cible
 * active. Activer/désactiver/supprimer une cible suffit — le moteur en tient
 * compte au tick suivant, sans réconciliation d'une table tierce.
 */
public interface FhirPushTargetService extends BaseObjectService<FhirPushTarget, String> {

    /** Toutes les cibles déclarées (plus récentes en tête). */
    List<FhirPushTarget> getTargets();

    /** Crée une cible. Renvoie l'entité créée. */
    FhirPushTarget createTarget(FhirPushTarget target);

    /** Met à jour une cible (par id). */
    FhirPushTarget updateTarget(String id, FhirPushTarget changes);

    /** Active/désactive une cible (le moteur cesse/reprend de la pousser). */
    void setTargetActive(String id, boolean active);

    /** Supprime une cible (le moteur cesse de la pousser). */
    void deleteTarget(String id);

    /** Cibles actives à pousser par le moteur natif (isActive="Y"). */
    List<FhirPushTarget> getActiveTargets();

    /**
     * Enregistre une TENTATIVE de push (met {@code lastAttempt=now}) dans sa propre
     * transaction, avant l'appel réseau — pour que la supervision reflète le tick
     * même si le POST échoue ensuite.
     */
    void markPushAttempt(String id);

    /**
     * Enregistre un push RÉUSSI : avance le point de reprise
     * ({@code lastPushed=checkpoint}) et met {@code lastSuccess=now}, dans sa
     * propre transaction. Le {@code checkpoint} est la borne haute de la fenêtre
     * poussée.
     */
    void markPushSuccess(String id, java.sql.Timestamp checkpoint);
}
