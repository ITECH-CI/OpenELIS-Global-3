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
package org.openelisglobal.dataexchange.order;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Banc d'essai (hors CI) comparant le coût de chargement des renseignements
 * d'une demande électronique selon trois stratégies de stockage, pour décider
 * du modèle e-order générique (incrément 4a du module d'échange unifié).
 *
 * <p>
 * Question tranchée : la « table plate » du dépôt avancé sert de cache de
 * lecture rapide (liste + pré-remplissage formulaire) SANS toucher FHIR. Le
 * seul argument en sa faveur est la performance : elle évite (a) la
 * désérialisation HAPI du message FHIR stocké dans
 * {@code electronic_order.data} et (b) les appels REST live au serveur FHIR. Ce
 * banc mesure UNIQUEMENT le coût CPU de lecture des champs (le a), le plus
 * déterminant pour le pré-remplissage :
 *
 * <ul>
 * <li><b>A/C — colonnes plates</b> : les champs sont déjà dénormalisés (table
 * plate ou colonnes), la lecture ≈ accès à une {@link Map}. Coût de référence
 * quasi nul.</li>
 * <li><b>B — parse HAPI</b> : {@code parseResource(Bundle)} complet puis
 * extraction des ~19 renseignements cliniques depuis le QuestionnaireResponse.
 * C'est ce que fait le chemin legacy.</li>
 * <li><b>B' — parse Jackson (JSON générique)</b> : désérialisation en arbre
 * {@link JsonNode} + navigation, sans construire les objets FHIR HAPI.
 * Représente l'option « indexer electronic_order.data sans HAPI ».</li>
 * </ul>
 *
 * <p>
 * Standalone : pur HAPI/Jackson, aucune dépendance Spring ni base — s'exécute
 * en quelques secondes. On cherche un ORDRE DE GRANDEUR (facteur), pas une
 * précision de 2 %. {@link Ignore} par défaut pour ne pas peser sur la CI ;
 * lancer via :
 *
 * <pre>
 * mvn -o test -Dtest=EorderLoadBenchmark -DfailIfNoTests=false \
 *     -Dsurefire.failIfNoSpecifiedTests=false surefire:test
 * </pre>
 *
 * (ou retirer temporairement {@code @Ignore}). Les mesures sont imprimées sur
 * stdout.
 */
public class EorderLoadBenchmark {

    private static final int WARMUP = 3000;
    private static final int ITERATIONS = 20000;

    /**
     * Code d'input VL présent dans le flux réel (cf.
     * StudyElectronicOrdersController).
     */
    private static final String VL_INPUT_CODE = "CI0050005AAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /**
     * Les 19 renseignements cliniques CV portés par la demande (linkId -> valeur).
     */
    private static final Map<String, String> CV_FIELDS = new HashMap<>();

    static {
        CV_FIELDS.put("hiv_status", "positive");
        CV_FIELDS.put("current_arv_treatment", "true");
        CV_FIELDS.put("arv_treatment_regime", "TDF+3TC+DTG");
        CV_FIELDS.put("arv_treatment_init_date", "2021-03-15");
        CV_FIELDS.put("current_arv_treatment_inns", "Tenofovir/Lamivudine/Dolutegravir");
        CV_FIELDS.put("pregnancy", "false");
        CV_FIELDS.put("suckle", "false");
        CV_FIELDS.put("vl_benefit", "routine_monitoring");
        CV_FIELDS.put("init_cd4_count", "350");
        CV_FIELDS.put("init_cd4_percent", "18");
        CV_FIELDS.put("init_cd4_date", "2021-03-10");
        CV_FIELDS.put("demand_cd4_count", "520");
        CV_FIELDS.put("demand_cd4_percent", "27");
        CV_FIELDS.put("demand_cd4_date", "2024-01-20");
        CV_FIELDS.put("prior_vl_value", "< 40");
        CV_FIELDS.put("prior_vl_date", "2023-07-01");
        CV_FIELDS.put("prior_vl_lab", "CeDReS Abidjan");
        CV_FIELDS.put("order_reason", "routine");
        CV_FIELDS.put("other_order_reason", "");
    }

    @Test
    @Ignore("Banc de performance manuel — retirer @Ignore ou lancer via JUnitCore pour mesurer")
    public void benchmarkLoadStrategies() throws Exception {
        FhirContext ctx = FhirContext.forR4();
        IParser parser = ctx.newJsonParser();
        ObjectMapper mapper = new ObjectMapper();

        Bundle bundle = buildRealisticOrderBundle();
        String json = parser.encodeResourceToString(bundle);

        System.out.println("=== EorderLoadBenchmark ===");
        System.out.println("Taille du JSON electronic_order.data : " + json.length() + " caractères ("
                + (json.getBytes("UTF-8").length / 1024.0) + " Ko)");
        System.out.println("Warmup=" + WARMUP + " iterations=" + ITERATIONS);
        System.out.println();

        // La table plate / colonnes : les champs sont déjà dénormalisés en
        // Map<String,String>.
        // Le "chargement" = itérer les entrées (ce que fait loadDataFromFlatTable sur
        // le Map JDBC).
        final Map<String, String> flatRow = new HashMap<>(CV_FIELDS);

        // --- Warmup (déclenche le JIT sur les 3 chemins) ---
        for (int i = 0; i < WARMUP; i++) {
            blackhole(readFromFlatRow(flatRow));
            blackhole(readByHapiParse(parser, json));
            blackhole(readByJacksonTree(mapper, json));
        }

        // --- A/C : lecture colonnes plates ---
        long t0 = System.nanoTime();
        long sinkA = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sinkA += readFromFlatRow(flatRow);
        }
        long flatNanos = System.nanoTime() - t0;

        // --- B : parse HAPI complet + extraction QR ---
        t0 = System.nanoTime();
        long sinkB = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sinkB += readByHapiParse(parser, json);
        }
        long hapiNanos = System.nanoTime() - t0;

        // --- B' : parse Jackson (arbre JSON générique) + navigation ---
        t0 = System.nanoTime();
        long sinkC = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sinkC += readByJacksonTree(mapper, json);
        }
        long jacksonNanos = System.nanoTime() - t0;

        printResult("A/C  colonnes plates (Map)         ", flatNanos, sinkA);
        printResult("B    parse HAPI Bundle + extract QR ", hapiNanos, sinkB);
        printResult("B'   parse Jackson tree + navigate  ", jacksonNanos, sinkC);
        System.out.println();
        System.out.printf("Facteur HAPI/plate    : %.1f x plus lent%n", (double) hapiNanos / flatNanos);
        System.out.printf("Facteur Jackson/plate : %.1f x plus lent%n", (double) jacksonNanos / flatNanos);
        System.out.printf("Facteur HAPI/Jackson  : %.1f x%n", (double) hapiNanos / jacksonNanos);
    }

    /** Stratégie A/C : les champs sont déjà colonnés — coût = itération du Map. */
    private static long readFromFlatRow(Map<String, String> row) {
        long acc = 0;
        for (Map.Entry<String, String> e : row.entrySet()) {
            String v = e.getValue();
            acc += v == null ? 0 : v.length();
        }
        return acc;
    }

    /**
     * Stratégie B : désérialisation HAPI du Bundle + extraction des renseignements
     * du QR.
     */
    private static long readByHapiParse(IParser parser, String json) {
        Bundle bundle = parser.parseResource(Bundle.class, json);
        long acc = 0;
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof QuestionnaireResponse) {
                QuestionnaireResponse qr = (QuestionnaireResponse) entry.getResource();
                acc += extractHapiItems(qr.getItem());
            }
        }
        return acc;
    }

    private static long extractHapiItems(List<QuestionnaireResponseItemComponent> items) {
        long acc = 0;
        for (QuestionnaireResponseItemComponent item : items) {
            acc += item.getLinkId().length();
            if (item.hasAnswer() && item.getAnswerFirstRep().hasValue()) {
                acc += item.getAnswerFirstRep().getValue().primitiveValue() == null ? 0
                        : item.getAnswerFirstRep().getValue().primitiveValue().length();
            }
            if (item.hasItem()) {
                acc += extractHapiItems(item.getItem());
            }
        }
        return acc;
    }

    /** Stratégie B' : arbre Jackson générique + navigation (sans objets FHIR). */
    private static long readByJacksonTree(ObjectMapper mapper, String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        long acc = 0;
        for (JsonNode entry : root.path("entry")) {
            JsonNode res = entry.path("resource");
            if ("QuestionnaireResponse".equals(res.path("resourceType").asText())) {
                acc += extractJacksonItems(res.path("item"));
            }
        }
        return acc;
    }

    private static long extractJacksonItems(JsonNode items) {
        long acc = 0;
        for (JsonNode item : items) {
            acc += item.path("linkId").asText().length();
            JsonNode answers = item.path("answer");
            for (JsonNode ans : answers) {
                // les valueX possibles
                if (ans.has("valueString"))
                    acc += ans.path("valueString").asText().length();
                if (ans.has("valueDate"))
                    acc += ans.path("valueDate").asText().length();
                if (ans.has("valueBoolean"))
                    acc += 1;
            }
            if (item.has("item")) {
                acc += extractJacksonItems(item.path("item"));
            }
        }
        return acc;
    }

    /**
     * Construit un Bundle réaliste : Patient + ServiceRequest +
     * QuestionnaireResponse portant les 19 renseignements cliniques CV — la forme
     * réelle reçue en PUSH (cf. FhirOrderReceptionServiceImpl).
     */
    private static Bundle buildRealisticOrderBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);

        Patient patient = new Patient();
        patient.setId("patient-1");
        patient.addIdentifier(new Identifier().setSystem("urn:oid:1.3.6.1.4.1.53864.1.3").setValue("90909000000"));
        patient.addName(new HumanName().setFamily("KOUASSI").addGiven("Aya").addGiven("Marie"));
        patient.setGender(AdministrativeGender.FEMALE);
        patient.setBirthDate(new Date(852076800000L)); // 1997-01-01
        bundle.addEntry().setResource(patient);

        ServiceRequest sr = new ServiceRequest();
        sr.setId("order-987654");
        sr.addIdentifier(new Identifier().setValue("987654"));
        sr.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        sr.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        sr.setSubject(new Reference("Patient/patient-1"));
        sr.setCode(new CodeableConcept().addCoding(
                new Coding().setSystem("http://loinc.org").setCode("25836-8").setDisplay("HIV RNA viral load")));
        bundle.addEntry().setResource(sr);

        QuestionnaireResponse qr = new QuestionnaireResponse();
        qr.setId("qr-1");
        qr.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
        qr.setSubject(new Reference("Patient/patient-1"));
        qr.addBasedOn(new Reference("ServiceRequest/order-987654"));
        for (Map.Entry<String, String> e : CV_FIELDS.entrySet()) {
            QuestionnaireResponseItemComponent item = qr.addItem();
            item.setLinkId(e.getKey());
            item.addAnswer().setValue(new StringType(e.getValue()));
        }
        // un input daté supplémentaire (date de demande VL) comme dans le flux réel
        QuestionnaireResponseItemComponent demandDate = qr.addItem();
        demandDate.setLinkId(VL_INPUT_CODE);
        demandDate.addAnswer().setValue(new DateType("2024-01-25"));
        bundle.addEntry().setResource(qr);

        return bundle;
    }

    /** Empêche le JIT d'éliminer le code mort pendant le warmup. */
    private static void blackhole(long v) {
        if (v == Long.MIN_VALUE) {
            throw new AssertionError("unreachable");
        }
    }

    private static void printResult(String label, long totalNanos, long sink) {
        double perOpMicros = totalNanos / (double) ITERATIONS / 1000.0;
        System.out.printf("%s : %8.3f µs/op   (total %6d ms, sink=%d)%n", label, perOpMicros, totalNanos / 1_000_000,
                sink);
    }

    /**
     * Liste utilitaire non utilisée directement — garde l'import ArrayList
     * significatif si besoin d'extension.
     */
    @SuppressWarnings("unused")
    private static List<String> keys() {
        return new ArrayList<>(CV_FIELDS.keySet());
    }
}
