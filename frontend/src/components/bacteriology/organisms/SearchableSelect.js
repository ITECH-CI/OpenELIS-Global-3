import React, { useState, useMemo } from "react";
import { ComboBox } from "@carbon/react";

/**
 * SearchableSelect - A searchable dropdown component using Carbon's ComboBox
 *
 * @param {string} id - Unique identifier for the component
 * @param {string} labelText - Label text to display
 * @param {array} items - Array of items {id, value} or {id, name}
 * @param {string|number} selectedValue - Currently selected value
 * @param {function} onChange - Callback when selection changes
 * @param {string} returnType - "value" (default) or "id"
 * @param {boolean} disabled - Whether the component is disabled
 * @param {string} placeholder - Placeholder text
 */
const SearchableSelect = ({
  id,
  labelText,
  items = [],
  selectedValue,
  onChange,
  returnType = "value",
  disabled = false,
  placeholder = "Rechercher...",
}) => {
  // Transform items to ComboBox format
  const comboBoxItems = useMemo(() => {
    return items.map((item) => {
      // Support multiple formats: IdValuePair {id, value}, Dictionary {id, value}, Organism {id, name}
      const label = item.value || item.name || item.label || String(item.id);
      return {
        id: String(item.id),
        label: label,
        // Store the original value for dictionaries that use 'value' as identifier
        originalValue: item.value,
      };
    });
  }, [items]);

  // Find selected item - support both id-based and value-based selection
  const selectedItem = useMemo(() => {
    if (!selectedValue) return null;

    // First try to match by value (for dictionaries)
    let found = comboBoxItems.find(
      (item) => item.originalValue === selectedValue,
    );

    // If not found, try to match by id
    if (!found) {
      found = comboBoxItems.find((item) => item.id === String(selectedValue));
    }

    // If still not found, try to match by label
    if (!found) {
      found = comboBoxItems.find((item) => item.label === selectedValue);
    }

    return found || null;
  }, [selectedValue, comboBoxItems]);

  const handleChange = ({ selectedItem }) => {
    if (selectedItem) {
      // Return id or value based on configuration
      if (returnType === "id") {
        onChange(selectedItem.id);
      } else {
        onChange(selectedItem.originalValue || selectedItem.id);
      }
    } else {
      onChange("");
    }
  };

  return (
    <ComboBox
      id={id}
      titleText={labelText}
      items={comboBoxItems}
      selectedItem={selectedItem}
      onChange={handleChange}
      disabled={disabled}
      placeholder={placeholder}
      itemToString={(item) => (item ? item.label : "")}
      shouldFilterItem={({ item, inputValue }) => {
        if (!inputValue) return true;
        return item.label.toLowerCase().includes(inputValue.toLowerCase());
      }}
    />
  );
};

export default SearchableSelect;
