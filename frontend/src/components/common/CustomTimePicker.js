import { useEffect, useState } from "react";
import { TimePicker } from "@carbon/react";

const CustomTimePicker = (props) => {
  const [currentTime, setCurrentTime] = useState(props.value || "");

  function handleTimePicker(e) {
    let time = e.target.value.replace(/\D/g, "");

    if (time.length >= 2) {
      time = time.slice(0, 2) + ":" + time.slice(2, 4);
    }
    if (time.length > 5) {
      time = time.slice(0, 5);
    }

    setCurrentTime(time);
    props.onChange(time);
  }

  useEffect(() => {
    props.onChange(currentTime);
  }, [currentTime]);

  return (
    <TimePicker
      id={props.id}
      value={currentTime}
      onChange={handleTimePicker}
      labelText={props.labelText || ""}
      placeholder="HH:MM"
      pattern="([0-1][0-9]|2[0-3]):[0-5][0-9]"
    />
  );
};

export default CustomTimePicker;
