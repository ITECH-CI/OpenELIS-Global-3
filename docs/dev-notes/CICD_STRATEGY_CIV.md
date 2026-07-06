# Stratégie CI/CD — fork CIV (branche `develop-civ`)

> Analyse de l'existant et plan de mise en place d'un pipeline CI/CD pour
> simplifier builds, installations et mises à jour multi-sites (dont zones à
> connexion limitée).

## 1. État des lieux (constaté dans le repo)

### Architecture de build
- **Backend** : WAR `OpenELIS-Global.war`, Maven, JDK 21. Version `3.3.1.0`
  définie dans `pom.xml` (`major.minor.state.fix` = `3.3.1.0`).
- **Frontend** : React **séparé** (PAS de `frontend-maven-plugin` → non packagé
  dans le WAR). Image Docker propre (`frontend/Dockerfile.prod`).
- **Images custom CIV** : backend + frontend uniquement.
- **Images upstream non touchées** : `postgres:14.4`, FHIR (HAPI), nginx-proxy,
  certgen, autoheal. (confirmé dans `docker-compose.yml` : tout en `itechuw/*`).

### Build manuel actuel (`oe-build-export.sh`)
- Build backend + frontend via `build.docker-compose.yml`.
- `docker save | gzip` → `*.tar.gz` transportés à la main → `docker load` sur
  le serveur. Aucun registry, pas de multi-arch, pas d'automatisation.

### Installeur upstream (`build.sh -i`) — IMPORTANT
- Assemble un installeur **100% offline** : embarque TOUTES les images en
  `.tar.gz` (OpenELIS, frontend, postgres, FHIR, autoheal, nginx) + template
  (`install/installerTemplate/linux/`) + scripts (`setupDocker.sh`,
  `configurePrimary.sh`, `setup_OpenELIS.py`, initDB, nginx.conf, compose…).
- Sortie : `OEInstaller/linux/<context>_<version>_Installer.tar.gz`.
- ⇒ **C'est le bon véhicule** pour des installations multi-sites en zones à
  connexion limitée.

### Workflows GitHub hérités (`.github/workflows/`)
| Workflow | État sur le fork |
|----------|------------------|
| `ci.yml` (build backend + couverture) | ✅ tourne (pas de garde repo) |
| `frontend-qa.yml` (Cypress) | ✅ réutilisable |
| `publish-and-test.yml` (images prod) | ❌ gardé `DIGI-UW` + `uses:` hardcodés |
| `publish-dev-backend-images.yml` | ❌ gardé `DIGI-UW` |
| `publish-dev-frontend-images.yml` | ❌ gardé `DIGI-UW` |
| `build-installer.yml` | ❌ `build.sh -ib develop` codé en dur |
| `label-merge-conflict.yml` | ❌ gardé `DIGI-UW` |
| `tx-pull.yml` / `tx-push.yml` | ❌ gardé `DIGI-UW` + Transifex upstream |

**Problème central** : presque tous gardés par
`if: github.repository == 'DIGI-UW/OpenELIS-Global-2'` → **skippés** sur
`ITECH-CI/OpenELIS-Global-3`. Et tous ciblent `develop`, pas `develop-civ`.

## 2. Décisions retenues
- **Registry** : GitHub Container Registry (`ghcr.io/itech-ci/...`) — auth via
  `GITHUB_TOKEN`, zéro secret à gérer.
- **Déploiement** : **installeur `.tar.gz` autonome** (offline) comme livrable
  principal (multi-sites, connexions limitées). Images ghcr en complément pour
  les updates en ligne quand c'est possible.
- **Déclenchement** : push `develop-civ` (image `:develop-civ`/`:latest-dev`) +
  tags git `v*` (release versionnée + installeur attaché à la GitHub Release).

## 3. Pipeline cible

```
 push develop-civ ─► [CI] build+test backend (mvn) + frontend (npm/cypress)
                      └─► build & push images ghcr  :develop-civ
 tag v3.3.x.x    ─► [RELEASE] build images ghcr  :3.3.x.x + :latest
                      └─► build installeur offline .tar.gz
                           └─► GitHub Release (asset .tar.gz)
```

### Sur site (mise à jour)
- **En ligne** : `docker compose pull && docker compose up -d` (compose pointant
  vers `ghcr.io/itech-ci/...`).
- **Offline** : télécharger l'installeur de la Release, le transférer, exécuter
  (les images sont dans le tar → `docker load` local, pas de pull réseau).

## 4. Plan d'implémentation (incrémental)

### Étape 1 — CI de base sur `develop-civ` (rapide, sans secret)
- Adapter `ci.yml` : ajouter `develop-civ` aux triggers ; moderniser les
  actions (`checkout@v4`, `setup-java@v4`). Garde la couverture.
- Adapter `frontend-qa.yml` pour `develop-civ`.
- **Effet** : chaque push valide build backend + lint/tests frontend.

### Étape 2 — Publier les images sur ghcr.io
- Nouveau workflow `civ-publish-images.yml` :
  - trigger : push `develop-civ` (+ `workflow_dispatch`).
  - login ghcr via `GITHUB_TOKEN` (`packages: write`).
  - build+push `ghcr.io/itech-ci/openelis-global-civ` (backend, `Dockerfile`)
    et `...-frontend-civ` (`frontend/Dockerfile.prod`), tag `:develop-civ` +
    `:sha-xxxx`. Multi-arch amd64/arm64 optionnel (coût CI).
- **Effet** : images CIV disponibles au pull, fin du transport de tar pour les
  sites connectés.

### Étape 3 — Versioning + Release + Installeur
- Nouveau workflow `civ-release.yml` :
  - trigger : tag `v*` (ex. `v3.3.1.0`).
  - build+push images ghcr taggées `:3.3.1.0` + `:latest`.
  - adapter/relancer la logique `build.sh -i` (sans `-b develop` hardcodé :
    builder depuis le checkout courant du tag) pour produire l'installeur
    offline, puis l'attacher à la **GitHub Release** (`softprops/action-gh-release`).
- **Effet** : une release = images + installeur offline reproductibles.

### Étape 4 — Compose de prod CIV
- Dupliquer `docker-compose.yml` → `docker-compose.civ.yml` pointant vers
  `ghcr.io/itech-ci/...` au lieu de `itechuw/*` pour backend+frontend (garder
  les images upstream pour fhir/proxy/db/certs).
- Documenter la procédure update en ligne / offline.

### Étape 5 — Nettoyage workflows hérités
- Désactiver/retirer les workflows upstream non pertinents (transifex,
  publish-and-test gardé DIGI-UW) pour clarifier l'onglet Actions.

## 5. Points d'attention
- **Submodule `dataexport`** : `ci.yml` le build (`mvn clean install`). Tout
  workflow build backend doit `checkout submodules: recursive` + builder
  `dataexport` avant le WAR.
- **`build.sh`** suppose des images nommées `openelisglobal:latest`,
  `openelisglobal-frontend:latest`, `hapi-fhir-jpaserver:latest`,
  `nginx-proxy`. À aligner avec les noms produits par le CI avant de générer
  l'installeur.
- **Multi-arch** : le serveur cible est probablement amd64 ; arm64 (Mac dev)
  optionnel. Limiter à amd64 réduit le temps CI.
- **Secrets** : ghcr n'en demande aucun (GITHUB_TOKEN). Si Transifex est voulu
  côté fork, recréer les tokens ; sinon désactiver tx-*.

## 6. TODO — tâches actionnables

> Statut actuel : build/release **manuels** (via `oe-build-export.sh`). Objectif :
> notre propre installeur CIV + CI/CD ghcr. À implémenter sur `develop-civ`.

### Lot A — CI de base (étape 1) + images ghcr (étape 2) — *à faire ensemble*
- [ ] `ci.yml` : ajouter `develop-civ` aux triggers `on.push`/`pull_request` ;
      moderniser `actions/checkout@v4`, `actions/setup-java@v4` (JDK 21) ;
      conserver `submodules: recursive` + build `dataexport` avant le WAR.
- [ ] `frontend-qa.yml` : ajouter `develop-civ` aux triggers (lint + Cypress).
- [ ] Nouveau `.github/workflows/civ-publish-images.yml` :
  - trigger `push: [develop-civ]` + `workflow_dispatch`.
  - `permissions: packages: write` ; login ghcr via `GITHUB_TOKEN`.
  - build+push `ghcr.io/itech-ci/openelis-global-civ` (backend `Dockerfile`,
    `build-args: SKIP_SPOTLESS=true`) tag `:develop-civ` + `:sha-<short>`.
  - build+push `ghcr.io/itech-ci/openelis-global-civ-frontend`
    (`frontend/Dockerfile.prod`) mêmes tags.
  - amd64 seul d'abord (ajouter arm64 plus tard si besoin Mac dev).
- [ ] Rendre les packages ghcr **publics** (ou gérer l'auth de pull sur site).

### Lot B — Notre propre installeur CIV (étape 3)
- [ ] Forker/adapter `build.sh` → `build-civ.sh` (ou param `-b $REF`) :
  - retirer le `develop` codé en dur ; builder depuis le checkout courant.
  - aligner les noms d'images attendus par `build.sh`
    (`openelisglobal:latest`, `openelisglobal-frontend:latest`,
    `hapi-fhir-jpaserver:latest`, `nginx-proxy`) avec ce que produit le build CIV.
- [ ] Adapter le template `install/installerTemplate/linux/` :
  - `templates/docker-compose.yml` : pointer backend+frontend vers les images
    CIV (chargées en local depuis les tar de l'installeur), garder
    fhir/proxy/db/certs upstream.
  - vérifier `setup_OpenELIS.py`, `setupDocker.sh`, `configurePrimary.sh`,
    `initDB/OpenELIS-Global.sql` (schéma à jour avec nos migrations 3.2.x.x).
- [ ] Nouveau `.github/workflows/civ-release.yml` :
  - trigger `push: tags: ['v*']`.
  - build+push images ghcr `:<version>` + `:latest`.
  - exécuter `build-civ.sh -i` → produire l'installeur offline `.tar.gz`.
  - attacher à la **GitHub Release** (`softprops/action-gh-release`).

### Lot C — Compose prod CIV + doc (étape 4)
- [ ] `docker-compose.civ.yml` : backend+frontend = `ghcr.io/itech-ci/...`,
      le reste upstream. Procédure update **en ligne** (`pull && up -d`).
- [ ] `docs/dev-notes/INSTALL_CIV.md` : install offline (depuis l'installeur)
      + update en ligne (depuis ghcr), pas à pas pour les opérateurs site.

### Lot D — Nettoyage (étape 5)
- [ ] Désactiver/retirer workflows hérités non pertinents pour le fork
      (`tx-pull`, `tx-push`, `publish-and-test` gardé DIGI-UW,
      `publish-dev-*-images` gardés DIGI-UW, `label-merge-conflict`).
- [ ] `oe-build-export.sh` : garder tant que le CI n'est pas en place, retirer
      une fois `civ-publish-images.yml` opérationnel.

### Pré-requis / décisions ouvertes
- [ ] Confirmer le namespace ghcr exact (`itech-ci` vs autre) + visibilité.
- [ ] Confirmer l'arch cible des serveurs (amd64 ?) pour limiter le multi-arch.
- [ ] Vérifier que `initDB/OpenELIS-Global.sql` de l'installeur reflète bien le
      schéma actuel (sinon une install from scratch partirait d'un schéma périmé).
