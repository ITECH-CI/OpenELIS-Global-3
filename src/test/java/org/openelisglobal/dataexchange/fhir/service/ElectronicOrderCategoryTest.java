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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.openelisglobal.dataexchange.order.controller.ElectronicOrderFilters;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderCategorie;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderOrigine;

/** Catégorie et origine d'une demande électronique reçue. */
public class ElectronicOrderCategoryTest {

    private static final String EXTENSION = "https://exemple.test/categorie-demande";

    private static Task task(String id) {
        Task task = new Task();
        task.setId(id);
        return task;
    }

    private static ElectronicOrder classify(Task task, ServiceRequest serviceRequest, String extensionUrl) {
        ElectronicOrder eOrder = new ElectronicOrder();
        TaskWorker.classify(eOrder, task, serviceRequest, "bdms-", extensionUrl);
        return eOrder;
    }

    @Test
    public void platformTag_shouldBePlatformRoutine() {
        Task task = task("4a1e0c55-0000-4000-8000-000000000001");
        task.getMeta().addTag(new Coding(TaskWorker.ORIGINE_TAG_SYSTEM, "plateforme", null));

        ElectronicOrder eOrder = classify(task, null, "");

        assertEquals(ElectronicOrderOrigine.PLATEFORME, eOrder.getOrigine());
        assertEquals(ElectronicOrderCategorie.ROUTINE, eOrder.getCategorie());
        assertTrue(eOrder.isRoutine());
    }

    @Test
    public void platformIdPrefix_shouldBePlatformRoutine() {
        ElectronicOrder eOrder = classify(task("bdms-1"), null, "");

        assertEquals(ElectronicOrderOrigine.PLATEFORME, eOrder.getOrigine());
        assertEquals(ElectronicOrderCategorie.ROUTINE, eOrder.getCategorie());
    }

    @Test
    public void sigdepTask_shouldBeSigdepStudy() {
        ElectronicOrder eOrder = classify(task("3f2b9a10-0000-4000-8000-000000000002"), null, "");

        assertEquals(ElectronicOrderOrigine.SIGDEP, eOrder.getOrigine());
        assertEquals(ElectronicOrderCategorie.ETUDE_CV, eOrder.getCategorie());
        assertFalse(eOrder.isRoutine());
    }

    @Test
    public void otherOriginTag_shouldNotBePlatform() {
        Task task = task("3f2b9a10-0000-4000-8000-000000000003");
        task.getMeta().addTag(new Coding(TaskWorker.ORIGINE_TAG_SYSTEM, "sigdep", null));

        assertEquals(ElectronicOrderOrigine.SIGDEP, classify(task, null, "").getOrigine());
    }

    @Test
    public void explicitCategory_shouldBeReadWhenConfigured() {
        Task task = task("bdms-2");
        task.addExtension(EXTENSION, new CodeType("ETUDE_CV"));

        assertEquals(ElectronicOrderCategorie.ETUDE_CV, classify(task, null, EXTENSION).getCategorie());
        // extension non réglée : ignorée, la demande de la plateforme reste de la
        // routine
        assertEquals(ElectronicOrderCategorie.ROUTINE, classify(task, null, "").getCategorie());
    }

    @Test
    public void explicitCategory_onServiceRequest_shouldBeRead() {
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.addExtension(EXTENSION, new Coding("https://exemple.test", "etude-cv", null));

        assertEquals(ElectronicOrderCategorie.ETUDE_CV,
                classify(task("bdms-3"), serviceRequest, EXTENSION).getCategorie());
    }

    @Test
    public void unknownExplicitCategory_shouldFallBackToRoutine() {
        Task task = task("bdms-4");
        task.addExtension(EXTENSION, new CodeType("INCONNUE"));

        assertEquals(ElectronicOrderCategorie.ROUTINE, classify(task, null, EXTENSION).getCategorie());
        assertNull(ElectronicOrderCategorie.fromCode(null));
    }

    @Test
    public void screens_shouldShowTheirOwnCategory() {
        ElectronicOrder routine = new ElectronicOrder();
        routine.setCategorie(ElectronicOrderCategorie.ROUTINE);
        ElectronicOrder study = new ElectronicOrder();
        study.setCategorie(ElectronicOrderCategorie.ETUDE_CV);
        ElectronicOrder legacy = new ElectronicOrder();
        List<ElectronicOrder> all = Arrays.asList(routine, study, legacy);

        assertEquals(Arrays.asList(routine), ElectronicOrderFilters.routine(all));
        assertEquals(Arrays.asList(study, legacy), ElectronicOrderFilters.study(all));
    }
}
