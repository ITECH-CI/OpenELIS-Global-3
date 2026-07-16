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
package org.openelisglobal.dataexchange.sync.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Curseur de pagination du pull des demandes VL (incrément 4c) : lit/écrit
 * l'unique ligne {@code eorder_pull_checkpoint (id=1)}. Le checkpoint est une
 * chaîne opaque {@code "<updatedAt>|<id>"} fournie par le serveur consolidé —
 * on ne l'interprète pas, on la renvoie telle quelle en {@code ?since=}.
 *
 * <p>
 * {@code REQUIRES_NEW} sur l'écriture : l'avancement du curseur est commité
 * indépendamment de l'import des demandes (comme l'uploader — le checkpoint
 * n'avance qu'après traitement d'une page).
 */
@Service
public class EorderPullCheckpointService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Checkpoint courant (chaîne vide si jamais initialisé -> pull depuis le
     * début).
     */
    @Transactional(readOnly = true)
    public String load() {
        try {
            Query<?> q = entityManager.unwrap(Session.class)
                    .createNativeQuery("SELECT checkpoint FROM clinlims.eorder_pull_checkpoint WHERE id = 1");
            Object v = q.uniqueResult();
            return v == null ? "" : v.toString();
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "load", "lecture checkpoint échouée : " + e);
            return "";
        }
    }

    /** Avance le curseur (upsert de la ligne unique). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(String checkpoint) {
        try {
            Query<?> q = entityManager.unwrap(Session.class).createNativeQuery(
                    "INSERT INTO clinlims.eorder_pull_checkpoint (id, checkpoint, updated_at) VALUES (1, :cp, now())"
                            + " ON CONFLICT (id) DO UPDATE SET checkpoint = EXCLUDED.checkpoint, updated_at = now()");
            q.setParameter("cp", checkpoint == null ? "" : checkpoint);
            q.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "save", "écriture checkpoint échouée : " + e);
        }
    }
}
