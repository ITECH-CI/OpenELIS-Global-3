 INSERT INTO
clinlims.dictionary(id, is_active, dict_entry, lastupdated, local_abbrev,
dictionary_category_id, display_key, sort_order,
name_localization_id)
VALUES
(nextval('dictionary_seq'), 'Y', 'Amikacin 30μg-Aminoglycosides', now(), 'amakacin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.amakacin', 400004900, null),

(nextval('dictionary_seq'), 'Y', 'Amoxicillin/Clavulanic acid 20/10μg-Beta-lactam+Inhibitors',
now(), 'amoxicillin_clavulanic_acid',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.amoxicillin_clavulanic_acid', 400004901, null),
(nextval('dictionary_seq'), 'Y', 'Ampicillin 10μg-Penicillins',
now(), 'ampicillin_10',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.ampicillin_10', 400004902, null),
(nextval('dictionary_seq'), 'Y', 'Ampicillin 2μg-Penicillins', now(),
'ampicillin_2',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.ampicillin_2', 400004903, null),
(nextval('dictionary_seq'), 'Y', 'Azithromycin 15μg-Macrolides',
now(), 'azithromycin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.azithromycin', 400004904, null),
(nextval('dictionary_seq'), 'Y', 'Aztreonam 30μg-Monobactams', now(),
'aztreonam',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.aztreonam', 400004905, null),
(nextval('dictionary_seq'), 'Y', 'Cefepime 30μg-Cephems', now(), 'cefepime',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.cefepime', 400004906, null),
(nextval('dictionary_seq'), 'Y', 'Cefixime 5μg-Cephems-Oral', now(), 'cefixime',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.cefixime', 400004907, null),
(nextval('dictionary_seq'), 'Y', 'Cefotaxime 5μg-Cephems', now(), 'cefotaxime',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.cefotaxime', 400004908, null),
(nextval('dictionary_seq'), 'Y', 'Cefoxitin 30μg-Cephems', now(),
'cefoxitin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.cefoxitin', 400004909, null),
(nextval('dictionary_seq'), 'Y', 'Ceftazidime 10μg-Cephems', now(), 'ceftazidime',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.ceftazidime', 400004910, null),
(nextval('dictionary_seq'), 'Y', 'Ceftriaxone 30μg-Cephems', now(),
'ceftriaxone',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.ceftriaxone', 400004911, null),
(nextval('dictionary_seq'), 'Y', 'Cefuroxime 30μg-Cephems', now(),
'cefuroxime',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.cefuroxime', 400004912, null),
(nextval('dictionary_seq'), 'Y', 'Chloramphenicol 30μg-Phenicols',
now(), 'chloramphenicol',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.chloramphenicol', 400004913, null),
(nextval('dictionary_seq'), 'Y', 'Ciprofloxacin 5μg-Quinolones',
now(), 'ciprofloxacin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.ciprofloxacin', 400004914, null),
(nextval('dictionary_seq'), 'Y', 'Clindamycin 2μg-Lincosamides',
now(), 'clindamycin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.clindamycin', 400004915, null),
(nextval('dictionary_seq'), 'Y', 'Colistin 10μg-Lipopeptides', now(),
'colistin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.colistin', 400004916, null),
(nextval('dictionary_seq'), 'Y', 'Doripenem 10μg-Penems', now(), 'doripenem',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.doripenem', 400004917, null),
(nextval('dictionary_seq'), 'Y', 'Ertapenem 10μg-Penems', now(), 'ertapenem',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.ertapenem', 400004918, null),
(nextval('dictionary_seq'), 'Y', 'Erythromycin 15μg-Macrolides', now(), 'erythromycin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.erythromycin', 400004919, null),
(nextval('dictionary_seq'), 'Y', 'Fosfomycin 200μg-Fosfomycins',
now(), 'fosfomycin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.fosfomycin', 400004920, null),
(nextval('dictionary_seq'), 'Y', 'Fusidic acid 10μg-Steroidals',
now(), 'fusidic_acid',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.fusidic_acid', 400004921, null),
(nextval('dictionary_seq'), 'Y', 'Gentamicin 30μg-Aminoglycosides',
now(), 'gentamicin_30',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.gentamicin_30', 400004922, null),
(nextval('dictionary_seq'), 'Y', 'Gentamicin 10μg-Aminoglycosides',
now(), 'gentamicin_10',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.gentamicin_10', 400004923, null),
(nextval('dictionary_seq'), 'Y', 'Imipenem 10μg-Penems', now(),
'imipenem',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.imipenem', 400004924, null),

(nextval('dictionary_seq'), 'Y', 'Levofloxacin 5μg-Quinolones', now(), 'levofloxacin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.levofloxacin', 400004925, null),
(nextval('dictionary_seq'), 'Y', 'Mecillinam (Amdinocillin)
10μg-Penicillins', now(), 'mecillinam',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.mecillinam', 400004926, null),
(nextval('dictionary_seq'), 'Y', 'Meropenem 10μg-Penems', now(),
'meropenem',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.meropenem', 400004927, null),
(nextval('dictionary_seq'), 'Y', 'Minocycline 30μg-Tetracyclines', now(), 'minocycline',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.minocycline', 400004928, null),
(nextval('dictionary_seq'), 'Y', 'Moxifloxacin 5μg-Quinolones',
now(), 'moxifloxacin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.moxifloxacin', 400004929, null),
(nextval('dictionary_seq'), 'Y', 'Nalidixic acid 30μg-Quinolones',
now(), 'nalidixic_acid',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.nalidixic_acid', 400004930, null),
(nextval('dictionary_seq'), 'Y', 'Netilmicin 10μg-Aminoglycosides',
now(), 'netilmicin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.netilmicin', 400004931, null),
(nextval('dictionary_seq'), 'Y', 'Nitrofurantoin 100μg-Nitrofurans',
now(), 'nitrofurantoin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.nitrofurantoin', 400004932, null),
(nextval('dictionary_seq'), 'Y', 'Norfloxacin 10μg-Quinolones',
now(), 'norfloxacin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.norfloxacin', 400004933, null),
(nextval('dictionary_seq'), 'Y', 'Ofloxacin 5μg-Quinolones', now(),
'ofloxacin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.ofloxacin', 400004934, null),
(nextval('dictionary_seq'), 'Y', 'Oxacillin 1μg-Penicillins', now(), 'oxacillin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.oxacillin', 400004935, null),
(nextval('dictionary_seq'), 'Y', 'Penicillin G 1unit-Penicillins', now(), 'penicillin_g',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.penicillin_g', 400004936, null),
(nextval('dictionary_seq'), 'Y', 'Piperacillin 30μg-Penicillins',
now(), 'piperacillin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.piperacillin', 400004937, null),
(nextval('dictionary_seq'), 'Y', 'Piperacillin/Tazobactam
30/6μg-Beta-lactam+Inhibitors', now(), 'piperacillin_tazobactam',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.piperacillin_tazobactam', 400004938, null),
(nextval('dictionary_seq'), 'Y', 'Rifampin 5μg-Ansamycins', now(),
'rifampin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.rifampin', 400004939, null),
(nextval('dictionary_seq'), 'Y', 'Afipia sp.', now(), 'afipia_sp',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.afipia_sp', 400004940, null),
(nextval('dictionary_seq'), 'Y', 'Spectinomycin 100μg-Aminocyclitols', now(), 'spectinomycin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.spectinomycin', 400004941, null),
(nextval('dictionary_seq'), 'Y', 'Teicoplanin 30μg-Glycopeptides',
now(), 'teicoplanin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.teicoplanin', 400004942, null),
(nextval('dictionary_seq'), 'Y', 'Tetracycline 30μg-Tetracyclines',
now(), 'tetracycline',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.tetracycline', 400004943, null),
(nextval('dictionary_seq'), 'Y', 'Ticarcillin 75μg-Penicillins',
now(), 'ticarcillin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.ticarcillin', 400004944, null),
(nextval('dictionary_seq'), 'Y', 'Ticarcillin/Clavulanic acid
75/10-15μg-Beta-lactam+Inhibitors', now(),
'ticarcillin_clavulanic_acid',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.ticarcillin_clavulanic_acid', 400004945, null),
(nextval('dictionary_seq'), 'Y', 'Tigecycline 15μg-Tetracyclines',
now(), 'tigecycline',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.tigecycline', 400004946, null),
(nextval('dictionary_seq'), 'Y', 'Tobramycin 10μg-Aminoglycosides',
now(), 'tobramycin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.tobramycin', 400004947, null),
(nextval('dictionary_seq'), 'Y', 'Trimethoprim/Sulfamethoxazole
1.25/23.75μg-Folate pathway inhibitors', now(),
'trimethoprim_sulfamethoxazole',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.trimethoprim_sulfamethoxazole', 400004948,
null),
(nextval('dictionary_seq'), 'Y', 'Vancomycin 5μg-Glycopeptides', now(), 'vancomycin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.vancomycin', 400004949, null),
(nextval('dictionary_seq'), 'Y', 'Cephalothin 30μg-Cephems', now(),
'cephalothin',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.cephalothin', 400004950, null),
(nextval('dictionary_seq'), 'Y', 'Metronidazole
10μg-Nitroimidazoles', now(), 'metronidazole',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.metronidazole', 400004951, null),
(nextval('dictionary_seq'), 'Y', 'Nitroxoline 30μg-Quinolones',
now(), 'nitroxoline',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.nitroxoline', 400004952, null),
(nextval('dictionary_seq'), 'Y', 'Trimethoprim 5μg-Folate pathway
inhibitors', now(), 'trimethoprim',
(SELECT id FROM clinlims.dictionary_category WHERE description =
'Bacteriology Antibiotics' LIMIT 1),
'dictionary.bacterio.trimethoprim', 400004953, null);