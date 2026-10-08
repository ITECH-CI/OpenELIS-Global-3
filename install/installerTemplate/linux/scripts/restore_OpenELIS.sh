#!/bin/bash
#
# Restauration d'une base OpenELIS (sauvegarde du site ou d'un autre site).
#
# Enchaîne, dans l'ordre, tout ce qu'une restauration exige :
#   1. dump de sécurité de la base ACTUELLE (retour arrière possible)
#   2. arrêt de la webapp et du serveur FHIR (plus aucune écriture)
#   3. schéma clinlims vierge puis restauration du dump
#   4. remise à zéro des tables FHIR (HAPI) : elles sont une projection du
#      métier, reconstruite ensuite ; les restaurer depuis un vieux dump
#      (large objects absents) empêche external-fhir-api de démarrer
#   5. RECRÉATION des conteneurs webapp et FHIR (pas un simple redémarrage :
#      repart d'un état neutre — index Lucene, fichiers de configuration)
#   6. attente du démarrage, puis reconstruction FHIR (optionnelle)
#
# Usage :
#   sudo ./restore_OpenELIS.sh <fichier_dump> [--keep-fhir] [--yes]
#
#   <fichier_dump>  .sql / .backup (format texte pg_dump), éventuellement .gz,
#                   ou dump au format custom (pg_dump -Fc)
#   --keep-fhir     conserve les tables FHIR du dump (déconseillé, sauf dump
#                   récent et complet fait avec pg_dump -b)
#   --yes           pas de confirmation interactive (déconseillé)
#
# Variables d'environnement (valeurs par défaut = installation standard) :
#   OE_DIR        dossier d'installation        (/var/lib/openelis-global)
#   COMPOSE_FILE  fichier docker compose        (celui qui pilote les conteneurs,
#                 lu sur leurs étiquettes ; à défaut $OE_DIR/docker-compose.yml)
#   DB_CONTAINER  conteneur PostgreSQL          (openelisglobal-database)
#   OE_URL        URL de l'application         (https://localhost/api/OpenELIS-Global)
#
set -uo pipefail

OE_DIR="${OE_DIR:-/var/lib/openelis-global}"
COMPOSE_FILE="${COMPOSE_FILE:-}"
COMPOSE_PROJECT=""
DB_CONTAINER="${DB_CONTAINER:-openelisglobal-database}"
OE_CONTAINER="${OE_CONTAINER:-openelisglobal-webapp}"
FHIR_CONTAINER="${FHIR_CONTAINER:-external-fhir-api}"
OE_SERVICE="${OE_SERVICE:-oe.openelis.org}"
FHIR_SERVICE="${FHIR_SERVICE:-fhir.openelis.org}"
OE_URL="${OE_URL:-https://localhost/api/OpenELIS-Global}"
BACKUP_DIR="${BACKUP_DIR:-$OE_DIR/backups}"
DB_USER=clinlims
DB_NAME=clinlims

DUMP=""
KEEP_FHIR=false
ASSUME_YES=false
for arg in "$@"; do
    case "$arg" in
    --keep-fhir) KEEP_FHIR=true ;;
    --yes) ASSUME_YES=true ;;
    -h | --help)
        sed -n '2,32p' "$0"
        exit 0
        ;;
    *) DUMP="$arg" ;;
    esac
done

STAMP=$(date +%Y%m%d_%H%M%S)
LOG="$BACKUP_DIR/restore_$STAMP.log"

say() { echo -e "$*" | tee -a "$LOG"; }
die() {
    say "\n❌ $*"
    say "Journal : $LOG"
    exit 1
}
psql_db() { docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" -qtA "$@"; }

# ---------------------------------------------------------------- vérifications
[ -n "$DUMP" ] || {
    sed -n '17,24p' "$0"
    exit 1
}
[ -f "$DUMP" ] || {
    echo "Fichier introuvable : $DUMP"
    exit 1
}
command -v docker >/dev/null || {
    echo "docker introuvable"
    exit 1
}
mkdir -p "$BACKUP_DIR" && chmod 700 "$BACKUP_DIR"
: >"$LOG" || {
    echo "Impossible d'écrire le journal dans $BACKUP_DIR (lancer avec sudo)"
    exit 1
}
docker ps --format '{{.Names}}' | grep -qx "$DB_CONTAINER" ||
    die "Le conteneur base de données '$DB_CONTAINER' n'est pas démarré (docker start $DB_CONTAINER)"

# Le compose qui pilote RÉELLEMENT les conteneurs est celui du dossier de
# l'installeur ; le docker-compose.yml de $OE_DIR n'en est qu'une copie
# d'archive. Avec la copie, docker compose crée un AUTRE projet, dont les
# conteneurs entrent en conflit de nom avec ceux en service (échec de l'étape 5).
# On lit donc le fichier et le projet sur les étiquettes du conteneur base de
# données, qui tourne pendant toute la restauration.
compose_label() { docker inspect -f "{{index .Config.Labels \"$1\"}}" "$DB_CONTAINER" 2>/dev/null; }
COMPOSE_PROJECT=$(compose_label com.docker.compose.project)
if [ -z "$COMPOSE_FILE" ]; then
    COMPOSE_FILE=$(compose_label com.docker.compose.project.config_files)
    COMPOSE_FILE=${COMPOSE_FILE%%,*}
    [ -n "$COMPOSE_FILE" ] && [ -f "$COMPOSE_FILE" ] || COMPOSE_FILE="$OE_DIR/docker-compose.yml"
fi
[ -f "$COMPOSE_FILE" ] || die "Fichier docker compose introuvable : $COMPOSE_FILE (variable COMPOSE_FILE)"

# format du dump : gzip ? custom (signature PGDMP) ? texte ?
if gzip -t "$DUMP" 2>/dev/null; then
    READER=(gzip -dc "$DUMP")
else
    READER=(cat "$DUMP")
fi
if [ "$("${READER[@]}" 2>/dev/null | head -c 5)" = "PGDMP" ]; then
    FORMAT=custom
else
    FORMAT=plain
fi
HAS_FHIR_DATA=false
# présence de lignes de données après l'en-tête COPY de hfj_res_ver
# (sous-shell sans pipefail : awk s'arrête tôt, le lecteur reçoit SIGPIPE)
if [ "$FORMAT" = plain ] && (
    set +o pipefail
    "${READER[@]}" 2>/dev/null | awk '/^COPY clinlims\.hfj_res_ver /{f=1;next} f&&/^\\\.$/{exit} f{n++} END{exit !(n>0)}'
); then
    HAS_FHIR_DATA=true
fi

say "=== Restauration OpenELIS — $(date '+%d/%m/%Y %H:%M') ==="
say "Dump            : $DUMP ($FORMAT$([ "${READER[0]}" = gzip ] && echo ", compressé"))"
say "Données FHIR    : $($HAS_FHIR_DATA && echo "présentes dans le dump" || echo "absentes (normal pour les sauvegardes automatiques)")"
say "Tables FHIR     : $($KEEP_FHIR && echo "CONSERVÉES (--keep-fhir)" || echo "remises à zéro puis reconstruites depuis les données métier")"
say "Compose         : $COMPOSE_FILE"
CURRENT=$(psql_db -c "SELECT count(*) FROM clinlims.sample" 2>/dev/null || echo "?")
say "Base actuelle   : $CURRENT demandes — elle sera REMPLACÉE"

if ! $ASSUME_YES; then
    echo
    read -r -p "Taper RESTAURER pour confirmer : " answer
    [ "$answer" = "RESTAURER" ] || die "Abandon (aucune modification)."
fi

# ---------------------------------------------------------- 1. dump de sécurité
SAFETY="$BACKUP_DIR/pre-restore_$STAMP.sql.gz"
say "\n[1/6] Dump de sécurité de la base actuelle → $SAFETY"
if psql_db -c "SELECT 1 FROM pg_namespace WHERE nspname='clinlims'" | grep -q 1; then
    docker exec "$DB_CONTAINER" pg_dump -U "$DB_USER" -n clinlims \
        --exclude-table-data='clinlims.hfj_*' --exclude-table-data='clinlims.trm_*' \
        --exclude-table-data='clinlims.mpi_*' --exclude-table-data='clinlims.npm_*' \
        --exclude-table-data='clinlims.bt2_*' "$DB_NAME" | gzip >"$SAFETY" ||
        die "Échec du dump de sécurité : rien n'a été modifié."
    chmod 600 "$SAFETY"
    say "      OK ($(du -h "$SAFETY" | cut -f1))"
else
    say "      schéma clinlims absent : rien à sauvegarder"
fi

# ------------------------------------------------------- 2. arrêt des écritures
say "\n[2/6] Arrêt de $OE_CONTAINER et $FHIR_CONTAINER"
docker stop "$OE_CONTAINER" "$FHIR_CONTAINER" >>"$LOG" 2>&1 || true

# ------------------------------------------------------------ 3. restauration
say "\n[3/6] Schéma clinlims vierge puis restauration (peut prendre plusieurs minutes)"
psql_db -c "DROP SCHEMA IF EXISTS clinlims CASCADE; CREATE SCHEMA clinlims AUTHORIZATION $DB_USER;" >>"$LOG" 2>&1 ||
    die "Impossible de recréer le schéma clinlims. Retour arrière : relancer ce script avec $SAFETY"
ERRLOG="$BACKUP_DIR/restore_${STAMP}_psql.log"
if [ "$FORMAT" = custom ]; then
    "${READER[@]}" | docker exec -i "$DB_CONTAINER" pg_restore -U "$DB_USER" -d "$DB_NAME" --no-owner >"$ERRLOG" 2>&1
else
    # pas d'ON_ERROR_STOP : le dump recrée le schéma (« already exists ») ;
    # les autres erreurs sont comptées ci-dessous
    "${READER[@]}" | docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -q >/dev/null 2>"$ERRLOG"
fi
# Erreurs connues et sans conséquence, écartées du décompte :
# - le schéma clinlims recréé juste avant ;
# - les fonctions/types de l'extension tablefunc (crosstab) qu'un ancien site
#   avait en copie dans clinlims : l'utilisateur clinlims ne peut pas créer de
#   fonction en langage C, mais l'extension est déjà fournie dans public
#   (vérifié ci-dessous) ;
# - un large object déjà présent (stockage partagé de la base, utilisé par HAPI
#   dont les tables sont remises à zéro à l'étape 4).
BENIGN='schema "clinlims" already exists|le schéma « clinlims » existe déjà|permission denied for language c|droit refusé pour le langage c|type "tablefunc_crosstab_[0-9]+" already exists|le type « tablefunc_crosstab_[0-9]+ » existe déjà|pg_largeobject_metadata_oid_index'
BENIGN_ERRORS=$(grep -E "ERROR|ERREUR" "$ERRLOG" | grep -c -E "$BENIGN")
REAL_ERRORS=$(grep -E "ERROR|ERREUR" "$ERRLOG" | grep -v -E "$BENIGN" | wc -l | tr -d ' ')
RESTORED=$(psql_db -c "SELECT count(*) FROM clinlims.sample" 2>/dev/null || echo "?")
say "      $RESTORED demandes restaurées, $REAL_ERRORS erreur(s) SQL ($BENIGN_ERRORS connue(s) sans conséquence écartée(s) ; détail : $ERRLOG)"
[ "$RESTORED" != "?" ] || die "La table sample est absente après restauration : dump invalide ? Retour arrière : $SAFETY"
[ "$REAL_ERRORS" -eq 0 ] || say "      ⚠️  Vérifier les erreurs ci-dessus avant de remettre le site en service."
# crosstab (extension tablefunc) : indispensable aux exports CSV
if psql_db -c "SELECT * FROM crosstab('SELECT ''a''::text, ''x''::text, ''1''::text') AS t(k text, x text)" >/dev/null 2>&1; then
    say "      crosstab (exports CSV) : OK"
else
    say "      ⚠️  crosstab indisponible : les exports CSV échoueront. Correctif :"
    say "         docker exec $DB_CONTAINER psql -U postgres -d $DB_NAME -c \"CREATE EXTENSION IF NOT EXISTS tablefunc SCHEMA public\""
fi

# --------------------------------------------------------------- 4. tables FHIR
if $KEEP_FHIR; then
    say "\n[4/6] Tables FHIR conservées (--keep-fhir)"
else
    say "\n[4/6] Remise à zéro des tables FHIR (HAPI les recrée au démarrage)"
    psql_db >>"$LOG" 2>&1 <<'SQL' || die "Échec de la remise à zéro des tables FHIR"
DO $$
DECLARE r record;
BEGIN
    FOR r IN SELECT tablename FROM pg_tables
             WHERE schemaname = 'clinlims' AND tablename ~ '^(hfj|trm|mpi|npm|bt2)_'
    LOOP
        EXECUTE format('DROP TABLE IF EXISTS clinlims.%I CASCADE', r.tablename);
    END LOOP;
END $$;
SQL
    say "      OK"
fi

# ------------------------------------------------ 5. recréation des conteneurs
say "\n[5/6] Recréation des conteneurs $OE_SERVICE et $FHIR_SERVICE"
docker compose -f "$COMPOSE_FILE" ${COMPOSE_PROJECT:+-p "$COMPOSE_PROJECT"} up -d --force-recreate \
    "$OE_SERVICE" "$FHIR_SERVICE" >>"$LOG" 2>&1 ||
    die "Échec de docker compose up (voir $LOG)"

say "      Attente du démarrage (migrations de base comprises, jusqu'à 15 min)…"
# « healthy » si le conteneur a un healthcheck, sinon son état (« running »)
health() { docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$1" 2>/dev/null; }
webapp_up() { curl -sk -o /dev/null -w '%{http_code}' "$OE_URL/LoginPage" 2>/dev/null | grep -q '^200$'; }
is_ok() { [ "$1" = healthy ] || [ "$1" = running ]; }
for _ in $(seq 1 90); do
    OE_H=$(health "$OE_CONTAINER")
    FHIR_H=$(health "$FHIR_CONTAINER")
    if is_ok "$OE_H" && is_ok "$FHIR_H" && webapp_up; then
        break
    fi
    [ "$OE_H" = exited ] && break
    sleep 10
done
say "      $OE_CONTAINER : $OE_H | $FHIR_CONTAINER : $FHIR_H"
if ! is_ok "$OE_H" || ! webapp_up; then
    say "      ⚠️  La webapp n'est pas saine. Dernières erreurs :"
    docker logs --tail 400 "$OE_CONTAINER" 2>&1 | grep -E "liquibase.exception|SEVERE|Context initialization failed" | tail -5 | tee -a "$LOG"
    die "Démarrage incomplet. Retour arrière possible avec : $0 $SAFETY"
fi

# ------------------------------------------------------ 6. reconstruction FHIR
say "\n[6/6] Reconstruction des ressources FHIR depuis les données métier"
REBUILD_CMD="$OE_URL/OEToFhir?checkAll=true&batchSize=100&threads=1"
DO_REBUILD=n
if ! $KEEP_FHIR && ! $ASSUME_YES; then
    read -r -p "      Lancer la reconstruction maintenant ? (o/N) " DO_REBUILD
fi
if [[ "$DO_REBUILD" =~ ^[oOyY] ]]; then
    read -r -p "      Identifiant administrateur OpenELIS : " OE_LOGIN
    read -r -s -p "      Mot de passe : " OE_PWD
    echo
    JAR=$(mktemp)
    curl -sk -c "$JAR" -b "$JAR" -o /dev/null "$OE_URL/LoginPage"
    curl -sk -c "$JAR" -b "$JAR" -o /dev/null -X POST "$OE_URL/ValidateLogin" \
        --data-urlencode "loginName=$OE_LOGIN" --data-urlencode "password=$OE_PWD"
    unset OE_PWD
    say "      Reconstruction en cours (long sur une grosse base)…"
    CODE=$(curl -sk -b "$JAR" -o "$BACKUP_DIR/restore_${STAMP}_fhir.json" -w '%{http_code}' --max-time 7200 "$REBUILD_CMD")
    rm -f "$JAR"
    say "      HTTP $CODE — détail : $BACKUP_DIR/restore_${STAMP}_fhir.json"
else
    say "      À lancer plus tard, connecté en administrateur dans le navigateur :"
    say "      $REBUILD_CMD"
fi

say "\n✅ Restauration terminée."
say "À vérifier maintenant :"
say "  • Admin > Informations du site : nom du site, préfixe labo, logos, en-tête d'un compte rendu"
say "  • Interopérabilité > Cibles FHIR : la reconstruction va RE-POUSSER toutes les ressources"
say "    vers les cibles actives ; les désactiver d'abord si le destinataire a déjà les données"
say "  • Mots de passe chiffrés (connexions externes, serveur consolidé) : illisibles si la base"
say "    vient d'un serveur avec un autre encryption.general.password → les ressaisir"
say "  • SITE_ID du fichier backup.conf cohérent avec le site restauré"
say "Retour arrière : $0 $SAFETY"
say "Journal : $LOG"
