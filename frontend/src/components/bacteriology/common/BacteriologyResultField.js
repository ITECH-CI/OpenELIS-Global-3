import {
  Checkbox,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  TextInput,
} from "@carbon/react";

/**
 * Reusable field component for bacteriology results
 */
const BacteriologyResultField = ({
  id,
  label,
  type = "text",
  value,
  onChange,
  options = [],
  disabled = false,
  required = false,
  placeholder = "",
  maxLength,
}) => {
  const handleChange = (e) => {
    const newValue = e.target ? e.target.value : e;
    onChange(newValue);
  };

  switch (type) {
    case "select":
      return (
        <Select
          id={id}
          labelText={label}
          value={value || ""}
          onChange={handleChange}
          disabled={disabled}
          invalid={required && !value}
        >
          <SelectItem value="" text={placeholder || "Select..."} />
          {options.map((option) => (
            <SelectItem
              key={option.id}
              value={option.id}
              text={option.name || option.label || option.value}
            />
          ))}
        </Select>
      );

    case "radio":
      return (
        <RadioButtonGroup
          legendText={label}
          name={id}
          valueSelected={value || ""}
          onChange={handleChange}
          disabled={disabled}
        >
          {options.map((option) => (
            <RadioButton
              key={option.id}
              id={`${id}_${option.id}`}
              labelText={option.name || option.label}
              value={option.id}
            />
          ))}
        </RadioButtonGroup>
      );

    case "checkbox":
      return (
        <Checkbox
          id={id}
          labelText={label}
          checked={value || false}
          onChange={(checked) => onChange(checked)}
          disabled={disabled}
        />
      );

    case "textarea":
      return (
        <TextInput
          id={id}
          labelText={label}
          value={value || ""}
          onChange={handleChange}
          disabled={disabled}
          invalid={required && !value}
          placeholder={placeholder}
          maxLength={maxLength}
          multiline
        />
      );

    default:
      return (
        <TextInput
          id={id}
          labelText={label}
          type={type}
          value={value || ""}
          onChange={handleChange}
          disabled={disabled}
          invalid={required && !value}
          placeholder={placeholder}
          maxLength={maxLength}
        />
      );
  }
};

export default BacteriologyResultField;
