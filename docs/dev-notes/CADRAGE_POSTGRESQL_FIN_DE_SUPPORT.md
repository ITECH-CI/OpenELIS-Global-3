# Cadrage : sortie de support de PostgreSQL 14

Note de cadrage, novembre 2026. Rien n'est migré ici : la note fixe le cadre
d'une bascule de version majeure à décider et à planifier.

## 1. Situation

| Version | Fin de support | Dernier correctif  |
| ------- | -------------- | ------------------ |
| 14      | 12/11/2026     | 14.25 (12/11/2026) |
| 15      | 11/11/2027     | 15.20              |
| 16      | 09/11/2028     | 16.16              |
| 17      | 08/11/2029     | 17.12              |
| 18      | 14/11/2030     | 18.7               |

Source : `https://www.postgresql.org/versions.json`, relevé le 15/11/2026.

Depuis le 12/11/2026, PostgreSQL 14 ne reçoit plus aucun correctif, de sécurité
compris. Les images Docker 14.x ne seront plus reconstruites que pour leur
système de base, puis plus du tout.

Le passage de 14.4 à 14.25 (images épinglées dans les compositions) rattrape les
correctifs publiés jusqu'à la fin de vie : c'est un palier, pas une solution.

**Constat à traiter en premier.** Par défaut, l'installeur hors ligne
(`install/installerTemplate/linux/setup_OpenELIS.py`, `LOCAL_DB = True`) utilise
un PostgreSQL installé sur l'hôte, avec des chemins en `/usr/lib/postgresql/12/`
: PostgreSQL 12 est hors support depuis novembre 2024. Le mode Docker (`#db`
dans `templates/docker-compose.yml`) embarque `postgres:14.4`, image que
`build-civ.sh` exporte dans l'installeur. L'inventaire des sites doit donc
établir, pour chacun, le mode (hôte ou Docker) et la version réellement en
service.

## 2. Branche cible

| Cible | Durée de support restante | Avis                                                                                                                                                                 |
| ----- | ------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 15    | 12 mois                   | À écarter : une deuxième bascule serait à prévoir dès 2027.                                                                                                          |
| 16    | 2 ans                     | Possible, sans avantage sur 17.                                                                                                                                      |
| 17    | 3 ans                     | Choix prudent : branche mûre (17.12).                                                                                                                                |
| 18    | 4 ans                     | **Recommandée** : 14 mois de correctifs (18.7) ; `pg_upgrade` y conserve les statistiques du planificateur, donc pas de période de requêtes lentes après la bascule. |

Compatibilité vérifiée côté applicatif : le pilote JDBC d'OpenELIS (42.7.2)
prend en charge les versions 17 et 18, et Hibernate 6 aussi. Le serveur HAPI
FHIR 6.6.0 embarque son propre pilote : à vérifier au banc (section 6).

Points de vigilance propres à la 18 :

- `initdb` active les sommes de contrôle des pages par défaut ; `pg_upgrade`
  exige que l'ancienne et la nouvelle grappe aient le même réglage. Créer la
  nouvelle grappe avec `--no-data-checksums` pour une bascule par `pg_upgrade`
  depuis une 14 sans sommes de contrôle ;
- les mots de passe stockés en MD5 déclenchent un avertissement. Les bases
  créées en 12 ou 13 peuvent en contenir : relever
  `SELECT rolname FROM pg_authid WHERE rolpassword LIKE 'md5%'`, puis redéfinir
  ces mots de passe en SCRAM avant la bascule.

## 3. À vérifier avant toute bascule, site par site

1. **Extensions** : `SELECT extname, extversion FROM pg_extension;`. Le schéma
   n'en utilise que deux : `plpgsql` et `tablefunc` (fonctions `crosstab` des
   rapports, attendues au nombre de trois par l'installeur). `tablefunc` fait
   partie des modules standard de l'image officielle ; sur une base installée
   sur l'hôte, le paquet `postgresql-contrib` de la nouvelle version doit être
   présent.
2. **Objets** : fonctions et déclencheurs PL/pgSQL (capture des changements du
   module d'échange), séquences (`resync_all_sequences`), grands objets
   éventuels (le script de restauration accepte les vidages faits avec
   `pg_dump -b`).
3. **Rôles** : `clinlims`, le rôle de sauvegarde et l'administrateur ;
   `pg_dumpall --globals-only` les reprend.
4. **Tables HAPI FHIR** : elles vivent dans le schéma `clinlims`
   (`currentSchema=clinlims`). La sauvegarde quotidienne (`DatabaseBackup.pl`)
   **exclut leurs données** (`hfj_*`, `trm_*`, `mpi_*`, `npm_*`, `bt2_*`). Un
   vidage pour bascule doit donc être complet, sans ces exclusions. À défaut, le
   magasin FHIR local devra être reconstruit après coup (rattrapage `OEToFhir`).
5. **Collations** : la base est en `en_US.utf8` (libc). Le changement d'image
   change aussi de glibc (2.31 en 14.4, 2.41 en 14.25 et 18.x). Sur 22 813
   chaînes de test (accents, ponctuation, chiffres), l'ordre de tri est
   identique en 2.31, 2.36 et 2.41. Ce n'est pas une preuve : après bascule,
   contrôler les index texte avec `amcheck` (`bt_index_check`), ou bien les
   reconstruire. La bascule par vidage et rechargement les reconstruit d'office.

## 4. Index de recherche Lucene

Hibernate Search 6.1.8 tient un index Lucene sur le système de fichiers
(`/var/lib/lucene_index`, volume `lucene_index-vol`). Cet index ne dépend pas de
la version de PostgreSQL : il référence les entités par leur identifiant.

- Avec `pg_upgrade`, les identifiants ne changent pas : l'index reste valable.
- Avec un vidage et rechargement, les identifiants et les séquences sont
  restaurés à l'identique : l'index reste valable lui aussi.

Par précaution, et parce que l'opération ne coûte que du temps machine, la
procédure prévoit une reconstruction après bascule par `GET /rest/reindex`
(`MassIndexerRestController`), suivie d'une recherche de patient de contrôle.

## 5. Méthode

| Critère        | Vidage et rechargement                                                                        | `pg_upgrade --link`                                                                                            |
| -------------- | --------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| Outillage      | `pg_dump` et `pg_restore`, déjà utilisés par la sauvegarde et la restauration de l'installeur | Binaires des deux versions dans un même conteneur (image dédiée à construire et à embarquer dans l'installeur) |
| Durée d'arrêt  | Proportionnelle au volume (index reconstruits)                                                | Quelques minutes, quel que soit le volume                                                                      |
| Collations     | Index reconstruits sous la nouvelle glibc                                                     | Index repris tels quels : contrôle `amcheck` obligatoire                                                       |
| Retour arrière | Ancienne grappe intacte                                                                       | Avec `--link`, l'ancienne grappe n'est plus utilisable dès que la nouvelle a démarré                           |
| Risque         | Faible, procédure connue des exploitants                                                      | Plus élevé, outillage nouveau                                                                                  |

**Méthode retenue : vidage et rechargement.** Les bases des sites sont de taille
modeste, la procédure s'appuie sur des scripts que les exploitants connaissent
déjà, et l'ancienne grappe reste disponible pour revenir en arrière.
`pg_upgrade` reste l'option de repli pour une base dont la durée de rechargement
dépasserait la fenêtre accordée.

Séquence type :

1. arrêt de l'application (webapp, FHIR), la base restant en service ;
2. `pg_dumpall --globals-only` puis `pg_dump -Fc` complet, tables HAPI comprises
   ;
3. arrêt de la 14 ; nouvelle grappe 18 sur un nouveau volume, l'ancien étant
   conservé ;
4. restauration des rôles, puis `pg_restore -j <cœurs>` ;
5. `ANALYZE`, contrôle des extensions et des fonctions `crosstab` ;
6. redémarrage de l'application (Liquibase vérifie le schéma), reconstruction de
   l'index Lucene, contrôles fonctionnels ;
7. conservation de l'ancien volume jusqu'à la recette du site.

## 6. Fenêtre d'indisponibilité

Mesure faite sur un banc jetable (poste de développement à SSD, base synthétique
`pgbench` de 2,25 Go, 14.25 vers 18.7) : vidage `-Fc` en 6 s, restauration
`-j 4` en 36 s, dont l'essentiel est la reconstruction des index. Les données
synthétiques se compressent bien mieux que des données réelles : la durée du
vidage est sous-estimée, celle de la restauration est réaliste.

Hypothèse de travail pour un serveur de site, sur disque mécanique, 3 à 5 fois
plus lent que le banc :

| Taille de base | Vidage + restauration | Fenêtre à annoncer (contrôles compris) |
| -------------- | --------------------- | -------------------------------------- |
| ≤ 2 Go         | 3 à 6 min             | 30 min                                 |
| 2 à 10 Go      | 6 à 30 min            | 1 h                                    |
| 10 à 50 Go     | 30 min à 2 h 30       | une demi-journée, ou `pg_upgrade`      |

À confirmer par une répétition sur une copie de la plus grosse base. Taille à
relever sur chaque site :
`SELECT pg_size_pretty(pg_database_size('clinlims'));`.

## 7. Ordre de passage

1. **Environnement de développement** : bascule de `dev.docker-compose.yml`,
   suite de tests et parcours fonctionnel complet (FHIR compris).
2. **Banc de mise à jour** : une version de l'installeur qui embarque la
   nouvelle image et le script de bascule, éprouvée sur une installation
   existante.
3. **Serveur central** (déploiement en ligne, `docker-compose.civ.yml`) : la
   plus grosse base et la seule exposée en permanence ; bascule en heures
   creuses, sauvegarde vérifiée la veille.
4. **Un site pilote isolé**, accompagné.
5. **Les autres sites isolés**, par vagues, avec l'installeur éprouvé au point 4
   ; les sites restés sur une base installée sur l'hôte (PostgreSQL 12) passent
   en mode Docker à cette occasion.

Une bascule n'est lancée sur un site qu'après une sauvegarde complète restaurée
avec succès sur une machine de test.
