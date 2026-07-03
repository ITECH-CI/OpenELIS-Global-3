#!/usr/bin/env bash
#
# build-civ.sh — Construit l'installeur offline OpenELIS-Global (fork CIV)
#                à partir du CODE COURANT (checkout actuel), sans toucher à git.
#
# Contrairement au build.sh upstream, ce script :
#   - ne fait AUCUN git checkout/pull destructif (build ce qui est là) ;
#   - vérifie ses prérequis (docker, submodule dataexport) et échoue proprement ;
#   - produit un installeur .tar.gz autonome (images embarquées) prêt à déployer
#     en zone déconnectée.
#
# Usage :
#   ./build-civ.sh                 # build complet + installeur
#   ./build-civ.sh --images-only   # build les images seulement (pas d'installeur)
#   VERSION=3.3.1.0 ./build-civ.sh # forcer la version (sinon lue depuis pom.xml)
#
# Prérequis sur la machine de build :
#   - docker (avec buildx) et docker compose
#   - le submodule dataexport initialisé (git submodule update --init --recursive)
#
set -euo pipefail

# --- Localisation ---
SCRIPT_DIR="$(cd -P "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
PROJECT_DIR="${SCRIPT_DIR}"
INSTALL_DIR="${PROJECT_DIR}/install"
TEMPLATE_DIR="${INSTALL_DIR}/installerTemplate/linux"
INSTALLER_CREATION_DIR="${PROJECT_DIR}/OEInstaller"

# --- Options ---
IMAGES_ONLY=false
for arg in "$@"; do
  case "$arg" in
    --images-only) IMAGES_ONLY=true ;;
    -h|--help) grep '^#' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Option inconnue: $arg" >&2; exit 1 ;;
  esac
done

log() { echo "[build-civ] $*"; }
fail() { echo "[build-civ][ERREUR] $*" >&2; exit 1; }

# --- Prérequis ---
command -v docker >/dev/null 2>&1 || fail "docker introuvable. Installez Docker."
docker compose version >/dev/null 2>&1 || fail "docker compose introuvable."
if [ ! -f "${PROJECT_DIR}/dataexport/pom.xml" ]; then
  fail "submodule 'dataexport' non initialisé. Lancez: git submodule update --init --recursive"
fi

# --- Version (depuis pom.xml sauf override) ---
if [ -z "${VERSION:-}" ]; then
  log "Lecture de la version depuis pom.xml…"
  VERSION="$(cd "${PROJECT_DIR}" && mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' \
    --non-recursive exec:exec 2>/dev/null || true)"
  # Fallback si le plugin exec n'est pas dispo : help:evaluate
  if [ -z "${VERSION}" ]; then
    VERSION="$(cd "${PROJECT_DIR}" && mvn -q help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null | tail -1)"
  fi
fi
[ -n "${VERSION}" ] || fail "Impossible de déterminer la version (essayez VERSION=x.y.z.w ./build-civ.sh)"
log "VERSION=${VERSION}"

# --- Noms d'images (doivent matcher docker-compose.yml du template et setup_OpenELIS.py) ---
IMG_BACKEND="openelisglobal"
IMG_FRONTEND="openelisglobal-frontend"
IMG_FHIR="hapi-fhir-jpaserver"
IMG_NGINX="nginx-proxy"
IMG_POSTGRES="postgres:14.4"
IMG_AUTOHEAL="willfarrell/autoheal:1.2.0"

# ============================================================
# 1) Build des images à partir du code courant
# ============================================================
log "Build backend (${IMG_BACKEND}) — WAR + dataexport dans le Dockerfile multi-stage"
docker build -f "${PROJECT_DIR}/Dockerfile" \
  --build-arg SKIP_SPOTLESS=true \
  -t "${IMG_BACKEND}:latest" "${PROJECT_DIR}"

log "Build frontend (${IMG_FRONTEND}) — Dockerfile.prod (npm build -> nginx)"
docker build -f "${PROJECT_DIR}/frontend/Dockerfile.prod" \
  -t "${IMG_FRONTEND}:latest" "${PROJECT_DIR}/frontend"

log "Build FHIR (${IMG_FHIR})"
docker build -f "${PROJECT_DIR}/fhir/Dockerfile" \
  -t "${IMG_FHIR}:latest" "${PROJECT_DIR}/fhir"

log "Build nginx-proxy (${IMG_NGINX})"
docker build -f "${PROJECT_DIR}/nginx-proxy/Dockerfile" \
  -t "${IMG_NGINX}:latest" "${PROJECT_DIR}/nginx-proxy"

log "Pull des images tierces (postgres, autoheal)"
docker pull "${IMG_POSTGRES}"
docker pull "${IMG_AUTOHEAL}"

if [ "${IMAGES_ONLY}" = true ]; then
  log "Images construites (--images-only). Fin."
  exit 0
fi

# ============================================================
# 2) Export des images en .tar.gz (noms attendus par l'installeur)
# ============================================================
log "Export des images (docker save | gzip)…"
cd "${PROJECT_DIR}"
docker save "${IMG_BACKEND}:latest"  | gzip > OpenELIS-Global_DockerImage.tar.gz
docker save "${IMG_FRONTEND}:latest" | gzip > ReactFrontend_DockerImage.tar.gz
docker save "${IMG_FHIR}:latest"     | gzip > JPAServer_DockerImage.tar.gz
docker save "${IMG_NGINX}:latest"    | gzip > NGINX_DockerImage.tar.gz
docker save "${IMG_POSTGRES}"        | gzip > Postgres_DockerImage.tar.gz
docker save "${IMG_AUTOHEAL}"        | gzip > AutoHeal_DockerImage.tar.gz

# ============================================================
# 3) Assemblage de l'installeur
# ============================================================
INSTALLER_NAME="OpenELIS-Global_${VERSION}_Installer"
DEST="${INSTALLER_CREATION_DIR}/linux/${INSTALLER_NAME}"

log "Assemblage de l'installeur dans ${DEST}"
rm -rf "${INSTALLER_CREATION_DIR}"
mkdir -p "${DEST}"
cp -r "${TEMPLATE_DIR}/"* "${DEST}/"

# Déposer les images dans dockerImage/ avec les noms attendus par setup_OpenELIS.py
mkdir -p "${DEST}/dockerImage"
cp OpenELIS-Global_DockerImage.tar.gz "${DEST}/dockerImage/OpenELIS-Global-${VERSION}.tar.gz"
cp ReactFrontend_DockerImage.tar.gz   "${DEST}/dockerImage/ReactFrontend_DockerImage.tar.gz"
cp JPAServer_DockerImage.tar.gz       "${DEST}/dockerImage/JPAServer_DockerImage.tar.gz"
cp NGINX_DockerImage.tar.gz           "${DEST}/dockerImage/NGINX_DockerImage.tar.gz"
cp Postgres_DockerImage.tar.gz        "${DEST}/dockerImage/Postgres_DockerImage.tar.gz"
cp AutoHeal_DockerImage.tar.gz        "${DEST}/dockerImage/AutoHeal_DockerImage.tar.gz"

chmod +x "${DEST}/scripts/"*.sh 2>/dev/null || true
chmod +x "${DEST}/install.sh" 2>/dev/null || true

# Archive finale
cd "${INSTALLER_CREATION_DIR}/linux"
tar -cf "${INSTALLER_NAME}.tar" "${INSTALLER_NAME}"
gzip -f "${INSTALLER_NAME}.tar"
cd "${PROJECT_DIR}"

# Nettoyage des tar intermédiaires à la racine
rm -f OpenELIS-Global_DockerImage.tar.gz ReactFrontend_DockerImage.tar.gz \
      JPAServer_DockerImage.tar.gz NGINX_DockerImage.tar.gz \
      Postgres_DockerImage.tar.gz AutoHeal_DockerImage.tar.gz

FINAL="${INSTALLER_CREATION_DIR}/linux/${INSTALLER_NAME}.tar.gz"
log "OK — installeur prêt :"
log "  ${FINAL}"
ls -lh "${FINAL}"
