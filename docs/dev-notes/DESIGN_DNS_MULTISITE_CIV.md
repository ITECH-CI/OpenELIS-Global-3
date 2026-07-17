# Conception — DNS local `oeglobal.local` + dnsmasq (déploiement multi-sites)

> Objectif : chaque site isolé accède à la **même URL** >
> `https://oeglobal.local`, sans domaine public ni internet, via un DNS local
> embarqué (dnsmasq) qui résout `*.oeglobal.local` vers l'IP du serveur du site.
>
> Basé sur la stratégie infra réseau + analyse d'impact du code. À implémenter
> APRÈS validation du test VM de l'installeur SSL-simplifié.

## Décisions actées

- **Noms internes `*.openelis.org` conservés** (conteneurs
  oe/fhir/frontend/proxy sur le réseau Docker privé `openelis-network`) —
  invisibles des clients, NON migrés.
- **`oeglobal.local`** = accès externe (clients) + certificat nginx uniquement.
- **dnsmasq intégré au docker-compose** de l'installeur (pas une couche
  séparée).
- **Aucun code Java impacté** (les URLs `*.openelis.org` du code sont des
  namespaces d'identifiants FHIR, pas des noms résolus par DNS).

## Ce qui marche DÉJÀ (bonne surprise)

- `nginx.conf` a un seul server block `default` par port → il sert **tout
  Host**, y compris `oeglobal.local`. Le redirect 80→443 utilise `$host` →
  préserve le domaine client. **Aucun changement fonctionnel nginx requis**
  (juste cosmétique).
- `create_nginx_certs()` est **idempotent** (skip si cert présent) → permet de
  pré-déposer un cert wildcard partagé.

## Changements nécessaires (délimités)

### 1. Certificat — étendre le SAN (`setup_OpenELIS.py::create_nginx_certs`, ~L1358-1359)

- SAN cible :
  `DNS:*.openelis.org,DNS:*.oeglobal.local,DNS:oeglobal.local,DNS:localhost`
- CN → `oeglobal.local`
- ⚠️ Idempotence : sur un site déjà installé (ancien cert `*.openelis.org`), le
  nouveau SAN ne s'applique QUE si on supprime `nginx.cert.pem`/`.key.pem`
  avant.

**Cert wildcard PARTAGÉ vs PAR-SITE** :

- Recommandé : **wildcard `*.oeglobal.local` partagé**, généré une fois,
  pré-déposé dans `/etc/openelis-global/` avant l'install (l'idempotence le
  préserve). Un seul cert à approuver côté clients de tous les sites.
- Compromis : clé privée dupliquée sur chaque serveur (compromission d'un =
  fuite pour tous). Acceptable en réseau isolé sans internet. À documenter.

### 2. nginx.conf template (cosmétique, recommandé)

- L16 `server_name __;` → `server_name oeglobal.local *.oeglobal.local;`
- L33 `X-Forwarded-Host $server_name;` → cohérent avec oeglobal.local
- Pas de changement de code (`create_nginx_files` copie verbatim).

### 3. SITE_IP — nouvelle valeur demandée à l'install

Répliquer EXACTEMENT le pattern `SITE_ID` (`setup_OpenELIS.py`) :

- globale `SITE_IP = ''`
- trio `is_site_ip_set()` / `get_site_ip()` / `set_site_ip()` (calqués
  L1079-1099), persisté dans `CONFIG_DIR + 'SITE_IP'`
- appel `get_set_site_ip()` dans `get_stored_user_values()` (L1008-1022)

### 4. Template dnsmasq — nouveau `templates/dnsmasq.conf`

```
address=/oeglobal.local/[% site_ip %]
listen-address=[% site_ip %]
bind-interfaces
local-service
```

(`address=/oeglobal.local/IP` couvre le domaine ET tous ses sous-domaines.)

### 5. Génération config dnsmasq (`create_dnsmasq_files()`)

Nouvelle fonction calquée sur `create_nginx_files()` (L483-494) : lit le
template, substitue `[% site_ip %]`, écrit dans `SECRETS_DIR`. Appelée depuis
`install_files_from_templates()` (L269-279).

### 6. Service dnsmasq dans docker-compose template (~L161)

```yaml
  dnsmasq.openelis.org:
    container_name: [% dnsmasq_name %]
    image: jpillora/dnsmasq
    network_mode: host          # doit écouter sur l'IP LAN du serveur (clients)
    cap_add:
      - NET_ADMIN
    volumes:
      - /var/lib/openelis-global/secrets/dnsmasq.conf:/etc/dnsmasq.conf:ro
    restart: always
    logging: *local-logging
```

- `network_mode: host` **obligatoire** (le DNS doit être joignable par les
  postes clients sur l'IP LAN, pas seulement depuis le réseau Docker interne).
  Incompatible avec l'attachement à `openelis-network` — non gênant (dnsmasq ne
  parle pas aux autres conteneurs).
- Substitutions `[% dnsmasq_name %]` + `[% site_ip %]` dans
  `create_docker_compose_file()`.

## Risques et mitigations

| Risque                                            | Mitigation                                                                                              |
| ------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| **Port 53 vs systemd-resolved** (Ubuntu)          | `listen-address=<SITE_IP>` + `bind-interfaces` (ne pas binder 127.0.0.53). Ne pas toucher l'hôte.       |
| **Cert wildcard partagé** = clé dupliquée         | Acceptable en réseau isolé ; documenter. Alternative par-site si besoin de sécurité accrue.             |
| **Clients doivent pointer leur DNS vers SITE_IP** | DHCP option 6 (routeur) ou config manuelle. Cœur de la stratégie — à documenter par site.               |
| **Cert auto-signé non approuvé**                  | Distribuer/importer le cert `*.oeglobal.local` dans le magasin de confiance des clients (GPO / manuel). |
| **Idempotence cert sur sites existants**          | Procédure : supprimer les .pem pour régénérer avec le nouveau SAN.                                      |
| **dnsmasq open resolver** (host network)          | `bind-interfaces` + `local-service` + `listen-address`.                                                 |

## Plan d'intégration (ordre)

1. Cert : étendre SAN + CN (`create_nginx_certs`). Décider partagé/par-site.
2. nginx.conf : server_name oeglobal.local (cosmétique).
3. SITE_IP : globale + trio is/get/set + appel dans get_stored_user_values.
4. `templates/dnsmasq.conf` : créer.
5. `create_dnsmasq_files()` : nouvelle fonction + appel dans
   install_files_from_templates.
6. Compose : service dnsmasq + substitutions.
7. Doc : clients→DNS SITE_IP, import cert, mitigation systemd-resolved,
   migration sites existants.

## Fichiers concernés

- `install/installerTemplate/linux/setup_OpenELIS.py` (pattern config
  L1008-1099, pipeline L269-279, nginx files L483-494, cert L1342-1370, compose
  L292-356)
- `install/installerTemplate/linux/templates/docker-compose.yml` (~L161, réseau
  L229)
- `install/installerTemplate/linux/templates/nginx.conf` (L16, L33)
- **nouveau** `install/installerTemplate/linux/templates/dnsmasq.conf`
