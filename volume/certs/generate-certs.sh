#!/bin/bash
#
# Génération des certificats TLS internes d'OpenELIS (service « certs »).
#
# Remplace l'image publique itechuw/certgen, qui embarquait des certificats
# figés : la MÊME clé privée pour toutes les installations, connue de tous.
# Ici, chaque installation génère sa propre clé au premier démarrage.
#
# Produit (mêmes chemins, formats et mots de passe que l'ancienne image, donc
# compatibles avec oe_server.xml, hapi_server.xml et nginx) :
#   /etc/ssl/private/apache-selfsigned.key   clé privée (nginx)
#   /etc/ssl/certs/apache-selfsigned.crt     certificat auto-signé (nginx)
#   /etc/openelis-global/keystore            PKCS12, clé + certificat (Tomcat, HAPI)
#   /etc/openelis-global/client_facing_keystore  copie du keystore
#   /etc/openelis-global/truststore          PKCS12, certificat de confiance (mTLS)
#
# Idempotent : ne régénère que si nos fichiers sont absents, si le certificat
# expire dans moins de 30 jours, ou si FORCE_REGEN=true. Les certificats de
# l'ancienne image publique (sans marqueur) sont remplacés au premier passage.
#
# Variables : KEYSTORE_PW (kspass), TRUSTSTORE_PW (tspass), CERT_CN (localhost),
#             CERT_SAN (noms supplémentaires, ex. "DNS:labo.exemple.ci,IP:10.0.0.5"),
#             CERT_DAYS (3650), FORCE_REGEN (false).
set -euo pipefail

strip_quotes() { local v="$1"; v="${v%\"}"; v="${v#\"}"; printf '%s' "$v"; }
KEYSTORE_PW=$(strip_quotes "${KEYSTORE_PW:-kspass}")
TRUSTSTORE_PW=$(strip_quotes "${TRUSTSTORE_PW:-tspass}")
CERT_CN=$(strip_quotes "${CERT_CN:-localhost}")
CERT_SAN=$(strip_quotes "${CERT_SAN:-}")
CERT_DAYS=${CERT_DAYS:-3650}
FORCE_REGEN=${FORCE_REGEN:-false}

KEY=/etc/ssl/private/apache-selfsigned.key
CRT=/etc/ssl/certs/apache-selfsigned.crt
STORE_DIR=/etc/openelis-global
MARKER=$STORE_DIR/.generated-by-oe-certs

mkdir -p /etc/ssl/private /etc/ssl/certs "$STORE_DIR"

reason=""
if [ "$FORCE_REGEN" = "true" ]; then
    reason="régénération forcée (FORCE_REGEN=true)"
elif [ ! -f "$MARKER" ]; then
    reason="certificats absents ou issus de l'ancienne image publique certgen"
elif [ ! -s "$KEY" ] || [ ! -s "$CRT" ] || [ ! -s "$STORE_DIR/keystore" ] || [ ! -s "$STORE_DIR/truststore" ]; then
    reason="fichier manquant"
elif ! openssl x509 -checkend $((30 * 24 * 3600)) -noout -in "$CRT" >/dev/null; then
    reason="certificat expirant dans moins de 30 jours"
fi

if [ -z "$reason" ]; then
    echo "[oe-certs] certificats présents et valides jusqu'au $(openssl x509 -enddate -noout -in "$CRT" | cut -d= -f2) : rien à faire."
    exit 0
fi

echo "[oe-certs] génération : $reason"
SAN="DNS:localhost,DNS:*.openelis.org,IP:127.0.0.1"
[ "$CERT_CN" != "localhost" ] && SAN="DNS:${CERT_CN},${SAN}"
[ -n "$CERT_SAN" ] && SAN="${SAN},${CERT_SAN}"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

openssl req -x509 -nodes -days "$CERT_DAYS" -newkey rsa:2048 \
    -keyout "$TMP/key.pem" -out "$TMP/crt.pem" \
    -subj "/C=CI/O=OpenELIS CIV/CN=${CERT_CN}" \
    -addext "subjectAltName=${SAN}" 2>/dev/null

openssl pkcs12 -export -name tomcat -inkey "$TMP/key.pem" -in "$TMP/crt.pem" \
    -out "$TMP/keystore" -passout "pass:${KEYSTORE_PW}"

keytool -import -noprompt -alias oeCert -file "$TMP/crt.pem" -storetype pkcs12 \
    -keystore "$TMP/truststore" -storepass "${TRUSTSTORE_PW}" >/dev/null

# remplacement en bloc (tous les fichiers proviennent de la même génération)
install -m 600 "$TMP/key.pem" "$KEY"
install -m 644 "$TMP/crt.pem" "$CRT"
install -m 644 "$TMP/keystore" "$STORE_DIR/keystore"
install -m 644 "$TMP/keystore" "$STORE_DIR/client_facing_keystore"
install -m 644 "$TMP/truststore" "$STORE_DIR/truststore"
date -u +%Y-%m-%dT%H:%M:%SZ >"$MARKER"

echo "[oe-certs] OK : CN=${CERT_CN}, SAN=${SAN}, valide jusqu'au $(openssl x509 -enddate -noout -in "$CRT" | cut -d= -f2)"
echo "[oe-certs] Redémarrer webapp, FHIR et proxy pour qu'ils chargent les nouveaux certificats."
