import { Select, SelectItem, Stack } from "@carbon/react";
import { useEffect, useRef, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import "../../App.css";
import "../../index.css";
import Questionnaire from "../common/Questionnaire";
import "../Style.css";
import { getFromOpenElisServer } from "../utils/Utils";

export const ProgramSelect = ({
  programChange = () => {
    console.debug("default programChange function does nothing");
  },
  orderFormValues,
  setOrderFormValues = () => {},
  editable,
}) => {
  const componentMounted = useRef(false);

  const intl = useIntl();

  const [programs, setPrograms] = useState([]);
  const [programCodes, setProgramCodes] = useState({});

  const fetchPrograms = (programsList) => {
    if (componentMounted.current) {
      setPrograms(programsList);
    }
  };
  const fetchProgramCodes = (programCodesList) => {
    if (!componentMounted.current) {
      return;
    }
    // Build id -> code map; fall back to provided shape if available.
    const codeMap = (programCodesList || []).reduce((acc, item) => {
      if (item?.id) {
        acc[item.id] = item.code || item.programCode || item.value || "";
      }
      return acc;
    }, {});
    setProgramCodes(codeMap);
    // If a program is already selected but code missing, fill it
    if (
      orderFormValues?.sampleOrderItems?.programId &&
      !orderFormValues?.sampleOrderItems?.programCode
    ) {
      const code =
        codeMap[orderFormValues.sampleOrderItems.programId] ||
        orderFormValues.sampleOrderItems.programCode ||
        "";
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          programCode: code,
        },
      });
    }
  };

  useEffect(() => {
    if (!orderFormValues?.sampleOrderItems?.programId) {
      const defaultProgram = programs.find((program) => {
        return program.value === "Routine Testing";
      });
      const defaultCode =
        (defaultProgram && programCodes[defaultProgram.id]) ||
        defaultProgram?.code ||
        "";
      if (defaultProgram?.id) {
        const syntheticEvent = {
          target: {
            value: defaultProgram.id,
          },
        };
        // Update local form values so the select shows the selection immediately.
        setOrderFormValues({
          ...orderFormValues,
          sampleOrderItems: {
            ...orderFormValues.sampleOrderItems,
            programId: defaultProgram.id,
            programCode: defaultCode,
          },
        });
        programChange(syntheticEvent, defaultProgram, defaultCode);
      }
    }
  }, [programs, programCodes]);

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/user-programs", fetchPrograms);
    getFromOpenElisServer("/rest/program_codes", fetchProgramCodes);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (
      programs.length > 0 &&
      orderFormValues?.sampleOrderItems?.programId &&
      !orderFormValues?.sampleOrderItems?.programCode
    ) {
      const code =
        programCodes[orderFormValues.sampleOrderItems.programId] || "";
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          programCode: code,
        },
      });
    }
  }, [programs, programCodes, orderFormValues?.sampleOrderItems?.programId]);

  return (
    <>
      <div className="formInlineDiv">
        {programs.length > 0 && (
          <div className="inputText">
            <Select
              id="additionalQuestionsSelect"
              labelText={intl.formatMessage({ id: "label.program" })}
              onChange={(e) => {
                const selectedProgram = programs.find(
                  (program) => program.id === e.target.value,
                );
                const selectedProgramCode =
                  programCodes[e.target.value] || selectedProgram?.code || "";
                // Update selection locally so the value is reflected in the UI immediately.
                setOrderFormValues({
                  ...orderFormValues,
                  sampleOrderItems: {
                    ...orderFormValues.sampleOrderItems,
                    programId: e.target.value,
                    programCode: selectedProgramCode,
                  },
                });
                programChange(e, selectedProgram, selectedProgramCode);
              }}
              value={orderFormValues?.sampleOrderItems?.programId}
              disabled={editable ? editable : false}
            >
              <SelectItem value="" text="" />
              {programs.map((program) => {
                return (
                  <SelectItem
                    key={program.id}
                    value={program.id}
                    text={program.value}
                  />
                );
              })}
            </Select>
          </div>
        )}
      </div>
    </>
  );
};

const OrderEntryAdditionalQuestions = ({
  orderFormValues,
  setOrderFormValues = () => {
    console.debug("default setOrderFormValues change function does nothing");
  },
}) => {
  const [questionnaire, setQuestionnaire] = useState(
    orderFormValues?.sampleOrderItems?.questionnaire,
  );
  const [questionnaireResponse, setQuestionnaireResponse] = useState(
    orderFormValues?.sampleOrderItems?.additionalQuestions,
  );

  const handleProgramSelection = (event, selectedProgram, programCode) => {
    if (!event?.target?.value) {
      setAdditionalQuestions({});
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          programId: "",
          programCode: "",
        },
      });
    } else {
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          programId: event.target.value,
          programCode: programCode || selectedProgram?.code || "",
        },
      });
      getFromOpenElisServer(
        "/rest/program/" + event.target.value + "/questionnaire",
        (res) => setAdditionalQuestions(res, event),
      );
    }
  };

  function convertQuestionnaireToResponse(questionnaire) {
    var items = [];
    if (questionnaire && "item" in questionnaire) {
      for (let i = 0; i < questionnaire.item.length; i++) {
        let currentItem = questionnaire.item[i];
        items.push({
          linkId: currentItem.linkId,
          definition: currentItem.definition,
          text: currentItem.text,
          answer: [],
        });
      }

      var convertedQuestionnaireResponse = {
        resourceType: "QuestionnaireResponse",
        id: "",
        questionnaire: "Questionnaire/" + questionnaire.id,
        status: "in-progress",
        item: items,
      };
      return convertedQuestionnaireResponse;
    }
    return null;
  }

  function setAdditionalQuestions(res, event) {
    console.debug(res);
    if ("item" in res) {
      setQuestionnaire(res);
      var convertedQuestionnaireResponse = convertQuestionnaireToResponse(res);
      setQuestionnaireResponse(convertedQuestionnaireResponse);
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          questionnaire: res,
          programId: event ? event.target.value : "",
          additionalQuestions: convertedQuestionnaireResponse,
        },
      });
    }
  }
  const getAnswer = (linkId) => {
    var responseItem = questionnaireResponse?.item?.find(
      (item) => item.linkId === linkId,
    );
    var questionnaireItem = questionnaire?.item?.find(
      (item) => item.linkId === linkId,
    );
    switch (questionnaireItem.type) {
      case "boolean":
        return responseItem?.answer
          ? responseItem?.answer[0]?.valueBoolean
          : "";
      case "decimal":
        return responseItem?.answer
          ? responseItem?.answer[0]?.valueDecimal
          : "";
      case "integer":
        return responseItem?.answer
          ? responseItem?.answer[0]?.valueInteger
          : "";
      case "date":
        return responseItem?.answer ? responseItem?.answer[0]?.valueDate : "";
      case "time":
        return responseItem?.answer ? responseItem?.answer[0]?.valueTime : "";
      case "string":
      case "text":
        return responseItem?.answer ? responseItem?.answer[0]?.valueString : "";
      case "quantity":
        return responseItem?.answer
          ? responseItem?.answer[0]?.valueQuantity
          : "";
      case "choice":
        if (responseItem?.answer) {
          return responseItem?.answer[0]?.valueCoding
            ? responseItem?.answer[0]?.valueCoding.code
            : responseItem?.answer[0]?.valueString;
        }
    }
  };

  const answerChange = (e) => {
    const { id, value } = e.target;

    var updatedQuestionnaireResponse = { ...questionnaireResponse };
    var responseItem = updatedQuestionnaireResponse.item.find(
      (item) => item.linkId === id,
    );
    var questionnaireItem = questionnaire.item.find(
      (item) => item.linkId === id,
    );
    responseItem.answer = [];
    if (value !== "") {
      switch (questionnaireItem.type) {
        case "boolean":
          responseItem.answer.push({ valueBoolean: value });
          break;
        case "decimal":
          responseItem.answer.push({ valueDecimal: value });
          break;
        case "integer":
          responseItem.answer.push({ valueInteger: value });
          break;
        case "date":
          responseItem.answer.push({ valueDate: value });
          break;
        case "time":
          responseItem.answer.push({ valueTime: value });
          break;
        case "string":
        case "text":
          responseItem.answer.push({ valueString: value });
          break;
        case "quantity":
          responseItem.answer.push({ valueQuantity: value });
          break;
        case "choice":
          //make single select and multiselect have the same shape to reuse code
          var items = value;
          if (!Array.isArray(items)) {
            items = [{ value: value }];
          }
          for (var i = 0; i < items.length; i++) {
            var curValue = items[i].value;
            var option = questionnaireItem?.answerOption?.find(
              (option) => option?.valueCoding?.code === curValue,
            );
            if (option) {
              responseItem.answer.push({ valueCoding: option.valueCoding });
            } else {
              option = questionnaireItem?.answerOption?.find(
                (option) => option.valueString === curValue,
              );
              if (option) {
                responseItem.answer.push({ valueString: option.valueString });
              } else {
                console.error(
                  "couldn't find a matching questionnaire answer for '" +
                    curValue +
                    "'",
                );
              }
            }
          }
          break;
      }
    }
    setQuestionnaireResponse(updatedQuestionnaireResponse);
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        additionalQuestions: updatedQuestionnaireResponse,
      },
    });
  };

  return (
    <>
      <Stack gap={10}>
        <div className="orderLegendBody">
          <h3>
            <FormattedMessage id="select.program" />
          </h3>
          <ProgramSelect
            programChange={handleProgramSelection}
            orderFormValues={orderFormValues}
            setOrderFormValues={setOrderFormValues}
          />
          <Questionnaire
            questionnaire={questionnaire}
            onAnswerChange={answerChange}
            getAnswer={getAnswer}
          />
          {questionnaireResponse && (
            <input
              type="hidden"
              name="additionalQuestions"
              value={questionnaireResponse}
            />
          )}
        </div>
      </Stack>
    </>
  );
};

export default OrderEntryAdditionalQuestions;
