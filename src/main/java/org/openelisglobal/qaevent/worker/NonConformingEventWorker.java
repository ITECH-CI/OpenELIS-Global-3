package org.openelisglobal.qaevent.worker;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.openelisglobal.qaevent.form.NonConformingEventForm;
import org.openelisglobal.qaevent.valueholder.NcEvent;

public interface NonConformingEventWorker {

    /** Crée l'événement et lui attribue son numéro d'ENC. */
    NcEvent create(String labOrderId, List<String> specimens, String systemUserId);

    boolean update(NonConformingEventForm form);

    boolean updateFollowUp(NonConformingEventForm form);

    void initFormForFollowUp(String nceNumber, NonConformingEventForm form)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException;

    void initFormForCorrectiveAction(String nceNumber, NonConformingEventForm form)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException;

    boolean updateCorrectiveAction(NonConformingEventForm form);

    boolean resolveNCEvent(NonConformingEventForm form);
}
