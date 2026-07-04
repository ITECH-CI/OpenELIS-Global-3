# DNS local mono-site pour OpenELIS-Global (CIV).
# Image dnsmasq PURE (sans UI/webproc) : lit directement /etc/dnsmasq.conf monté
# par l'installeur, résout oeglobal.local et forwarde le reste vers le DNS amont.
FROM alpine:3.19

RUN apk add --no-cache dnsmasq bind-tools

# Healthcheck : dnsmasq doit résoudre oeglobal.local sur lui-même (127.0.0.1).
ADD healthcheck.sh /healthcheck.sh
RUN chmod 755 /healthcheck.sh

EXPOSE 53/udp 53/tcp

# -k : garde dnsmasq au premier plan (PID 1). La config vient de /etc/dnsmasq.conf.
CMD ["dnsmasq", "-k", "--conf-file=/etc/dnsmasq.conf"]
