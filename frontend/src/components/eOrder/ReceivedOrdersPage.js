import React, { useContext, useEffect, useState } from "react";
import { Button } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import ReceivedOrders from "./ReceivedOrders";
import LocalOrderForm from "./LocalOrderForm";
import { NotificationKinds } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import { getFromOpenElisServer } from "../utils/Utils";

// Page de la fenêtre « Demandes reçues » (route /ElectronicOrders). Le bouton
// « Nouvelle demande » n'apparaît que si la saisie locale est activée et que
// l'utilisateur a le droit « Enregistrer des demandes ».
const ReceivedOrdersPage = () => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const [settings, setSettings] = useState({
    enabled: false,
    permitted: false,
  });
  const [formOpen, setFormOpen] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    getFromOpenElisServer("/rest/LocalOrderEntrySettings", (res) =>
      setSettings({
        enabled: !!(res && res.enabled),
        permitted: !!(res && res.permitted),
      }),
    );
  }, []);

  const newOrderButton =
    settings.enabled && settings.permitted ? (
      <Button data-testid="newLocalOrder" onClick={() => setFormOpen(true)}>
        <FormattedMessage id="localOrder.new" />
      </Button>
    ) : null;

  return (
    <>
      <ReceivedOrders newOrderButton={newOrderButton} reloadKey={reloadKey} />
      <LocalOrderForm
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onCreated={(requestNumber) => {
          setFormOpen(false);
          setReloadKey((key) => key + 1);
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage(
              { id: "localOrder.created" },
              { number: requestNumber || "" },
            ),
          });
          setNotificationVisible(true);
        }}
      />
    </>
  );
};

export default ReceivedOrdersPage;
