package org.openelisglobal.qaevent.service;

public interface NceNumberService {

    /**
     * Numéro du prochain événement non conforme, tiré du compteur transactionnel
     * des numéros de laboratoire ; chaque appel consomme un numéro.
     */
    String nextNceNumber();
}
