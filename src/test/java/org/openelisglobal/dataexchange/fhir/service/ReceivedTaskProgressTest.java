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
import static org.junit.Assert.assertTrue;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.junit.Test;
import org.openelisglobal.dataexchange.fhir.service.ReceivedTaskProgress.Etape;

/**
 * Étapes d'une demande reçue reportées sur sa Task : status et businessStatus,
 * sans recul ni remplacement d'un état terminal.
 */
public class ReceivedTaskProgressTest {

    private static Task receivedAccepted() {
        Task received = new Task();
        received.setId("bdms-t1");
        received.setStatus(TaskStatus.ACCEPTED);
        received.setBusinessStatus(ReceivedTaskProgress.businessStatus(Etape.ACCEPTEE));
        return received;
    }

    private static Task orderTask(TaskStatus status) {
        Task order = new Task();
        order.setId("bon-1");
        order.setStatus(status);
        return order;
    }

    private static Task finishedOrderTask() {
        Task order = orderTask(TaskStatus.COMPLETED);
        order.addOutput().setType(new CodeableConcept().setText("compte rendu"))
                .setValue(new Reference("DiagnosticReport/dr-1"));
        return order;
    }

    private static String step(Task received) {
        Coding coding = received.getBusinessStatus().getCodingFirstRep();
        assertEquals(ReceivedTaskProgress.BUSINESS_STATUS_SYSTEM, coding.getSystem());
        return coding.getCode();
    }

    private final FhirTransformServiceImpl transformService = new FhirTransformServiceImpl();

    @Test
    public void fullSequence_shouldGoThroughEveryStep() {
        Task received = receivedAccepted();
        assertEquals("acceptee", step(received));

        assertTrue(transformService.updateReferringTaskWithTaskInfo(received, orderTask(TaskStatus.READY)));
        assertEquals(TaskStatus.INPROGRESS, received.getStatus());
        assertEquals("echantillon-recu", step(received));

        assertTrue(transformService.updateReferringTaskWithTaskInfo(received, orderTask(TaskStatus.INPROGRESS)));
        assertEquals(TaskStatus.INPROGRESS, received.getStatus());
        assertEquals("en-analyse", step(received));

        assertTrue(transformService.updateReferringTaskWithTaskInfo(received, finishedOrderTask()));
        assertEquals(TaskStatus.COMPLETED, received.getStatus());
        assertEquals("resultat-disponible", step(received));
        assertEquals(1, received.getOutput().size());
        assertEquals("DiagnosticReport/dr-1", ((Reference) received.getOutputFirstRep().getValue()).getReference());
    }

    @Test
    public void earlierEventAfterCompletion_shouldLeaveTaskCompleted() {
        Task received = receivedAccepted();
        transformService.updateReferringTaskWithTaskInfo(received, finishedOrderTask());

        assertFalse(transformService.updateReferringTaskWithTaskInfo(received, orderTask(TaskStatus.READY)));
        assertFalse(transformService.updateReferringTaskWithTaskInfo(received, orderTask(TaskStatus.INPROGRESS)));
        assertFalse(transformService.updateReferringTaskWithTaskInfo(received, finishedOrderTask()));

        assertEquals(TaskStatus.COMPLETED, received.getStatus());
        assertEquals("resultat-disponible", step(received));
        assertEquals(1, received.getOutput().size());
    }

    @Test
    public void stepNeverGoesBack() {
        Task received = receivedAccepted();
        transformService.updateReferringTaskWithTaskInfo(received, orderTask(TaskStatus.INPROGRESS));

        assertFalse(transformService.updateReferringTaskWithTaskInfo(received, orderTask(TaskStatus.READY)));
        assertEquals("en-analyse", step(received));
    }

    @Test
    public void rejectedSample_shouldBeRefuseeWithReason() {
        Task received = receivedAccepted();
        Task order = orderTask(TaskStatus.REJECTED);
        order.setStatusReason(new CodeableConcept().setText("échantillon hémolysé"));

        assertTrue(transformService.updateReferringTaskWithTaskInfo(received, order));

        assertEquals(TaskStatus.REJECTED, received.getStatus());
        assertEquals("refusee", step(received));
        assertEquals("échantillon hémolysé", received.getStatusReason().getText());
        assertFalse(transformService.updateReferringTaskWithTaskInfo(received, finishedOrderTask()));
    }

    @Test
    public void cancelledOrder_shouldBeAnnulee() {
        Task received = receivedAccepted();

        assertTrue(transformService.updateReferringTaskWithTaskInfo(received, orderTask(TaskStatus.CANCELLED)));

        assertEquals(TaskStatus.CANCELLED, received.getStatus());
        assertEquals("annulee", step(received));
    }

    @Test
    public void receivedTaskWithoutBusinessStatus_shouldStillProgress() {
        Task received = new Task();
        received.setStatus(TaskStatus.ACCEPTED);

        assertTrue(ReceivedTaskProgress.applyOrderTask(received, orderTask(TaskStatus.READY)));
        assertEquals("echantillon-recu", step(received));
    }

    @Test
    public void orderTaskWithoutStep_shouldChangeNothing() {
        Task received = receivedAccepted();

        assertFalse(transformService.updateReferringTaskWithTaskInfo(received, orderTask(TaskStatus.NULL)));
        assertFalse(transformService.updateReferringTaskWithTaskInfo(received, new Task()));
        assertEquals("acceptee", step(received));
    }
}
