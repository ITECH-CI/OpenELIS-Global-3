import React from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  Tag,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  Link,
  Tile,
  Button,
} from "@carbon/react";
import { Download } from "@carbon/icons-react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "interop.endpoints.title",
    link: "/MasterListsPage#InteropEndpoints",
  },
];

// Documentation STATIQUE des endpoints d'interopérabilité exposés/consommés par
// OpenELIS. Source de vérité = le code (contrôleurs REST + nginx + moteurs
// @Scheduled) ; cette liste est maintenue à la main car elle change rarement et
// sert de référence aux intégrateurs tiers. Le context path des `/rest/...` est
// `/api/OpenELIS-Global/`. Le `/fhir` est le serveur HAPI, exposé via nginx.

// A — Endpoints réellement exposés aux systèmes tiers (SIGDEP / HIE / SHR).
const EXPOSED = [
  {
    method: "GET",
    url: "https://<hôte>/fhir/**",
    auth: "token",
    authLabel: "Jeton dynamique (nginx auth_request) + IP allowlist",
    desc: "Lecture FHIR R4 du store HAPI par les tiers (mini-HIE). Le serveur HAPI n'est jamais publié directement : nginx termine le TLS, valide le jeton, puis proxifie en HTTP interne. Lecture seule appliquée par le contrôle d'accès.",
  },
  {
    method: "POST",
    url: "https://<hôte>/fhir-in/order",
    auth: "token",
    authLabel: "Jeton (nginx auth_request) + IP allowlist",
    desc: "Réception PUSH d'un Bundle FHIR d'ordre (ServiceRequest / QuestionnaireResponse / Patient / Specimen, sans Task) depuis un tiers. Crée un ElectronicOrder « en attente ». 201 accepté / 422 non exploitable.",
  },
  {
    method: "GET",
    url: "https://<hôte>/api/OpenELIS-Global/rest/fhir-gateway/auth",
    auth: "internal",
    authLabel: "Interne — appelé uniquement par nginx (auth_request)",
    desc: "Point de validation d'accès pour nginx : vérifie le jeton présenté par le tiers (Authorization: Bearer ou X-API-Key), la méthode (lecture seule), la ressource autorisée et le quota. Renvoie 200 / 401 / 403 (429→403). N'expose aucune donnée.",
  },
];

// Ressources FHIR R4 réellement PRODUITES par OpenELIS et lisibles via /fhir
// (le store HAPI expose le référentiel R4 complet, mais seules celles-ci
// contiennent des données OE). Pour chaque ressource : les recherches typiques
// et l'identifiant métier posé par OE (Reference.identifier / .identifier).
// La liste des ressources autorisées PAR TIERS est restreinte côté passerelle
// (fhir_gateway_client.allowed_resources).
const FHIR_RESOURCES = [
  {
    type: "Patient",
    search:
      "?identifier=urn:oid:1.3.6.1.4.1.53864.1.3|<matricule CNAM>  ·  ?_id=<uuid>",
    idSystem:
      "urn:oid:1.3.6.1.4.1.53864.1.3 (CNAM) + openelis-global.org/pat_uuid",
    desc: "Patient (identité pivot = matricule CNAM). Rapprochement MPI par identifier.",
  },
  {
    type: "ServiceRequest",
    search:
      "?requisition=http://openelis-global.org/samp_labNo|<n° de bon>  ·  ?identifier=…/analysis_uuid|<uuid>",
    idSystem:
      "requisition = samp_labNo (bon) · identifier = analysis_uuid (test)",
    desc: "Une demande par analyse ; toutes les demandes d'un bon partagent la même requisition (n° de labo). status par test.",
  },
  {
    type: "DiagnosticReport",
    search:
      "?identifier=http://openelis-global.org/sampleItem_uuid|<uuid>  ·  ?identifier=…/samp_labNo|<n°>  ·  ?subject=Patient/<id>",
    idSystem:
      "identifier = sampleItem_uuid (clé, 1 DR/échantillon) + samp_labNo",
    desc: "1 rapport par échantillon (sampleItem). category = discipline. result = les Observations de l'échantillon.",
  },
  {
    type: "Observation",
    search:
      "?identifier=http://openelis-global.org/result_uuid|<uuid>  ·  ?subject=Patient/<id>  ·  ?code=<LOINC>",
    idSystem: "identifier = result_uuid",
    desc: "Un résultat par analyte. code = LOINC (+ code local) si mappé. value + unité UCUM.",
  },
  {
    type: "Specimen",
    search: "?identifier=http://openelis-global.org/sampleItem_uuid|<uuid>",
    idSystem: "identifier = sampleItem_uuid",
    desc: "Un échantillon (prélèvement) physique du bon.",
  },
  {
    type: "Task",
    search:
      "?identifier=http://openelis-global.org/order_accessionNumber|<n° de bon>",
    idSystem: "identifier = order_uuid + order_accessionNumber (samp_labNo)",
    desc: "Suivi du bon (workflow labo). basedOn = les ServiceRequest ; output = les DiagnosticReport par échantillon.",
  },
  {
    type: "Practitioner",
    search: "?identifier=http://openelis-global.org/provider_uuid|<uuid>",
    idSystem: "identifier = provider_uuid",
    desc: "Prescripteur (requester des ServiceRequest).",
  },
  {
    type: "Organization",
    search: "?identifier=<system DATIM/local>|<code>",
    idSystem: "datim_org_code / identifiant local",
    desc: "Structure demandeuse / site référant.",
  },
];

// C — Flux sortants : OpenELIS appelle un service tiers / le serveur consolidé.
// Ce ne sont PAS des endpoints exposés (aucune écoute), mais des clients HTTP
// déclenchés par des tâches planifiées ou des événements métier.
const OUTBOUND = [
  {
    target: "{cibles fhir_push_target}",
    method: "POST (Bundle transaction FHIR)",
    trigger: "@Scheduled (~60 s)",
    desc: "Moteur de push FHIR natif : lit le delta du store FHIR local et pousse un Bundle transaction (PUT idempotent) vers chaque serveur FHIR configuré. Auth par cible (Bearer / Basic / aucune), HTTPS forcé sauf autorisation HTTP explicite.",
  },
  {
    target: "{serveur consolidé}/syncOrders",
    method: "POST JSON (Basic → JWT)",
    trigger: "@Scheduled",
    desc: "Remontée consolidée des ordres / résultats vers le serveur consolidé. Ré-authentification et rejeu automatiques une fois sur 401.",
  },
  {
    target: "{serveur consolidé}/v1/vl-requests",
    method: "GET JSON (Bearer JWT)",
    trigger: "@Scheduled",
    desc: "Pull entrant des demandes de charge virale (VL) depuis le serveur consolidé (pagination par curseur). Importées comme ElectronicOrder.",
  },
  {
    target: "{serveur consolidé}/v1/vl-requests/ack",
    method: "POST JSON (Bearer JWT)",
    trigger: "Après import d'une demande VL",
    desc: "Accusé de réception qui retire la demande du flux de pull côté serveur consolidé.",
  },
  {
    target: "{serveur consolidé}/v1/vl-requests/events",
    method: "POST JSON (Bearer JWT)",
    trigger: "Changement de statut / résultat",
    desc: "Pousse un événement de statut ou de résultat VL (dérivé du statut Analysis) vers le serveur consolidé.",
  },
  {
    target: "{serveurs FHIR distants (referral)}",
    method: "GET / search FHIR",
    trigger: "@Scheduled (~120 s)",
    desc: "Poll des tâches referral (workflow OpenELIS↔OpenELIS) sur les stores FHIR distants configurés, pour rapatrier les résultats des demandes émises.",
  },
];

// B — Configuration admin : écrans dédiés (déjà dans le menu Interopérabilité).
const CONFIG_SCREENS = [
  {
    labelId: "fhir.push.title",
    hash: "#FhirPushTargets",
    baseUrl: "/rest/fhir-push-targets",
  },
  {
    labelId: "consolidatedSync.title",
    hash: "#ConsolidatedSyncConfig",
    baseUrl: "/rest/consolidated-sync-config",
  },
  {
    labelId: "fhir.gateway.title",
    hash: "#FhirGateway",
    baseUrl: "/rest/fhir-gateway/*",
  },
  {
    labelId: "fhir.sync.monitor.title",
    hash: "#FhirSyncMonitor",
    baseUrl: "/rest/fhir-sync/*",
  },
  {
    labelId: "terminology.import.title",
    hash: "#TerminologyImport",
    baseUrl: "/rest/terminology-import/*",
  },
];

const methodTagType = (method) => {
  if (method.startsWith("GET")) return "green";
  if (method.startsWith("POST")) return "blue";
  if (method.startsWith("PUT")) return "purple";
  return "gray";
};

const authTag = (auth, label) => {
  const type =
    auth === "token" ? "teal" : auth === "internal" ? "gray" : "cyan";
  return (
    <Tag type={type} size="sm" title={label}>
      {label}
    </Tag>
  );
};

// Échappe le pipe pour ne pas casser les tableaux Markdown.
const mdCell = (s) => String(s == null ? "" : s).replace(/\|/g, "\\|");

// Génère la documentation Markdown à partir des mêmes données que l'écran.
// `intl` sert à traduire les libellés des écrans de config (section B).
const buildMarkdown = (intl) => {
  const host =
    typeof window !== "undefined" && window.location
      ? window.location.host
      : "<hôte>";
  const now =
    typeof window !== "undefined" ? new Date().toISOString().slice(0, 10) : "";
  const L = [];
  L.push(`# OpenELIS — API & endpoints d'interopérabilité`);
  L.push("");
  L.push(`> Hôte : \`${host}\` · Généré le ${now}`);
  L.push("");
  L.push(
    `Référence des points d'échange FHIR/REST d'OpenELIS pour les systèmes tiers (HIE, SHR, SIGDEP). Le chemin de contexte des URL \`/rest/…\` est \`/api/OpenELIS-Global/\`. Le \`/fhir\` est le serveur HAPI, exposé uniquement via nginx (jeton + liste d'adresses autorisées).`,
  );
  L.push("");

  L.push(`## 1. Exposés aux tiers (entrants)`);
  L.push("");
  L.push(`| Méthode | URL | Authentification | Description |`);
  L.push(`|---|---|---|---|`);
  EXPOSED.forEach((e) =>
    L.push(
      `| ${e.method} | \`${mdCell(e.url)}\` | ${mdCell(e.authLabel)} | ${mdCell(e.desc)} |`,
    ),
  );
  L.push("");

  L.push(`### Ressources FHIR lisibles sous /fhir`);
  L.push("");
  L.push(
    `Ressources FHIR R4 produites par OpenELIS et interrogeables en lecture (\`GET /fhir/<Ressource>?…\`). Les ressources réellement accessibles à un tiers donné sont restreintes par sa politique d'accès (passerelle).`,
  );
  L.push("");
  L.push(
    `| Ressource | Recherches typiques | Identifiants métier | Description |`,
  );
  L.push(`|---|---|---|---|`);
  FHIR_RESOURCES.forEach((r) =>
    L.push(
      `| ${r.type} | \`${mdCell(r.search)}\` | \`${mdCell(r.idSystem)}\` | ${mdCell(r.desc)} |`,
    ),
  );
  L.push("");

  L.push(`## 2. Flux sortants (OpenELIS → externe)`);
  L.push("");
  L.push(
    `OpenELIS appelle ces services (aucun endpoint exposé) : tâches planifiées et événements métier.`,
  );
  L.push("");
  L.push(`| Cible | Méthode | Déclencheur | Description |`);
  L.push(`|---|---|---|---|`);
  OUTBOUND.forEach((f) =>
    L.push(
      `| \`${mdCell(f.target)}\` | ${mdCell(f.method)} | ${mdCell(f.trigger)} | ${mdCell(f.desc)} |`,
    ),
  );
  L.push("");

  L.push(`## 3. Configuration (écrans dédiés)`);
  L.push("");
  L.push(
    `Les flux ci-dessus se configurent depuis ces écrans (session administrateur requise). Les endpoints REST sous-jacents ne sont pas destinés aux tiers.`,
  );
  L.push("");
  L.push(`| Écran | Endpoints REST |`);
  L.push(`|---|---|`);
  CONFIG_SCREENS.forEach((s) =>
    L.push(
      `| ${mdCell(intl.formatMessage({ id: s.labelId }))} | \`${mdCell(s.baseUrl)}\` |`,
    ),
  );
  L.push("");
  return L.join("\n");
};

const downloadMarkdown = (intl) => {
  const md = buildMarkdown(intl);
  const blob = new Blob([md], { type: "text/markdown;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "openelis-interop-endpoints.md";
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
};

function InteropEndpoints() {
  const intl = useIntl();

  return (
    <>
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage
                  id="interop.endpoints.title"
                  defaultMessage="API & endpoints d'interopérabilité"
                />
              </Heading>
            </Section>
            <p style={{ margin: "0.5rem 0 1rem" }}>
              <FormattedMessage
                id="interop.endpoints.intro"
                defaultMessage="Référence des points d'échange FHIR/REST d'OpenELIS pour les systèmes tiers (HIE, SHR, SIGDEP). Le chemin de contexte des URL /rest/… est /api/OpenELIS-Global/. Le /fhir est le serveur HAPI, exposé uniquement via nginx (jeton + liste d'adresses autorisées)."
              />
            </p>
            <Button
              kind="tertiary"
              size="sm"
              renderIcon={Download}
              onClick={() => downloadMarkdown(intl)}
            >
              <FormattedMessage
                id="interop.endpoints.export"
                defaultMessage="Exporter (Markdown)"
              />
            </Button>
          </Column>
        </Grid>

        {/* A — Exposés aux tiers */}
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Heading style={{ fontSize: "1.1rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="interop.endpoints.exposed.title"
                defaultMessage="Exposés aux tiers (entrants)"
              />
            </Heading>
            <p style={{ margin: "0.25rem 0 0.75rem", color: "#525252" }}>
              <FormattedMessage
                id="interop.endpoints.exposed.desc"
                defaultMessage="Ce que les systèmes externes appellent sur OpenELIS. Protégés par nginx (jeton dynamique + liste d'adresses autorisées)."
              />
            </p>
            <TableContainer>
              <Table size="lg">
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      <FormattedMessage
                        id="interop.endpoints.col.method"
                        defaultMessage="Méthode"
                      />
                    </TableHeader>
                    <TableHeader>URL</TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="interop.endpoints.col.auth"
                        defaultMessage="Authentification"
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="interop.endpoints.col.desc"
                        defaultMessage="Description"
                      />
                    </TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {EXPOSED.map((e, i) => (
                    <TableRow key={`exp-${i}`}>
                      <TableCell>
                        <Tag type={methodTagType(e.method)} size="sm">
                          {e.method}
                        </Tag>
                      </TableCell>
                      <TableCell>
                        <code>{e.url}</code>
                      </TableCell>
                      <TableCell>{authTag(e.auth, e.authLabel)}</TableCell>
                      <TableCell>{e.desc}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Column>
        </Grid>

        {/* A' — Ressources FHIR lisibles via /fhir */}
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Heading style={{ fontSize: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage
                id="interop.endpoints.resources.title"
                defaultMessage="Ressources FHIR lisibles sous /fhir"
              />
            </Heading>
            <p style={{ margin: "0.25rem 0 0.75rem", color: "#525252" }}>
              <FormattedMessage
                id="interop.endpoints.resources.desc"
                defaultMessage="Ressources FHIR R4 produites par OpenELIS et interrogeables en lecture (GET /fhir/<Ressource>?…). Les ressources réellement accessibles à un tiers donné sont restreintes par sa politique d'accès (passerelle)."
              />
            </p>
            <TableContainer>
              <Table size="lg">
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      <FormattedMessage
                        id="interop.endpoints.col.resource"
                        defaultMessage="Ressource"
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="interop.endpoints.col.search"
                        defaultMessage="Recherches typiques (GET /fhir/…)"
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="interop.endpoints.col.idsystem"
                        defaultMessage="Identifiants métier"
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="interop.endpoints.col.desc"
                        defaultMessage="Description"
                      />
                    </TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {FHIR_RESOURCES.map((r, i) => (
                    <TableRow key={`res-${i}`}>
                      <TableCell>
                        <Tag type="magenta" size="sm">
                          {r.type}
                        </Tag>
                      </TableCell>
                      <TableCell>
                        <code style={{ fontSize: "0.8rem" }}>{r.search}</code>
                      </TableCell>
                      <TableCell>
                        <code style={{ fontSize: "0.78rem", color: "#6f6f6f" }}>
                          {r.idSystem}
                        </code>
                      </TableCell>
                      <TableCell>{r.desc}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Column>
        </Grid>

        {/* C — Flux sortants */}
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Heading style={{ fontSize: "1.1rem", marginTop: "2rem" }}>
              <FormattedMessage
                id="interop.endpoints.outbound.title"
                defaultMessage="Flux sortants (OpenELIS → externe)"
              />
            </Heading>
            <p style={{ margin: "0.25rem 0 0.75rem", color: "#525252" }}>
              <FormattedMessage
                id="interop.endpoints.outbound.desc"
                defaultMessage="OpenELIS appelle ces services (aucun endpoint exposé) : tâches planifiées et événements métier."
              />
            </p>
            <TableContainer>
              <Table size="lg">
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      <FormattedMessage
                        id="interop.endpoints.col.target"
                        defaultMessage="Cible"
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="interop.endpoints.col.method"
                        defaultMessage="Méthode"
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="interop.endpoints.col.trigger"
                        defaultMessage="Déclencheur"
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage
                        id="interop.endpoints.col.desc"
                        defaultMessage="Description"
                      />
                    </TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {OUTBOUND.map((f, i) => (
                    <TableRow key={`out-${i}`}>
                      <TableCell>
                        <code>{f.target}</code>
                      </TableCell>
                      <TableCell>{f.method}</TableCell>
                      <TableCell>{f.trigger}</TableCell>
                      <TableCell>{f.desc}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Column>
        </Grid>

        {/* B — Config admin (résumé + liens) */}
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Heading style={{ fontSize: "1.1rem", marginTop: "2rem" }}>
              <FormattedMessage
                id="interop.endpoints.config.title"
                defaultMessage="Configuration (écrans dédiés)"
              />
            </Heading>
            <p style={{ margin: "0.25rem 0 0.75rem", color: "#525252" }}>
              <FormattedMessage
                id="interop.endpoints.config.desc"
                defaultMessage="Les flux ci-dessus se configurent depuis ces écrans (session administrateur requise). Les endpoints REST sous-jacents ne sont pas destinés aux tiers."
              />
            </p>
            <Tile>
              <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
                {CONFIG_SCREENS.map((s, i) => (
                  <li key={`cfg-${i}`} style={{ padding: "0.35rem 0" }}>
                    <Link href={`/MasterListsPage${s.hash}`}>
                      {intl.formatMessage({ id: s.labelId })}
                    </Link>
                    {"  "}
                    <code style={{ color: "#6f6f6f", marginLeft: "0.5rem" }}>
                      {s.baseUrl}
                    </code>
                  </li>
                ))}
              </ul>
            </Tile>
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default injectIntl(InteropEndpoints);
