package org.openelisglobal.bacteriology.service;

import java.util.ArrayList;
import java.util.List;

import org.openelisglobal.bacteriology.dao.BacteriologyOrganismDAO;
import org.openelisglobal.bacteriology.valueholder.BacteriologyOrganism;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BacteriologyOrganismServiceImpl extends BaseObjectServiceImpl<BacteriologyOrganism, Integer>
        implements BacteriologyOrganismService {

    @Autowired
    private BacteriologyOrganismDAO baseObjectDAO;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    BacteriologyOrganismServiceImpl() {
        super(BacteriologyOrganism.class);
    }

    @Override
    protected BacteriologyOrganismDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public BacteriologyOrganism save(BacteriologyOrganism organism) {
        try {
            if (organism.getId() == null) {
                Integer id = insert(organism);
                organism.setId(id);
            } else {
                update(organism);
            }
            return organism;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new RuntimeException("Failed to save organism: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BacteriologyOrganism getByGroupId(Integer resultGroupId) {
        try {
            return baseObjectDAO.getByGroupId(resultGroupId);
        } catch (Exception e) {
            LogEvent.logError(e);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BacteriologyOrganism> getOrganismsByAnalysisId(Integer analysisId) {
        try {
            return baseObjectDAO.getOrganismsByAnalysisId(analysisId);
        } catch (Exception e) {
            LogEvent.logError(e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public void deactivateOrganismsForAnalysis(Integer analysisId) {
        try {
            baseObjectDAO.deactivateOrganismsForAnalysis(analysisId);
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new RuntimeException("Failed to deactivate organisms: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteOrganism(Integer id, String sysUserId) {
        try {
            BacteriologyOrganism organism = get(id);
            if (organism != null) {
                organism.setIsActive(false);
                organism.setLastupdated(new java.sql.Timestamp(System.currentTimeMillis()));
                update(organism);
            }
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new RuntimeException("Failed to delete organism: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> getAllOrganismNames() {
        try {
            DictionaryCategory category = dictionaryCategoryService.getDictionaryCategoryByName("Organisms");
            if (category != null) {
                return dictionaryService.getDictionaryEntriesByCategoryId(category.getId());
            }
            LogEvent.logWarn(this.getClass().getSimpleName(), "getAllOrganismNames",
                    "Dictionary category 'Organisms' not found");
            return new ArrayList<>();
        } catch (Exception e) {
            LogEvent.logError(e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> getAllOrganismNamesSorted() {
        try {
            return dictionaryService.getDictionaryEntrysByCategoryNameLocalizedSort("Organisms");
        } catch (Exception e) {
            LogEvent.logError(e);
            return new ArrayList<>();
        }
    }

}
