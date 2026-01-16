const nationalityDetails = [
  {
    label: "COSTA RICA",
    value: "COSTA RICAN",
    valueFr: "Costaricain(e)",
  },
  {
    label: "CROATIA",
    value: "CROTIAN",
    valueFr: "Croate",
  },
  {
    label: "CUBA",
    value: "CUBAN",
    valueFr: "Cubain(e)",
  },
  {
    label: "CYPRUS",
    value: "CYPRIOT",
    valueFr: "Chypriote",
  },
  {
    label: "CZECH REPUBLIC",
    value: "CZECH",
    valueFr: "Tchèque",
  },
  {
    label: "NORTH KOREA DEM PEOPLES",
    value: "NORTH KOREAN",
    valueFr: "Nord-Coréen(ne)",
  },
  {
    label: "REPUBLIC OF CONGO",
    value: "CONGOLESE (BRAZ)",
    valueFr: "Congolais(e) (Brazzaville)",
  },
  {
    label: "DENMARK",
    value: "DANE",
    valueFr: "Danois(e)",
  },
  {
    label: "DJIBOUTI",
    value: "DHIBOUTI",
    valueFr: "Djiboutien(ne)",
  },
  {
    label: "DOMINICA",
    value: "DOMINICAN",
    valueFr: "Dominiquais(e)",
  },
  {
    label: "EGYPT",
    value: "EGYPTIAN",
    valueFr: "Égyptien(ne)",
  },
  {
    label: "EL SALVADOR",
    value: "SALVADORAN",
    valueFr: "Salvadorien(ne)",
  },
  {
    label: "EQUATORIAL GUINEA",
    value: "EQUATORIAL GUINEA",
    valueFr: "Équato-Guinéen(ne)",
  },
  {
    label: "ERITREA",
    value: "ERITREAN",
    valueFr: "Érythréen(ne)",
  },
  {
    label: "ESTONIA",
    value: "ESTONIAN",
    valueFr: "Estonien(ne)",
  },
  {
    label: "PAKISTAN",
    value: "PAKISTANI",
    valueFr: "Pakistanais(e)",
  },
  {
    label: "PALAU",
    value: "PALAUAN",
    valueFr: "Palaosien(ne)",
  },
  {
    label: "PAPUA NEW GUINEA",
    value: "PAPUA NEW GUINEAN",
    valueFr: "Papou(e)-Néo-Guinéen(ne)",
  },
  {
    label: "PARAGUAY",
    value: "PARAGUAYAN",
    valueFr: "Paraguayen(ne)",
  },
  {
    label: "PERU",
    value: "PERUVIAN",
    valueFr: "Péruvien(ne)",
  },
  {
    label: "PHILIPPINES",
    value: "FILIPINO",
    valueFr: "Philippin(e)",
  },
  {
    label: "PITCAIRN",
    value: "PITCAIRN",
    valueFr: "Pitcairnais(e)",
  },
  {
    label: "POLAND",
    value: "POLE",
    valueFr: "Polonais(e)",
  },
  {
    label: "PUERTO RICO",
    value: "PUERTO RICAN",
    valueFr: "Portoricain(e)",
  },
  {
    label: "QATAR",
    value: "QATAR",
    valueFr: "Qatarien(ne)",
  },
  {
    label: "REPUBLIC OF MOLDOVA",
    value: "MOLDOVAN",
    valueFr: "Moldave",
  },
  {
    label: "REPUBLIC OF KOREA  SOUTH",
    value: "SOUTH KOREAN",
    valueFr: "Sud-Coréen(ne)",
  },
  {
    label: "RUSSIAN FEDERATION",
    value: "RUSSIAN",
    valueFr: "Russe",
  },
  {
    label: "RUWANDA",
    value: "RWANDAN",
    valueFr: "Rwandais(e)",
  },
  {
    label: "ST. HELENA",
    value: "ST. HELENA",
    valueFr: "Saint-Hélénien(ne)",
  },
  {
    label: "SAINT KITTS AND NEVIS",
    value: "SAINT KITTS AND NEVIS",
    valueFr: "Kittitien(ne)",
  },
  {
    label: "SAINT VINCENT GRENADIN",
    value: "SAINT VINCENT GRENADINES",
    valueFr: "Saint-Vincentais(e)",
  },
  {
    label: "SAMOA",
    value: "SAMOAN",
    valueFr: "Samoan(e)",
  },
  {
    label: "SAN MARINO",
    value: "SAN MARINO",
    valueFr: "Saint-Marinais(e)",
  },
  {
    label: "SAO TOME AND PRINCIPE",
    value: "SAO TOME AND PRINCIPE",
    valueFr: "Santoméen(ne)",
  },
  {
    label: "SAUDI ARABIA",
    value: "SAUDI",
    valueFr: "Saoudien(ne)",
  },
  {
    label: "SENEGAL",
    value: "SENEGALESE",
    valueFr: "Sénégalais(e)",
  },
  {
    label: "SEYCHELLES",
    value: "SEYCHELLES",
    valueFr: "Seychellois(e)",
  },
  {
    label: "SINGAPORE",
    value: "SINGAPOREAN",
    valueFr: "Singapourien(ne)",
  },
  {
    label: "SLOVAKIA",
    value: "SLOVAK",
    valueFr: "Slovaque",
  },
  {
    label: "SOLOMON  ISLANDS",
    value: "SOLOMON ISLANDS",
    valueFr: "Salomonais(e)",
  },
  {
    label: "SOMALIA",
    value: "SOMALI",
    valueFr: "Somalien(ne)",
  },
  {
    label: "SOUTH AFRICA",
    value: "SOUTH AFRICAN",
    valueFr: "Sud-Africain(e)",
  },
  {
    label: "SOUTH GEORGIA SANWICH ISL",
    value: "SOUTH GEORGIA SANWICH ISL",
    valueFr: "Sud-Géorgien(ne)",
  },
  {
    label: "SPAIN",
    value: "SPANISH",
    valueFr: "Espagnol(e)",
  },
  {
    label: "SUDAN",
    value: "SUDANESE",
    valueFr: "Soudanais(e)",
  },
  {
    label: "SVALBARD JAN MEYEN ISLAND",
    value: "SVALBARD JAN MEYEN ISLAND",
    valueFr: "Svalbardien(ne)",
  },
  {
    label: "SWAZILAND",
    value: "SWAZILAND",
    valueFr: "Swazi(e)",
  },
  {
    label: "SWEDEN",
    value: "SWEDE",
    valueFr: "Suédois(e)",
  },
  {
    label: "SWITZERLAND",
    value: "SWISS",
    valueFr: "Suisse",
  },
  {
    label: "TAIWAN, PROVINCE OF CHINA",
    value: "TAIWANESE",
    valueFr: "Taïwanais(e)",
  },
  {
    label: "TAJIKISTAN",
    value: "TAJIKISTANI",
    valueFr: "Tadjik(e)",
  },
  {
    label: "THAILAND",
    value: "THAI",
    valueFr: "Thaïlandais(e)",
  },
  {
    label: "REP OF NORTH MACEDONIA",
    value: "MACEDONIAN",
    valueFr: "Macédonien(ne)",
  },
  {
    label: "TOGO",
    value: "TOGO",
    valueFr: "Togolais(e)",
  },
  {
    label: "TOKELAU",
    value: "TOKELAU",
    valueFr: "Tokelauien(ne)",
  },
  {
    label: "TUNISIA",
    value: "TUNISIAN",
    valueFr: "Tunisien(ne)",
  },
  {
    label: "TURKEY",
    value: "TURK",
    valueFr: "Turc(que)",
  },
  {
    label: "TURKS AND CAICOS ISLANDS",
    value: "TURKS AND CAICOS ISLANDS",
    valueFr: "Turquais(e)",
  },
  {
    label: "TUVALU",
    value: "TUVALUAN",
    valueFr: "Tuvaluan(e)",
  },
  {
    label: "UGANDA",
    value: "UGANDAN",
    valueFr: "Ougandais(e)",
  },
  {
    label: "UKRAINE",
    value: "UKRAINIAN",
    valueFr: "Ukrainien(ne)",
  },
  {
    label: "UNITED ARAB EMIRATES",
    value: "EMIRATI",
    valueFr: "Émirien(ne)",
  },
  {
    label: "UNITED KINGDOM",
    value: "BRITISH - CITIZEN",
    valueFr: "Britannique",
  },
  {
    label: "UK - DEPENDANT",
    value: "BRITISH - DEPENDANT",
    valueFr: "Britannique - Dépendant",
  },
  {
    label: "UK - NATIONAL",
    value: "BRITISH - NATIONAL",
    valueFr: "Britannique - National",
  },
  {
    label: "UK - OVERSEAS CITIZEN",
    value: "BRITISH- OVERSEAS CITIZEN",
    valueFr: "Britannique - Citoyen d'outre-mer",
  },
  {
    label: "UK -SUBJECT",
    value: "BRITISH - SUBJECT",
    valueFr: "Britannique - Sujet",
  },
  {
    label: "UNITED STATES",
    value: "AMERICAN",
    valueFr: "Américain(e)",
  },
  {
    label: "US MINOR OUTLYING ISLANDS",
    value: "US MINOR OUTLYING ISLANDS",
    valueFr: "Américain(e) des îles mineures",
  },
  {
    label: "URUGUAY",
    value: "URUGUAYAN",
    valueFr: "Uruguayen(ne)",
  },
  {
    label: "UZBEKISTAN",
    value: "UZBEKISTANI",
    valueFr: "Ouzbek(e)",
  },
  {
    label: "VANUATU",
    value: "VANUATU",
    valueFr: "Vanuatuan(e)",
  },
  {
    label: "VATICAN CITY STATE",
    value: "VATICAN",
    valueFr: "Vatican(e)",
  },
  {
    label: "VIETNAM",
    value: "VIETNAMESE",
    valueFr: "Vietnamien(ne)",
  },
  {
    label: "VIRGIN ISLANDS (BRITISH)",
    value: "VIRGIN ISLANDS (BRITISH)",
    valueFr: "Vierge britannique",
  },
  {
    label: "VIRGIN ISLANDS (US)",
    value: "VIRGIN ISLANDS (US)",
    valueFr: "Vierge américain(e)",
  },
  {
    label: "WALLIS AND FUTUNA ISLANDS",
    value: "WALLIS AND FUTUNA ISLANDS",
    valueFr: "Wallisien(ne) et Futunien(ne)",
  },
  {
    label: "WESTERN SAHARA",
    value: "WESTERN SAHARA",
    valueFr: "Sahraoui(e)",
  },
  {
    label: "YEMEN",
    value: "YEMENI",
    valueFr: "Yéménite",
  },
  {
    label: "ZAMBIA",
    value: "ZAMBIAN",
    valueFr: "Zambien(ne)",
  },
  {
    label: "ZIMBABWE ZWE",
    value: "ZIMBABWEAN ZWE",
    valueFr: "Zimbabwéen(ne)",
  },
  {
    label: "ETHIOPIA",
    value: "ETHIOPIAN",
    valueFr: "Éthiopien(ne)",
  },
  {
    label: "FALKLAND ISLANDS",
    value: "FALKLAND ISLANDS",
    valueFr: "Malouine(s)",
  },
  {
    label: "FAROE ISLANDS",
    value: "FAROE ISLANDS",
    valueFr: "Féroïen(ne)",
  },
  {
    label: "FIJI",
    value: "FIJIAN",
    valueFr: "Fidjien(ne)",
  },
  {
    label: "FINLAND",
    value: "FINN",
    valueFr: "Finlandais(e)",
  },
  {
    label: "FRANCE",
    value: "FRENCH",
    valueFr: "Français(e)",
  },
  {
    label: "FRANCE, METROPOLITAN",
    value: "FRANCE, METROPOLITAN",
    valueFr: "Français(e) métropolitain(e)",
  },
  {
    label: "FRENCH GUYIANA",
    value: "GUIANESE (FRENCH)",
    valueFr: "Guyanais(e) français(e)",
  },
  {
    label: "FRENCH POLYNESIA",
    value: "FRENCH POLYNESIAN",
    valueFr: "Polynésien(ne) français(e)",
  },
  {
    label: "FRENCH SOUTHERN TERRITORY",
    value: "FRENCH SOUTHERN TERRITORY",
    valueFr: "Français(e) des terres australes",
  },
  {
    label: "GABON",
    value: "GABON",
    valueFr: "Gabonais(e)",
  },
  {
    label: "GAMBIA",
    value: "GAMBIAN",
    valueFr: "Gambien(ne)",
  },
  {
    label: "GEORGIA",
    value: "GEORGIAN",
    valueFr: "Géorgien(ne)",
  },
  {
    label: "GHANA",
    value: "GHANAIAN",
    valueFr: "Ghanéen(ne)",
  },
  {
    label: "GIBRALTA",
    value: "GIBRALTAR",
    valueFr: "Gibraltarien(ne)",
  },
  {
    label: "GREECE",
    value: "GREEK",
    valueFr: "Grec(que)",
  },
  {
    label: "GREENLAND",
    value: "GREENLAND",
    valueFr: "Groenlandais(e)",
  },
  {
    label: "GRENADA",
    value: "GRENADIAN",
    valueFr: "Grenadien(ne)",
  },
  {
    label: "GUADELOUPE",
    value: "GUADELOUPE",
    valueFr: "Guadeloupéen(ne)",
  },
  {
    label: "GUAM",
    value: "GUAM",
    valueFr: "Guamien(ne)",
  },
  {
    label: "GUATEMALA",
    value: "GUATEMALA",
    valueFr: "Guatémaltèque",
  },
  {
    label: "GUINEA",
    value: "GUINEAN",
    valueFr: "Guinéen(ne)",
  },
  {
    label: "GUINEA - BISSAU",
    value: "GUINEA - BISSAU NATIONAL",
    valueFr: "Bissau-Guinéen(ne)",
  },
  {
    label: "GUYANA",
    value: "GUYANESE",
    valueFr: "Guyanien(ne)",
  },
  {
    label: "HAITI",
    value: "HAITIAN",
    valueFr: "Haïtien(ne)",
  },
  {
    label: "HEARD AND MCDONALD ISLAND",
    value: "HEARD AND MCDONALD ISLAND",
    valueFr: "Heard-et-MacDonald",
  },
  {
    label: "HONDURAS",
    value: "HONDURAN",
    valueFr: "Hondurien(ne)",
  },
  {
    label: "HUNGARY",
    value: "HUNGARIAN",
    valueFr: "Hongrois(e)",
  },
  {
    label: "ICELAND",
    value: "ICELANDER",
    valueFr: "Islandais(e)",
  },
  {
    label: "INDONESIA",
    value: "INDONESIAN",
    valueFr: "Indonésien(ne)",
  },
  {
    label: "IRAN",
    value: "IRANIAN",
    valueFr: "Iranien(ne)",
  },
  {
    label: "IRAQ",
    value: "IRAQI",
    valueFr: "Irakien(ne)",
  },
  {
    label: "IRELAND",
    value: "IRISH",
    valueFr: "Irlandais(e)",
  },
  {
    label: "ISRAEL",
    value: "ISRAELI",
    valueFr: "Israélien(ne)",
  },
  {
    label: "JAPAN",
    value: "JAPANESE",
    valueFr: "Japonais(e)",
  },
  {
    label: "KENYA",
    value: "KENYAN",
    valueFr: "Kényan(e)",
  },
  {
    label: "KIRIBATI",
    value: "KIRIBATIAN",
    valueFr: "Kiribatien(ne)",
  },
  {
    label: "KUWAIT",
    value: "KUWAITI",
    valueFr: "Koweïtien(ne)",
  },
  {
    label: "KYRGYZSTAN",
    value: "KYRGYSTANI",
    valueFr: "Kirghize",
  },
  {
    label: "LATVIA",
    value: "LATVIAN",
    valueFr: "Letton(ne)",
  },
  {
    label: "LEBANON",
    value: "LEBANESE",
    valueFr: "Libanais(e)",
  },
  {
    label: "LESOTHO",
    value: "LESOTHO",
    valueFr: "Lesothan(e)",
  },
  {
    label: "LIBERIA",
    value: "LIBERIAN",
    valueFr: "Libérien(ne)",
  },
  {
    label: "LITHUANIA",
    value: "LITHUANIAN",
    valueFr: "Lituanien(ne)",
  },
  {
    label: "MACAU",
    value: "MACAU",
    valueFr: "Macanais(e)",
  },
  {
    label: "MADAGASCAR",
    value: "MADAGASCAR",
    valueFr: "Malgache",
  },
  {
    label: "MALAWI",
    value: "MALAWIAN",
    valueFr: "Malawien(ne)",
  },
  {
    label: "MALAYSIA",
    value: "MALAYSIAN",
    valueFr: "Malaisien(ne)",
  },
  {
    label: "MALDIVES",
    value: "MALDIVIAN",
    valueFr: "Maldivien(ne)",
  },
  {
    label: "MARSHALL ISLANDS",
    value: "MARSHALL ISLANDS",
    valueFr: "Marshallais(e)",
  },
  {
    label: "MARTINIQUE",
    value: "MARTINIQUE",
    valueFr: "Martiniquais(e)",
  },
  {
    label: "MAURITANIA",
    value: "MAURITANIAN",
    valueFr: "Mauritanien(ne)",
  },
  {
    label: "MAURITIUS",
    value: "MAURITIAN",
    valueFr: "Mauricien(ne)",
  },
  {
    label: "MAYOTTE",
    value: "MAYOTTE",
    valueFr: "Mahorais(e)",
  },
  {
    label: "MEXICO",
    value: "MEXICAN",
    valueFr: "Mexicain(e)",
  },
  {
    label: "US PACIFIC ISLANDS",
    value: "US PACIFIC ISLANDS",
    valueFr: "Pacifique américain",
  },
  {
    label: "UPPER VOLTA",
    value: "UPPER VOLTA",
    valueFr: "Voltaïque",
  },
  {
    label: "MAURITIUS (ISLAND)",
    value: "MAURITIUS (ISLAND)",
    valueFr: "Mauricien(ne) (Île)",
  },
  {
    label: "DIEGO GARCIA (MTIUS)",
    value: "DIEGO GARCIA (MTIUS)",
    valueFr: "Diego-Garcien(ne)",
  },
  {
    label: "PEROS BANHOS (MTIUS)",
    value: "PEROS BANHOS (MTIUS)",
    valueFr: "Peros Banhos",
  },
  {
    label: "STATELESS",
    value: "STATELESS",
    valueFr: "Apatride",
  },
  {
    label: "YEMEN DEMOCRATIC",
    value: "YEMEN DEMOCRATIC",
    valueFr: "Yéménite démocratique",
  },
  {
    label: "CANTON  I",
    value: "CANTON  I",
    valueFr: "Cantonais(e)",
  },
  {
    label: "NOT SPECIFIED",
    value: "NOT SPECIFIED",
    valueFr: "Non spécifié",
  },
  {
    label: "PACIFIC ISLANDS",
    value: "PACIFIC ISLANDS",
    valueFr: "Pacifique",
  },
  {
    label: "HIGH SEAS",
    value: "HIGH SEAS",
    valueFr: "Haute mer",
  },
  {
    label: "RODRIGUES (MTIUS)",
    value: "RODRIGUES (MTIUS)",
    valueFr: "Rodriguais(e)",
  },
  {
    label: "JOHNSTON ISLAND",
    value: "JOHNSTON ISLAND",
    valueFr: "Johnstonien(ne)",
  },
  {
    label: "KAMPUCHEA",
    value: "KAMPUCHEA",
    valueFr: "Kampuchéen(ne)",
  },
  {
    label: "ST PIERRE ",
    value: "ST PIERRE ",
    valueFr: "Saint-Pierrais(e)",
  },
  {
    label: "AGALEGA (MTIUS)",
    value: "AGALEGA (MTIUS)",
    valueFr: "Agaléen(ne)",
  },
  {
    label: "DOMINICAN REPUBLIC",
    value: "DOMINICAN REPUBLIC",
    valueFr: "Dominicain(e)",
  },
  {
    label: "DRONNING MAUD LAND",
    value: "DRONNING MAUD LAND",
    valueFr: "Terre de la Reine-Maud",
  },
  {
    label: "GERMANY, FED. REP OF",
    value: "GERMANY, FED. REP OF",
    valueFr: "Allemand(e) de l'Ouest",
  },
  {
    label: "CZECHOSLOVAKIA",
    value: "CZECHOSLOVAKIA",
    valueFr: "Tchécoslovaque",
  },
  {
    label: "ST. BRANDON (MTIUS)",
    value: "ST. BRANDON (MTIUS)",
    valueFr: "Saint-Brandonais(e)",
  },
  {
    label: "BYELORUSSIAN SSR",
    value: "BYELORUSSIAN SSR",
    valueFr: "Biélorusse RSS",
  },
  {
    label: "ST. LUCIA",
    value: "ST. LUCIA",
    valueFr: "Saint-Lucien(ne)",
  },
  {
    label: "USSR",
    value: "USSR",
    valueFr: "Soviétique",
  },
  {
    label: "ZAIRE",
    value: "ZAIRE",
    valueFr: "Zaïrois(e)",
  },
  {
    label: "WAKE ISLAND",
    value: "WAKE ISLAND",
    valueFr: "Wakien(ne)",
  },
  {
    label: "MADEIRA ISLANDS",
    value: "MADEIRA ISLANDS",
    valueFr: "Madérien(ne)",
  },
  {
    label: "MOSCOW",
    value: "MOSCOW",
    valueFr: "Moscovite",
  },
  {
    label: "MOSCOW",
    value: "MOSCOW",
    valueFr: "Moscovite",
  },
  {
    label: "ISLAND OF GUERNSEY",
    value: "ISLAND OF GUERNSEY",
    valueFr: "Guernesiais(e)",
  },
  {
    label: "MAURITIUS-MIG",
    value: "MAURITIAN-MIG",
    valueFr: "Mauricien(ne)-MIG",
  },
  {
    label: "JAMAICA",
    value: "JAMAICAN",
    valueFr: "Jamaïcain(e)",
  },
  {
    label: "JORDAN",
    value: "JORDANIAN",
    valueFr: "Jordanien(ne)",
  },
  {
    label: "KAZAKHSTAN",
    value: "KAZAKH",
    valueFr: "Kazakh(e)",
  },
  {
    label: "LAO DEMOCRATIC REPUBLIC",
    value: "LAO",
    valueFr: "Laotien(ne)",
  },
  {
    label: "LIBYA",
    value: "LIBYAN",
    valueFr: "Libyen(ne)",
  },
  {
    label: "LIECHTENSTEIN",
    value: "LIECHTENSTEINER",
    valueFr: "Liechtensteinois(e)",
  },
  {
    label: "LUXEMBOURG",
    value: "LUXEMBURG",
    valueFr: "Luxembourgeois(e)",
  },
  {
    label: "MALI",
    value: "MALIAN",
    valueFr: "Malien(ne)",
  },
  {
    label: "MALTA",
    value: "MALTESE",
    valueFr: "Maltais(e)",
  },
  {
    label: "KOSOVO",
    value: "KOSOVAN",
    valueFr: "Kosovar(e)",
  },
  {
    label: "SAHARA",
    value: "SAHARA DEMO REP",
    valueFr: "Sahraoui(e)",
  },
  {
    label: "SIBERIA",
    value: "SIBERIAN",
    valueFr: "Sibérien(ne)",
  },
  {
    label: "MONROVIA",
    value: "MONROVIAN",
    valueFr: "Monrovien(ne)",
  },
  {
    label: "INTERPOL",
    value: "INTERPOL REPRESENTATIVE",
    valueFr: "Représentant Interpol",
  },
  {
    label: "MONTENEGRO MNE",
    value: "MONTENEGRO MNE",
    valueFr: "Monténégrin(e)",
  },
  {
    label: "UNITED  NATIONS",
    value: "UN REPRESENTATIVE(UNA)",
    valueFr: "Représentant ONU",
  },
  {
    label: "ISLAND OF JERSEY",
    value: "ISLAND OF JERSEY",
    valueFr: "Jersiais(e)",
  },
  {
    label: "DEMOCRATIC REP. OF CONGO",
    value: "CONGOLESE (DRC)",
    valueFr: "Congolais(e) (RDC)",
  },
  {
    label: "AFRICAN REINSURANCE SORPO",
    value: "AFRICAN INSURANCE",
    valueFr: "Assurance africaine",
  },
  {
    label: "REPUBLIC OF SOMALILAND",
    value: "SOMALILANDER",
    valueFr: "Somalilandais(e)",
  },
  {
    label: "ISLE OF MAN",
    value: "MANN",
    valueFr: "Mannois(e)",
  },
  {
    label: "SOUTH SUDAN",
    value: "SOUTH SUDANESE",
    valueFr: "Sud-Soudanais(e)",
  },
  {
    label: "ZIMBABWE ZIM",
    value: "ZIMBABWEAN ZIM",
    valueFr: "Zimbabwéen(ne) ZIM",
  },
  {
    label: "SERBIA SRB",
    value: "SERBIAN SRB",
    valueFr: "Serbe",
  },
  {
    label: "ROMANIA ROU",
    value: "ROMANIAN ROU",
    valueFr: "Roumain(e)",
  },
  {
    label: "UNITED NATIONS",
    value: "UN REPRESENTATIVE (UNO)",
    valueFr: "Représentant ONU (UNO)",
  },
  {
    label: "MIG",
    value: "MIG",
    valueFr: "MIG",
  },
  {
    label: "FEDERAL STATE MICRONESIA",
    value: "MICRONESIAN",
    valueFr: "Micronésien(ne)",
  },
  {
    label: "MONACO",
    value: "MONACO",
    valueFr: "Monégasque",
  },
  {
    label: "MONGOLIA",
    value: "MONGOLIAN",
    valueFr: "Mongol(e)",
  },
  {
    label: "MONTSERRAT",
    value: "MONTSERRAT",
    valueFr: "Montserratien(ne)",
  },
  {
    label: "MOROCCO",
    value: "MOROCCAN",
    valueFr: "Marocain(e)",
  },
  {
    label: "MOZAMBIQUE",
    value: "MOZAMBIQUE",
    valueFr: "Mozambicain(e)",
  },
  {
    label: "MYANMAR",
    value: "MYANMAR",
    valueFr: "Birman(e)",
  },
  {
    label: "NAMIBIA",
    value: "NAMIBIAN",
    valueFr: "Namibien(ne)",
  },
  {
    label: "NAURU",
    value: "NAURU",
    valueFr: "Nauruan(e)",
  },
  {
    label: "NEPAL",
    value: "NEPALESE",
    valueFr: "Népalais(e)",
  },
  {
    label: "NETHERLAND",
    value: "DUTCH",
    valueFr: "Néerlandais(e)",
  },
  {
    label: "NETHERLANDS ANTILLES",
    value: "NETHERLANDS ANTILLES",
    valueFr: "Antillais(e) néerlandais(e)",
  },
  {
    label: "NEUTRAL ZONE",
    value: "NEUTRAL ZONE",
    valueFr: "Zone neutre",
  },
  {
    label: "NEW CALEDONIA",
    value: "NEW CALEDONIAN",
    valueFr: "Néo-Calédonien(ne)",
  },
  {
    label: "NEW ZEALAND",
    value: "NEW ZEALANDER",
    valueFr: "Néo-Zélandais(e)",
  },
  {
    label: "NICARAGUA",
    value: "NICARAGUAN",
    valueFr: "Nicaraguayen(ne)",
  },
  {
    label: "NIGER",
    value: "NIGERIEN",
    valueFr: "Nigérien(ne)",
  },
  {
    label: "NIGERIA",
    value: "NIGERIAN",
    valueFr: "Nigérian(e)",
  },
  {
    label: "NIUE",
    value: "NIUE",
    valueFr: "Niuéen(ne)",
  },
  {
    label: "NORFOLK ISLAND",
    value: "NORFOLK ISLAND",
    valueFr: "Norfolkais(e)",
  },
  {
    label: "NOTHERN MARIANA ISLANDS",
    value: "NOTHERN MARIANA ISLANDS",
    valueFr: "Marianais(e) du Nord",
  },
  {
    label: "NORWAY",
    value: "NORWEGIAN",
    valueFr: "Norvégien(ne)",
  },
  {
    label: "OMAN",
    value: "OMAN",
    valueFr: "Omanais(e)",
  },
  {
    label: "GERMANY",
    value: "GERMAN",
    valueFr: "Allemand(e)",
  },
  {
    label: "ITALY",
    value: "ITALIAN",
    valueFr: "Italien(ne)",
  },
  {
    label: "PORTUGAL",
    value: "PORTUGUESE",
    valueFr: "Portugais(e)",
  },
  {
    label: "SLOVENIA ",
    value: "SLOVENE",
    valueFr: "Slovène",
  },
  {
    label: "PALASTINE",
    value: "PALASTINIAN",
    valueFr: "Palestinien(ne)",
  },
  {
    label: "MIANMAR",
    value: "MIANMARIAN",
    valueFr: "Birman(e)",
  },
  {
    label: "YUGOSLOVIA",
    value: "YUGOSLOVIAN",
    valueFr: "Yougoslave",
  },
  {
    label: "TRINIDAD AND TOBAGO",
    value: "TRINIDADIAN",
    valueFr: "Trinidadien(ne)",
  },
  {
    label: "SERBIA SCG",
    value: "SERB SCG",
    valueFr: "Serbe SCG",
  },
  {
    label: "SURINAME",
    value: "SURINAMIAN",
    valueFr: "Surinamais(e)",
  },
  {
    label: "SYRIAN ARAB REPUBLIC",
    value: "SYRIAN",
    valueFr: "Syrien(ne)",
  },
  {
    label: "EAST TIMOR",
    value: "EAST TIMORESE",
    valueFr: "Est-Timorais(e)",
  },
  {
    label: "REPUBLIC OF TANZANIA",
    value: "TANZANIAN",
    valueFr: "Tanzanien(ne)",
  },
  {
    label: "VENEZUELA",
    value: "VENEZUELAN",
    valueFr: "Vénézuélien(ne)",
  },
  {
    label: "ARGENTINA",
    value: "ARGENTINE",
    valueFr: "Argentin(e)",
  },
  {
    label: "CENTRAL AFRICAN REPUBLIC",
    value: "CENTRAL AFRICAN REPUBLIC",
    valueFr: "Centrafricain(e)",
  },
  {
    label: "ECUADOR",
    value: "ECUDORIAN",
    valueFr: "Équatorien(ne)",
  },
  {
    label: "UK - PROTECTED",
    value: "BRITISH - PROTECTED",
    valueFr: "Britannique - Protégé",
  },
  {
    label: "PANAMA",
    value: "PANAMANIAN",
    valueFr: "Panaméen(ne)",
  },
  {
    label: "ROMANIA ROM",
    value: "ROMANIAN ROM",
    valueFr: "Roumain(e) ROM",
  },
  {
    label: "SIERRA LEONE",
    value: "SIERRA LEONEAN",
    valueFr: "Sierra-Léonais(e)",
  },
  {
    label: "TURKMENISTAN",
    value: "TURKMENISTANI",
    valueFr: "Turkmène",
  },
  {
    label: "IVORY COAST",
    value: "IVOIRIAN",
    valueFr: "Ivoirien(ne)",
  },
  {
    label: "TONGA",
    value: "TONGAN",
    valueFr: "Tongien(ne)",
  },
  {
    label: "CAYMAN ISLANDS",
    value: "CAYMEN ISLANDS",
    valueFr: "Caïmanais(e)",
  },
  {
    label: "ANGUILLA",
    value: "ANGUILLAN",
    valueFr: "Anguillan(e)",
  },
  {
    label: "COOK ISLANDS",
    value: "COOK ISLANDS",
    valueFr: "Cookien(ne)",
  },
  {
    label: "REUNION",
    value: "REUNIONNAIS",
    valueFr: "Réunionnais(e)",
  },
  {
    label: "AUSTRALIA",
    value: "AUSTRALIAN",
    valueFr: "Australien(ne)",
  },
  {
    label: "BANGLADESH",
    value: "BANGLADESHI",
    valueFr: "Bangladais(e)",
  },
  {
    label: "BRAZIL",
    value: "BRAZILIAN",
    valueFr: "Brésilien(ne)",
  },
  {
    label: "CANADA",
    value: "CANADIAN",
    valueFr: "Canadien(ne)",
  },
  {
    label: "SRI LANKA",
    value: "SRI LANKAN",
    valueFr: "Sri-Lankais(e)",
  },
  {
    label: "INDIA",
    value: "INDIAN",
    valueFr: "Indien(ne)",
  },
  {
    label: "HONG KONG",
    value: "HONG KONG",
    valueFr: "Hongkongais(e)",
  },
  {
    label: "AFGHANISTAN",
    value: "AFGHAN",
    valueFr: "Afghan(e)",
  },
  {
    label: "ALBANIA",
    value: "ALBANIAN",
    valueFr: "Albanais(e)",
  },
  {
    label: "ALGERIA",
    value: "ALGERIAN",
    valueFr: "Algérien(ne)",
  },
  {
    label: "AMERICAN SAMOA",
    value: "AMERICAN SAMOAN",
    valueFr: "Samoan(e) américain(e)",
  },
  {
    label: "ANDORRA",
    value: "ANDORRAN",
    valueFr: "Andorran(e)",
  },
  {
    label: "ANGOLA",
    value: "ANGOLAN",
    valueFr: "Angolais(e)",
  },
  {
    label: "ANTARCTICA",
    value: "ANTARCTICAN",
    valueFr: "Antarctique",
  },
  {
    label: "ANTIGUA AND BARBUDA",
    value: "ANTIGUAN",
    valueFr: "Antiguais(e) et Barbudien(ne)",
  },
  {
    label: "ARMENIA",
    value: "ARMENIAN",
    valueFr: "Arménien(ne)",
  },
  {
    label: "ARUBA",
    value: "ARUBAN",
    valueFr: "Arubain(e)",
  },
  {
    label: "AUSTRIA",
    value: "AUSTRIAN",
    valueFr: "Autrichien(ne)",
  },
  {
    label: "AZERBAIJAN",
    value: "AZERI",
    valueFr: "Azéri(e)",
  },
  {
    label: "BAHAMAS",
    value: "BAHAMIAN",
    valueFr: "Bahaméen(ne)",
  },
  {
    label: "BAHRAIN",
    value: "BAHRAINI",
    valueFr: "Bahreïni(e)",
  },
  {
    label: "BARBADOS",
    value: "BARBADIAN",
    valueFr: "Barbadien(ne)",
  },
  {
    label: "BELARUS",
    value: "BELARUSIAN",
    valueFr: "Biélorusse",
  },
  {
    label: "BELGIUM",
    value: "BELGIAN",
    valueFr: "Belge",
  },
  {
    label: "BELIZE",
    value: "BELIZEAN",
    valueFr: "Bélizien(ne)",
  },
  {
    label: "BENIN",
    value: "BENIN",
    valueFr: "Béninois(e)",
  },
  {
    label: "BERMUDA",
    value: "BERMUDIAN",
    valueFr: "Bermudien(ne)",
  },
  {
    label: "BHUTAN",
    value: "BHUTANIAN",
    valueFr: "Bhoutanais(e)",
  },
  {
    label: "BOLIVIA",
    value: "BOLIVIAN",
    valueFr: "Bolivien(ne)",
  },
  {
    label: "BOZNIA HERZEGOVNIA",
    value: "BOSNIAN",
    valueFr: "Bosniaque",
  },
  {
    label: "BOTSWANA",
    value: "BOTSWANIAN",
    valueFr: "Botswanais(e)",
  },
  {
    label: "BOUVET ISLAND",
    value: "BOUVET ISLAND",
    valueFr: "Bouvet",
  },
  {
    label: "BRITISH INDIAN OCEAN",
    value: "BRITISH INDIAN OCEAN",
    valueFr: "Britannique océan Indien",
  },
  {
    label: "BRUNEI DARUSSALAM",
    value: "BRUNIE DARUSSALAM",
    valueFr: "Brunéien(ne)",
  },
  {
    label: "BULGARIA",
    value: "BULGARIAN",
    valueFr: "Bulgare",
  },
  {
    label: "BURKINA FASO",
    value: "BURKINABE",
    valueFr: "Burkinabè",
  },
  {
    label: "BURUNDI",
    value: "BURUNDIAN",
    valueFr: "Burundais(e)",
  },
  {
    label: "CAMBODIA",
    value: "CAMBODIAN",
    valueFr: "Cambodgien(ne)",
  },
  {
    label: "CAMEROON",
    value: "CAMEROONIAN",
    valueFr: "Camerounais(e)",
  },
  {
    label: "CAPE VERDE",
    value: "CAPE VERDEAN",
    valueFr: "Cap-Verdien(ne)",
  },
  {
    label: "CHAD",
    value: "CHADIAN",
    valueFr: "Tchadien(ne)",
  },
  {
    label: "CHILE",
    value: "CHILEAN",
    valueFr: "Chilien(ne)",
  },
  {
    label: "CHINA",
    value: "CHINESE",
    valueFr: "Chinois(e)",
  },
  {
    label: "CHRISTMAS ISLANDS",
    value: "CHRISTMAS ISLANDS",
    valueFr: "Christmas",
  },
  {
    label: "COCOS (KEELING) ISLANDS",
    value: "COCOS (KEELING) ISLANDS",
    valueFr: "Cocos",
  },
  {
    label: "COLOMBIA",
    value: "COLOMBIAN",
    valueFr: "Colombien(ne)",
  },
  {
    label: "COMOROS",
    value: "COMORIAN",
    valueFr: "Comorien(ne)",
  },
  {
    label: "MONTENEGRO MON",
    value: "MONTENEGRO MON",
    valueFr: "Monténégrin(e) MON",
  },
];

const valuesList = [];

for (var nationality in nationalityDetails) {
  var nationalityItem = nationalityDetails[nationality];
  valuesList.push({
    value: nationalityItem.value,
    label: nationalityItem.valueFr,
  });
}

export const nationalityList = valuesList.sort((a, b) =>
  a.label.localeCompare(b.label),
);
