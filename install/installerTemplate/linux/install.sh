#!/usr/bin/env bash
#
# install.sh — Amorçage de l'installation OpenELIS-Global CIV.
#
# Vérifie les prérequis, lance l'installeur, puis affiche un récapitulatif.
# À exécuter en root depuis le dossier de l'installeur décompressé :
#     sudo ./install.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd -P "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "${SCRIPT_DIR}"

err() { echo "[install][ERREUR] $*" >&2; }
info() { echo "[install] $*"; }

# --- Root ---
if [ "$(id -u)" -ne 0 ]; then
  err "Ce script doit être exécuté en root. Relancez avec: sudo ./install.sh"
  exit 1
fi

# --- Docker ---
if ! command -v docker >/dev/null 2>&1; then
  err "Docker n'est pas installé."
  echo
  echo "Installez Docker (une fois, avec connexion) puis relancez :"
  echo "  Ubuntu/Debian : https://docs.docker.com/engine/install/"
  echo "  Rapide        : curl -fsSL https://get.docker.com | sh"
  exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
  err "Le plugin 'docker compose' est absent."
  echo "  Installez docker-compose-plugin (ex: apt-get install docker-compose-plugin)"
  exit 1
fi

# --- Python 3 ---
if ! command -v python3 >/dev/null 2>&1; then
  err "python3 est requis. Installez-le (ex: apt-get install python3)."
  exit 1
fi

# --- Espace disque (avertissement si < 10 Go) ---
avail_kb=$(df -k . | awk 'NR==2 {print $4}')
if [ "${avail_kb:-0}" -lt 10485760 ]; then
  info "Attention : moins de 10 Go d'espace libre détecté. L'installation peut échouer."
fi

info "Prérequis OK. Démarrage de l'installation…"
echo

# --- Lancement de l'installeur (auto-détecte install vs mise à jour) ---
python3 ./setup_OpenELIS.py -m update-install

# --- Récapitulatif ---
echo
echo "============================================================"
echo " Installation terminée."
echo
echo " Accès        : https://<adresse-du-serveur>/"
echo " Identifiants : admin / adminADMIN!  (à changer immédiatement)"
echo
echo " Secrets/config :"
echo "   - Clé de chiffrement : /var/lib/openelis-global/config/ENCRYPTION_KEY"
echo "     (à sauvegarder ! doit rester identique aux mises à jour)"
echo "   - Config générée     : /etc/openelis-global/"
echo
echo " État des conteneurs :"
echo "   docker compose -f /etc/openelis-global/docker-compose.yml ps"
echo "============================================================"
