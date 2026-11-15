import GroupedTimeline from "./grouped-timeline";
import "./grouped-timeline.styles.scss";
import useGetManyObstreeData from "./useObstreeData";

// Feuille globale, sans module CSS : rien à exporter.
const styles = {};

export default GroupedTimeline;
export { GroupedTimeline, styles, useGetManyObstreeData };
