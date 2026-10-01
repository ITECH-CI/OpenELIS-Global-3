FROM hapiproject/hapi:v6.6.0-tomcat

USER root
# curl (healthcheck). Debian 11 « bullseye » est en fin de vie : le dépôt
# security.debian.org liste encore ses paquets mais les renvoie en 404 (et
# archive.debian.org n'a pas bullseye-security). Installation normale d'abord,
# sinon repli sur le dépôt principal seul (curl 7.74 de bullseye main).
RUN (apt-get update && apt-get install -y --no-install-recommends curl) \
    || (sed -i -e '/security.debian.org/d' -e '/bullseye-updates/d' /etc/apt/sources.list \
        && rm -rf /var/lib/apt/lists/* \
        && apt-get update \
        && apt-get install -y --no-install-recommends curl) \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd tomcat; \
    groupadd tomcat-ssl-cert -g 8443; \ 
    useradd -M -s /bin/bash -u 8443 tomcat_admin; \
    usermod -a -G tomcat,tomcat-ssl-cert tomcat_admin; \
    chown -R tomcat_admin:tomcat /opt/bitnami/tomcat; \
    chown -R tomcat_admin:tomcat /target; \
    chmod g-w,o-rwx /opt/bitnami/tomcat; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf; \
    chmod o-rwx /opt/bitnami/tomcat/logs; \
    chmod o-rwx /opt/bitnami/tomcat/temp; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/bin; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/webapps; \
    chmod 770 /opt/bitnami/tomcat/conf/catalina.policy; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf/catalina.properties; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf/context.xml; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf/logging.properties; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf/server.xml; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf/tomcat-users.xml; \
    chmod g-w,o-rwx /opt/bitnami/tomcat/conf/web.xml;\
    chown -R tomcat_admin:tomcat /bitnami/tomcat/

USER tomcat_admin
