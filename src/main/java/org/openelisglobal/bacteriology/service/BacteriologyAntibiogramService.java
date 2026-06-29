package org.openelisglobal.bacteriology.service;

import java.util.List;
import org.openelisglobal.bacteriology.valueholder.BacteriologyAntibiogram;
import org.openelisglobal.dictionary.valueholder.Dictionary;

public interface BacteriologyAntibiogramService {

    BacteriologyAntibiogram get(Integer id);

    BacteriologyAntibiogram save(BacteriologyAntibiogram antibiogram);

    BacteriologyAntibiogram update(BacteriologyAntibiogram antibiogram);

    List<BacteriologyAntibiogram> getAntibiogramsByOrganismId(Integer organismId);

    List<BacteriologyAntibiogram> getAntibiogramsByOrganismIds(List<Integer> organismIds);

    BacteriologyAntibiogram getByOrganismAndAntibiotic(Integer organismId, Integer antibioticDictId);

    List<Dictionary> getAllAntibiotics();

    List<Dictionary> getAllAntibioticsSorted();

    Dictionary getAntibioticById(Integer antibioticDictId);

    void deactivateAntibiogramsForAnalysis(Integer analysisId);

    List<BacteriologyAntibiogram> getAntibiogramsByAnalysisId(Integer analysisId);

    void deleteAntibiogram(Integer id, String sysUserId);
}
