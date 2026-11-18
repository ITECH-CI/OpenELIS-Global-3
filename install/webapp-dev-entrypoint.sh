#!/bin/sh
# Démarrage de la webapp dans la pile de développement (dev.docker-compose.yml).
#
# Le WAR est compilé sur le poste (mvn clean install -DskipTests) et lu dans le
# dépôt, monté en lecture seule sur /workspace. Il est recopié à chaque
# démarrage, et les répertoires dépliés par Tomcat sont effacés : un
# « docker restart openelisglobal-webapp » sert toujours le dernier WAR
# construit, jamais une version dépliée plus ancienne.
set -e

WAR_SOURCE="/workspace/target/OpenELIS-Global.war"
WEBAPPS="${CATALINA_HOME}/webapps"

if [ ! -f "${WAR_SOURCE}" ]; then
    echo "[dev] ${WAR_SOURCE} introuvable." >&2
    echo "[dev] Construire le WAR sur le poste : mvn clean install -DskipTests" >&2
    exit 1
fi

rm -rf "${WEBAPPS}/OpenELIS-Global" "${WEBAPPS}/api#OpenELIS-Global"
cp "${WAR_SOURCE}" "${WEBAPPS}/OpenELIS-Global.war"
echo "[dev] WAR copié depuis le poste ($(date -r "${WAR_SOURCE}" '+%d/%m/%Y %H:%M'))."

exec /docker-entrypoint.sh
