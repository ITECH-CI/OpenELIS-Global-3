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

# --- Prérequis système (curl, python3, Docker) : installés automatiquement si
#     absents ; nécessite internet UNE fois. Sans internet -> message + arrêt.
info "Vérification/installation des prérequis (curl, python3, Docker)…"
bash "${SCRIPT_DIR}/scripts/setupDocker.sh" || {
  err "Prérequis non satisfaits (voir message ci-dessus)."
  exit 1
}

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
echo
echo " ***********************************************************"
echo " *  /!\\  SÉCURITÉ — ÉTAPE OBLIGATOIRE                      *"
echo " *  Identifiants par défaut : admin / adminADMIN!          *"
echo " *  CHANGEZ le mot de passe admin DÈS LE PREMIER LOGIN.    *"
echo " *  (compte par défaut connu publiquement = accès total).  *"
echo " ***********************************************************"
echo
echo " Secrets/config :"
echo "   - Clé de chiffrement : /var/lib/openelis-global/config/ENCRYPTION_KEY"
echo "     (à sauvegarder ! doit rester identique aux mises à jour)"
echo "   - Config générée     : /etc/openelis-global/"
echo
echo " État des conteneurs (depuis ce dossier d'installation) :"
echo "   sudo docker compose ps"
echo "============================================================"
