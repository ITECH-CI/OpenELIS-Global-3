INSERT INTO clinlims.dictionary
    (id, is_active, dict_entry, lastupdated, local_abbrev,
     dictionary_category_id, display_key, sort_order, name_localization_id)
VALUES
-- Penicillins
(nextval('dictionary_seq'),'Y','AMOXICILLIN',now(),'AMOXICILLIN',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.amoxicillin',410000000,null),

(nextval('dictionary_seq'),'Y','AMOXICILLIN-CLAVULANIC ACID',now(),'AMOXICILLIN_CLAVULANIC',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.amoxicillin_clavulanic',410000010,null),

(nextval('dictionary_seq'),'Y','AMPICILLIN',now(),'AMPICILLIN',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.ampicillin',410000020,null),

(nextval('dictionary_seq'),'Y','BENZYLPENICILLIN',now(),'BENZYLPENICILLIN',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.benzylpenicillin',410000030,null),

(nextval('dictionary_seq'),'Y','CLOXACILLIN',now(),'CLOXACILLIN',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.cloxacillin',410000040,null),

(nextval('dictionary_seq'),'Y','FLUCLOXACILLIN',now(),'FLUCLOXACILLIN',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.flucloxacillin',410000050,null),

(nextval('dictionary_seq'),'Y','PIPERACILLIN-TAZOBACTAM',now(),'PIPERACILLIN_TAZOBACTAM',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.piperacillin_tazobactam',410000060,null),

(nextval('dictionary_seq'),'Y','TICARCILLIN-CLAVULANIC ACID',now(),'TICARCILLIN_CLAVULANIC',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.ticarcillin_clavulanic',410000070,null),

-- Cephalosporins
(nextval('dictionary_seq'),'Y','CEFAZOLIN',now(),'CEFAZOLIN',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.cefazolin',410000100,null),

(nextval('dictionary_seq'),'Y','CEFUROXIME',now(),'CEFUROXIME',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.cefuroxime',410000110,null),

(nextval('dictionary_seq'),'Y','CEFTRIAXONE',now(),'CEFTRIAXONE',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.ceftriaxone',410000120,null),

(nextval('dictionary_seq'),'Y','CEFOTAXIME',now(),'CEFOTAXIME',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.cefotaxime',410000130,null),

(nextval('dictionary_seq'),'Y','CEFTAZIDIME',now(),'CEFTAZIDIME',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.ceftazidime',410000140,null),

(nextval('dictionary_seq'),'Y','CEFEPIME',now(),'CEFEPIME',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.cefepime',410000150,null),

(nextval('dictionary_seq'),'Y','CEFIXIME',now(),'CEFIXIME',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.cefixime',410000160,null),

-- Carbapenems
(nextval('dictionary_seq'),'Y','IMIPENEM',now(),'IMIPENEM',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.imipenem',410000200,null),

(nextval('dictionary_seq'),'Y','MEROPENEM',now(),'MEROPENEM',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.meropenem',410000210,null),

(nextval('dictionary_seq'),'Y','ERTAPENEM',now(),'ERTAPENEM',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.ertapenem',410000220,null),

-- Macrolides
(nextval('dictionary_seq'),'Y','ERYTHROMYCIN',now(),'ERYTHROMYCIN',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.erythromycin',410000300,null),

(nextval('dictionary_seq'),'Y','AZITHROMYCIN',now(),'AZITHROMYCIN',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.azithromycin',410000310,null),

(nextval('dictionary_seq'),'Y','CLARITHROMYCIN',now(),'CLARITHROMYCIN',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.clarithromycin',410000320,null),

(nextval('dictionary_seq'),'Y','SPIRAMYCIN',now(),'SPIRAMYCIN',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.spiramycin',410000330,null),

-- Fluoroquinolones
(nextval('dictionary_seq'),'Y','CIPROFLOXACIN',now(),'CIPROFLOXACIN',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.ciprofloxacin',410000400,null),

(nextval('dictionary_seq'),'Y','LEVOFLOXACIN',now(),'LEVOFLOXACIN',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.levofloxacin',410000410,null),

(nextval('dictionary_seq'),'Y','OFLOXACIN',now(),'OFLOXACIN',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.ofloxacin',410000420,null),

(nextval('dictionary_seq'),'Y','MOXIFLOXACIN',now(),'MOXIFLOXACIN',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.moxifloxacin',410000430,null),

(nextval('dictionary_seq'),'Y','NORFLOXACIN',now(),'NORFLOXACIN',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.norfloxacin',410000440,null),

-- Aminoglycosides
(nextval('dictionary_seq'),'Y','GENTAMICIN',now(),'GENTAMICIN_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.gentamicin',410000500,null),

(nextval('dictionary_seq'),'Y','AMIKACIN',now(),'AMIKACIN_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.amikacin',410000510,null),

(nextval('dictionary_seq'),'Y','TOBRAMYCIN',now(),'TOBRAMYCIN_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.tobramycin',410000520,null),

(nextval('dictionary_seq'),'Y','NETILMICIN',now(),'NETILMICIN_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.netilmicin',410000530,null),

-- Glycopeptides
(nextval('dictionary_seq'),'Y','VANCOMYCIN',now(),'VANCOMYCIN_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.vancomycin',410000600,null),

(nextval('dictionary_seq'),'Y','TEICOPLANIN',now(),'TEICOPLANIN_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.teicoplanin',410000610,null),

-- Tetracyclines
(nextval('dictionary_seq'),'Y','DOXYCYCLINE',now(),'DOXYCYCLINE',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.doxycycline',410000700,null),

(nextval('dictionary_seq'),'Y','TETRACYCLINE',now(),'TETRACYCLINE_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.tetracycline',410000710,null),

(nextval('dictionary_seq'),'Y','MINOCYCLINE',now(),'MINOCYCLINE_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.minocycline',410000720,null),

-- Others
(nextval('dictionary_seq'),'Y','METRONIDAZOLE',now(),'METRONIDAZOLE_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.metronidazole',410000800,null),

(nextval('dictionary_seq'),'Y','TRIMETHOPRIM-SULFAMETHOXAZOLE',now(),'COTRIMOXAZOLE',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.trimethoprim_sulfamethoxazole',410000810,null),

(nextval('dictionary_seq'),'Y','TRIMETHOPRIM',now(),'TRIMETHOPRIM_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.trimethoprim',410000820,null),

(nextval('dictionary_seq'),'Y','NITROFURANTOIN',now(),'NITROFURANTOIN_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.nitrofurantoin',410000830,null),

(nextval('dictionary_seq'),'Y','FOSFOMYCIN',now(),'FOSFOMYCIN_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.fosfomycin',410000840,null),

(nextval('dictionary_seq'),'Y','CLINDAMYCIN',now(),'CLINDAMYCIN_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.clindamycin',410000850,null),

(nextval('dictionary_seq'),'Y','LINEZOLID',now(),'LINEZOLID',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.linezolid',410000860,null),

(nextval('dictionary_seq'),'Y','RIFAMPICIN',now(),'RIFAMPICIN_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.rifampicin',410000870,null),

(nextval('dictionary_seq'),'Y','FUSIDIC ACID',now(),'FUSIDIC_ACID_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.fusidic_acid',410000880,null),

(nextval('dictionary_seq'),'Y','COLISTIN',now(),'COLISTIN_TH',
 (SELECT id FROM clinlims.dictionary_category WHERE description = 'Therapeutic Antibiotics' LIMIT 1),
 'dictionary.therapeutic_abx.colistin',410000890,null);