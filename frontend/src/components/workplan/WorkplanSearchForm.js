import React, { useEffect, useRef, useState } from "react";
import {
  Column,
  Form,
  Grid,
  Section,
  Button,
  Link,
  TextInput,
} from "@carbon/react";
import { ArrowLeft, ArrowRight } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import CustomDatePicker from "../common/CustomDatePicker";
import "../Style.css";
import TestSectionSelectForm from "./TestSectionSelectForm";
import TestSelectForm from "./TestSelectForm";
import PanelSelectForm from "./PanelSelectForm";
import PrioritySelectForm from "./PrioritySelectForm";
import { getFromOpenElisServer } from "../utils/Utils";

export default function WorkplanSearchForm(props) {
  const mounted = useRef(false);
  const [selectedValue, setSelectedValue] = useState("");
  const [selectedLabel, setSelectedLabel] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [nextPage, setNextPage] = useState(null);
  const [previousPage, setPreviousPage] = useState(null);
  const [pagination, setPagination] = useState(false);
  const [currentApiPage, setCurrentApiPage] = useState(null);
  const [totalApiPages, setTotalApiPages] = useState(null);
  const [url, setUrl] = useState("");
  // filtres optionnels (n° labo, période de réception), appliqués côté serveur
  const [labNumber, setLabNumber] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [appliedFilters, setAppliedFilters] = useState("");
  const intl = useIntl();

  const buildFilterQuery = (lab, start, end) =>
    (lab.trim() ? "&labNumber=" + encodeURIComponent(lab.trim()) : "") +
    (start ? "&startDate=" + encodeURIComponent(start) : "") +
    (end ? "&endDate=" + encodeURIComponent(end) : "");

  const applyFilters = (e) => {
    e?.preventDefault();
    setAppliedFilters(buildFilterQuery(labNumber, startDate, endDate));
  };

  const clearFilters = () => {
    setLabNumber("");
    setStartDate("");
    setEndDate("");
    setAppliedFilters("");
  };

  let title = "";
  let urlToPost = "";
  const type = props.type;
  switch (type) {
    case "test":
      title = <FormattedMessage id="workplan.test.types" />;
      urlToPost = "/rest/WorkPlanByTest?test_id=";
      break;
    case "panel":
      title = <FormattedMessage id="workplan.panel.types" />;
      urlToPost = "/rest/WorkPlanByPanel?panel_id=";
      break;
    case "unit":
      title = <FormattedMessage id="workplan.unit.types" />;
      urlToPost = "/rest/WorkPlanByTestSection?test_section_id=";
      break;
    case "priority":
      title = <FormattedMessage id="workplan.priority.list" />;
      urlToPost = "/rest/WorkPlanByPriority?priority=";
      break;
    default:
      title = "";
  }

  const handleSelectedValue = (v, l) => {
    if (mounted.current) {
      setSelectedValue(v);
      setSelectedLabel(l);
      props.selectedValue(v);
      props.selectedLabel(l);
    }
  };

  const getTestsList = (res) => {
    if (mounted.current) {
      props.createTestsList(res);
      if (res.paging) {
        var { totalPages, currentPage } = res.paging;
        if (totalPages > 1) {
          setPagination(true);
          setCurrentApiPage(currentPage);
          setTotalApiPages(totalPages);
          if (parseInt(currentPage) < parseInt(totalPages)) {
            setNextPage(parseInt(currentPage) + 1);
          } else {
            setNextPage(null);
          }
          if (parseInt(currentPage) > 1) {
            setPreviousPage(parseInt(currentPage) - 1);
          } else {
            setPreviousPage(null);
          }
        }
      }
      setIsLoading(false);
    }
  };

  const loadNextResultsPage = () => {
    setIsLoading(true);
    getFromOpenElisServer(url + "&page=" + nextPage, getTestsList);
  };

  const loadPreviousResultsPage = () => {
    setIsLoading(true);
    getFromOpenElisServer(url + "&page=" + previousPage, getTestsList);
  };

  useEffect(() => {
    mounted.current = true;
    setIsLoading(true);
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
    setUrl(urlToPost + selectedValue + appliedFilters);
    getFromOpenElisServer(
      urlToPost + selectedValue + appliedFilters,
      getTestsList,
    );
    return () => {
      mounted.current = false;
    };
  }, [selectedValue, appliedFilters]);

  useEffect(() => {
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
  }, []);

  return (
    <>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <h5 className="contentHeader2">
              <FormattedMessage id="label.form.searchby" />
              &nbsp; {title}{" "}
            </h5>
          </Section>
        </Column>
      </Grid>
      <Grid fullWidth={true}>
        <Column sm={4} md={4} lg={6}>
          <Form className="container-form">
            {type === "test" && (
              <TestSelectForm title={title} value={handleSelectedValue} />
            )}
            {type === "panel" && (
              <PanelSelectForm title={title} value={handleSelectedValue} />
            )}
            {type === "unit" && (
              <TestSectionSelectForm
                title={title}
                value={handleSelectedValue}
              />
            )}
            {type === "priority" && (
              <PrioritySelectForm title={title} value={handleSelectedValue} />
            )}
          </Form>
        </Column>
        <Column sm={4} md={8} lg={10}>
          <Form className="container-form" onSubmit={applyFilters}>
            {/* une seule ligne : n° labo, période du/au, boutons (retour à la
                ligne seulement si l'écran est trop étroit) */}
            <div
              style={{
                display: "flex",
                flexWrap: "wrap",
                alignItems: "flex-end",
                gap: "1rem",
              }}
            >
              <div style={{ flex: "1 1 12rem", minWidth: "10rem" }}>
                <TextInput
                  id="workplanLabNumberFilter"
                  labelText={intl.formatMessage({
                    id: "workplan.filter.labNumber",
                  })}
                  value={labNumber}
                  onChange={(e) => setLabNumber(e.target.value)}
                />
              </div>
              <div>
                <CustomDatePicker
                  id="workplanStartDateFilter"
                  labelText={intl.formatMessage({
                    id: "workplan.filter.startDate",
                  })}
                  value={startDate}
                  updateStateValue={true}
                  onChange={(date) => setStartDate(date)}
                />
              </div>
              <div>
                <CustomDatePicker
                  id="workplanEndDateFilter"
                  labelText={intl.formatMessage({
                    id: "workplan.filter.endDate",
                  })}
                  value={endDate}
                  updateStateValue={true}
                  onChange={(date) => setEndDate(date)}
                />
              </div>
              <div style={{ whiteSpace: "nowrap" }}>
                <Button type="submit" size="md">
                  <FormattedMessage id="workplan.filter.apply" />
                </Button>{" "}
                <Button kind="ghost" size="md" onClick={clearFilters}>
                  <FormattedMessage id="workplan.filter.clear" />
                </Button>
              </div>
            </div>
          </Form>
        </Column>
        <Column sm={1} md={2} lg={4}>
          {isLoading && (
            <img
              src={`images/loading.gif`}
              alt="Loading ..."
              width="60"
              height="60"
            />
          )}
        </Column>
      </Grid>
      <hr />
      <br />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          {selectedLabel && (
            <Section>
              <h4 className="contentHeader1">&nbsp;</h4>
            </Section>
          )}
        </Column>
      </Grid>
      <>
        {pagination && (
          <Grid>
            <Column lg={14} />
            <Column
              lg={2}
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: "10px",
                width: "110%",
              }}
            >
              <Link>
                {currentApiPage} / {totalApiPages}
              </Link>
              <div style={{ display: "flex", gap: "10px" }}>
                <Button
                  hasIconOnly
                  id="loadpreviousresults"
                  onClick={loadPreviousResultsPage}
                  disabled={previousPage != null ? false : true}
                  renderIcon={ArrowLeft}
                  iconDescription="previous"
                ></Button>
                <Button
                  hasIconOnly
                  id="loadnextresults"
                  onClick={loadNextResultsPage}
                  disabled={nextPage != null ? false : true}
                  renderIcon={ArrowRight}
                  iconDescription="next"
                ></Button>
              </div>
            </Column>
          </Grid>
        )}
      </>
    </>
  );
}
