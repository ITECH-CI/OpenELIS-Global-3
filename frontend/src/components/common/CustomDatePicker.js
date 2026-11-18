import React, { useEffect, useState, useContext } from "react";
import { DatePicker, DatePickerInput } from "@carbon/react";
import { format } from "date-fns";
import { ConfigurationContext } from "../layout/Layout";

/**
 * Date d'affichage (jj/mm/aaaa, ou mm/jj/aaaa) postérieure à aujourd'hui.
 */
export function isFutureDisplayDate(value, dayFirst) {
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value || "");
  if (!match) {
    return false;
  }
  const day = Number(dayFirst ? match[1] : match[2]);
  const month = Number(dayFirst ? match[2] : match[1]);
  const date = new Date(Number(match[3]), month - 1, day);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return date > today;
}

const CustomDatePicker = (props) => {
  const [currentDate, setCurrentDate] = useState(
    props.value ? props.value : "",
  );
  const { configurationProperties } = useContext(ConfigurationContext);
  // Avec futureDateErrorText, une date future saisie est conservée et signalée
  // sous le champ, au lieu d'être effacée sans message par le calendrier.
  const futureDateErrorText = props.disallowFutureDate
    ? props.futureDateErrorText
    : undefined;
  const futureDateRejected =
    !!futureDateErrorText &&
    isFutureDisplayDate(
      currentDate,
      configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR",
    );
  function handleDatePickerChange(e) {
    if (!e || !e[0]) {
      setCurrentDate("");
      props.onChange("");
      return;
    }
    let date = new Date(e[0]);
    const formatDate = format(
      new Date(date),
      configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR"
        ? "dd/MM/yyyy"
        : "MM/dd/yyyy",
    );
    setCurrentDate(formatDate);
    props.onChange(formatDate);
  }

  function handleInputChange(e) {
    const inputValue = e.target.value;

    const isFrenchLocale =
      configurationProperties.DEFAULT_DATE_LOCALE === "fr-FR";
    const partialDateRegex = isFrenchLocale
      ? /^(\d{0,2})(\/(\d{0,2})(\/(\d{0,4})?)?)?$/
      : /^(\d{0,2})(\/(\d{0,2})(\/(\d{0,4})?)?)?$/;

    const fullDateRegex = isFrenchLocale
      ? /^(0[1-9]|[12][0-9]|3[01])\/(0[1-9]|1[0-2])\/\d{4}$/
      : /^(0[1-9]|1[0-2])\/(0[1-9]|[12][0-9]|3[01])\/\d{4}$/;

    if (partialDateRegex.test(inputValue)) {
      e.target.value = inputValue;
    } else {
      e.target.value = ""; // Clear invalid input
    }
  }

  useEffect(() => {
    props.onChange(currentDate);
  }, [currentDate]);

  useEffect(() => {
    if (props.updateStateValue) {
      setCurrentDate(props.value);
    }
  }, [props.value]);

  return (
    <>
      <DatePicker
        id={props.id}
        dateFormat={
          configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR"
            ? "d/m/Y"
            : "m/d/Y"
        }
        className={props.className}
        datePickerType="single"
        value={currentDate}
        onChange={(e) => handleDatePickerChange(e)}
        maxDate={
          props.disallowFutureDate && !futureDateErrorText
            ? format(
                new Date(),
                configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR"
                  ? "dd/MM/yyyy"
                  : "MM/dd/yyyy",
              )
            : ""
        }
        minDate={
          props.minDate
            ? props.minDate
            : props.disallowPastDate
              ? format(
                  new Date(),
                  configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR"
                    ? "dd/MM/yyyy"
                    : "MM/dd/yyyy",
                )
              : ""
        }
      >
        <DatePickerInput
          id={props.id}
          placeholder={
            configurationProperties.DEFAULT_DATE_LOCALE == "fr-FR"
              ? "dd/mm/yyyy"
              : "mm/dd/yyyy"
          }
          type="text"
          labelText={props.labelText}
          invalid={props.invalid || futureDateRejected}
          invalidText={
            futureDateRejected ? futureDateErrorText : props.invalidText
          }
          disabled={props.disabled}
          onChange={handleInputChange}
        />
      </DatePicker>
    </>
  );
};

export default CustomDatePicker;
