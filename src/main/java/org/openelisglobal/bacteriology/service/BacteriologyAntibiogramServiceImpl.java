package org.openelisglobal.bacteriology.service;

import java.util.ArrayList;
import java.util.List;

import org.openelisglobal.bacteriology.dao.BacteriologyAntibiogramDAO;
import org.openelisglobal.bacteriology.dao.BacteriologyOrganismDAO;
import org.openelisglobal.bacteriology.dao.BacteriologyResultGroupDAO;
import org.openelisglobal.bacteriology.valueholder.BacteriologyAntibiogram;
import org.openelisglobal.bacteriology.valueholder.BacteriologyOrganism;
import org.openelisglobal.bacteriology.valueholder.BacteriologyResultGroup;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BacteriologyAntibiogramServiceImpl extends BaseObjectServiceImpl<BacteriologyAntibiogram, Integer>
		implements BacteriologyAntibiogramService {

	@Autowired
	private BacteriologyAntibiogramDAO baseObjectDAO;

	@Autowired
	private DictionaryService dictionaryService;

	@Autowired
	private DictionaryCategoryService dictionaryCategoryService;

	@Autowired
	private BacteriologyOrganismDAO bacteriologyOrganismDAO;

	@Autowired
	private BacteriologyResultGroupDAO bacteriologyResultGroupDAO;

	BacteriologyAntibiogramServiceImpl() {
		super(BacteriologyAntibiogram.class);
	}

	@Override
	protected BacteriologyAntibiogramDAO getBaseObjectDAO() {
		return baseObjectDAO;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Dictionary> getAllAntibiotics() {
		try {
			DictionaryCategory category = dictionaryCategoryService
					.getDictionaryCategoryByName("Bacteriology Antibiotics");
			if (category != null) {
				return dictionaryService.getDictionaryEntriesByCategoryId(category.getId());
			}
			LogEvent.logWarn(this.getClass().getSimpleName(), "getAllAntibiotics",
					"Dictionary category 'Bacteriology Antibiotics' not found");
			return new ArrayList<>();
		} catch (Exception e) {
			LogEvent.logError(e);
			return new ArrayList<>();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<Dictionary> getAllAntibioticsSorted() {
		try {
			return dictionaryService.getDictionaryEntrysByCategoryNameLocalizedSort("Bacteriology Antibiotics");
		} catch (Exception e) {
			LogEvent.logError(e);
			return new ArrayList<>();
		}
	}

	@Override
	@Transactional
	public BacteriologyAntibiogram save(BacteriologyAntibiogram antibiogram) {
		try {
			if (antibiogram.getId() == null) {
				Integer id = insert(antibiogram);
				antibiogram.setId(id);
			} else {
				update(antibiogram);
			}
			return antibiogram;
		} catch (Exception e) {
			LogEvent.logError(e);
			throw new RuntimeException("Failed to save antibiogram: " + e.getMessage(), e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<BacteriologyAntibiogram> getAntibiogramsByOrganismId(Integer organismId) {
		try {
			return baseObjectDAO.getAntibiogramsByOrganismId(organismId);
		} catch (Exception e) {
			LogEvent.logError(e);
			return new ArrayList<>();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public BacteriologyAntibiogram getByOrganismAndAntibiotic(Integer organismId, Integer antibioticDictId) {
		try {
			return baseObjectDAO.getByOrganismAndAntibiotic(organismId, antibioticDictId);
		} catch (Exception e) {
			LogEvent.logError(e);
			return null;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<BacteriologyAntibiogram> getAntibiogramsByAnalysisId(Integer analysisId) {
		try {
			List<BacteriologyAntibiogram> allAntibiograms = new ArrayList<>();

			// Get all culture groups for this analysis
			List<BacteriologyResultGroup> cultureGroups = bacteriologyResultGroupDAO
					.getGroupsByAnalysisAndType(analysisId, "CULTURE");

			// For each culture group, get organism groups and their antibiograms
			for (BacteriologyResultGroup cultureGroup : cultureGroups) {
				List<BacteriologyResultGroup> organismGroups = bacteriologyResultGroupDAO
						.getChildGroups(cultureGroup.getId());

				for (BacteriologyResultGroup organismGroup : organismGroups) {
					// Get the organism for this group
					BacteriologyOrganism organism = bacteriologyOrganismDAO.getByGroupId(organismGroup.getId());
					if (organism != null) {
						// Get antibiograms for this organism
						List<BacteriologyAntibiogram> antibiograms = baseObjectDAO
								.getAntibiogramsByOrganismId(organism.getId());
						allAntibiograms.addAll(antibiograms);
					}
				}
			}

			return allAntibiograms;
		} catch (Exception e) {
			LogEvent.logError(e);
			return new ArrayList<>();
		}
	}

	@Override
	@Transactional
	public void deactivateAntibiogramsForAnalysis(Integer analysisId) {
		try {
			baseObjectDAO.deactivateAntibiogramsForAnalysis(analysisId);
		} catch (Exception e) {
			LogEvent.logError(e);
			throw new RuntimeException("Failed to deactivate antibiograms: " + e.getMessage(), e);
		}
	}

	@Override
	@Transactional
	public void deleteAntibiogram(Integer id, String sysUserId) {
		try {
			BacteriologyAntibiogram antibiogram = get(id);
			if (antibiogram != null) {
				antibiogram.setIsActive(false);
				antibiogram.setLastupdated(new java.sql.Timestamp(System.currentTimeMillis()));
				antibiogram.setSysUserId(sysUserId);
				update(antibiogram);
			}
		} catch (Exception e) {
			LogEvent.logError(e);
			throw new RuntimeException("Failed to delete antibiogram: " + e.getMessage(), e);
		}
	}

	@Override
	public Dictionary getAntibioticById(Integer antibioticDictId) {
		return dictionaryService.getDictionaryById(antibioticDictId.toString());
	}
}
