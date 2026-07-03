# OpenELIS-Global CIV — Guide d'exploitation (installation, mise à jour, backup, désinstallation)

> Guide opérateur pour déployer et maintenir OpenELIS-Global (fork CIV) sur un
> serveur Linux, via l'**installeur offline** produit par `build-civ.sh` ou par
> le pipeline CI (asset de GitHub Release).
>
> L'installeur embarque toutes les images Docker (`.tar.gz`) : **aucune
> connexion internet n'est requise** pour l'application. Seul Docker doit être
> présent au préalable (voir prérequis).

---

## 1. Prérequis serveur

| Élément | Détail |
|---------|--------|
| OS | Linux (Ubuntu/Debian recommandé, 64-bit amd64) |
| Docker | **Docker Engine + plugin `docker compose` installés au préalable** (l'installeur ne les installe pas en mode offline). Vérifier : `docker --version` et `docker compose version`. |
| Droits | Exécution en **root** (`sudo`) |
| Python | Python 3 (pour `setup_OpenELIS.py`) |
| Espace disque | ~10 Go libres (images + volumes DB) |
| Certificats TLS | `keystore`, `truststore`, `client_facing_keystore` (PKCS12) placés dans `/etc/openelis-global/` **avant** l'installation (voir §2.1). |

L'installeur crée/utilise deux arborescences :
- `/etc/openelis-global/` — configuration générée (compose, properties, certs, secrets)
- `/var/lib/openelis-global/` — données (backup_dir, secrets, plugins, config, initDB, volume DB)

---

## 2. Installation (site neuf)

### 2.1 Préparer les certificats TLS (une fois)
Le script **vérifie** la présence de `keystore` / `truststore` / `client_facing_keystore`
dans `/etc/openelis-global/` mais ne les génère pas. Deux options :

- **Réutiliser** ceux d'un déploiement existant (recommandé pour homogénéité), ou
- **Générer** des certificats auto-signés PKCS12 (keytool/openssl) — noter les mots
  de passe keystore/truststore, ils seront demandés à l'install.

```bash
sudo mkdir -p /etc/openelis-global
# copier keystore, truststore, client_facing_keystore dans /etc/openelis-global/
sudo chmod 644 /etc/openelis-global/keystore /etc/openelis-global/truststore
```

### 2.2 Transférer et décompresser l'installeur
```bash
# depuis la machine de build ou la Release GitHub
scp OpenELIS-Global_3.3.1.0_Installer.tar.gz operateur@serveur:/opt/
ssh operateur@serveur
cd /opt
tar -xzf OpenELIS-Global_3.3.1.0_Installer.tar.gz
cd OpenELIS-Global_3.3.1.0_Installer
```

### 2.3 (Option) Ajuster `setup.ini`
Par défaut la base tourne **dans un conteneur Docker** (`provide_database=True`).
À modifier seulement pour un postgres hôte/distant. Les valeurs par défaut
conviennent à la majorité des installations mono-serveur.

### 2.4 Lancer l'installation
```bash
sudo python3 ./setup_OpenELIS.py -m install
# ou simplement (auto-détecte install vs update) :
sudo python3 ./setup_OpenELIS.py
```

Le script :
1. génère les mots de passe (clinlims, admin postgres, backup) —
   **⚠️ le mot de passe admin postgres s'affiche UNE SEULE FOIS : le noter** ;
2. demande interactivement les paramètres du site :
   - **SITE_ID** (numéro de labo, 5 caractères),
   - **KEYSTORE_PASSWORD / TRUSTSTORE_PASSWORD** (vérifiés),
   - **ENCRYPTION_KEY** — ⚠️ **doit être identique** entre installations/mises à
     jour d'un même site (sinon données chiffrées illisibles). La conserver.
   - REMOTE_FHIR_SOURCE, CS_SERVER, fuseau horaire (TZ), hôtes externes, FHIR_IDENTIFIER ;
3. charge les images (`docker load`) depuis `dockerImage/*.tar.gz` ;
4. initialise la base (schéma + données depuis `initDB/OpenELIS-Global.sql`) ;
5. génère la config dans `/etc/openelis-global/` ;
6. démarre la stack : `docker compose up -d` ;
7. configure l'utilisateur de backup + le cron de sauvegarde quotidienne.

### 2.5 Vérifier
```bash
docker compose -f /etc/openelis-global/docker-compose.yml ps   # tous "Up"
curl -k https://localhost/                                     # frontend
```
Accès : `https://<serveur>/` — login admin par défaut `admin` / `adminADMIN!`
(**à changer immédiatement**).

---

## 3. Mise à jour (nouvelle version)

La mise à jour **préserve la base de données** (le volume de données n'est pas
touché ; les migrations Liquibase s'appliquent au démarrage du webapp).

```bash
# transférer + décompresser le NOUVEL installeur (version supérieure)
tar -xzf OpenELIS-Global_3.3.2.0_Installer.tar.gz
cd OpenELIS-Global_3.3.2.0_Installer
sudo python3 ./setup_OpenELIS.py -m update
```

Le mode `update` :
1. effectue un **backup de la base** avant toute opération ;
2. arrête et supprime les anciens conteneurs/images ;
3. charge les nouvelles images (`docker load`) ;
4. régénère compose/properties/certs (réutilise les valeurs stockées : SITE_ID,
   ENCRYPTION_KEY, etc. — pas de re-saisie) ;
5. redémarre `docker compose up -d` ;
6. réinstalle le cron de backup.

> Le mode par défaut (`sans -m`) fait `update-install` : il détecte
> automatiquement s'il faut installer ou mettre à jour.

---

## 4. Sauvegarde (backup)

### Backup automatique
Un cron quotidien (`/etc/cron.d` → `openElis`, ~14h01) exécute `DatabaseBackup.pl` :
`pg_dump` du schéma `clinlims`, compression gzip, rotation dans
`/var/lib/openelis-global/backup_dir/` (daily / cumulative), purge > 30 jours,
et copie sur clé USB si `/media/USB0/Backup` est monté.

### Backup manuel (avant intervention)
```bash
# Dump manuel de la base (conteneur DB)
docker exec openelisglobal-database \
  pg_dump -U clinlims -d clinlims | gzip > ~/backup_clinlims_$(date +%F).sql.gz
```

### Restauration
```bash
gunzip -c backup_clinlims_YYYY-MM-DD.sql.gz | \
  docker exec -i openelisglobal-database psql -U clinlims -d clinlims
```

> ⚠️ Points de vigilance connus : `sendOffsite()` (envoi hors-site) est désactivé
> par défaut ; le mode "host DB" de `DatabaseBackup.pl` a un bug historique
> (`else if` au lieu de `elsif`). En config Docker DB standard, le backup
> fonctionne.

---

## 5. Désinstallation

```bash
cd OpenELIS-Global_<version>_Installer
sudo python3 ./setup_OpenELIS.py -m uninstall
```

Le mode `uninstall` (interactif, demande confirmation) :
1. effectue un backup de la base ;
2. arrête + supprime tous les conteneurs et images OpenELIS ;
3. supprime la base (volume de données) ;
4. supprime le cron de backup ;
5. supprime `/etc/openelis-global/` et `/var/lib/openelis-global/`.

> ⚠️ **Irréversible** hors backup. S'assurer d'avoir un backup exploitable avant.

---

## 6. Dépannage rapide

| Symptôme | Piste |
|----------|-------|
| Conteneur `certs` en redémarrage | certificat expiré — warning, non bloquant. Régénérer keystore/truststore si besoin. |
| 502 sur `/` | frontend pas prêt (webpack) ou backend down. `docker compose logs <service>`. |
| Backend ne démarre pas | vérifier connexion DB dans les logs webapp ; la DB doit être `healthy` avant. |
| Sauvegarde de demande figée | vérifier que le conteneur FHIR répond (écriture FHIR synchrone à la création). |
| Page admin "Modifier les tests" en 500 | corrigé côté code (NPE SiteInformation) ; s'assurer d'être sur une version ≥ celle du fix. |

Logs :
```bash
docker compose -f /etc/openelis-global/docker-compose.yml logs -f oe.openelis.org
```

---

## 7. Construire l'installeur (pour les mainteneurs)

### En local
```bash
git submodule update --init --recursive   # dataexport requis
./build-civ.sh                             # -> OEInstaller/linux/OpenELIS-Global_<version>_Installer.tar.gz
```
Options : `--images-only` (build images sans installeur), `VERSION=x.y.z.w` (forcer la version).

### En CI (à venir — cf. CICD_STRATEGY_CIV.md)
Build automatique sur tag `v*` → images ghcr + installeur attaché à la **GitHub
Release** (téléchargeable). Voir la roadmap dans `CICD_STRATEGY_CIV.md`.
