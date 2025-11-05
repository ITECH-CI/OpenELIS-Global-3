import React, { useState, useRef, useEffect } from 'react';
import { ChevronDown } from '@carbon/icons-react';
import './CustomSelect.css';

const CustomSelect = ({ id, name, labelText, value, onChange, required, children }) => {
  const [isOpen, setIsOpen] = useState(false);
  const selectRef = useRef(null);

  const options = React.Children.toArray(children).map(child => ({
    text: child.props.text,
    value: child.props.value,
    icon: child.props.icon
  }));

  const selected = options.find(opt => opt.value === value);

  useEffect(() => {
    const handleClick = (e) => {
      if (selectRef.current && !selectRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const handleSelect = (option) => {
    onChange({ target: { name, value: option.value } });
    setIsOpen(false);
  };

  return (
    <div className="custom-select-wrapper" ref={selectRef}>
      {labelText && <label htmlFor={id}>{labelText}{required && '*'}</label>}
      
      <div className="custom-select" onClick={() => setIsOpen(!isOpen)}>
        <div className="custom-select-value">
          {selected?.icon}
          <span>{selected?.text || 'Sélectionner...'}</span>
        </div>
        <ChevronDown size={16} />
      </div>

      {isOpen && (
        <ul className="custom-select-dropdown">
          {options.map((option, i) => (
            <li key={i} onClick={() => handleSelect(option)}>
              <span>{option.text}</span>
               {option.icon}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

const CustomSelectItem = () => null;

export { CustomSelect, CustomSelectItem };