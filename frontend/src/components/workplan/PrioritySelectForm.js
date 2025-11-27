import { Column, Grid, Select, SelectItem } from "@carbon/react";
import { useEffect, useRef, useState } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import "../Style.css";

function PanelSelectForm(props) {
  const mounted = useRef(false);
  const [tests, setTests] = useState([]);
  const [defaultPriorityId, setDefaultPriorityId] = useState("");
  const [defaultPriorityLabel, setDefaultPriorityLabel] = useState("");

  const handleChange = (e) => {
    props.value(e.target.value, e.target.selectedOptions[0].text);
  };

  const getTests = (res) => {
    if (mounted.current) {
      setTests(res);
    }
  };

  const intl = useIntl();

  useEffect(() => {
    mounted.current = true;
    let priorityId = new URLSearchParams(window.location.search).get(
      "priority",
    );
    priorityId = priorityId ? priorityId : "";
    //  getFromOpenElisServer("/rest/priorities", (fetchedPriorities) => {
    let priority = priorities.find((priority) => priority.id === priorityId);
    let priorityLabel = priority
      ? priority.value
      : intl.formatMessage({ id: "input.placeholder.selectPriority" });
    setDefaultPriorityId(priorityId);
    setDefaultPriorityLabel(priorityLabel);
    props.value(priorityId, priorityLabel);
    getTests(priorities);
    // });
    return () => {
      mounted.current = false;
    };
  }, []);

  return (
    <>
      <Grid fullWidth={true}>
        <Column sm={4} md={8} lg={16}>
          <Select
            defaultValue="placeholder-item"
            id="select-1"
            invalidText={
              <FormattedMessage id="workplan.panel.selection.error.msg" />
            }
            helperText={props.title}
            labelText=""
            onChange={handleChange}
          >
            <SelectItem text={defaultPriorityLabel} value={defaultPriorityId} />
            {tests
              .filter((item) => item.id !== defaultPriorityId)
              .map((item, idx) => {
                return (
                  <SelectItem key={idx} text={item.value} value={item.id} />
                );
              })}
          </Select>
        </Column>
      </Grid>
    </>
  );
}

export default injectIntl(PanelSelectForm);
