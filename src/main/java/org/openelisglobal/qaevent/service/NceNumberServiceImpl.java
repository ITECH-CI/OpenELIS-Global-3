package org.openelisglobal.qaevent.service;

import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.service.AccessionService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Numérotation des événements non conformes. Le compteur est celui des numéros
 * de laboratoire (accession_number_info), sous une clé propre aux ENC,
 * {@code ENC-<site>-<aa>} : remise à zéro par site et par année, sans
 * changement de schéma ni nouvelle valeur d'AccessionFormat.
 */
@Service
public class NceNumberServiceImpl implements NceNumberService {

    @Autowired
    private AccessionService accessionService;

    @Override
    @Transactional
    public String nextNceNumber() {
        String site = NceNumberFormat.normalizeSite(
                ConfigurationProperties.getInstance().getPropertyValue(Property.ACCESSION_NUMBER_PREFIX));
        if (site.isEmpty()) {
            LogEvent.logWarn(getClass().getSimpleName(), "nextNceNumber",
                    "préfixe de site (ACCESSION_NUMBER_PREFIX) vide : numéro d'ENC sans champ site");
        }
        String year = DateUtil.getTwoDigitYear();
        long sequence = accessionService.getNextNumberIncrement(NceNumberFormat.counterKey(site, year),
                AccessionFormat.SITEYEARNUM);
        if (sequence > NceNumberFormat.FOUR_DIGIT_MAX) {
            LogEvent.logWarn(getClass().getSimpleName(), "nextNceNumber",
                    "plus de " + NceNumberFormat.FOUR_DIGIT_MAX + " ENC cette année pour le site '" + site
                            + "' : numéro sur " + String.valueOf(sequence).length() + " chiffres");
        }
        String nceNumber = NceNumberFormat.format(site, year, sequence);
        if (nceNumber.length() > NceNumberFormat.MAX_LENGTH) {
            throw new LIMSRuntimeException("numéro d'ENC " + nceNumber + " plus long que " + NceNumberFormat.MAX_LENGTH
                    + " caractères : préfixe de site (ACCESSION_NUMBER_PREFIX) trop long");
        }
        return nceNumber;
    }
}
