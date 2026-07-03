# Conception — Installeur CIV simplifié (SSL allégé + auto-install)

> Objectif : une installation **fiable, minimale, quasi "un clic"** pour du
> personnel MOH non-technique, sur des sites d'extension (souvent zones à
> connexion limitée). Trois axes : (A) alléger le SSL, (B) tout automatiser,
> (C) Docker en prérequis documenté.
>
> Ce document est la CONCEPTION à valider avant implémentation. Rien n'est
> encore codé.

---

## Axe A — SSL simplifié : TLS uniquement à la porte d'entrée

### Principe
Les conteneurs communiquent sur un **réseau Docker privé isolé** (bridge, non
exposé). Le TLS interne (backend Tomcat 8443, FHIR 8443 mTLS) n'apporte donc
**aucune sécurité réelle** — juste de la complexité (keystores, truststores,
mots de passe, certs qui expirent, conteneur `certs` qui boucle).

### Cible
```
Internet/LAN ──HTTPS(443)──► nginx ──┬── http://frontend:80        (déjà en clair)
   (cert auto-signé 10 ans)          ├── http://oe:8080           (était https:8443)
                                     └── (oe) ── http://fhir:8080  (était https:8443)
```
- **nginx** : garde HTTPS en entrée, cert **auto-signé 10 ans** (au lieu de 365 j).
- **backend / FHIR** : écoutent en **HTTP clair** sur le réseau interne.
- **Suppression** : keystore, truststore, client_facing_keystore internes.

### Points de changement (cartographie validée — 10 items)

| # | Fichier | Action |
|---|---------|--------|
| 1 | `templates/oe_server.xml` | Connecteur HTTPS 8443 → **HTTP 8080**, retirer keystore/truststore |
| 2 | `templates/hapi_server.xml` | Connecteur HTTPS 8443 → **HTTP 8080**, retirer keystore/truststore/clientAuth |
| 3 | `templates/nginx.conf` | `proxy_pass https://oe:8443` → **`http://oe:8080`** (garder `ssl_certificate` d'entrée) |
| 4 | `templates/docker-compose.yml` | Retirer ports 8443/8444, secrets keystore/truststore/client_facing_keystore (services oe & fhir + bloc global), bloc `JAVA_OPTS` TLS du FHIR |
| 5 | `setup_OpenELIS.py` L112 | `LOCAL_FHIR_SERVER_ADDRESS` → **`http://fhir.openelis.org:8080/fhir/`** |
| 6 | `templates/common.properties` | Retirer `server.ssl.*` (key-store/trust-store) |
| 7 | **`HttpClientConfig.java`** L63-71 | **Rendre keystore/truststore optionnels** — sinon crash Spring au boot sans keystore. ⚠️ **Seul vrai code Java.** |
| 8 | `install/openelis_healthcheck.sh` L13 | `https://oe:8443/...` → `http://oe:8080/...` |
| 9 | `templates/healthcheck.sh` (FHIR) L13 | HTTPS+cert P12 → `http://fhir:8080/...` |
| 10 | `frontend/Dockerfile.certs` + `create_nginx_certs()` | Générer **directement** une paire `nginx.cert.pem`/`nginx.key.pem` auto-signée **10 ans**, supprimer keystore/truststore/client_facing_keystore |

### ⚠️ Verrou critique — `HttpClientConfig.java`
`sslContext()` (L67-71) appelle `loadKeyMaterial`/`loadTrustMaterial` de façon
**inconditionnelle** → si on retire les secrets keystore sans adapter ce code,
le backend **ne démarre plus**. Ce client sert aux appels sortants (FHIR local,
mais aussi Odoo, recherche patient externe, DataSubmitter…).

**Solution** : rendre le chargement SSL **conditionnel** — si `server.ssl.key-store`
/`trust-store` sont absents/non définis, construire un `CloseableHttpClient`
standard (sans mTLS). Les appels FHIR passant désormais en HTTP interne, le mTLS
n'est plus requis. Les appels sortants HTTPS externes (serveur consolidé) restent
possibles via le truststore système par défaut de la JVM.

### Cert d'entrée auto-signé 10 ans
- Générer une paire `nginx.cert.pem` + `nginx.key.pem` (RSA 2048, `-days 3650`,
  SAN incluant `*.openelis.org` + hostname/IP du serveur).
- Où : soit dans `frontend/Dockerfile.certs` simplifié, soit dans une fonction
  `create_nginx_certs()` de `setup_OpenELIS.py` (préféré : pas de dépendance à
  une image, généré à l'install sur le serveur avec son vrai hostname).
- nginx continue de lire `cert.crt`/`cert.key`/`key_pass` (inchangé côté conf).
- Navigateur : avertissement auto-signé (normal, réseau interne/labo).

### ✅ Passage à un VRAI certificat (serveur en ligne, nom de domaine)
Cette archi est **conçue pour ça** : le certificat est isolé sur nginx uniquement.
Pour un serveur public avec un vrai cert (Let's Encrypt, cert acheté), il suffit
de **remplacer 2 fichiers** que nginx lit, puis redémarrer le proxy :
```
/etc/openelis-global/nginx.cert.pem   ← vrai certificat (fullchain)
/etc/openelis-global/nginx.key.pem    ← vraie clé privée
docker restart openelisglobal-proxy
```
**Aucune reconfiguration** du backend, du FHIR, du compose ou des mots de passe :
ils sont en HTTP interne et ignorent totalement le certificat. C'est l'avantage
majeur vs l'archi actuelle où le cert est dérivé dans un keystore PKCS12 partagé
entre 3 conteneurs (remplacement = régénérer keystore+truststore+reconfig ×3).

Note d'implémentation : prévoir que nginx accepte une clé **avec OU sans**
passphrase (`ssl_password_file` optionnel) — un vrai cert Let's Encrypt a une clé
sans passphrase. Option future (non requise) : mode Let's Encrypt auto (Caddy /
certbot) pour les sites à domaine public ; le design "2 PEM à remplacer" couvre
déjà ce besoin manuellement.

---

## Axe B — Tout automatiser (installation quasi "un clic")

### Cible : minimiser les questions et créer TOUT automatiquement

**Créés automatiquement par l'installeur** (déjà en partie fait par setup.py) :
- dossiers `/etc/openelis-global/`, `/var/lib/openelis-global/{backup,secrets,plugins,config,initDB}`
- volumes Docker (via `docker compose up`)
- mots de passe (clinlims, admin postgres, backup) — générés aléatoirement
- **cert nginx auto-signé 10 ans** (nouveau — plus de prérequis certs manuel !)
- config (compose, properties, cron backup)

**Valeurs par défaut sensées** (pour supprimer/réduire les questions) :
| Paramètre | Défaut proposé | Reste demandé ? |
|-----------|----------------|-----------------|
| SITE_ID | à saisir (identifie le labo) | **Oui** (essentiel, unique par site) |
| TZ | `Africa/Abidjan` (CIV) par défaut | Non (override possible dans setup.ini) |
| ENCRYPTION_KEY | généré aléatoirement à la 1ère install, **persisté** | Non (mais sauvegardé/affiché une fois) |
| KEYSTORE/TRUSTSTORE_PASSWORD | supprimés (plus de keystores internes) | Non |
| REMOTE_FHIR / CS_SERVER | vide par défaut (mono-site) | Non (setup.ini si besoin) |
| FHIR_IDENTIFIER | dérivé du SITE_ID | Non |

**Résultat visé** : `sudo ./install.sh` → 1 seule vraie question (SITE_ID), le
reste automatique. Un mot de passe admin affiché + sauvegardé.

### Wrapper `install.sh` (nouveau — ergonomie MOH)
Un script d'amorçage à la racine de l'installeur qui :
1. vérifie les prérequis (Docker, root, espace disque) et **échoue avec un
   message clair** sinon ;
2. lance `python3 setup_OpenELIS.py -m update-install` ;
3. affiche à la fin l'URL d'accès + identifiants + emplacement des mots de passe.

### ⚠️ ENCRYPTION_KEY — point de vigilance
Elle **doit rester identique** entre installs/updates d'un même site (sinon
données chiffrées illisibles). Génération auto à la 1ère install + persistée
dans `/var/lib/openelis-global/config/` + **affichée + sauvegardée** dans un
fichier de récap. À documenter fortement pour les updates/réinstalls.

---

## Axe C — Docker en prérequis (documenté)

Décision : **ne pas** embarquer les .deb Docker (fragile, dépend OS/version).
- L'installeur **vérifie** la présence de Docker + `docker compose` et échoue
  proprement avec la commande d'install à copier-coller si absent.
- La doc d'install (`INSTALL_CIV.md`) fournit la procédure d'installation Docker
  pour Ubuntu/Debian (à faire une fois, avec connexion, avant le déploiement
  offline de l'app).

---

## Plan d'implémentation (ordre proposé)

**Lot 1 — Code Java (le verrou)**
- [ ] `HttpClientConfig.java` : chargement SSL conditionnel (keystore optionnel).
- [ ] Vérifier build + démarrage backend sans keystore (test local).

**Lot 2 — Templates TLS interne → HTTP**
- [ ] `oe_server.xml`, `hapi_server.xml` : connecteurs HTTP 8080.
- [ ] `nginx.conf` : proxy_pass http://oe:8080.
- [ ] `docker-compose.yml` (template) : retirer ports/secrets/JAVA_OPTS TLS internes.
- [ ] `common.properties` : retirer server.ssl.*.
- [ ] `healthcheck.sh` (FHIR) + `openelis_healthcheck.sh` : HTTP.
- [ ] `setup_OpenELIS.py` : LOCAL_FHIR_SERVER_ADDRESS en http.

**Lot 3 — Cert nginx auto-signé 10 ans**
- [ ] `create_nginx_certs()` (ou Dockerfile.certs) : paire pem auto-signée 10 ans,
      découplée des keystores. Supprimer génération keystore/truststore.

**Lot 4 — Auto-install + ergonomie**
- [ ] Valeurs par défaut dans setup.ini + setup_OpenELIS.py (TZ, ENCRYPTION_KEY auto…).
- [ ] Wrapper `install.sh` (prérequis + lancement + récap).
- [ ] `build-civ.sh` : inclure `install.sh` dans l'installeur.

**Lot 5 — Validation bout-en-bout**
- [ ] Rebuild installeur (`build-civ.sh`), test install propre sur VM/serveur test.
- [ ] Vérifier : accès HTTPS entrée OK, backend↔FHIR HTTP OK, saisie/validation/
      rapport OK, backup cron OK.
- [ ] Mettre à jour `INSTALL_CIV.md` (plus de prérequis certs, install simplifiée).

---

## Risques / points ouverts
- **HttpClientConfig** sert aussi aux appels HTTPS externes (serveur consolidé) :
  s'assurer que le client "sans keystore" gère quand même le TLS sortant standard.
- **dev.docker-compose.yml** : à aligner sur la nouvelle archi (ou garder l'ancien
  pour le dev ? à décider — le dev n'est pas commité).
- **Compat données existantes** : sites déjà installés en TLS interne → l'update
  régénère compose/config, donc bascule en HTTP interne au prochain update. À tester.
- **Réplication (configurePrimary/Secondary)** : non impactée par le TLS applicatif
  (c'est du postgres streaming), mais à re-vérifier.
