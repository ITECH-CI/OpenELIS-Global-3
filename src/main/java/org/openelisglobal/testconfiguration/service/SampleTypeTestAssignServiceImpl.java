package org.openelisglobal.testconfiguration.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SampleTypeTestAssignServiceImpl implements SampleTypeTestAssignService {

    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    @Override
    @Transactional
    public void update(TypeOfSample typeOfSample, String testId, List<String> typeOfSamplesTestIDs, String sampleTypeId,
            boolean deleteExistingTypeOfSampleTest, boolean updateTypeOfSample, TypeOfSample deActivateTypeOfSample,
            String systemUserId) {
        if (deleteExistingTypeOfSampleTest) {
            for (String typeOfSamplesTestID : typeOfSamplesTestIDs) {
                typeOfSampleTestService.delete(typeOfSamplesTestID, systemUserId);
            }
        }

        if (updateTypeOfSample) {
            typeOfSampleService.update(typeOfSample);
        }

        TypeOfSampleTest typeOfSampleTest = new TypeOfSampleTest();
        typeOfSampleTest.setTestId(testId);
        typeOfSampleTest.setTypeOfSampleId(sampleTypeId);
        typeOfSampleTest.setSysUserId(systemUserId);
        typeOfSampleTest.setLastupdatedFields();

        typeOfSampleTestService.insert(typeOfSampleTest);

        if (deActivateTypeOfSample != null) {
            typeOfSampleService.update(deActivateTypeOfSample);
        }
    }

    @Override
    @Transactional
    public void syncAssignments(String testId, List<String> desiredSampleTypeIds, String systemUserId) {
        List<String> desired = desiredSampleTypeIds == null ? new ArrayList<>() : desiredSampleTypeIds;

        // Types actuellement associés au test
        List<TypeOfSampleTest> existing = typeOfSampleTestService.getTypeOfSampleTestsForTest(testId);
        List<String> currentSampleTypeIds = existing == null ? new ArrayList<>()
                : existing.stream().map(TypeOfSampleTest::getTypeOfSampleId).collect(Collectors.toList());

        // 1) Retirer les associations qui ne sont plus voulues
        for (TypeOfSampleTest tost : existing) {
            if (!desired.contains(tost.getTypeOfSampleId())) {
                typeOfSampleTestService.delete(tost.getId(), systemUserId);
            }
        }

        // 2) Ajouter les nouvelles associations (et réactiver un type inactif)
        for (String sampleTypeId : desired) {
            if (!currentSampleTypeIds.contains(sampleTypeId)) {
                TypeOfSample typeOfSample = typeOfSampleService.getTransientTypeOfSampleById(sampleTypeId);
                if (typeOfSample != null && typeOfSample.getIsActive() == false) {
                    typeOfSample.setIsActive(true);
                    typeOfSample.setSysUserId(systemUserId);
                    typeOfSampleService.update(typeOfSample);
                }

                TypeOfSampleTest typeOfSampleTest = new TypeOfSampleTest();
                typeOfSampleTest.setTestId(testId);
                typeOfSampleTest.setTypeOfSampleId(sampleTypeId);
                typeOfSampleTest.setSysUserId(systemUserId);
                typeOfSampleTest.setLastupdatedFields();
                typeOfSampleTestService.insert(typeOfSampleTest);
            }
        }
    }
}
