#!/bin/bash
#
# setupDocker.sh — Prépare le système et installe les prérequis si absents :
#   curl, python3, net-tools, openssh-server, ufw (firewall),
#   Docker Engine + plugin docker compose ; met à jour le système (apt upgrade)
#   et ouvre les ports nécessaires (SSH 22, HTTP 80, HTTPS 443, DNS 53).
#
# Comportement :
#   - si tout est déjà présent : ne fait rien (offline OK) ;
#   - si un élément manque : nécessite une connexion internet. Sans internet,
#     affiche un message clair et ARRÊTE l'installation.
#
set -euo pipefail

log() { echo "[setupDocker] $*"; }
warn() { echo "[setupDocker][ATTENTION] $*" >&2; }
fail() { echo "[setupDocker][ERREUR] $*" >&2; exit 1; }

have() { command -v "$1" >/dev/null 2>&1; }
# Paquet apt installé ?
pkg_installed() { dpkg -s "$1" >/dev/null 2>&1; }

# Configure le firewall UFW : ouvre les ports AVANT d'activer (ne jamais couper
# l'accès SSH distant). Idempotent (relançable sans effet de bord).
#   22  SSH  | 80 HTTP | 443 HTTPS | 53 DNS (dnsmasq, mode local ; ouvert d'emblée,
#   inoffensif en mode online où aucun service n'écoute sur 53).
configure_firewall() {
    have ufw || { warn "ufw absent, firewall non configuré."; return 0; }
    log "Configuration du firewall (UFW) : ouverture 22/80/443/53 puis activation…"
    sudo ufw allow 22/tcp     >/dev/null 2>&1 || true   # SSH
    sudo ufw allow 80/tcp     >/dev/null 2>&1 || true   # HTTP  (redirection -> HTTPS)
    sudo ufw allow 443/tcp    >/dev/null 2>&1 || true   # HTTPS (OpenELIS)
    sudo ufw allow 53/tcp     >/dev/null 2>&1 || true   # DNS (dnsmasq) TCP
    sudo ufw allow 53/udp     >/dev/null 2>&1 || true   # DNS (dnsmasq) UDP
    # --force enable : n'invite pas (sinon bloque un script). Sans effet si déjà actif.
    sudo ufw --force enable   >/dev/null 2>&1 || true
    sudo ufw status verbose || true
}

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
# Paquets de préparation système (ajoutés) :
have ssh                 || need_install=true   # openssh-server (fournit sshd + 'ssh')
have ufw                 || need_install=true   # firewall
pkg_installed net-tools  || need_install=true   # ifconfig/netstat (diagnostic)

if [ "$need_install" = false ]; then
    log "Prérequis déjà présents. Rien à installer (mode hors ligne OK)."
    # On (re)configure quand même le firewall — idempotent, ne coupe rien.
    configure_firewall
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
    have curl                || echo "    - curl"
    have python3             || echo "    - python3"
    have ssh                 || echo "    - openssh-server"
    have ufw                 || echo "    - ufw (firewall)"
    pkg_installed net-tools  || echo "    - net-tools"
    have docker              || echo "    - docker (Docker Engine)"
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
# NEEDRESTART_MODE=a : redémarre les services impactés automatiquement, SANS
# ouvrir l'écran interactif needrestart qui bloquerait le script (Ubuntu 22.04+).
export NEEDRESTART_MODE=a

# --- Mise à jour des sources + du système (demandé) ---
# 'upgrade' (pas 'dist-upgrade') : ne change pas de noyau ni ne retire de paquets.
# --force-confold : conserve les fichiers de conf existants sans prompt.
log "Mise à jour des sources et du système (apt update && apt upgrade)…"
sudo apt-get update
sudo apt-get -y -o Dpkg::Options::="--force-confold" upgrade

# --- curl + python3 + outils système (net-tools, openssh-server) ---
log "Installation des paquets de base (curl, python3, net-tools, openssh-server, ufw)…"
sudo apt-get install -y ca-certificates curl python3 net-tools openssh-server ufw
# Activer SSH (accès distant au serveur)
sudo systemctl enable --now ssh 2>/dev/null || sudo systemctl enable --now sshd 2>/dev/null || true

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

# --- Ajout de l'utilisateur (non-root) au groupe docker (éviter sudo) ---
# SUDO_USER = l'utilisateur qui a lancé 'sudo'. Ignoré si root direct.
TARGET_USER="${SUDO_USER:-}"
if [ -n "$TARGET_USER" ] && [ "$TARGET_USER" != "root" ]; then
    log "Ajout de '$TARGET_USER' au groupe docker (pour utiliser docker sans sudo)…"
    getent group docker >/dev/null || sudo groupadd docker
    sudo usermod -aG docker "$TARGET_USER"
    warn "'$TARGET_USER' doit se déconnecter/reconnecter (ou reboot) pour que le groupe docker prenne effet."
fi

# --- Firewall (ports ouverts AVANT activation) ---
configure_firewall

log "Prérequis installés avec succès."
