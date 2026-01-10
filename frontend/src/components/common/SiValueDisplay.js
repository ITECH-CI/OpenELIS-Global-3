import { Information } from "@carbon/icons-react";
import { Tooltip } from "@carbon/react";
import PropTypes from "prop-types";
import { useIntl } from "react-intl";
import "./SiValueDisplay.css";

/**
 * SiValueDisplay Component
 *
 * Displays test result values with optional SI (International System) conversion.
 * Shows traditional value on first line and SI value on second line when available.
 *
 * @param {Object} props - Component props
 * @param {string} props.traditionalValue - The traditional/original value
 * @param {string} props.traditionalUom - The traditional unit of measure
 * @param {string} props.siValue - The SI converted value (optional)
 * @param {string} props.siUom - The SI unit of measure (optional)
 * @param {string} props.className - Additional CSS classes
 * @param {boolean} props.showTooltip - Whether to show conversion info tooltip
 * @param {number} props.significantDigits - Number of decimal places for SI value (default: 2)
 *
 * @example
 * <SiValueDisplay
 *   traditionalValue="12.5"
 *   traditionalUom="g/dL"
 *   siValue="125.0"
 *   siUom="g/L"
 *   significantDigits={1}
 * />
 * // Renders: "12.5 g/dL" on first line
 * //          "125.0 g/L" on second line (italics, secondary color)
 */
const SiValueDisplay = ({
  traditionalValue,
  traditionalUom,
  siValue,
  siUom,
  className = "",
  showTooltip = true,
  significantDigits = 2,
}) => {
  const intl = useIntl();
  const hasSiConversion = siValue && siValue.trim() !== "";

  // Format SI value with significant digits
  const formatSiValue = (value) => {
    if (!value) return value;
    const numValue = parseFloat(value);
    if (isNaN(numValue)) return value;
    return numValue.toFixed(significantDigits);
  };

  const containerClass = `si-value-display ${className}`;

  return (
    <div className={containerClass}>
      <div className="traditional-line">
        <span className="traditional-value">
          {traditionalValue}
          {traditionalUom && <span className="uom"> {traditionalUom}</span>}
        </span>
      </div>

      {hasSiConversion && (
        <div className="si-line">
          <span className="si-value">
            {"("}
            <span>
              {formatSiValue(siValue)}
              {siUom && <span className="uom"> {siUom}</span>}
            </span>
            {")"}
          </span>

          {showTooltip && (
            <Tooltip
              align="top"
              label={intl.formatMessage({ id: "si.tooltip.label" })}
              className="si-tooltip"
            >
              <button
                className="si-info-button"
                type="button"
                aria-label={intl.formatMessage({ id: "si.tooltip.aria" })}
              >
                <Information size={14} />
              </button>
            </Tooltip>
          )}
        </div>
      )}
    </div>
  );
};

SiValueDisplay.propTypes = {
  traditionalValue: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
    .isRequired,
  traditionalUom: PropTypes.string,
  siValue: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  siUom: PropTypes.string,
  className: PropTypes.string,
  showTooltip: PropTypes.bool,
  significantDigits: PropTypes.number,
};

export default SiValueDisplay;
