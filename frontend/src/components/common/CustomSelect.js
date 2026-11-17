import React from "react";
import { Select, SelectItem } from "@carbon/react";
import { useIntl } from "react-intl";

const CustomSelect = (props) => {
  const intl = useIntl();
  const handleSelect = (e) => {
    const value = e.target.value;
    props.onChange(value);
  };
  return (
    <>
      <Select
        onChange={handleSelect}
        labelText={props.labelText || ""}
        id={props.id}
        defaultValue={props.value ? props.value : ""}
        value={props.value ? props.value : ""}
        disabled={props.disabled}
      >
        <SelectItem
          text={
            props.placeholder ||
            intl.formatMessage({ id: "label.select.placeholder" })
          }
          value=""
        />
        {props.defaultSelect && (
          <SelectItem
            text={props.defaultSelect.value}
            value={props.defaultSelect.id}
          />
        )}

        {props.options != null &&
          props.options.map((option, index) => {
            return (
              <SelectItem key={index} text={option.value} value={option.id} />
            );
          })}
      </Select>
    </>
  );
};

export default CustomSelect;
