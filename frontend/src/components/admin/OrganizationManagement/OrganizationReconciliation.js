import React, { useEffect, useState } from "react";
import {
  Button,
  InlineNotification,
  Modal,
  RadioButton,
  RadioButtonGroup,
  Search,
  Select,
  SelectItem,
  StructuredListBody,
  StructuredListCell,
  StructuredListHead,
  StructuredListRow,
  StructuredListWrapper,
  Tag,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";

// Réconciliation d'une organisation avec le référentiel national des
// établissements (MFL) : recherche de l'organisation cible, aperçu (historique
// de chacune, survivante proposée), fusion ; fusions récentes annulables.

const STATUT_COULEUR = { MFL: "green", FUSIONNE: "teal", LEGACY: "gray" };

export function StatutReferentiel({ statut }) {
  if (!statut) {
    return "";
  }
  return (
    <Tag size="sm" type={STATUT_COULEUR[statut] || "gray"}>
      <FormattedMessage id={"organization.referentiel.statut." + statut} />
    </Tag>
  );
}

function libelleOrganisation(o) {
  return (o.code ? o.code + " - " : "") + o.nom;
}

function Historique({ detail }) {
  if (!detail) {
    return "";
  }
  return (
    <ul className="referentiel-historique">
      {Object.entries(detail).map(([cle, valeur]) => (
        <li key={cle}>
          <FormattedMessage id={"organization.referentiel.historique." + cle} />{" "}
          : {valeur}
        </li>
      ))}
    </ul>
  );
}

export function OrganizationReconciliationModal({
  open,
  sourceId,
  onClose,
  onDone,
}) {
  const intl = useIntl();
  const [terme, setTerme] = useState("");
  const [niveau, setNiveau] = useState("");
  const [resultats, setResultats] = useState([]);
  const [cible, setCible] = useState(null);
  const [apercu, setApercu] = useState(null);
  const [survivante, setSurvivante] = useState("");
  const [motif, setMotif] = useState("");
  const [erreur, setErreur] = useState("");
  const [enCours, setEnCours] = useState(false);

  useEffect(() => {
    if (!open) {
      setTerme("");
      setResultats([]);
      setCible(null);
      setApercu(null);
      setSurvivante("");
      setMotif("");
      setErreur("");
    }
  }, [open]);

  useEffect(() => {
    if (!open || terme.trim().length < 2) {
      setResultats([]);
      return;
    }
    const minuteur = setTimeout(() => {
      getFromOpenElisServer(
        "/rest/organization-referentiel/recherche?terme=" +
          encodeURIComponent(terme.trim()) +
          (niveau ? "&niveau=" + niveau : ""),
        (res) =>
          setResultats(
            Array.isArray(res) ? res.filter((o) => o.id !== sourceId) : [],
          ),
      );
    }, 300);
    return () => clearTimeout(minuteur);
  }, [terme, niveau, open, sourceId]);

  useEffect(() => {
    if (!cible) {
      setApercu(null);
      return;
    }
    setErreur("");
    getFromOpenElisServer(
      "/rest/organization-referentiel/apercu?a=" + sourceId + "&b=" + cible.id,
      (res) => {
        if (!res || res.erreur) {
          setErreur(res ? res.erreur : "");
          setApercu(null);
        } else {
          setApercu(res);
          setSurvivante(res.survivanteProposee);
        }
      },
    );
  }, [cible, sourceId]);

  const fusionner = () => {
    setEnCours(true);
    setErreur("");
    postToOpenElisServerJsonResponse(
      "/rest/organization-referentiel/fusion",
      JSON.stringify({ a: sourceId, b: cible.id, survivante, motif }),
      (res) => {
        setEnCours(false);
        if (!res || res.erreur) {
          setErreur(res ? res.erreur : "");
        } else {
          onDone(res);
        }
      },
    );
  };

  const empechements = apercu ? apercu.empechements || [] : [];

  return (
    <Modal
      open={open}
      size="lg"
      modalHeading={intl.formatMessage({
        id: "organization.referentiel.fusion.titre",
      })}
      primaryButtonText={intl.formatMessage({
        id: "organization.referentiel.fusion.confirmer",
      })}
      secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
      primaryButtonDisabled={
        !apercu || empechements.length > 0 || !survivante || enCours
      }
      onRequestClose={onClose}
      onRequestSubmit={fusionner}
    >
      <p>
        <FormattedMessage id="organization.referentiel.fusion.aide" />
      </p>
      <br />
      <div style={{ display: "flex", gap: "1rem" }}>
        <Search
          id="referentiel-recherche"
          labelText={intl.formatMessage({
            id: "organization.referentiel.fusion.recherche",
          })}
          placeholder={intl.formatMessage({
            id: "organization.referentiel.fusion.recherche",
          })}
          value={terme}
          onChange={(e) => setTerme(e.target.value)}
        />
        <Select
          id="referentiel-niveau"
          labelText=""
          hideLabel
          value={niveau}
          onChange={(e) => setNiveau(e.target.value)}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "organization.referentiel.niveau.tous",
            })}
          />
          {["ETABLISSEMENT", "DISTRICT", "REGION"].map((n) => (
            <SelectItem
              key={n}
              value={n}
              text={intl.formatMessage({
                id: "organization.referentiel.niveau." + n,
              })}
            />
          ))}
        </Select>
      </div>
      {resultats.length > 0 && !cible && (
        <StructuredListWrapper selection>
          <StructuredListBody>
            {resultats.map((o) => (
              <StructuredListRow key={o.id} onClick={() => setCible(o)}>
                <StructuredListCell>
                  {libelleOrganisation(o)}
                </StructuredListCell>
                <StructuredListCell>
                  <StatutReferentiel statut={o.statut} />
                </StructuredListCell>
                <StructuredListCell>
                  {[o.parent, o.grandParent].filter(Boolean).join(" / ")}
                </StructuredListCell>
                <StructuredListCell>
                  <FormattedMessage
                    id="organization.referentiel.historique.total"
                    values={{ n: o.historique }}
                  />
                </StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      )}
      {cible && (
        <Button kind="ghost" size="sm" onClick={() => setCible(null)}>
          <FormattedMessage id="organization.referentiel.fusion.autreCible" />
        </Button>
      )}
      {apercu && (
        <>
          <StructuredListWrapper>
            <StructuredListHead>
              <StructuredListRow head>
                {apercu.organisations.map((o) => (
                  <StructuredListCell head key={o.id}>
                    {libelleOrganisation(o)}{" "}
                    <StatutReferentiel statut={o.statut} />
                  </StructuredListCell>
                ))}
              </StructuredListRow>
            </StructuredListHead>
            <StructuredListBody>
              <StructuredListRow>
                {apercu.organisations.map((o) => (
                  <StructuredListCell key={o.id}>
                    {[o.parent, o.grandParent].filter(Boolean).join(" / ")}
                    <Historique detail={o.detail} />
                  </StructuredListCell>
                ))}
              </StructuredListRow>
            </StructuredListBody>
          </StructuredListWrapper>
          <RadioButtonGroup
            legendText={intl.formatMessage({
              id: "organization.referentiel.fusion.survivante",
            })}
            name="survivante"
            valueSelected={survivante}
            onChange={(v) => setSurvivante(v)}
          >
            {apercu.organisations.map((o) => (
              <RadioButton
                key={o.id}
                id={"survivante-" + o.id}
                value={o.id}
                labelText={
                  libelleOrganisation(o) +
                  (o.id === apercu.survivanteProposee
                    ? " (" +
                      intl.formatMessage({
                        id: "organization.referentiel.fusion.proposee",
                      }) +
                      ")"
                    : "")
                }
              />
            ))}
          </RadioButtonGroup>
          <br />
          <TextInput
            id="referentiel-motif"
            labelText={intl.formatMessage({
              id: "organization.referentiel.fusion.motif",
            })}
            value={motif}
            onChange={(e) => setMotif(e.target.value)}
          />
        </>
      )}
      {empechements.map((e) => (
        <InlineNotification key={e} kind="warning" title="" subtitle={e} />
      ))}
      {erreur && <InlineNotification kind="error" title="" subtitle={erreur} />}
    </Modal>
  );
}

export function FusionsRecentes({ version, onAnnulee }) {
  const intl = useIntl();
  const [fusions, setFusions] = useState([]);
  const [erreur, setErreur] = useState("");

  useEffect(() => {
    getFromOpenElisServer("/rest/organization-referentiel/fusions", (res) =>
      setFusions(Array.isArray(res) ? res : []),
    );
  }, [version]);

  const annuler = (fusionId) => {
    if (
      !window.confirm(
        intl.formatMessage({
          id: "organization.referentiel.annuler.confirmer",
        }),
      )
    ) {
      return;
    }
    postToOpenElisServerJsonResponse(
      "/rest/organization-referentiel/fusion/" + fusionId + "/annulation",
      "{}",
      (res) => {
        if (!res || res.erreur) {
          setErreur(res ? res.erreur : "");
        } else {
          onAnnulee(res);
        }
      },
    );
  };

  if (fusions.length === 0) {
    return "";
  }
  return (
    <>
      <h5>
        <FormattedMessage id="organization.referentiel.fusions.titre" />
      </h5>
      {erreur && <InlineNotification kind="error" title="" subtitle={erreur} />}
      <StructuredListWrapper>
        <StructuredListBody>
          {fusions.map((f) => (
            <StructuredListRow key={f.fusionId}>
              <StructuredListCell>{f.date.substring(0, 16)}</StructuredListCell>
              <StructuredListCell>
                {f.sourceNom} → {f.survivanteNom}
              </StructuredListCell>
              <StructuredListCell>{f.motif}</StructuredListCell>
              <StructuredListCell>
                {f.annulee ? (
                  <Tag size="sm" type="gray">
                    <FormattedMessage id="organization.referentiel.annulee" />
                  </Tag>
                ) : (
                  <Button
                    kind="ghost"
                    size="sm"
                    onClick={() => annuler(f.fusionId)}
                  >
                    <FormattedMessage id="organization.referentiel.annuler" />
                  </Button>
                )}
              </StructuredListCell>
            </StructuredListRow>
          ))}
        </StructuredListBody>
      </StructuredListWrapper>
    </>
  );
}
