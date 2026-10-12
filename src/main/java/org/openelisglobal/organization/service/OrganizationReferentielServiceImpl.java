package org.openelisglobal.organization.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dataexchange.fhir.service.SampleFhirSyncTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Requêtes en JDBC direct (et non en requêtes natives Hibernate, dont la
 * découverte automatique des colonnes et le typage des paramètres nuls
 * échouaient ici). Elles participent à la transaction Spring (même source de
 * données que le gestionnaire JPA).
 */
@Service
public class OrganizationReferentielServiceImpl implements OrganizationReferentielService {

    private static final String ID_REGEX = "^[0-9]{1,10}$";

    private static final String ETAT = "SELECT o.id, o.name AS nom, o.short_name AS code, o.ref_statut AS statut,"
            + " o.ref_niveau AS niveau, o.is_active = 'Y' AS actif, p.name AS parent, gp.name AS grand_parent,"
            + " clinlims.ref_historique(o.id) AS historique, o.fusionne_dans,"
            + " (SELECT r.facility_code FROM clinlims.ref_etablissement_dhis2 r WHERE r.facility_code = o.short_name)"
            + "   AS code_referentiel"
            + " FROM clinlims.organization o LEFT JOIN clinlims.organization p ON p.id = o.org_id"
            + " LEFT JOIN clinlims.organization gp ON gp.id = p.org_id";

    private final NamedParameterJdbcTemplate jdbc;

    @Autowired
    private FhirTransformService fhirTransformService;
    @Autowired
    private SampleFhirSyncTrigger sampleFhirSyncTrigger;
    @Autowired
    private DisplayListService displayListService;

    @Autowired
    public OrganizationReferentielServiceImpl(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> rechercher(String terme, String niveau) {
        if (GenericValidator.isBlankOrNull(terme) || terme.trim().length() < 2) {
            return new ArrayList<>();
        }
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("code", terme.trim() + "%")
                .addValue("terme", terme.trim())
                .addValue("niveau", GenericValidator.isBlankOrNull(niveau) ? "" : niveau);
        List<Map<String, Object>> resultat = new ArrayList<>();
        for (Map<String, Object> l : jdbc.queryForList(ETAT
                + " WHERE o.fusionne_dans IS NULL AND (o.short_name ILIKE :code"
                + "       OR clinlims.ref_nom_normalise(o.name) LIKE '%' || clinlims.ref_nom_normalise(:terme) || '%')"
                + "   AND (:niveau = '' OR o.ref_niveau = :niveau)"
                + " ORDER BY (o.ref_statut IN ('MFL', 'FUSIONNE')) DESC NULLS LAST, (o.is_active = 'Y') DESC, o.name"
                + " LIMIT 30", p)) {
            resultat.add(organisation(l));
        }
        return resultat;
    }

    private Map<String, Object> organisation(Map<String, Object> l) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("id", String.valueOf(l.get("id")));
        o.put("nom", l.get("nom"));
        o.put("code", l.get("code"));
        o.put("statut", l.get("statut"));
        o.put("niveau", l.get("niveau"));
        o.put("actif", Boolean.TRUE.equals(l.get("actif")));
        o.put("parent", l.get("parent"));
        o.put("grandParent", l.get("grand_parent"));
        o.put("historique", ((Number) l.get("historique")).longValue());
        o.put("fusionneDans", l.get("fusionne_dans") == null ? null : String.valueOf(l.get("fusionne_dans")));
        o.put("codeReferentiel", l.get("code_referentiel"));
        return o;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> apercu(String organisationA, String organisationB) {
        verifierIds(organisationA, organisationB);
        Map<String, Object> resultat = new LinkedHashMap<>();
        List<Map<String, Object>> organisations = new ArrayList<>();
        for (String id : Arrays.asList(organisationA, organisationB)) {
            List<Map<String, Object>> lignes = jdbc.queryForList(ETAT + " WHERE o.id = CAST(:id AS numeric)",
                    new MapSqlParameterSource("id", id));
            if (lignes.isEmpty()) {
                throw new LIMSRuntimeException("Organisation introuvable : " + id);
            }
            Map<String, Object> o = organisation(lignes.get(0));
            o.put("detail", detailHistorique(id));
            organisations.add(o);
        }
        resultat.put("organisations", organisations);
        resultat.put("survivanteProposee", String.valueOf(jdbc.queryForObject("SELECT o.id FROM clinlims.organization o"
                + " WHERE o.id IN (CAST(:a AS numeric), CAST(:b AS numeric))"
                + " ORDER BY clinlims.ref_historique(o.id) DESC, (o.id::text = coalesce((SELECT nullif(trim(value), '')"
                + "   FROM clinlims.site_information WHERE name = 'siteId'), '')) DESC, (o.is_active = 'Y') DESC, o.id"
                + " LIMIT 1", new MapSqlParameterSource().addValue("a", organisationA).addValue("b", organisationB),
                Object.class)));
        List<String> empechements = new ArrayList<>();
        Object codeA = organisations.get(0).get("codeReferentiel");
        Object codeB = organisations.get(1).get("codeReferentiel");
        if (codeA != null && codeB != null && !codeA.equals(codeB)) {
            empechements.add("Les deux organisations portent deux codes d'établissement différents (" + codeA + " / "
                    + codeB + ") : retirer d'abord le code erroné.");
        }
        for (Map<String, Object> o : organisations) {
            if (o.get("fusionneDans") != null) {
                empechements.add(
                        "L'organisation " + o.get("id") + " est déjà fusionnée dans " + o.get("fusionneDans") + ".");
            }
        }
        resultat.put("empechements", empechements);
        return resultat;
    }

    private Map<String, Object> detailHistorique(String id) {
        return jdbc.queryForMap("SELECT"
                + " (SELECT count(*) FROM clinlims.sample_requester sr WHERE sr.requester_id = CAST(:id AS numeric)"
                + "   AND sr.requester_type_id = (SELECT id FROM clinlims.requester_type"
                + "   WHERE requester_type = 'organization')) AS \"echantillons\","
                + " (SELECT count(*) FROM clinlims.electronic_order WHERE organization_id = CAST(:id AS numeric))"
                + "   AS \"demandesElectroniques\","
                + " (SELECT count(*) FROM clinlims.referral WHERE organization_id = CAST(:id AS numeric))"
                + "   AS \"transferts\","
                + " (SELECT count(*) FROM clinlims.organization WHERE org_id = CAST(:id AS numeric))"
                + "   AS \"organisationsRattachees\","
                + " (SELECT count(*) FROM clinlims.patient_identity pi JOIN clinlims.patient_identity_type pit"
                + "   ON pit.id = pi.identity_type_id WHERE pit.identity_type IN ('HEALTH REGION', 'HEALTH DISTRICT')"
                + "   AND pi.identity_data = :id) AS \"adressesPatients\"", new MapSqlParameterSource("id", id));
    }

    @Override
    @Transactional
    public Map<String, Object> fusionner(String organisationA, String organisationB, String survivante, String motif) {
        verifierIds(organisationA, organisationB);
        if (!GenericValidator.isBlankOrNull(survivante) && !survivante.matches(ID_REGEX)) {
            throw new LIMSRuntimeException("Organisation survivante invalide : " + survivante);
        }
        Number fusionId = jdbc.queryForObject(
                "SELECT clinlims.ref_fusionner(CAST(:a AS numeric),"
                        + " CAST(:b AS numeric), CAST(nullif(:s, '') AS numeric), :motif, 'manuelle')",
                new MapSqlParameterSource().addValue("a", organisationA).addValue("b", organisationB)
                        .addValue("s", GenericValidator.isBlankOrNull(survivante) ? "" : survivante)
                        .addValue("motif", GenericValidator.isBlankOrNull(motif) ? "fusion manuelle" : motif.trim()),
                Number.class);
        Map<String, Object> fusion = jdbc.queryForMap("SELECT s.org_id AS source, c.org_id AS cible"
                + " FROM clinlims.ref_etablissement_rapprochement s JOIN clinlims.ref_etablissement_rapprochement c"
                + " ON c.cible_id = s.id AND c.decision = 'fusion_cible' WHERE s.id = :id",
                new MapSqlParameterSource("id", fusionId.longValue()));
        String source = String.valueOf(fusion.get("source"));
        String cible = String.valueOf(fusion.get("cible"));
        List<String> echantillons = echantillonsDeplaces(fusionId.longValue());
        republierApresCommit(Arrays.asList(source, cible), echantillons);
        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("fusionId", fusionId.longValue());
        resultat.put("source", source);
        resultat.put("survivante", cible);
        resultat.put("echantillonsDeplaces", echantillons.size());
        return resultat;
    }

    @Override
    @Transactional
    public Map<String, Object> annuler(long fusionId) {
        MapSqlParameterSource p = new MapSqlParameterSource("id", fusionId);
        List<String> echantillons = echantillonsDeplaces(fusionId);
        List<String> organisations = new ArrayList<>();
        jdbc.queryForList("SELECT org_id FROM clinlims.ref_etablissement_rapprochement"
                + " WHERE (id = :id AND decision = 'fusion_source') OR (cible_id = :id AND decision = 'fusion_cible')",
                p, Object.class).forEach(o -> organisations.add(String.valueOf(o)));
        jdbc.queryForList("SELECT clinlims.ref_annuler_fusion(:id)", p);
        republierApresCommit(organisations, echantillons);
        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("fusionId", fusionId);
        resultat.put("organisations", organisations);
        resultat.put("echantillonsRestaures", echantillons.size());
        return resultat;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> fusions() {
        List<Map<String, Object>> resultat = new ArrayList<>();
        for (Map<String, Object> l : jdbc.queryForList("SELECT s.id, s.date_decision, s.decision, s.motif,"
                + " s.org_id AS source_id, s.ancien_nom AS source_nom, c.org_id AS survivante_id,"
                + " c.ancien_nom AS survivante_nom, s.version,"
                + " (SELECT count(*) FROM clinlims.ref_fusion_reference j WHERE j.fusion_id = s.id) AS refs"
                + " FROM clinlims.ref_etablissement_rapprochement s"
                + " JOIN clinlims.ref_etablissement_rapprochement c ON c.cible_id = s.id"
                + "  AND c.decision IN ('fusion_cible', 'fusion_cible_annulee')"
                + " WHERE s.decision IN ('fusion_source', 'fusion_source_annulee')" + " ORDER BY s.id DESC LIMIT 100",
                new MapSqlParameterSource())) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("fusionId", ((Number) l.get("id")).longValue());
            f.put("date", String.valueOf(l.get("date_decision")));
            f.put("annulee", String.valueOf(l.get("decision")).endsWith("_annulee"));
            f.put("motif", l.get("motif"));
            f.put("sourceId", String.valueOf(l.get("source_id")));
            f.put("sourceNom", l.get("source_nom"));
            f.put("survivanteId", String.valueOf(l.get("survivante_id")));
            f.put("survivanteNom", l.get("survivante_nom"));
            f.put("origine", l.get("version"));
            f.put("referencesDeplacees", ((Number) l.get("refs")).longValue());
            resultat.add(f);
        }
        return resultat;
    }

    private List<String> echantillonsDeplaces(long fusionId) {
        List<String> ids = new ArrayList<>();
        jdbc.queryForList(
                "SELECT DISTINCT sr.sample_id FROM clinlims.ref_fusion_reference j"
                        + " JOIN clinlims.sample_requester sr ON sr.id::text = j.pk_valeurs"
                        + " WHERE j.fusion_id = :id AND j.table_name = 'sample_requester'",
                new MapSqlParameterSource("id", fusionId), Object.class).forEach(id -> ids.add(String.valueOf(id)));
        return ids;
    }

    private void verifierIds(String... ids) {
        for (String id : ids) {
            if (id == null || !id.matches(ID_REGEX)) {
                throw new LIMSRuntimeException("Identifiant d'organisation invalide : " + id);
            }
        }
        if (ids.length == 2 && ids[0].equals(ids[1])) {
            throw new LIMSRuntimeException("Une organisation ne peut pas être fusionnée avec elle-même");
        }
    }

    /**
     * Après validation de la transaction : listes de sélection rafraîchies, et
     * organisations et échantillons concernés republiés en FHIR (asynchrone ; un
     * échec FHIR n'annule pas la fusion, il est tracé dans le suivi FHIR).
     */
    private void republierApresCommit(List<String> organisations, List<String> echantillons) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    displayListService.refreshLists();
                    fhirTransformService.transformPersistOrganizations(organisations);
                    echantillons.forEach(sampleFhirSyncTrigger::triggerForSample);
                } catch (Exception e) {
                    LogEvent.logWarn(OrganizationReferentielServiceImpl.class.getSimpleName(), "republier",
                            "republication FHIR après fusion : " + e.getMessage());
                }
            }
        });
    }
}
