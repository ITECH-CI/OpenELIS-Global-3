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
| Docker, curl, python3 | **Installés automatiquement** par l'installeur s'ils manquent (curl, python3, Docker Engine + plugin `docker compose`). ⚠ Cette installation des prérequis **nécessite internet UNE fois** ; sans connexion, l'installeur s'arrête avec un message clair. L'application elle-même s'installe ensuite hors ligne. |
| Droits | Exécution en **root** (`sudo`) |
| Espace disque | ~10 Go libres (images + volumes DB) |
| Certificats TLS | **Aucun prérequis** — le certificat d'entrée (nginx) est **auto-généré** (auto-signé 10 ans). Voir §7 pour utiliser un vrai certificat. |

> **Architecture TLS simplifiée** : le TLS est terminé uniquement sur nginx (la
> porte d'entrée). Les conteneurs (backend, FHIR) communiquent en HTTP clair sur
> un réseau Docker privé isolé. Plus aucun keystore/truststore interne à gérer.

L'installeur crée/utilise deux arborescences :
- `/etc/openelis-global/` — configuration générée (server.xml, certs nginx, setup.ini)
- `/var/lib/openelis-global/` — données + **`docker-compose.yml`** (backup_dir, secrets,
  plugins, config, initDB, volume DB). C'est depuis ce compose que tournent les conteneurs.

---

## 2. Installation (site neuf)

### 2.1 Transférer et décompresser l'installeur
```bash
# depuis la machine de build ou la Release GitHub
scp OpenELIS-Global_3.3.1.0_Installer.tar.gz operateur@serveur:/opt/
ssh operateur@serveur
cd /opt
tar -xzf OpenELIS-Global_3.3.1.0_Installer.tar.gz
cd OpenELIS-Global_3.3.1.0_Installer
```

### 2.2 (Option) Ajuster `setup.ini`
Par défaut la base tourne **dans un conteneur Docker** (`provide_database=True`).
À modifier seulement pour un postgres hôte/distant. Les valeurs par défaut
conviennent à la majorité des installations mono-serveur.

### 2.3 Lancer l'installation
```bash
sudo ./install.sh
```
Le wrapper `install.sh` : installe les prérequis manquants (curl, python3,
Docker — nécessite internet une fois), lance l'installeur, puis affiche un
récapitulatif. Équivalent manuel : `sudo python3 ./setup_OpenELIS.py -m update-install`.

**Langue** : l'installeur est en **français par défaut**. Pour l'anglais :
`OE_INSTALL_LANG=en sudo ./install.sh`.

Le script :
1. génère automatiquement les mots de passe (clinlims, admin postgres, backup,
   passphrase de la clé nginx). Le mot de passe admin postgres est **affiché ET
   enregistré** dans `/var/lib/openelis-global/config/postgres_admin.password`
   (chmod 600) ;
2. génère automatiquement le **certificat d'entrée nginx** (auto-signé 10 ans) ;
3. génère automatiquement la **clé de chiffrement** (persistée dans
   `/var/lib/openelis-global/config/ENCRYPTION_KEY`) — **⚠️ à sauvegarder : elle
   doit rester identique aux mises à jour, sinon les données chiffrées sont
   perdues** ;
4. **fuseau horaire par défaut** : `Africa/Abidjan` (modifiable) ;
5. demande uniquement les paramètres réellement propres au site :
   - **SITE_ID** (numéro de labo, 5 caractères) — la seule vraie question ;
   - REMOTE_FHIR_SOURCE, CS_SERVER, hôtes externes, FHIR_IDENTIFIER — **optionnels**
     (appuyer sur Entrée pour passer sur un site mono-serveur) ;
6. charge les images (`docker load`) depuis `dockerImage/*.tar.gz` ;
7. initialise la base (schéma + données depuis `initDB/OpenELIS-Global.sql`) ;
8. génère la config dans `/etc/openelis-global/` et le `docker-compose.yml` dans
   `/var/lib/openelis-global/`, démarre `docker compose up -d`, configure le cron
   de sauvegarde quotidienne ;
9. écrit un **récapitulatif complet** dans
   `/var/lib/openelis-global/config/INSTALL_SUMMARY.txt` (chmod 600) : URL d'accès,
   identifiants, mot de passe admin, emplacement de la clé de chiffrement.

> ⚠️ Le webapp met **plusieurs minutes** à démarrer la première fois (déploiement
> WAR + migrations Liquibase). Il apparaît en `health: starting` pendant ce temps —
> c'est normal. Attendre qu'il passe `healthy` avant de conclure à un problème.

### 2.4 Vérifier
```bash
docker compose -f /var/lib/openelis-global/docker-compose.yml ps   # tous "Up"
curl -k https://localhost/                                     # frontend
```
Accès : `https://<serveur>/` — login admin par défaut `admin` / `adminADMIN!`
(**à changer immédiatement**). L'avertissement navigateur (certificat auto-signé)
est normal ; voir §7 pour un vrai certificat.

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
3. supprime le réseau Docker `openelis-network` (sous-réseau fixe `172.20.1.0/24`)
   pour éviter qu'un bridge résiduel n'entre en conflit à la prochaine
   installation (conteneurs injoignables / connection reset) ;
4. supprime la base (volume de données) ;
5. supprime le cron de backup ;
6. supprime `/etc/openelis-global/` et `/var/lib/openelis-global/`.

> Un simple `docker compose down` **ne supprime pas** le réseau (subnet fixe
> conservé). Le réseau n'est nettoyé qu'à l'`uninstall` (ou recréé proprement au
> prochain démarrage). Si un bridge fantôme subsiste malgré tout, le supprimer
> manuellement : `docker network rm openelis-network`.

> ⚠️ **Irréversible** hors backup. S'assurer d'avoir un backup exploitable avant.

---

## 6. Dépannage rapide

| Symptôme | Piste |
|----------|-------|
| Avertissement certificat navigateur | normal (auto-signé). Voir §7 pour un vrai certificat. |
| webapp reste `health: starting` longtemps | normal les premières minutes (WAR + Liquibase). Compter jusqu'à ~8 min. Suivre `docker logs openelisglobal-webapp`. |
| webapp `unhealthy` / redémarre en boucle + page blanche | vérifier le healthcheck : `docker exec openelisglobal-webapp curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/OpenELIS-Global/health` doit renvoyer **200** (pas 302). Un 302→https indique une contrainte TLS interne résiduelle. |
| 502 sur `/` | frontend pas prêt ou backend down. `docker compose logs <service>`. |
| Backend ne démarre pas | vérifier connexion DB dans les logs webapp ; la DB doit être `healthy` avant. |
| Sauvegarde de demande figée | vérifier que le conteneur FHIR répond (écriture FHIR synchrone à la création). |
| Page admin "Modifier les tests" en 500 | corrigé côté code (NPE SiteInformation) ; s'assurer d'être sur une version ≥ celle du fix. |
| Mot de passe admin / clé de chiffrement oubliés | voir `/var/lib/openelis-global/config/INSTALL_SUMMARY.txt`, `postgres_admin.password`, `ENCRYPTION_KEY`. |

Logs :
```bash
docker compose -f /var/lib/openelis-global/docker-compose.yml logs -f oe.openelis.org
```

---

## 7. Utiliser un vrai certificat (serveur en ligne / nom de domaine)

Par défaut nginx utilise un certificat **auto-signé 10 ans** (avertissement
navigateur, sans conséquence sur la sécurité du chiffrement). Pour un serveur
public avec un vrai certificat (Let's Encrypt, cert acheté), il suffit de
**remplacer 2 fichiers** puis redémarrer le proxy :

```bash
# Remplacer par vos fichiers réels
sudo cp fullchain.pem /etc/openelis-global/nginx.cert.pem
sudo cp privkey.pem   /etc/openelis-global/nginx.key.pem
sudo docker restart openelisglobal-proxy
```

Aucune reconfiguration du backend, du FHIR ou du compose : ils sont en HTTP
interne et ignorent le certificat. Si la vraie clé n'a **pas** de passphrase
(cas Let's Encrypt), retirer la ligne `ssl_password_file` de
`/etc/openelis-global/nginx.conf` avant de redémarrer.

---

## 8. Construire l'installeur (pour les mainteneurs)

### En local
```bash
git submodule update --init --recursive   # dataexport requis
./build-civ.sh                             # -> OEInstaller/linux/OpenELIS-Global_<version>_Installer.tar.gz
```
Options : `--images-only` (build images sans installeur), `VERSION=x.y.z.w` (forcer la version).

### En CI (à venir — cf. CICD_STRATEGY_CIV.md)
Build automatique sur tag `v*` → images ghcr + installeur attaché à la **GitHub
Release** (téléchargeable). Voir la roadmap dans `CICD_STRATEGY_CIV.md`.
