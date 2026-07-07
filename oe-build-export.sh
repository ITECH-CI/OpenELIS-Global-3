#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./build-export.sh 3.2.3.10
# or:
#   VERSION=3.2.3.10 ./build-export.sh

VERSION="${1:-${VERSION:-}}"
[[ -n "$VERSION" ]] || { echo "Usage: $0 <VERSION>  (ex: $0 3.2.3.10)"; exit 1; }

BUILD_COMPOSE="build.docker-compose.yml"
OUT_DIR=".."

BACKEND_SRC="openelisglobal-webapp-oe3:latest"
FRONTEND_SRC="frontend-oe3:latest"

BACKEND_TAG="itechuw/openelis-global-2:${VERSION}"
FRONTEND_TAG="itechuw/openelis-global-2-frontend:${VERSION}"

BACKEND_OUT="${OUT_DIR}/OpenELIS-Global_DockerImage.tar.gz"
FRONTEND_OUT="${OUT_DIR}/ReactFrontend_DockerImage.tar.gz"

echo "[INFO] VERSION=${VERSION}"

# --- Build backend ---
echo "[INFO] Build backend (oe.openelis.org)"
docker compose -f "${BUILD_COMPOSE}" build oe.openelis.org
echo "[INFO] Tag backend: ${BACKEND_SRC} -> ${BACKEND_TAG}"
docker tag "${BACKEND_SRC}" "${BACKEND_TAG}"

echo "[INFO] Export backend to ${BACKEND_OUT}"
docker save "${BACKEND_TAG}" | gzip > "${BACKEND_OUT}"

# --- Build frontend ---
echo "[INFO] Build frontend (frontend.openelis.org)"
docker compose -f "${BUILD_COMPOSE}" build frontend.openelis.org
echo "[INFO] Tag frontend: ${FRONTEND_SRC} -> ${FRONTEND_TAG}"
docker tag "${FRONTEND_SRC}" "${FRONTEND_TAG}"

echo "[INFO] Export frontend to ${FRONTEND_OUT}"
docker save "${FRONTEND_TAG}" | gzip > "${FRONTEND_OUT}"

# --- Sanity checks ---
echo "[INFO] Sanity checks"
docker image inspect "${BACKEND_TAG}" >/dev/null
docker image inspect "${FRONTEND_TAG}" >/dev/null

ls -lh "${BACKEND_OUT}" "${FRONTEND_OUT}"

echo "[OK] Done."
echo "Backend:  ${BACKEND_TAG} -> ${BACKEND_OUT}"
echo "Frontend: ${FRONTEND_TAG} -> ${FRONTEND_OUT}"
