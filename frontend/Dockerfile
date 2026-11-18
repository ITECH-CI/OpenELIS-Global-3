# frontend/Dockerfile
#
# Image unique du frontend, en plusieurs étapes nommées ; l'appelant choisit
# l'étape par `--target=` ou par `build.target:` dans une composition.
#
#   base    — Node, dépendances (npm ci) et sources
#   dev     — serveur de développement Vite (port 80, rechargement à chaud)
#   build   — `npm run build`, produit dist/ (étape intermédiaire)
#   runtime — nginx qui sert dist/ en HTTP sur le port 80
#
# Sans `--target`, Docker construit la dernière étape : `runtime` doit rester
# en dernier (images publiées, installeur, build.docker-compose.yml).

# ---- base ----
FROM node:24.22.3-alpine AS base
RUN apk update && apk upgrade && apk add --no-cache bash
WORKDIR /app
ENV PATH=/app/node_modules/.bin:$PATH
COPY package.json package-lock.json .npmrc ./
# Le postinstall applique les correctifs de patches/ (patch-package).
COPY patches ./patches
# Cypress tourne dans ses propres travaux : son binaire n'a rien à faire ici.
RUN CYPRESS_INSTALL_BINARY=0 npm ci
COPY . ./

# ---- dev ----
FROM base AS dev
EXPOSE 80
CMD ["npm", "start"]

# ---- build ----
FROM base AS build
RUN npm run build

# ---- runtime ----
FROM nginx:1.30.6-alpine AS runtime
USER root
COPY --from=build /app/dist /usr/share/nginx/html
COPY --from=build /app/nginx/nginx.conf /etc/nginx/conf.d/default.conf
RUN apk update && apk upgrade && apk add --no-cache bash curl
ADD nginx/healthcheck.sh /healthcheck.sh
RUN chown nginx:nginx /healthcheck.sh && chmod 770 /healthcheck.sh
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
