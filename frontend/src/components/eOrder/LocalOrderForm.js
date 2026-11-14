import React, { useEffect, useState } from "react";
import {
  Button,
  FilterableMultiSelect,
  InlineNotification,
  Modal,
  Select,
  SelectItem,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";

// « Nouvelle demande » : saisie légère d'une demande par un opérateur hors du
// laboratoire. La demande rejoint « Demandes reçues » ; le laboratoire la
// reprend par « Prendre en charge ». Rien n'est émis avant.

export const emptyLocalOrder = {
  patientId: "",
  providerLastName: "",
  providerFirstName: "",
  requestingOrganizationId: "",
  testIds: [],
  venueNumber: "",
  priority: "ROUTINE",
};

const LocalOrderForm = ({ open, onClose, onCreated }) => {
  const intl = useIntl();
  const [order, setOrder] = useState(emptyLocalOrder);
  const [search, setSearch] = useState("");
  const [patients, setPatients] = useState([]);
  const [searching, setSearching] = useState(false);
  const [sites, setSites] = useState([]);
  const [tests, setTests] = useState([]);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) {
      return;
    }
    setOrder(emptyLocalOrder);
    setPatients([]);
    setSearch("");
    setError(null);
    getFromOpenElisServer("/rest/displayList/REFERRAL_ORGANIZATIONS", (res) =>
      setSites(Array.isArray(res) ? res : []),
    );
    getFromOpenElisServer("/rest/displayList/ORDERABLE_TESTS", (res) =>
      setTests(Array.isArray(res) ? res : []),
    );
  }, [open]);

  const set = (key, value) =>
    setOrder((previous) => ({ ...previous, [key]: value }));

  // Recherche par numéro CMU, nom ou numéro interne : un terme de chiffres
  // cherche le numéro (CMU ou interne), sinon le nom.
  const searchPatients = () => {
    const term = search.trim();
    if (!term) {
      return;
    }
    const digits = term.replace(/[\s.-]/g, "");
    const byNumber = /^[0-9A-Za-z]+$/.test(digits) && /[0-9]/.test(digits);
    const params = new URLSearchParams({
      lastName: byNumber ? "" : term,
      firstName: "",
      STNumber: byNumber ? digits : "",
      subjectNumber: byNumber ? digits : "",
      nationalID: byNumber ? digits : "",
      labNumber: "",
      guid: "",
      dateOfBirth: "",
      gender: "",
      suppressExternalSearch: "true",
    });
    setSearching(true);
    getFromOpenElisServer(
      "/rest/patient-search-results?" + params.toString(),
      (res) => {
        setSearching(false);
        setPatients(
          res && Array.isArray(res.patientSearchResults)
            ? res.patientSearchResults
            : [],
        );
      },
    );
  };

  const submit = () => {
    if (!order.patientId || order.testIds.length === 0) {
      setError(intl.formatMessage({ id: "localOrder.required" }));
      return;
    }
    setSaving(true);
    postToOpenElisServer(
      "/rest/LocalOrderEntry",
      JSON.stringify(order),
      (status, body) => {
        setSaving(false);
        if (status === 200) {
          onCreated(body && body.requestNumber);
        } else {
          setError(
            (body && body.error) ||
              intl.formatMessage({ id: "localOrder.error" }),
          );
        }
      },
    );
  };

  return (
    <Modal
      open={open}
      size="lg"
      modalHeading={intl.formatMessage({ id: "localOrder.title" })}
      primaryButtonText={intl.formatMessage({ id: "localOrder.save" })}
      secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
      primaryButtonDisabled={saving}
      onRequestClose={onClose}
      onRequestSubmit={submit}
    >
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}
      <div style={{ display: "flex", gap: "0.5rem", alignItems: "flex-end" }}>
        <TextInput
          id="localOrderPatientSearch"
          labelText={intl.formatMessage({ id: "localOrder.patient.search" })}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              searchPatients();
            }
          }}
        />
        <Button kind="tertiary" size="md" onClick={searchPatients}>
          <FormattedMessage id="label.button.search" />
        </Button>
      </div>
      <p style={{ margin: "0.5rem 0" }}>
        <FormattedMessage id="localOrder.patient.create" />{" "}
        <a href="/PatientManagement" target="_blank" rel="noreferrer">
          <FormattedMessage id="localOrder.patient.createLink" />
        </a>
      </p>
      {!searching && patients.length > 0 && (
        <Select
          id="localOrderPatient"
          labelText={intl.formatMessage({ id: "localOrder.patient" })}
          value={order.patientId}
          onChange={(e) => set("patientId", e.target.value)}
        >
          <SelectItem value="" text="" />
          {patients.map((p) => (
            <SelectItem
              key={p.patientID}
              value={p.patientID}
              text={[p.lastName, p.firstName, p.nationalId, p.birthdate]
                .filter(Boolean)
                .join(" — ")}
            />
          ))}
        </Select>
      )}
      <div style={{ display: "flex", gap: "0.5rem", marginTop: "1rem" }}>
        <TextInput
          id="localOrderProviderLastName"
          labelText={intl.formatMessage({
            id: "localOrder.prescriber.lastName",
          })}
          value={order.providerLastName}
          onChange={(e) => set("providerLastName", e.target.value)}
        />
        <TextInput
          id="localOrderProviderFirstName"
          labelText={intl.formatMessage({
            id: "localOrder.prescriber.firstName",
          })}
          value={order.providerFirstName}
          onChange={(e) => set("providerFirstName", e.target.value)}
        />
      </div>
      <Select
        id="localOrderSite"
        labelText={intl.formatMessage({ id: "receivedOrders.facility" })}
        value={order.requestingOrganizationId}
        onChange={(e) => set("requestingOrganizationId", e.target.value)}
      >
        <SelectItem value="" text="" />
        {sites.map((site) => (
          <SelectItem key={site.id} value={site.id} text={site.value} />
        ))}
      </Select>
      <FilterableMultiSelect
        id="localOrderTests"
        titleText={intl.formatMessage({ id: "receivedOrders.column.exams" })}
        items={tests}
        itemToString={(item) => (item ? item.value : "")}
        onChange={({ selectedItems }) =>
          set(
            "testIds",
            selectedItems.map((item) => item.id),
          )
        }
      />
      <div style={{ display: "flex", gap: "0.5rem", marginTop: "1rem" }}>
        <TextInput
          id="localOrderVenue"
          labelText={intl.formatMessage({ id: "localOrder.venue" })}
          value={order.venueNumber}
          onChange={(e) => set("venueNumber", e.target.value)}
        />
        <Select
          id="localOrderPriority"
          labelText={intl.formatMessage({
            id: "receivedOrders.column.priority",
          })}
          value={order.priority}
          onChange={(e) => set("priority", e.target.value)}
        >
          {["ROUTINE", "ASAP", "STAT"].map((priority) => (
            <SelectItem
              key={priority}
              value={priority}
              text={intl.formatMessage({
                id: "receivedOrders.priority." + priority,
              })}
            />
          ))}
        </Select>
      </div>
    </Modal>
  );
};

export default LocalOrderForm;
