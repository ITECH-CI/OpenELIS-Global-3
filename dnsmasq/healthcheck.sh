#!/bin/sh
# Healthy si dnsmasq résout oeglobal.lan sur lui-même.
# (network_mode: host -> dnsmasq écoute sur l'IP LAN ; on interroge 127.0.0.1
#  qui est aussi servi par le processus.)
if nslookup oeglobal.lan 127.0.0.1 >/dev/null 2>&1; then
    exit 0
else
    exit 1
fi
