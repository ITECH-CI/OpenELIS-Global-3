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
package org.openelisglobal.dataexchange.service.order;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.dataexchange.order.dao.ElectronicOrderDAO;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderCategorie;

/**
 * Prise en charge d'une demande à plusieurs examens : les autres demandes
 * électroniques de la même Task sont reprises dans le bon.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ElectronicOrderSiblingTest {

    @Mock
    private ElectronicOrderDAO baseObjectDAO;
    @Mock
    private IStatusService statusService;

    @InjectMocks
    private ElectronicOrderServiceImpl service;

    @Before
    public void setUp() {
        when(statusService.getStatusID(ExternalOrderStatus.Cancelled)).thenReturn("2");
        when(statusService.getStatusID(ExternalOrderStatus.Completed)).thenReturn("3");
        when(statusService.getStatusID(ExternalOrderStatus.NonConforming)).thenReturn("4");
    }

    private static ElectronicOrder order(String id, String taskId, String statusId) {
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setId(id);
        eOrder.setStatusId(statusId);
        eOrder.setCategorie(ElectronicOrderCategorie.ROUTINE);
        eOrder.setData("{\"resourceType\":\"Task\",\"id\":\"" + taskId + "\",\"basedOn\":[{\"id\":\"x\"}]}");
        return eOrder;
    }

    @Test
    public void otherOpenExamsOfTheTask_shouldBeSiblings() {
        ElectronicOrder taken = order("1", "bdms-t1", "1");
        ElectronicOrder open = order("2", "bdms-t1", "1");
        ElectronicOrder closed = order("3", "bdms-t1", "2");
        ElectronicOrder otherTask = order("4", "bdms-t10", "1");
        when(baseObjectDAO.getElectronicOrdersByTaskId(ElectronicOrderCategorie.ROUTINE, "bdms-t1"))
                .thenReturn(Arrays.asList(taken, open, closed, otherTask));

        List<ElectronicOrder> siblings = service.getActiveSiblingElectronicOrders(taken);

        assertEquals(Arrays.asList(open), siblings);
    }

    @Test
    public void studyOrder_shouldHaveNoSibling() {
        ElectronicOrder study = order("1", "5d0c3e1a-0000-4000-8000-000000000001", "1");
        study.setCategorie(ElectronicOrderCategorie.ETUDE_CV);

        assertTrue(service.getActiveSiblingElectronicOrders(study).isEmpty());
    }

    @Test
    public void taskId_shouldBeReadFromData() {
        assertEquals("bdms-t1", service.getTaskId(order("1", "bdms-t1", "1")));
        ElectronicOrder broken = new ElectronicOrder();
        broken.setData("pas du JSON");
        assertNull(service.getTaskId(broken));
    }
}
