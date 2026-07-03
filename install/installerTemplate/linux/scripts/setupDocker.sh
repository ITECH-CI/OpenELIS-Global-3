#!/bin/bash
#
# setupDocker.sh — Vérifie et installe les prérequis système si absents :
#   curl, python3, Docker Engine + plugin docker compose.
#
# Comportement :
#   - si tout est déjà présent : ne fait rien (offline OK) ;
#   - si un élément manque : nécessite une connexion internet. Sans internet,
#     affiche un message clair et ARRÊTE l'installation.
#
set -euo pipefail

log() { echo "[setupDocker] $*"; }
fail() { echo "[setupDocker][ERREUR] $*" >&2; exit 1; }

have() { command -v "$1" >/dev/null 2>&1; }

# Y a-t-il une connexion internet vers le dépôt Docker ?
has_internet() {
    curl -fsS --max-time 8 -o /dev/null https://download.docker.com/linux/ubuntu/gpg 2>/dev/null \
      || ping -c1 -W3 8.8.8.8 >/dev/null 2>&1
}

need_install=false
have curl    || need_install=true
have python3 || need_install=true
have docker  || need_install=true
docker compose version >/dev/null 2>&1 || need_install=true

if [ "$need_install" = false ]; then
    log "Prérequis déjà présents (curl, python3, docker, docker compose). Rien à installer."
    exit 0
fi

# Quelque chose manque -> besoin d'internet.
log "Certains prérequis sont manquants. Vérification de la connexion internet…"
if ! has_internet; then
    echo
    echo "  ============================================================"
    echo "  PAS DE CONNEXION INTERNET détectée."
    echo "  Les prérequis suivants doivent être installés, ce qui"
    echo "  nécessite une connexion internet :"
    have curl    || echo "    - curl"
    have python3 || echo "    - python3"
    have docker  || echo "    - docker (Docker Engine)"
    docker compose version >/dev/null 2>&1 || echo "    - docker compose (plugin)"
    echo
    echo "  Connectez la machine à internet UNE FOIS pour installer ces"
    echo "  prérequis, puis relancez l'installation. L'application"
    echo "  elle-même s'installe ensuite hors ligne."
    echo "  ============================================================"
    echo
    fail "Prérequis manquants et pas d'internet."
fi

export DEBIAN_FRONTEND=noninteractive

# --- curl + python3 (paquets de base) ---
if ! have curl || ! have python3; then
    log "Installation de curl / python3…"
    sudo apt-get update
    sudo apt-get install -y ca-certificates curl python3
fi

# --- Docker Engine + plugins ---
if ! have docker || ! docker compose version >/dev/null 2>&1; then
    log "Installation de Docker Engine + plugins…"

    # Retirer d'anciennes versions/conflits éventuels (best effort)
    conflicts="$(dpkg --get-selections docker.io docker-compose docker-compose-v2 docker-doc podman-docker containerd runc 2>/dev/null | cut -f1 || true)"
    if [ -n "$conflicts" ]; then
        sudo apt-get remove -y $conflicts || true
    fi

    # Clé GPG officielle Docker
    sudo apt-get update
    sudo apt-get install -y ca-certificates curl
    sudo install -m 0755 -d /etc/apt/keyrings
    sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    sudo chmod a+r /etc/apt/keyrings/docker.asc

    # Dépôt Apt Docker
    sudo tee /etc/apt/sources.list.d/docker.sources >/dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi

# Vérification finale
have docker || fail "Docker n'a pas pu être installé."
docker compose version >/dev/null 2>&1 || fail "Le plugin docker compose est absent après installation."
log "Prérequis installés avec succès."
