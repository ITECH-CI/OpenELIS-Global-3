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
- `/var/lib/openelis-global/` — données (backup_dir, secrets, plugins, config, initDB,
  volume DB). Contient une **copie d'archive** du `docker-compose.yml`, **non** utilisée
  pour piloter les conteneurs.

> ⚠️ Les conteneurs sont démarrés par `docker compose up` **depuis le dossier de
> l'installer** (le `docker-compose.yml` qui s'y trouve). Pour vérifier/gérer les
> conteneurs, se placer dans ce dossier et utiliser `sudo docker compose ps`
> (la copie sous `/var/lib/` n'est qu'une sauvegarde de référence).

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
2. génère automatiquement le **certificat d'entrée nginx** (auto-signé 10 ans,
   **propre à cette installation**) pour le nom local `oeglobal.local` (le SAN
   inclut aussi l'IP du serveur et `localhost`) ;
3. génère automatiquement la **clé de chiffrement** (persistée dans
   `/var/lib/openelis-global/config/ENCRYPTION_KEY`) — **⚠️ à sauvegarder : elle
   doit rester identique aux mises à jour, sinon les données chiffrées sont
   perdues** ;
4. **fuseau horaire par défaut** : `Africa/Abidjan` (modifiable) ;
5. demande uniquement les paramètres réellement propres au site :
   - **SITE_ID** (numéro de labo, 5 caractères) ;
   - **SERVER_IP_ADDRESS** (IP LAN du serveur) : **auto-détectée** et proposée par
     défaut — appuyer sur Entrée pour l'accepter, ou saisir l'IP correcte si la
     machine a plusieurs interfaces. C'est l'IP vers laquelle `oeglobal.local`
     résout (voir §9) ;
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
# depuis le dossier d'installation (c'est ce compose qui pilote les conteneurs)
sudo docker compose ps                 # tous "Up"
curl -k https://localhost/             # frontend
```
Accès : `https://oeglobal.local/` (ou `https://<IP-du-serveur>/`) — login admin
par défaut `admin` / `adminADMIN!` (**à changer immédiatement**). L'avertissement
navigateur (certificat auto-signé) est normal ; voir §7 pour un vrai certificat.

> Pour que `https://oeglobal.local/` fonctionne depuis les **postes clients**, ils
> doivent utiliser le serveur comme DNS (voir §9). Depuis le serveur lui-même,
> l'accès par IP fonctionne immédiatement.

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
# depuis le dossier d'installation
sudo docker compose logs -f oe.openelis.org
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

---

## 9. Accès local par nom `oeglobal.local` (DNS embarqué)

Chaque installation embarque un **serveur DNS local** (conteneur `dnsmasq`,
`network_mode: host`) qui résout `oeglobal.local` vers l'IP du serveur
(`SERVER_IP_ADDRESS`, demandée à l'install). But : les postes clients accèdent
tous à la **même URL** `https://oeglobal.local/` sans domaine public ni internet.

### Côté serveur
Rien à faire : le DNS et le certificat (SAN `oeglobal.local` + IP) sont générés à
l'installation. Vérifier le conteneur : `sudo docker compose ps` (service
`dnsmasq.openelis.org`).

### Côté postes clients — pointer le DNS vers le serveur
Chaque poste qui doit ouvrir `https://oeglobal.local/` doit utiliser le **serveur
OpenELIS comme DNS** (adresse = `SERVER_IP_ADDRESS`). Deux options :
- **Recommandé** : configurer l'option DHCP 6 (serveur DNS) du routeur/box du
  labo pour distribuer l'IP du serveur à tous les postes.
- **Manuel** : renseigner l'IP du serveur comme DNS dans la config réseau du poste.

Pour supprimer l'avertissement de certificat, importer le certificat
`/etc/openelis-global/nginx.cert.pem` dans le magasin de confiance des postes
(GPO en environnement Windows, ou import manuel).

### Dépannage DNS
| Symptôme | Piste |
|----------|-------|
| Le conteneur `dnsmasq` redémarre / port 53 occupé | Sur Ubuntu, `systemd-resolved` écoute sur `127.0.0.53`. dnsmasq est configuré avec `listen-address=<IP serveur>` + `bind-interfaces` pour **ne pas** entrer en conflit. Si le port 53 de l'IP LAN est déjà pris par un autre service, le libérer. |
| `oeglobal.local` inconnu depuis un poste | Le poste n'utilise pas le serveur comme DNS. Vérifier sa config DNS / l'option DHCP 6. Tester : `nslookup oeglobal.local <IP-serveur>`. |
| Accès OK par IP mais pas par nom | Idem : problème de résolution DNS côté client, pas côté serveur. |

> **Mono-site** : il n'y a pas de gestion multisite. Chaque serveur a son propre
> DNS et son propre certificat `oeglobal.local`. Pour changer l'IP du serveur
> après coup, éditer `/var/lib/openelis-global/config/SERVER_IP_ADDRESS`, puis
> relancer une mise à jour (régénère dnsmasq.conf) — ou supprimer le certificat
> pour qu'il soit régénéré avec la nouvelle IP dans son SAN.
