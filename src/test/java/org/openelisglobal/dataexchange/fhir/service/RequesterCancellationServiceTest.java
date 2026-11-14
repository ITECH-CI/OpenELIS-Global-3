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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.dataexchange.fhir.service.RequesterCancellationService.Outcome;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * Annulation d'une demande par le demandeur (Task bdms-* passée à cancelled).
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class RequesterCancellationServiceTest {

    private static final String ENTERED = "10";
    private static final String CANCELLED = "20";
    private static final String ORDER_NUMBER = "bdms-sr-1";

    @Mock
    private ElectronicOrderService electronicOrderService;
    @Mock
    private SampleService sampleService;
    @Mock
    private IStatusService statusService;
    @Mock
    private NoteService noteService;

    @InjectMocks
    private RequesterCancellationServiceImpl service;

    private ElectronicOrder eOrder;

    @Before
    public void setUp() {
        when(statusService.getStatusID(ExternalOrderStatus.Cancelled)).thenReturn(CANCELLED);
        when(statusService.getStatusID(ExternalOrderStatus.Completed)).thenReturn("21");
        when(statusService.getStatusID(ExternalOrderStatus.NonConforming)).thenReturn("22");
        eOrder = new ElectronicOrder();
        eOrder.setId("1");
        eOrder.setExternalId(ORDER_NUMBER);
        eOrder.setStatusId(ENTERED);
        when(electronicOrderService.getElectronicOrdersByExternalId(ORDER_NUMBER))
                .thenReturn(new ArrayList<>(Arrays.asList(eOrder)));
    }

    private static Task cancelledTask() {
        Task task = new Task();
        task.setId("bdms-t1");
        task.setStatus(TaskStatus.CANCELLED);
        return task;
    }

    @Test
    public void notTakenYet_shouldBeCancelledAndLeaveToProcessList() {
        assertEquals(Outcome.CANCELLED,
                service.applyRequesterCancellation(cancelledTask(), Arrays.asList(ORDER_NUMBER)));

        assertEquals(CANCELLED, eOrder.getStatusId());
        assertNotNull(eOrder.getRequesterCancelledAt());
        verify(electronicOrderService).update(eOrder);
        verify(noteService, never()).insert(any(Note.class));
    }

    @Test
    public void alreadyTaken_shouldOnlyBeFlagged_withNoteOnTheOrder() {
        Sample sample = mock(Sample.class);
        when(sampleService.getSampleByReferringId(ORDER_NUMBER)).thenReturn(sample);
        Note note = new Note();
        when(noteService.createSavableNote(eq(sample), eq(NoteType.INTERNAL), anyString(), anyString(), anyString()))
                .thenReturn(note);

        assertEquals(Outcome.FLAGGED_TAKEN,
                service.applyRequesterCancellation(cancelledTask(), Arrays.asList(ORDER_NUMBER)));

        assertEquals(ENTERED, eOrder.getStatusId());
        assertNotNull(eOrder.getRequesterCancelledAt());
        verify(noteService).insert(note);
    }

    @Test
    public void cancelledTwice_shouldHaveNoEffectTheSecondTime() {
        service.applyRequesterCancellation(cancelledTask(), Arrays.asList(ORDER_NUMBER));

        assertEquals(Outcome.NO_EFFECT,
                service.applyRequesterCancellation(cancelledTask(), Arrays.asList(ORDER_NUMBER)));
        verify(electronicOrderService, times(1)).update(eOrder);
    }

    @Test
    public void unknownOrder_shouldNeverCreateAnything() {
        when(electronicOrderService.getElectronicOrdersByExternalId("bdms-sr-inconnue"))
                .thenReturn(Collections.emptyList());

        assertEquals(Outcome.NO_EFFECT,
                service.applyRequesterCancellation(cancelledTask(), Arrays.asList("bdms-sr-inconnue")));
        verify(electronicOrderService, never()).insert(any());
        verify(electronicOrderService, never()).update(any());
    }

    @Test
    public void orderAlreadyClosedByLaboratory_shouldNotBeReactivatedNorChanged() {
        eOrder.setStatusId("22"); // refusée par le laboratoire

        assertEquals(Outcome.NO_EFFECT,
                service.applyRequesterCancellation(cancelledTask(), Arrays.asList(ORDER_NUMBER)));
        assertEquals("22", eOrder.getStatusId());
        assertNull(eOrder.getRequesterCancelledAt());
    }

    @Test
    public void cancellationWrittenByTheLaboratory_shouldBeIgnored() {
        Task task = cancelledTask();
        task.setBusinessStatus(ReceivedTaskProgress.businessStatus(ReceivedTaskProgress.Etape.ANNULEE));

        assertEquals(Outcome.LABORATORY_CANCELLATION,
                service.applyRequesterCancellation(task, Arrays.asList(ORDER_NUMBER)));
        verify(electronicOrderService, never()).update(any());
    }
}
