package org.openelisglobal.testconfiguration.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.hibernate.HibernateException;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testconfiguration.form.SampleTypeTestAssignForm;
import org.openelisglobal.testconfiguration.service.SampleTypeTestAssignService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class SampleTypeTestAssignRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "testId", "sampleTypeId", "sampleTypeIds",
            "deactivateSampleTypeId" };

    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;
    @Autowired
    private SampleTypeTestAssignService sampleTypeTestAssignService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/SampleTypeTestAssign")
    public SampleTypeTestAssignForm showSampleTypeTestAssign(HttpServletRequest request) {
        SampleTypeTestAssignForm form = new SampleTypeTestAssignForm();

        setupDisplayItems(form);

        // return findForward(FWD_SUCCESS, form);
        return form;
    }

    private void setupDisplayItems(SampleTypeTestAssignForm form) {
        List<IdValuePair> typeOfSamples = DisplayListService.getInstance()
                .getListWithLeadingBlank(DisplayListService.ListType.SAMPLE_TYPE);
        LinkedHashMap<IdValuePair, List<IdValuePair>> sampleTypesTestsMap = new LinkedHashMap<>(typeOfSamples.size());

        for (IdValuePair sampleTypePair : typeOfSamples) {
            List<IdValuePair> tests = new ArrayList<>();
            sampleTypesTestsMap.put(sampleTypePair, tests);
            List<Test> testList = typeOfSampleService.getAllTestsBySampleTypeId(sampleTypePair.getId());

            for (Test test : testList) {
                if (test.isActive()) {
                    // Nom du test SANS le type entre parenthèses : un test peut être
                    // associé à plusieurs types, la parenthèse (1er type) n'avait pas
                    // de sens ici et était souvent incorrecte.
                    tests.add(new IdValuePair(test.getId(), test.getLocalizedTestName().getLocalizedValue()));
                }
            }
        }

        // we can't just append the original list because that list is in the cache
        List<IdValuePair> joinedList = new ArrayList<>(typeOfSamples);
        joinedList.addAll(DisplayListService.getInstance().getList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE));
        form.setSampleTypeList(joinedList);
        form.setSampleTypeTestList(sampleTypesTestsMap);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "sampleTypeAssignDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/SampleTypeTestAssign";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "sampleTypeAssignDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        return null;
    }

    // Types d'échantillon actuellement associés à un test (pour présélectionner
    // le multi-select côté UI).
    @GetMapping(value = "/SampleTypeTestAssign/test/{testId}")
    public List<String> getSampleTypesForTest(@PathVariable String testId) {
        List<TypeOfSampleTest> existing = typeOfSampleTestService.getTypeOfSampleTestsForTest(testId);
        if (existing == null) {
            return new ArrayList<>();
        }
        return existing.stream().map(TypeOfSampleTest::getTypeOfSampleId).collect(Collectors.toList());
    }

    @PostMapping(value = "/SampleTypeTestAssign")
    public SampleTypeTestAssignForm postSampleTypeTestAssign(HttpServletRequest request,
            @RequestBody @Valid SampleTypeTestAssignForm form, BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            setupDisplayItems(form);
            return form;
        }
        String testId = form.getTestId();
        String systemUserId = getSysUserId(request);

        // Multi-type : synchronise l'ensemble des types voulus pour le test
        // (ajoute les nouveaux, retire les décochés).
        try {
            sampleTypeTestAssignService.syncAssignments(testId, form.getSampleTypeIds(), systemUserId);
        } catch (HibernateException e) {
            LogEvent.logError(e);
        }

        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
        DisplayListService.getInstance().refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);

        return form;
    }
}
