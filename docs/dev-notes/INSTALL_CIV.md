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
| **Mémoire (RAM)** | **8 Go recommandé**, **4 Go minimum strict**. La pile fait tourner plusieurs JVM (webapp Tomcat/Spring + FHIR HAPI) en plus de PostgreSQL ; sous 4 Go le conteneur FHIR ou webapp peut être tué par manque de mémoire (OOM) et redémarrer en boucle. Prévoir aussi un peu de **swap**. |
| Espace disque | ~10 Go libres (images + volumes DB) |
| CPU / architecture | amd64 (x86_64). ⚠ Exécuter le bundle amd64 sur un hôte **arm64** (VM de test Apple Silicon) passe par l'émulation → démarrage **très lent** (webapp/FHIR peuvent mettre 10-20 min) et empreinte mémoire accrue. Sur serveur amd64 réel : démarrage en quelques minutes. |
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
   **propre à cette installation**) pour le nom local `oeglobal.lan` (le SAN
   inclut aussi l'IP du serveur et `localhost`) ;
3. génère automatiquement la **clé de chiffrement** (persistée dans
   `/var/lib/openelis-global/config/ENCRYPTION_KEY`) — **⚠️ à sauvegarder : elle
   doit rester identique aux mises à jour, sinon les données chiffrées sont
   perdues** ;
4. **fuseau horaire par défaut** : `Africa/Abidjan` (modifiable) ;
5. demande uniquement les paramètres réellement propres au site :
   - **SITE_ID** (numéro de labo, 5 caractères) ;
   - **Mode de déploiement** : `[1] local` (défaut) ou `[2] en ligne` :
     - **local** → accès LAN par `oeglobal.lan`, DNS embarqué (dnsmasq), cert
       auto-signé (cas des centres/labos, voir §9) ;
     - **en ligne** → nom de domaine public, **pas** de dnsmasq, votre vrai
       certificat (voir §7). Demande alors le **nom de domaine** ;
   - *(mode local uniquement)* **SERVER_IP_ADDRESS** (IP LAN du serveur) :
     **auto-détectée** et proposée par défaut — Entrée pour l'accepter, ou saisir
     l'IP correcte si la machine a plusieurs interfaces (§9) ;
   - *(mode local uniquement)* **UPSTREAM_DNS** (DNS amont pour Internet) :
     **auto-détecté** (routeur du centre) et proposé par défaut. Entrée pour
     l'accepter ; laisser vide pour un site totalement isolé (§9) ;
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
Accès : `https://oeglobal.lan/` (ou `https://<IP-du-serveur>/`) — login admin
par défaut `admin` / `adminADMIN!` (**à changer immédiatement**). L'avertissement
navigateur (certificat auto-signé) est normal ; voir §7 pour un vrai certificat.

> Pour que `https://oeglobal.lan/` fonctionne depuis les **postes clients**, ils
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
| FHIR ou webapp reste `health: starting` très longtemps | D'abord distinguer **OOM** de **lenteur** : `docker inspect <ctn> --format '{{.State.OOMKilled}} {{.RestartCount}}'`. Si `true`/redémarrages → manque de RAM : porter à **8 Go** (§1) + swap. Si `false` + `0` redémarrage mais CPU élevé (`docker stats`) → le service **démarre juste lentement** (typique en **émulation arm64**, ou CPU/DB saturés) : patienter, ne pas conclure trop tôt ; un `free -h` qui swappe beaucoup aggrave la lenteur. |
| Sauvegarde de demande figée | vérifier que le conteneur FHIR répond (écriture FHIR synchrone à la création). |
| Page admin "Modifier les tests" en 500 | corrigé côté code (NPE SiteInformation) ; s'assurer d'être sur une version ≥ celle du fix. |
| Mot de passe admin / clé de chiffrement oubliés | voir `/var/lib/openelis-global/config/INSTALL_SUMMARY.txt`, `postgres_admin.password`, `ENCRYPTION_KEY`. |

Logs :
```bash
# depuis le dossier d'installation
sudo docker compose logs -f oe.openelis.org
```

---

## 7. Serveur en ligne (nom de domaine public + vrai certificat)

Choisir le mode **en ligne** à l'install (question « mode de déploiement ») :
- **aucun dnsmasq** n'est installé (le domaine est résolu par le DNS public) ;
- `server_name` nginx = votre domaine ; le certificat est généré pour le domaine.

### Utiliser votre vrai certificat
Deux façons, au choix :

**A. Le déposer AVANT l'install** (recommandé) — l'installeur détecte les PEM
présents et les conserve (il ne génère alors aucun cert) :
```bash
sudo mkdir -p /etc/openelis-global
sudo cp fullchain.pem /etc/openelis-global/nginx.cert.pem
sudo cp privkey.pem   /etc/openelis-global/nginx.key.pem
# puis lancer sudo ./install.sh en mode en ligne
```

**B. Le remplacer APRÈS coup** — si l'install a généré un cert auto-signé de
secours (avertissement navigateur en attendant) :
```bash
sudo cp fullchain.pem /etc/openelis-global/nginx.cert.pem
sudo cp privkey.pem   /etc/openelis-global/nginx.key.pem
sudo docker restart openelisglobal-proxy
```

Aucune reconfiguration du backend, du FHIR ou du compose : ils sont en HTTP
interne et ignorent le certificat. Si la vraie clé n'a **pas** de passphrase
(cas Let's Encrypt), retirer la ligne `ssl_password_file` de
`/var/lib/openelis-global/secrets/nginx.conf` avant de redémarrer.

> Le même mécanisme (déposer les 2 PEM) fonctionne aussi en **mode local** si
> vous disposez d'un cert pour `oeglobal.lan`.

---

## 8. Construire l'installeur (pour les mainteneurs)

### En local
```bash
git submodule update --init --recursive   # dataexport requis
./build-civ.sh                             # -> OEInstaller/linux/OpenELIS-Global_<version>_Installer.tar.gz
```
Options : `--images-only` (build images sans installeur), `VERSION=x.y.z.w` (forcer la version).

> **Plateforme** : les images sont construites/tirées en `linux/amd64` (cible =
> serveurs Linux amd64), même si la machine de build est arm64 (Mac Apple
> Silicon). Le `docker-compose` fixe aussi `platform: linux/amd64` sur chaque
> service. Pour cibler une autre architecture :
> `TARGET_PLATFORM=linux/arm64 ./build-civ.sh` (et adapter le `platform:` du template).

> ⚠️ **Tester le bundle amd64 dans une VM arm64** (ex. Ubuntu Parallels sur Mac
> Apple Silicon) : Docker Engine natif dans la VM n'a pas l'émulation QEMU, donc
> exécuter un binaire amd64 échoue avec `exec format error` (typiquement le
> conteneur `dnsmasq` : `exec /usr/local/bin/webproc: exec format error`).
> **C'est un artefact du test en VM arm64, pas un bug du bundle** : les serveurs
> de déploiement réels (amd64) exécutent nativement, sans émulation. Pour tester
> quand même dans la VM arm64, installer les handlers binfmt une fois :
> ```bash
> sudo docker run --privileged --rm tonistiigi/binfmt --install all
> sudo docker compose up -d   # relancer depuis le dossier de l'installer
> ```

### En CI (à venir — cf. CICD_STRATEGY_CIV.md)
Build automatique sur tag `v*` → images ghcr + installeur attaché à la **GitHub
Release** (téléchargeable). Voir la roadmap dans `CICD_STRATEGY_CIV.md`.

---

## 9. Accès local par nom `oeglobal.lan` (DNS embarqué)

Chaque installation embarque un **serveur DNS local** (conteneur `dnsmasq`,
`network_mode: host`) qui résout `oeglobal.lan` vers l'IP du serveur
(`SERVER_IP_ADDRESS`, demandée à l'install). But : les postes clients accèdent
tous à la **même URL** `https://oeglobal.lan/` sans domaine public ni internet.

### Forward Internet (les clients gardent l'accès au web)
Comme les postes clients utilisent ce DNS pour **toute** leur résolution (pas
seulement `oeglobal.lan`), dnsmasq **forwarde tout le reste** vers un **DNS
amont** — typiquement le **routeur du centre** (qui a Internet). Cette adresse
(`UPSTREAM_DNS`) est demandée à l'install : **auto-détectée** (passerelle par
défaut du serveur) et proposée par défaut ; appuyer sur Entrée pour l'accepter.

- **Renseigné** → `oeglobal.lan` en local, tout le reste vers le routeur : les
  clients conservent Internet. C'est le cas normal d'un centre connecté.
- **Laissé vide** → **mode isolé** : dnsmasq ne répond QUE `oeglobal.lan` (option
  `no-resolv`, aucune sortie DNS). À réserver aux zones totalement déconnectées où
  les clients n'ont de toute façon pas d'Internet.

> Modifier après coup : éditer `/var/lib/openelis-global/config/UPSTREAM_DNS`
> (une IP, ou vide pour le mode isolé) puis relancer une mise à jour (régénère
> `dnsmasq.conf`).

### Côté serveur
Rien à faire : le DNS et le certificat (SAN `oeglobal.lan` + IP) sont générés à
l'installation. Vérifier le conteneur : `sudo docker compose ps` (service
`dnsmasq.openelis.org`).

### Côté postes clients — pointer le DNS vers le serveur
Chaque poste qui doit ouvrir `https://oeglobal.lan/` doit utiliser le **serveur
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
| `oeglobal.lan` inconnu depuis un poste | Le poste n'utilise pas le serveur comme DNS. Vérifier sa config DNS / l'option DHCP 6. Tester : `nslookup oeglobal.lan <IP-serveur>`. |
| Accès OK par IP mais pas par nom | Idem : problème de résolution DNS côté client, pas côté serveur. |
| Les postes clients n'ont plus Internet | `UPSTREAM_DNS` vide (mode isolé) ou incorrect : dnsmasq ne forwarde pas. Renseigner l'IP du routeur dans `/var/lib/openelis-global/config/UPSTREAM_DNS` puis relancer une mise à jour. Tester : `nslookup google.com <IP-serveur>`. |

> **Mono-site** : il n'y a pas de gestion multisite. Chaque serveur a son propre
> DNS et son propre certificat `oeglobal.lan`. Pour changer l'IP du serveur
> après coup, éditer `/var/lib/openelis-global/config/SERVER_IP_ADDRESS`, puis
> relancer une mise à jour (régénère dnsmasq.conf) — ou supprimer le certificat
> pour qu'il soit régénéré avec la nouvelle IP dans son SAN.
