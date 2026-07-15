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
 * Gestion administrable des cibles de push FHIR distant (mini-HIE). Chaque
 * cible est projetée dans un {@code DataExportTask} du module
 * {@code dataexport} (le moteur d'export). Toute modification déclenche une
 * synchronisation : upsert des tâches d'export pour les cibles actives,
 * suppression pour les cibles inactives/supprimées.
 */
public interface FhirPushTargetService extends BaseObjectService<FhirPushTarget, String> {

    /** Toutes les cibles déclarées (plus récentes en tête). */
    List<FhirPushTarget> getTargets();

    /**
     * Crée une cible puis synchronise le moteur d'export. Renvoie l'entité créée.
     */
    FhirPushTarget createTarget(FhirPushTarget target);

    /** Met à jour une cible (par id) puis synchronise. */
    FhirPushTarget updateTarget(String id, FhirPushTarget changes);

    /**
     * Active/désactive une cible puis synchronise (désactiver retire la tâche
     * d'export).
     */
    void setTargetActive(String id, boolean active);

    /** Supprime une cible puis synchronise (retire la tâche d'export). */
    void deleteTarget(String id);

    /**
     * Réconcilie l'état des {@code DataExportTask} avec les cibles : upsert pour
     * chaque cible active, suppression de la tâche pour chaque cible inactive.
     * Idempotent ; appelé après chaque modification.
     */
    void syncToDataExport();
}
