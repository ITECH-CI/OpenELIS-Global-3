# Guide Administrateur — OpenELIS Global (CILNSP)

Version cible : **3.3.0.0** Fork : `ITECH-CI/OpenELIS-Global-3` (branche
`develop`)

> Ce guide couvre l'installation, l'exploitation et l'administration de
> l'instance OpenELIS Global déployée à partir du fork CILNSP. Il s'adresse aux
> administrateurs système / responsables IT du laboratoire.

---

## 1. Architecture déployée

Le système est constitué de 5 conteneurs Docker orchestrés via Compose :

| Service                 | Conteneur                  | Rôle                                                                           |
| ----------------------- | -------------------------- | ------------------------------------------------------------------------------ |
| `oe.openelis.org`       | `openelisglobal-webapp`    | Application Java (Tomcat + war OpenELIS), expose `:8080` / `:8443`             |
| `frontend.openelis.org` | `openelisglobal-front-end` | App React (interface utilisateur), accède au backend via le proxy              |
| `proxy`                 | `openelisglobal-proxy`     | Nginx reverse proxy, expose `:80` / `:443` vers le frontend + `/api/...` → war |
| `fhir.openelis.org`     | `external-fhir-api`        | API FHIR externe (optionnelle, expositions sur `:8081`/`:8444`)                |
| `database`              | `openelisglobal-database`  | PostgreSQL 14.4 (DB `clinlims`, schéma `clinlims`), exposée sur `:15432`       |
| `certs`                 | `oe-certs`                 | Génération des keystores TLS                                                   |

Trois fichiers compose existent :

- `dev.docker-compose.yml` — environnement de développement (rebuild local du
  war).
- `app.docker-compose.yml` — déploiement applicatif (utilise des images
  publiées).
- `build.docker-compose.yml` — pipeline de build.

L'usage courant pour la **production** est `app.docker-compose.yml` ; en
développement on travaille avec `dev.docker-compose.yml`.

---

## 2. Démarrage et arrêt

### Premier démarrage

```bash
cd /chemin/vers/OpenELIS-Global-2

# Démarrer tous les services
docker compose -f app.docker-compose.yml up -d

# Vérifier que tout tourne
docker compose -f app.docker-compose.yml ps
```

### Arrêt propre

```bash
docker compose -f app.docker-compose.yml down
```

### Redémarrage du seul webapp

```bash
docker compose -f app.docker-compose.yml restart oe.openelis.org
```

### Suivi des logs

```bash
# Tomcat (backend)
docker logs -f openelisglobal-webapp

# Réinjecter dans un grep (ex : erreurs récentes)
docker logs openelisglobal-webapp --since 10m 2>&1 | grep -iE "error|exception" | tail -20
```

---

## 3. Accès

- **URL utilisateur** : `https://<host>/` (Nginx → frontend React)
- **API** : `https://<host>/api/OpenELIS-Global/`
- **DB** : `psql -h <host> -p 15432 -U clinlims -d clinlims`

Le compte par défaut au premier démarrage est défini dans le mot de passe
`DEFAULT_PW` du conteneur webapp.

---

## 4. Configuration site

Toutes les configurations dynamiques sont stockées dans la table
`clinlims.site_information`. Elles sont éditables depuis l'interface :

> **Admin → Configurations → \<Domaine\>**

Domaines principaux (`clinlims.site_information_domain`) :

| Domaine                | Usage                                                                  |
| ---------------------- | ---------------------------------------------------------------------- |
| `siteIdentity`         | Nom du site, préfixe accession, mode training, langue par défaut, etc. |
| `formating`            | Format des dates, numéros, etc.                                        |
| `resultConfiguration`  | Comportement de saisie/affichage des résultats                         |
| `resultReporting`      | Génération des rapports                                                |
| `printedReportsConfig` | Mise en page des rapports imprimés                                     |
| `patientEntryConfig`   | Champs requis sur la saisie patient                                    |
| `sampleEntryConfig`    | Champs requis sur la saisie échantillon                                |
| `non_conformityConfig` | Workflow non-conformités                                               |
| `workplanConfig`       | Workplans                                                              |
| `validationConfig`     | Validation des résultats                                               |

### Propriétés clefs spécifiques à cette version

| Propriété                  | Valeur attendue   | Effet                                                                                                                                |
| -------------------------- | ----------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `configuration name`       | `CI_GENERAL`      | Mode site (impacte certains workflows)                                                                                               |
| `Accession number prefix`  | `CHRSP`           | Préfixe des numéros d'ordonnance (à adapter au CHR cible)                                                                            |
| `default language locale`  | `fr-FR`           | Langue par défaut interface                                                                                                          |
| `default date locale`      | `fr-FR`           | Format date                                                                                                                          |
| `showAuditOnPatientReport` | `false`           | **3.3.0.0** : masque le bandeau "Rapport corrigé", les tests Canceled et les notes "Résultat corrigé" sur le rapport patient routine |
| `showSampleQuantityAndUom` | `false` ou `true` | Affiche Quantité + UoM dans /SamplePatientEntry                                                                                      |
| `National ID required`     | `false`           | National ID non requis (numéro CMU saisi mais non bloquant)                                                                          |
| `TrainingInstallation`     | `true`/`false`    | Affiche un bandeau "INSTALLATION DE FORMATION" si true                                                                               |

Modifier ces valeurs **directement dans la base** est possible :

```sql
UPDATE clinlims.site_information
SET value = 'true', lastupdated = now()
WHERE name = 'showAuditOnPatientReport';
```

Note : un redémarrage de Tomcat est nécessaire pour propager le cache.

---

## 5. Sauvegardes

### Sauvegarde DB quotidienne (recommandée)

```bash
# Dump complet du schéma clinlims
docker exec openelisglobal-database pg_dump -U clinlims -d clinlims \
  --format=custom --file=/tmp/clinlims-$(date +%F).dump

docker cp openelisglobal-database:/tmp/clinlims-$(date +%F).dump \
  /var/backups/openelis/
```

Pour automatiser, ajouter une crontab sur l'hôte :

```cron
0 2 * * * docker exec openelisglobal-database pg_dump -U clinlims -d clinlims \
  --format=custom --file=/tmp/clinlims-$(date +\%F).dump && \
  docker cp openelisglobal-database:/tmp/clinlims-$(date +\%F).dump \
  /var/backups/openelis/
```

### Restauration

```bash
docker cp /var/backups/openelis/clinlims-2026-05-15.dump \
  openelisglobal-database:/tmp/

docker exec openelisglobal-database pg_restore -U clinlims -d clinlims \
  --clean --if-exists /tmp/clinlims-2026-05-15.dump
```

⚠️ Une restauration écrase la DB courante. À tester d'abord sur un environnement
de staging.

### Sauvegardes complémentaires

- `volume/properties/SystemConfiguration.properties` — paramètres applicatifs
  (URLs, credentials).
- `volume/properties/common.properties` — secrets (à conserver hors VCS).
- `volume/plugins/` — plugins déposés manuellement.
- `volume/logs/` — utile pour l'audit.
- `volume/ocl/` — données OCL importées.

---

## 6. Mise à jour vers une nouvelle version

### Procédure standard (compose applicatif)

```bash
cd /chemin/vers/OpenELIS-Global-2

# 1. Sauvegarder la DB (cf §5)
docker exec openelisglobal-database pg_dump -U clinlims -d clinlims \
  --format=custom --file=/tmp/clinlims-pre-upgrade.dump

# 2. Récupérer la nouvelle version
git fetch origin
git checkout develop
git pull origin develop

# 3. Recréer les conteneurs (images mises à jour si tags pinnés à `develop`)
docker compose -f app.docker-compose.yml pull
docker compose -f app.docker-compose.yml up -d --force-recreate

# 4. Suivre le démarrage
docker logs -f openelisglobal-webapp
```

### Migrations Liquibase

Les migrations s'appliquent **automatiquement** au démarrage de Tomcat. La
3.3.0.0 inclut notamment :

- `resync_all_sequences.xml` (runAlways) — resync de toutes les séquences SQL
  pour éviter les violations de PK (ex : création de méthodes qui plantait sur
  `system_module`).
- `create_test_method_table.xml` — table de mapping méthode ↔ test.
- `add_show_audit_on_patient_report_config.xml` — flag audit rapport.
- `site_information_defaults_3_2_3_13.xml` — defaults environnement (langue
  fr-FR, National ID non requis, reclassement domaines).

Pour inspecter l'état des migrations :

```sql
SELECT id, author, filename, dateexecuted
FROM clinlims.databasechangelog
ORDER BY dateexecuted DESC LIMIT 20;
```

### Mise à jour en environnement de dev (war custom)

Si vous compilez localement (rare en prod) :

```bash
# Le war est bind-monté dans le conteneur via dev.docker-compose.yml
# (ligne : - ./target/OpenELIS-Global.war:/usr/local/tomcat/webapps/OpenELIS-Global.war)
mvn -q -o package -DskipTests -Dspotless.check.skip=true
# Tomcat hot-déploie automatiquement (~30s)
```

---

## 7. Tâches admin courantes dans l'interface

### 7.1 Gérer les tests

`Admin → Test Management →` :

- **Add test** : ajoute un nouveau test.
- **Modify tests** : modifie un test existant (libellés, type de résultat,
  modalités, panels associés). _Cette page a été corrigée dans la 3.3.0.0 : le
  bouton "Save" passe désormais le validator et persiste les options de liste._
- **Test activation** : active/désactive en masse.
- **Test conditionnel (parent → enfant)** _(nouveau 3.3.0.0)_ : configurer un
  test enfant qui n'apparaît dans Logbook que si le test parent retourne une
  valeur précise. Exemple : afficher _Densité parasitaire_ uniquement lorsque
  _Goutte Epaisse_ est _Positif_.
- **Assigner méthodes ↔ tests** _(nouveau 3.3.0.0)_ : pour chaque test, déclarer
  la liste des méthodes valides (ou inversement). La liste _Test method_ dans
  Logbook est ensuite filtrée selon ce mapping.

### 7.2 Gérer les méthodes

`Admin → Test Management → Manage methods`

- Créer une nouvelle méthode (FR + EN).
- L'activer via le toggle ou en l'assignant à au moins un test (l'activation se
  fait automatiquement à l'assignation).

### 7.3 Bactériologie

- **Sample entry** : sélecteur de type d'échantillon recherchable (ComboBox),
  type d'ordonnance Interne/Externe obligatoire.
- **Saisie résultats** : sections Macroscopie / Microscopie / Culture +
  identification organismes + antibiogrammes.
- **Validation biologique** : possibilité d'ajouter une note d'interprétation
  par échantillon (visible dans le rapport bacterio sous _Remarques générales du
  laboratoire_).
- **Rapport bacterio** : généré en A4, regroupé par culture, antibiogrammes
  masqués tant que la culture n'est pas finalisée.

---

## 8. Vérifications de santé

### Versions

```bash
# Version applicative (war)
docker exec openelisglobal-webapp cat \
  /usr/local/tomcat/webapps/OpenELIS-Global/WEB-INF/classes/build.properties
# attendu : project.version=3.3.0.0

# Version DB (dernière migration)
docker exec openelisglobal-database psql -U clinlims -d clinlims -c \
  "SELECT id, author, filename FROM clinlims.databasechangelog ORDER BY dateexecuted DESC LIMIT 1;"
```

### Health check API

```bash
curl -sk https://localhost/api/OpenELIS-Global/rest/open-configuration-properties \
  | python3 -m json.tool | head -20
```

### Espace disque

```bash
docker system df
du -sh volume/* 2>/dev/null
```

---

## 9. Troubleshooting

### Symptôme : « duplicate key value violates unique constraint » à la création

Cause : séquence SQL désynchronisée (`MAX(id) > nextval()`). Fix automatique au
prochain démarrage via la migration `resync_all_sequences`. En urgence :

```sql
SELECT setval('clinlims.system_module_seq',
              (SELECT MAX(id::int)+1 FROM clinlims.system_module));
```

### Symptôme : la version affichée dans l'interface ne change pas après build

Vérifier que le war local est bien bind-monté (uniquement en dev) :

```bash
grep "OpenELIS-Global.war" dev.docker-compose.yml
# Doit être DÉ-commentée :
# - ./target/OpenELIS-Global.war:/usr/local/tomcat/webapps/OpenELIS-Global.war
```

Puis recréer le conteneur :

```bash
docker compose -f dev.docker-compose.yml up -d --force-recreate oe.openelis.org
```

### Symptôme : 500 sur `/rest/MethodCreate`

Voir §9 séquences. Sinon, examiner les logs :

```bash
docker logs openelisglobal-webapp --since 5m 2>&1 | grep -iE "method|exception" | tail
```

### Symptôme : rapport patient affiche encore "Rapport corrigé" / tests annulés

Vérifier la valeur du flag dans `site_information` :

```sql
SELECT value FROM clinlims.site_information
WHERE name = 'showAuditOnPatientReport';
```

Doit être `false`. Redémarrer Tomcat après modification :

```bash
docker compose -f app.docker-compose.yml restart oe.openelis.org
```

### Symptôme : connexion DB refusée depuis le webapp

Vérifier que le conteneur DB est healthy et que `datasource.password` est lu :

```bash
docker compose -f app.docker-compose.yml ps
cat volume/properties/datasource.password
docker logs openelisglobal-webapp 2>&1 | grep -i "datasource\|connection" | tail
```

---

## 10. Sécurité

- Le mot de passe par défaut **doit être changé** au premier démarrage.
- Les fichiers `volume/properties/common.properties` et
  `volume/properties/datasource.password` contiennent des secrets : à ne jamais
  commiter, droits 600 sur l'hôte.
- TLS : géré par le conteneur `oe-certs` qui peuple `key_trust-store-volume` au
  démarrage. Pour remplacer par un cert officiel, monter `/etc/openelis-global`
  en clair avec votre keystore.
- Les logs (`volume/logs/oeLogs`, `volume/logs/tomcatLogs`) peuvent contenir des
  données sensibles : rotation et permissions à configurer.

---

## 11. Support

- Logs applicatifs : `volume/logs/oeLogs`
- Logs Tomcat : `volume/logs/tomcatLogs`
- Tests d'intégration et notes de release : `RELEASE_NOTES.md`
- Documentation upstream complète : <https://docs.openelis-global.org/>

Pour un incident bloquant :

1. Snapshot DB (`pg_dump`).
2. Joindre les logs des 1 h précédant l'incident.
3. Indiquer la version `project.version` (cf §8) et la dernière migration
   appliquée.
