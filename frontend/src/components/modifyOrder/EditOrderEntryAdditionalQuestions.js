import { InlineLoading, Stack } from "@carbon/react";
import { useEffect, useState } from "react";
import { FormattedMessage } from "react-intl";
import "../../App.css";
import "../../index.css";
import { ProgramSelect } from "../addOrder/OrderEntryAdditionalQuestions";
import Questionnaire from "../common/Questionnaire";
import "../Style.css";
import { getFromOpenElisServer } from "../utils/Utils";

const EditOrderEntryAdditionalQuestions = ({
  orderFormValues,
  setOrderFormValues = () => {
    console.debug("default setOrderFormValues change function does nothing");
  },
}) => {
  const [questionnaire, setQuestionnaire] = useState({});
  const [questionnaireResponse, setQuestionnaireResponse] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (orderFormValues?.sampleOrderItems?.programId) {
      getFromOpenElisServer(
        "/rest/program/" +
          orderFormValues.sampleOrderItems.programId +
          "/questionnaire",
        setDefaultAdditionalQuestions,
      );
    }
    if (orderFormValues?.sampleOrderItems?.labNo) {
      if (!orderFormValues.sampleOrderItems.programId) {
        setLoading(false);
      }
    }
  }, [orderFormValues]);

  const handleProgramSelection = (event, _selectedProgram, programCode) => {
    if (!event.target.value) {
      setAdditionalQuestions(null);
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          programId: "",
          programCode: "",
        },
      });
    } else {
      getFromOpenElisServer(
        "/rest/program/" + event.target.value + "/questionnaire",
        setAdditionalQuestions,
      );
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          programId: event.target.value,
          programCode: programCode || "",
        },
      });
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

  function setAdditionalQuestions(res) {
    console.debug(res);
    if (res !== "Check server logs") {
      setQuestionnaire(res);
      var convertedQuestionnaireResponse = convertQuestionnaireToResponse(res);
      setQuestionnaireResponse(convertedQuestionnaireResponse);
    }
  }

  function setDefaultAdditionalQuestions(res) {
    console.debug(res);
    if (res !== "Check server logs") {
      setQuestionnaire(res);
      setQuestionnaireResponse(
        orderFormValues.sampleOrderItems.additionalQuestions,
      );
    }
    if (loading) {
      setLoading(false);
    }
  }

  // Symétrique à OrderEntryAdditionalQuestions.answerChange : sans ce callback
  // le Questionnaire affichait les valeurs en lecture seule et toute édition
  // sur la page de modification était silencieusement perdue (additionalQuestions
  // n'était jamais mis à jour). On reconstruit la même mécanique : mise à jour
  // de questionnaireResponse + propagation dans orderFormValues.sampleOrderItems.
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
          // Pour réutiliser la même mécanique entre single/multi select on
          // normalise la valeur en tableau.
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

  const getAnswer = (linkId) => {
    var responseItem = questionnaireResponse?.item?.find(
      (item) => item.linkId === linkId,
    );
    var questionnaireItem = questionnaire?.item?.find(
      (item) => item.linkId === linkId,
    );
    switch (questionnaireItem.type) {
      case "boolean":
        return responseItem?.answer?.[0]?.valueBoolean ?? "";
      case "decimal":
        return responseItem?.answer?.[0]?.valueDecimal ?? "";
      case "integer":
        return responseItem?.answer?.[0]?.valueInteger ?? "";
      case "date":
        return responseItem?.answer?.[0]?.valueDate ?? "";
      case "time":
        return responseItem?.answer?.[0]?.valueTime ?? "";
      case "string":
      case "text":
        return responseItem?.answer?.[0]?.valueString ?? "";
      case "quantity":
        return responseItem?.answer?.[0]?.valueQuantity ?? "";
      case "choice":
        if (responseItem?.answer?.[0]) {
          const firstAnswer = responseItem.answer[0];
          return (
            firstAnswer?.valueCoding?.code ?? firstAnswer?.valueString ?? ""
          );
        }
        return "";
    }
  };

  return (
    <>
      <Stack gap={10}>
        <div className="orderLegendBody">
          <h3>
            <FormattedMessage id="label.program" />
          </h3>
          <ProgramSelect
            orderFormValues={orderFormValues}
            programChange={handleProgramSelection}
            setOrderFormValues={setOrderFormValues}
            editable={true}
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
          {loading && <InlineLoading />}
        </div>
      </Stack>
    </>
  );
};

export default EditOrderEntryAdditionalQuestions;
