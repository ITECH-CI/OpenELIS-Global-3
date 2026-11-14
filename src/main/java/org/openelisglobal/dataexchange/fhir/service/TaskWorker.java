package org.openelisglobal.dataexchange.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.order.action.IOrderExistanceChecker;
import org.openelisglobal.dataexchange.order.action.IOrderExistanceChecker.CheckResult;
import org.openelisglobal.dataexchange.order.action.IOrderInterpreter.InterpreterResults;
import org.openelisglobal.dataexchange.order.action.IOrderInterpreter.OrderType;
import org.openelisglobal.dataexchange.order.action.IOrderPersister;
import org.openelisglobal.dataexchange.order.action.MessagePatient;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderCategorie;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderOrigine;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;

public class TaskWorker {

    @Autowired
    private FhirContext fhirContext;

    public enum TaskResult {
        OK, DUPLICATE_ORDER, NON_CANCELABLE_ORDER, MESSAGE_ERROR
    }

    private String message = "";
    private Task task = new Task();
    private ServiceRequest serviceRequest = null;
    private Patient patient = new Patient();
    private TaskInterpreter interpreter;
    private IOrderExistanceChecker existanceChecker;
    private IOrderPersister persister;
    private IStatusService statusService;
    private List<InterpreterResults> interpretResults;
    private CheckResult checkResult;

    public TaskWorker(Task incomingTask, String incomingMessage, ServiceRequest incomingServiceRequest,
            Patient incomingPatient) {
        task = incomingTask;
        message = incomingMessage;
        serviceRequest = incomingServiceRequest;
        patient = incomingPatient;
    }

    public void setInterpreter(TaskInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    public void setExistanceChecker(IOrderExistanceChecker orderExistanceChecker) {
        existanceChecker = orderExistanceChecker;
    }

    public void setPersister(IOrderPersister taskPersister) {
        persister = taskPersister;
    }

    private IStatusService getStatusService() {
        if (statusService == null) {
            statusService = SpringContext.getBean(IStatusService.class);
        }
        return statusService;
    }

    public void setStatusService(IStatusService statusService) {
        this.statusService = statusService;
    }

    public List<InterpreterResults> getMessageErrors() {
        return interpretResults;
    }

    public List<String> getUnsupportedTests() {
        return interpreter.getUnsupportedTests();
    }

    public List<String> getUnsupportedPanels() {
        return interpreter.getUnsupportedPanels();
    }

    public CheckResult getExistanceCheckResult() {
        return checkResult;
    }

    private TaskResult lastResult;

    public TaskResult handleOrderRequest() throws IllegalStateException {
        lastResult = handleOrder();
        return lastResult;
    }

    /**
     * Motif court d'un refus (Task.statusReason), relayé par la plateforme au
     * demandeur ; {@code null} si la demande est acceptée. Ne contient jamais de
     * donnée nominative.
     */
    public String getStatusReason() {
        if (lastResult == null || lastResult == TaskResult.OK) {
            return null;
        }
        if (lastResult == TaskResult.DUPLICATE_ORDER) {
            return "demande déjà reçue";
        }
        if (lastResult == TaskResult.NON_CANCELABLE_ORDER) {
            return "annulation impossible";
        }
        InterpreterResults first = interpretResults == null || interpretResults.isEmpty() ? null
                : interpretResults.get(0);
        if (first == null) {
            return "demande non conforme";
        }
        switch (first) {
        case UNSUPPORTED_TESTS:
            List<String> codes = interpreter == null ? null : interpreter.getUnsupportedTests();
            return codes == null || codes.isEmpty() ? "examen non reconnu : aucun code LOINC"
                    : "examen non reconnu : LOINC " + String.join(", ", codes);
        case MISSING_PATIENT_GUID:
        case MISSING_PATIENT_IDENTIFIER:
            return "patient non identifiable";
        case MISSING_PATIENT_DOB:
            return "date de naissance du patient absente";
        case MISSING_PATIENT_GENDER:
            return "sexe du patient absent";
        case MISSING_ORDER_NUMBER:
            return "numéro de demande absent";
        default:
            return "demande non conforme";
        }
    }

    private TaskResult handleOrder() throws IllegalStateException {
        if (interpreter == null || persister == null || existanceChecker == null) {
            throw new IllegalStateException("Interpreter, existanceChecker or persister have not been set");
        }

        interpretResults = interpreter.interpret(task, serviceRequest, patient);
        String referringOrderNumber = interpreter.getReferringOrderNumber();
        OrderType orderType = interpreter.getOrderType();
        OrderPriority priority = interpreter.getOrderPriority();
        MessagePatient patient = interpreter.getMessagePatient();
        checkResult = existanceChecker.check(referringOrderNumber);

        if (interpretResults.get(0) == InterpreterResults.OK) {

            // checkResult = existanceChecker.check(referringOrderNumber);
            switch (checkResult) {
            case ORDER_FOUND_QUEUED:
                if (orderType == OrderType.CANCEL) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                            "cancelling order: " + referringOrderNumber);
                    cancelOrder(referringOrderNumber);
                    return TaskResult.OK;
                } else {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                            "duplicate order found: " + referringOrderNumber);
                    return TaskResult.DUPLICATE_ORDER;
                }
            case ORDER_FOUND_INPROGRESS:
                LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                        "order: " + referringOrderNumber + " is duplicate or already in progress");
                return orderType == OrderType.CANCEL ? TaskResult.NON_CANCELABLE_ORDER : TaskResult.DUPLICATE_ORDER;
            case NOT_FOUND:
                if (orderType == OrderType.CANCEL) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                            "cant cancel order not found");
                    return TaskResult.NON_CANCELABLE_ORDER;
                } else {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                            "no order found, entering order: " + referringOrderNumber);
                    insertNewOrder(referringOrderNumber, message, patient, priority, ExternalOrderStatus.Entered);
                    return TaskResult.OK;
                }
            case ORDER_FOUND_CANCELED:
                if (orderType == OrderType.CANCEL) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                            "can't cancel already cancelled order: " + referringOrderNumber);
                    return TaskResult.NON_CANCELABLE_ORDER;
                } else {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                            "order found cancelled, entering order: " + referringOrderNumber);
                    insertNewOrder(referringOrderNumber, message, patient, priority, ExternalOrderStatus.Entered);
                    return TaskResult.OK;
                }
            default:
                LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                        "undetermined issue in correctly interpreted request: " + interpretResults.get(0).toString()
                                + " check result: " + checkResult + " for: " + referringOrderNumber + " " + orderType
                                + " ");
                insertNewOrder(referringOrderNumber, message, patient, priority, ExternalOrderStatus.NonConforming);
                return TaskResult.MESSAGE_ERROR;
            }

        } else if (interpretResults.get(0) == InterpreterResults.UNSUPPORTED_TESTS
                && checkResult == CheckResult.ORDER_FOUND_QUEUED) {
            if (orderType == OrderType.CANCEL) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                        "cancelling order: " + referringOrderNumber + " despite wrong test specified");
                cancelOrder(referringOrderNumber);
                return TaskResult.OK;
            } else {
                LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                        "order: " + referringOrderNumber + " already entered");
                return TaskResult.DUPLICATE_ORDER;
            }
        } else if (interpretResults.get(0) == InterpreterResults.UNSUPPORTED_TESTS) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                    "TaskWorker:unsupported tests: " + referringOrderNumber + orderType);
            insertNewOrder(referringOrderNumber, message, patient, priority, ExternalOrderStatus.NonConforming);
            return TaskResult.MESSAGE_ERROR;
        } else if (interpretResults.get(0) == InterpreterResults.MISSING_PATIENT_GUID
                || interpretResults.get(0) == InterpreterResults.MISSING_PATIENT_DOB
                || interpretResults.get(0) == InterpreterResults.MISSING_PATIENT_GENDER
                || interpretResults.get(0) == InterpreterResults.MISSING_PATIENT_IDENTIFIER) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest", "missing patient info: "
                    + interpretResults.get(0).toString() + "for" + referringOrderNumber + " " + orderType + " ");
            insertNewOrder(referringOrderNumber, message, patient, priority, ExternalOrderStatus.NonConforming);
            return TaskResult.MESSAGE_ERROR;
        } else {
            LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest", "undetermined issue: "
                    + interpretResults.get(0).toString() + " for: " + referringOrderNumber + " " + orderType + " ");
            insertNewOrder(referringOrderNumber, message, patient, priority, ExternalOrderStatus.NonConforming);
            return TaskResult.MESSAGE_ERROR;
        }
    }

    private void cancelOrder(String referringOrderNumber) {
        LogEvent.logDebug(this.getClass().getSimpleName(), "cancelOrder", "cancelOrder: ");
        persister.cancelOrder(referringOrderNumber);
    }

    private void insertNewOrder(String referringOrderNumber, String message, MessagePatient patient,
            OrderPriority orderPriority, ExternalOrderStatus eoStatus) {
        LogEvent.logDebug(this.getClass().getSimpleName(), "insertNewOrder",
                "TaskWorker:insertNewOrder: " + referringOrderNumber);
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setExternalId(referringOrderNumber);
        eOrder.setData(message);
        eOrder.setStatusId(getStatusService().getStatusID(eoStatus));
        eOrder.setOrderTimestamp(DateUtil.getNowAsTimestamp());
        eOrder.setSysUserId(persister.getServiceUserId());
        eOrder.setType(ElectronicOrderType.FHIR);
        eOrder.setPriority(orderPriority);
        populateDisplayFields(eOrder);
        populateCategoryAndOrigin(eOrder);

        persister.persist(patient, eOrder);
    }

    public static final String ORIGINE_TAG_SYSTEM = "https://hie.ci/echanges/origine";
    public static final String ORIGINE_TAG_PLATEFORME = "plateforme";

    private FhirConfig fhirConfig;

    public void setFhirConfig(FhirConfig fhirConfig) {
        this.fhirConfig = fhirConfig;
    }

    private FhirConfig getFhirConfig() {
        if (fhirConfig == null) {
            fhirConfig = SpringContext.getBean(FhirConfig.class);
        }
        return fhirConfig;
    }

    /**
     * Catégorie et origine de la demande. Best-effort : en cas d'erreur, la demande
     * est reçue sans catégorie (elle reste alors dans l'écran d'étude, comme
     * avant).
     */
    private void populateCategoryAndOrigin(ElectronicOrder eOrder) {
        try {
            FhirConfig config = getFhirConfig();
            classify(eOrder, task, serviceRequest, config.getEorderPlateformeIdPrefix(),
                    config.getEorderCategorieExtension());
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "populateCategoryAndOrigin",
                    "catégorie et origine non renseignées (non bloquant) : " + e.getMessage());
        }
    }

    /**
     * Demande de la plateforme (Task étiquetée origine|plateforme, ou identifiant
     * au préfixe réservé) : origine PLATEFORME, catégorie ROUTINE sauf catégorie
     * explicite dans l'extension réglée. Toute autre demande vient du flux SIGDEP :
     * origine SIGDEP, catégorie ETUDE_CV.
     */
    static void classify(ElectronicOrder eOrder, Task task, ServiceRequest serviceRequest, String platformIdPrefix,
            String categoryExtensionUrl) {
        if (isFromPlatform(task, platformIdPrefix)) {
            eOrder.setOrigine(ElectronicOrderOrigine.PLATEFORME);
            ElectronicOrderCategorie explicit = explicitCategory(task, categoryExtensionUrl);
            if (explicit == null) {
                explicit = explicitCategory(serviceRequest, categoryExtensionUrl);
            }
            eOrder.setCategorie(explicit != null ? explicit : ElectronicOrderCategorie.ROUTINE);
        } else {
            eOrder.setOrigine(ElectronicOrderOrigine.SIGDEP);
            eOrder.setCategorie(ElectronicOrderCategorie.ETUDE_CV);
        }
    }

    static boolean isFromPlatform(Task task, String platformIdPrefix) {
        if (task == null) {
            return false;
        }
        if (task.hasMeta()) {
            for (Coding tag : task.getMeta().getTag()) {
                if (ORIGINE_TAG_SYSTEM.equals(tag.getSystem())
                        && ORIGINE_TAG_PLATEFORME.equalsIgnoreCase(tag.getCode())) {
                    return true;
                }
            }
        }
        String id = task.getIdElement() == null ? null : task.getIdElement().getIdPart();
        return platformIdPrefix != null && !platformIdPrefix.trim().isEmpty() && id != null
                && id.startsWith(platformIdPrefix.trim());
    }

    private static ElectronicOrderCategorie explicitCategory(DomainResource resource, String extensionUrl) {
        if (resource == null || extensionUrl == null || extensionUrl.trim().isEmpty()) {
            return null;
        }
        Extension extension = resource.getExtensionByUrl(extensionUrl.trim());
        if (extension == null || extension.getValue() == null) {
            return null;
        }
        Type value = extension.getValue();
        if (value instanceof Coding) {
            return ElectronicOrderCategorie.fromCode(((Coding) value).getCode());
        }
        if (value instanceof CodeableConcept) {
            return ElectronicOrderCategorie.fromCode(((CodeableConcept) value).getCodingFirstRep().getCode());
        }
        return value.isPrimitive() ? ElectronicOrderCategorie.fromCode(value.primitiveValue()) : null;
    }

    /**
     * Renseigne les colonnes d'AFFICHAGE dénormalisées de l'ordre (module d'échange
     * unifié, incrément 4a), pour lister les demandes SANS appel au serveur FHIR
     * (cf. DESIGN_INTEROP_MODULE_CIV.md §6.1). Best-effort et générique (tout type
     * d'examen) : chaque champ absent reste {@code null}, le détail complet demeure
     * disponible dans {@code electronic_order.data}. Ne doit jamais faire échouer
     * la réception d'un ordre — d'où le try/catch défensif.
     */
    private void populateDisplayFields(ElectronicOrder eOrder) {
        try {
            // Nom du test : le test déjà résolu par l'interpréteur (LOINC -> Test).
            // On utilise EXACTEMENT la même source que l'affichage de la liste
            // (getLocalizedTestName().getLocalizedValue(), cf.
            // StudyElectronicOrdersController) pour que le libellé dénormalisé et
            // celui du repli FHIR soient identiques — sinon deux demandes du même test
            // s'afficheraient différemment selon leur ancienneté. Pas de type de
            // prélèvement accolé : à la réception aucun échantillon n'existe encore.
            if (interpreter != null && interpreter.getTest() != null
                    && interpreter.getTest().getLocalizedTestName() != null) {
                eOrder.setTestName(interpreter.getTest().getLocalizedTestName().getLocalizedValue());
            }
            if (serviceRequest != null) {
                // Date de prélèvement : occurrenceDateTime du ServiceRequest si présent.
                if (serviceRequest.hasOccurrenceDateTimeType()
                        && serviceRequest.getOccurrenceDateTimeType().getValue() != null) {
                    eOrder.setCollectionDate(
                            new java.sql.Timestamp(serviceRequest.getOccurrenceDateTimeType().getValue().getTime()));
                }
                // Nom du site demandeur : display du requester s'il est porté par le SR.
                if (serviceRequest.hasRequester() && serviceRequest.getRequester().hasDisplay()) {
                    eOrder.setRequestingFacilityName(serviceRequest.getRequester().getDisplay());
                }
            }
        } catch (Exception e) {
            // Best-effort strict : renseigner ces colonnes d'affichage ne doit JAMAIS
            // faire échouer la réception d'un ordre. On rattrape donc tout (y compris une
            // éventuelle exception de résolution du nom localisé / session Hibernate).
            LogEvent.logWarn(this.getClass().getSimpleName(), "populateDisplayFields",
                    "colonnes d'affichage e-order non renseignées (non bloquant) : " + e.getMessage());
        }
    }
}
