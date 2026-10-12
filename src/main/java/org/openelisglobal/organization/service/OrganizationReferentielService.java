package org.openelisglobal.organization.service;

import java.util.List;
import java.util.Map;

/**
 * Réconciliation des organisations avec le référentiel national des
 * établissements (DHIS2 / MFL) : recherche, aperçu et exécution d'une fusion,
 * annulation. La logique est dans les fonctions SQL du référentiel
 * (liquibase/3.2.x.x/sql/referentiel_etablissements.sql).
 */
public interface OrganizationReferentielService {

    /**
     * Organisations (non fusionnées) dont le code commence par {@code terme} ou
     * dont le nom le contient ; établissements, régions et districts du référentiel
     * en tête. {@code niveau} : REGION, DISTRICT, ETABLISSEMENT ou null.
     */
    List<Map<String, Object>> rechercher(String terme, String niveau);

    /**
     * Aperçu d'une fusion entre deux organisations : leur état, l'historique qui
     * les cite (par nature de donnée), l'organisation qui survivrait et les
     * éventuels empêchements.
     */
    Map<String, Object> apercu(String organisationA, String organisationB);

    /**
     * Fusionne deux organisations (la survivante est celle qui a le plus
     * d'historique, sauf si {@code survivante} est précisée) puis republie en FHIR
     * les deux organisations et les échantillons concernés.
     */
    Map<String, Object> fusionner(String organisationA, String organisationB, String survivante, String motif);

    /** Annule une fusion et republie en FHIR ce qu'elle avait modifié. */
    Map<String, Object> annuler(long fusionId);

    /** Fusions récentes (annulées comprises), les plus récentes d'abord. */
    List<Map<String, Object>> fusions();
}
