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
package org.openelisglobal.common.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.serviceBeans.ResultSaveBean;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * Marqueur de correction : posé quand la valeur d'un résultat déjà validé
 * change, que les résultats soient tous validés ou non (« valider tous les
 * résultats »).
 */
public class ResultCorrectionMarkerTest {

    private static final Map<Class<?>, Object> BEANS = new HashMap<>();
    private static Object previousFactory;
    private static ResultService resultService;

    /** Valeur « en base » du résultat existant, relue par resultService.getData. */
    private String storedValue;

    @BeforeClass
    public static void installSpringFactory() throws Exception {
        Field factory = SpringContext.class.getDeclaredField("factory");
        factory.setAccessible(true);
        previousFactory = factory.get(null);
        AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
        when(beanFactory.getBean(any(Class.class)))
                .thenAnswer(invocation -> BEANS.computeIfAbsent(invocation.getArgument(0), c -> mock((Class<?>) c)));
        factory.set(null, beanFactory);
        resultService = (ResultService) BEANS.computeIfAbsent(ResultService.class, c -> mock(ResultService.class));
    }

    @AfterClass
    public static void restoreSpringFactory() throws Exception {
        Field factory = SpringContext.class.getDeclaredField("factory");
        factory.setAccessible(true);
        factory.set(null, previousFactory);
    }

    @Before
    public void setUp() {
        storedValue = "0.42";
        doAnswer(invocation -> {
            Result result = invocation.getArgument(0);
            result.setValue(storedValue);
            return null;
        }).when(resultService).getData(any(Result.class));
    }

    private static Analysis analysis(boolean hasValidationDate) {
        Analysis analysis = mock(Analysis.class);
        when(analysis.getId()).thenReturn("259");
        when(analysis.getReleasedDate()).thenReturn(hasValidationDate ? java.sql.Date.valueOf("2026-10-01") : null);
        return analysis;
    }

    private static ResultSaveBean numericResult(String value) {
        ResultSaveBean bean = new ResultSaveBean();
        bean.setResultId("212");
        bean.setResultType("N");
        bean.setResultValue(value);
        bean.setTestId("1");
        bean.setReportable("Y");
        return bean;
    }

    private Result save(ResultSaveService service, String newValue) {
        List<Result> results = service.createResultsFromTestResultItem(numericResult(newValue), new ArrayList<>());
        assertEquals(1, results.size());
        return results.get(0);
    }

    @Test
    public void validatedResultChanged_validateAllResults_shouldBeMarked() {
        // « valider tous les résultats » = true : l'analyse validée a une date de
        // validation ; la saisie la remet « à valider ».
        ResultSaveService service = new ResultSaveService(analysis(true), "1");

        Result result = save(service, "0.55");

        assertEquals("0.55", result.getValue());
        assertNotNull(result.getCorrectionDate());
    }

    @Test
    public void validatedResultChanged_validateAllResultsFalse_shouldBeMarked() {
        // « valider tous les résultats » = false : l'analyse avait été finalisée
        // directement, sans date de validation ; la saisie indique qu'elle l'était.
        ResultSaveService service = new ResultSaveService(analysis(false), "1");
        service.setPreviouslyValidated(true);

        assertNotNull(save(service, "0.55").getCorrectionDate());
    }

    @Test
    public void valueEditedDuringFirstValidation_shouldNotBeMarked() {
        // La validation pose la date avant d'enregistrer la valeur : l'appelant
        // indique que le résultat n'était pas encore validé.
        ResultSaveService service = new ResultSaveService(analysis(true), "1");
        service.setPreviouslyValidated(false);

        assertNull(save(service, "0.55").getCorrectionDate());
    }

    @Test
    public void neverValidatedResult_shouldNotBeMarked() {
        assertNull(save(new ResultSaveService(analysis(false), "1"), "0.55").getCorrectionDate());
    }

    @Test
    public void equivalentValue_shouldNotBeMarked() {
        storedValue = "25";

        assertNull(save(new ResultSaveService(analysis(true), "1"), "25.0").getCorrectionDate());
    }

    @Test
    public void firstCorrectionDate_shouldBeKept() {
        Timestamp first = Timestamp.valueOf("2026-11-01 10:00:00");
        doAnswer(invocation -> {
            Result result = invocation.getArgument(0);
            result.setValue(storedValue);
            result.setCorrectionDate(first);
            return null;
        }).when(resultService).getData(any(Result.class));

        assertEquals(first, save(new ResultSaveService(analysis(true), "1"), "0.60").getCorrectionDate());
    }
}
