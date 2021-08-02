import { faCog } from "@fortawesome/free-solid-svg-icons/faCog";
import { faDatabase } from "@fortawesome/free-solid-svg-icons/faDatabase";
import { faFolderOpen } from "@fortawesome/free-solid-svg-icons/faFolderOpen";
import { faPlay } from "@fortawesome/free-solid-svg-icons/faPlay";
import { faProjectDiagram } from "@fortawesome/free-solid-svg-icons/faProjectDiagram";
import { faSave } from "@fortawesome/free-solid-svg-icons/faSave";
import { faShapes } from "@fortawesome/free-solid-svg-icons/faShapes";
import { faSignOutAlt } from "@fortawesome/free-solid-svg-icons/faSignOutAlt";
import { faStop } from "@fortawesome/free-solid-svg-icons/faStop";
import { faUserShield } from "@fortawesome/free-solid-svg-icons/faUserShield";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import IconButton from "@material-ui/core/IconButton";
import clsx from "clsx";
import { ipcRenderer, IpcRendererEvent } from "electron";
import React from "react";
import { SplitPane } from "react-collapse-pane";
import { useHistory } from "react-router-dom";
import { SnackbarContext } from "../app";
import { StudioButton } from "../common/button/button";
import { StudioIconButton } from "../common/button/icon-button";
import { StudioSelect } from "../common/select/select";
import { StudioTable } from "../common/table/table";
import { StudioTabItem, StudioTabPanel, StudioTabs } from "../common/tabs/tabs";
import { useInterval } from "../common/use-interval";
import { ConceptData, ConceptMapData, MatchQueryRequest, MatchQueryResponsePart } from "../ipc/event-args";
import { routes } from "../router";
import { studioStyles } from "../styles/studio-styles";
import { TypeDBVisualiserData, ForceGraphVertex } from "../typedb-visualiser";
import { CodeEditor } from "./code-editor";
import { AnswerGraphStatus, QueryVisualiser } from "./query-visualiser";
import { workspaceStyles } from "./workspace-styles";
import { databaseState, dbServerState, themeState } from "../state/state";
import moment from "moment";
import CSS from "csstype";

import "./ace-theme-studio-dark";

function msToTime(duration: number) {
    let milliseconds: string | number = Math.floor(duration % 1000),
        seconds: string | number = Math.floor((duration / 1000) % 60),
        minutes: string | number = Math.floor((duration / (1000 * 60)) % 60),
        hours: string | number = Math.floor((duration / (1000 * 60 * 60)) % 24);

    milliseconds = String(milliseconds).padStart(3, "0");
    hours = String(hours).padStart(2, "0");
    minutes = String(minutes).padStart(2, "0");
    seconds = String(seconds).padStart(2, "0");

    return (hours !== "00" ? hours + ":" : "") + minutes + ":" + seconds + "." + milliseconds;
}

enum ResultsTab {
    LOG,
    GRAPH,
    TABLE,
}

interface AnswerTable {
    headings: string[];
    rows: string[][];
    initialGridTemplateColumns: CSS.Property.GridTemplateColumns;
}

interface GraphElementIDRegistry {
    nextID: number;
    types: {[label: string]: number};
    things: {[label: string]: number};
}

const tabs: StudioTabItem[] = [{ label: "Query1.tql", key: "0" }];
const resultsTabs: StudioTabItem[] = [
    { label: "Log", key: "0" },
    { label: "Graph", key: "1" },
    { label: "Table", key: "2" },
];

const leftSidebar: StudioTabItem[] = [
    { label: "Permissions", key: "Permissions", icon: <FontAwesomeIcon icon={faUserShield} style={{marginLeft: 3}}/> },
    { label: "Schema Explorer", key: "Schema Explorer", icon: <FontAwesomeIcon icon={faShapes}/> },
];

const rightSidebar: StudioTabItem[] = [
    { label: "Settings", key: "Settings", icon: <FontAwesomeIcon icon={faCog} style={{marginRight: 3}}/> },
    { label: "Graph Explorer", key: "Graph Explorer", icon: <FontAwesomeIcon icon={faProjectDiagram}/> },
];

export const WorkspaceScreen: React.FC = () => {
    console.log("WorkspaceScreen() called");
    const theme = themeState.use()[0];
    const classes = Object.assign({}, studioStyles({ theme }), workspaceStyles({ theme }));

    const [db, setDB] = databaseState.use();
    const [dbServer, setDBServer] = dbServerState.use();
    const [code, setCode] = React.useState("match $x sub thing;\noffset 0;\nlimit 1000;\n");
    const [answerGraphStatus, setAnswerGraphStatus] = React.useState<AnswerGraphStatus>({
        vertexCount: null,
        edgeCount: null,
        queryRunTime: null,
    });
    // const [answerGraph, setAnswerGraph] = React.useState<TypeDBVisualiserData.Graph>(null);
    // const [answerTable, setAnswerTable] = React.useState<AnswerTable>(null);
    const { setSnackbar } = React.useContext(SnackbarContext);
    const [principalStatus, setPrincipalStatus] = React.useState("Ready");
    const [zoom, setZoom] = React.useState("100");
    const [query, setQuery] = React.useState<{ time: number, text: string }>(null);
    const [queryResult, setQueryResult] = React.useState<string>(null);
    const [queryRunning, setQueryRunning] = React.useState(false);
    const [queryRunTime, setQueryRunTime] = React.useState<string>(null);
    // const [queryStartTime, setQueryStartTime] = React.useState<number>(null);
    const [queryEndTime, setQueryEndTime] = React.useState<number>(null);
    const [timeQuery, setTimeQuery] = React.useState(false);
    const [queryCancelled, setQueryCancelled] = React.useState(false);
    const [selectedVertex, setSelectedVertex] = React.useState<ForceGraphVertex>(null);
    const routerHistory = useHistory();

    const updateZoom = (_scale: number) => {
        // TODO: currently this causes a massive performance hit - maybe breaking down WorkspaceScreen into
        //       sub-components would fix it?
        // setZoom(`${(scale * 100).toPrecision(3)}`);
    }

    const runQuery = () => {
        setQuery({ time: Date.now(), text: code });
        setAnswerGraphStatus({ vertexCount: 0, edgeCount: 0, queryRunTime: 0 });
    };

    const cancelQuery = () => {
        if (!queryRunning) return; // resolves race condition between resolving onClick and processing query response
        ipcRenderer.send("cancel-query-request");
        setPrincipalStatus("Ready");
        setQueryRunning(false);
        setQueryEndTime(Date.now());
        setTimeQuery(true);
        setQueryCancelled(true);
        // const answerCountString = `${rawAnswers.length} answer${rawAnswers.length !== 1 ? "s" : ""}`;
        // setQueryResult(`${answerCountString} (interrupted)`);
        addLogEntry("Query cancelled by user");
    };

    const runOrCancelQuery = () => {
        if (!queryRunning) runQuery();
        else cancelQuery();
    };

    const signOut = () => {
        routerHistory.push(routes.login);
    };

    // useInterval(() => {
    //     if (queryRunning) setQueryRunTime(msToTime(Date.now() - queryStartTime));
    //     else if (timeQuery) {
    //         setQueryRunTime(msToTime(queryEndTime - queryStartTime));
    //         setTimeQuery(false);
    //     }
    // }, 40);

    const [selectedIndex, setSelectedIndex] = React.useState(0);
    const [selectedResultsTab, setSelectedResultsTab] = React.useState(ResultsTab.GRAPH);

    const switchResultsTab = (tab: ResultsTab) => {
        setSelectedResultsTab(tab);
        // TODO: Once the graph is constructed on the backend, we can make sense of this again
        // if (tab === ResultsTab.GRAPH) {
        //     setVisualiserData(answerGraph);
        // }
    }

    const formatLogDate = (date: Date) => moment(date).format("DD-MM-YY HH:mm:ss.SSS");
    const [resultsLog, setResultsLog] = React.useState(`${formatLogDate(new Date())} - Connected to database '${databaseState.use()[0]}'`);
    const addLogEntry = (entry: string) => {
        const lines = entry.split("\n");
        const formattedLines = [lines[0]].concat(lines.slice(1).map(line => " ".repeat(24) + line));
        setResultsLog(resultsLog + `\n\n${formatLogDate(new Date())} - ${formattedLines.join("\n").trim()}`);
    }

    const computeWorkspaceSplitPaneInitialWidths = () => {
        const workspacePaneWidth = window.innerWidth - 56;
        const graphExplorerInitialWidth = 200;
        const queryPaneInitialWidth = workspacePaneWidth - 200;
        return [queryPaneInitialWidth, graphExplorerInitialWidth];
    }

    return (
        <>
            <div className={classes.appBar}>
                <FontAwesomeIcon icon={faDatabase}/>
                <StudioSelect value={db} setValue={setDB} variant="filled">
                    {dbServer.dbs.map(db => <option value={db}>{db}</option>)}
                </StudioSelect>
                <StudioIconButton size="smaller" onClick={() => null}>
                    <FontAwesomeIcon icon={faFolderOpen}/>
                </StudioIconButton>
                <StudioIconButton size="smaller" onClick={() => null}>
                    <FontAwesomeIcon icon={faSave}/>
                </StudioIconButton>
                <StudioIconButton size="smaller" onClick={runOrCancelQuery} classes={{root: queryRunning && classes.stopIcon}}>
                    <FontAwesomeIcon icon={queryRunning ? faStop : faPlay}/>
                </StudioIconButton>
                <div className={classes.filler}/>
                <IconButton size="small" aria-label="sign-out" color="inherit" onClick={signOut}>
                    <FontAwesomeIcon icon={faSignOutAlt}/>
                </IconButton>
            </div>
            <div className={classes.workspaceView}>
                <div className={classes.leftSidebar}>
                    <StudioTabs orientation="bottomToTop" items={leftSidebar} classes={{tab: classes.sidebarTab}}/>
                </div>
                <div className={classes.workspaceSplitPane}>
                    <SplitPane split="vertical" initialSizes={computeWorkspaceSplitPaneInitialWidths()} minSizes={[undefined, 180]}>
                        <div className={classes.querySplitPane}>
                            <SplitPane split="horizontal" initialSizes={[1, 3]}>
                                <div className={classes.editorPane}>
                                    <StudioTabs selectedIndex={selectedIndex} setSelectedIndex={setSelectedIndex} items={tabs}
                                                classes={{ root: classes.editorTabs, tabGroup: classes.editorTabGroup, tab: classes.editorTab }}
                                                showCloseButton showAddButton>
                                        <StudioTabPanel index={0} selectedIndex={selectedIndex} className={classes.editorTabPanel}>
                                            <CodeEditor content={code} setContent={setCode}/>
                                        </StudioTabPanel>
                                    </StudioTabs>
                                </div>
                                <div className={classes.resultsPane}>
                                    <StudioTabs selectedIndex={selectedResultsTab} setSelectedIndex={switchResultsTab}
                                                items={resultsTabs} classes={{root: classes.resultsTabs, tabGroup: classes.resultsTabGroup, tab: classes.resultsTab}}>
                                        <StudioTabPanel index={0} selectedIndex={selectedResultsTab} className={classes.resultsTabPanel}>
                                            <pre className={classes.resultsLog}><div>{resultsLog}</div></pre>
                                        </StudioTabPanel>
                                        <StudioTabPanel index={1} selectedIndex={selectedResultsTab} className={classes.resultsTabPanel}>
                                            <QueryVisualiser query={query} db={db} theme={theme} onStatus={setAnswerGraphStatus}/>
                                        </StudioTabPanel>
                                        <StudioTabPanel index={2} selectedIndex={selectedResultsTab} className={clsx(classes.resultsTabPanel, classes.resultsTablePanel)}>
                                            {/*{answerTable &&*/}
                                            {/*<StudioTable headings={[""].concat(answerTable.headings)} minCellWidth={40} sizing="resizable"*/}
                                            {/*             initialGridTemplateColumns={answerTable.initialGridTemplateColumns} className={classes.resultsTable}>*/}
                                            {/*    {answerTable.rows.map((row, idx) => (*/}
                                            {/*        <tr>*/}
                                            {/*            <th>{idx + 1}</th>*/}
                                            {/*            {row.map(cell => <td><span>{cell}</span></td>)}*/}
                                            {/*        </tr>*/}
                                            {/*    ))}*/}
                                            {/*</StudioTable>}*/}
                                        </StudioTabPanel>
                                    </StudioTabs>
                                </div>
                            </SplitPane>
                        </div>
                        <div className={classes.sidebarWindowGroup}>
                            <SplitPane split="horizontal" initialSizes={[1, 3]}>
                                <div>
                                    <div className={classes.sidebarWindowHeader}>
                                        Query Settings
                                    </div>
                                    <div className={classes.querySettingsBody}>
                                        Fast mode is <strong>enabled.</strong> Queries will run in less time than Neo4j takes to boot up.
                                    </div>
                                </div>
                                <div>
                                    <div className={classes.sidebarWindowHeader}>
                                        Graph Explorer
                                    </div>
                                    <div className={classes.graphExplorerBody}>
                                        {!selectedVertex && <p style={{padding: 8}}>Select a vertex from the graph to inspect it.</p>}
                                        {selectedVertex &&
                                        <>
                                            <StudioTable headings={["Property", "Value"]} minCellWidth={50} sizing="fixed"
                                                         initialGridTemplateColumns="minmax(80px, 1fr) minmax(80px, 2fr)" className={classes.graphExplorerTable}>
                                                <tr>
                                                    <td><span>Label</span></td>
                                                    <td><span>{selectedVertex.label}</span></td>
                                                </tr>
                                                <tr>
                                                    <td><span>Encoding</span></td>
                                                    <td><span>{selectedVertex.encoding}</span></td>
                                                </tr>
                                            </StudioTable>
                                            {/*<StudioButton size="smaller" type="primary" onClick={loadConnectedAttributes}>Load attribute ownerships</StudioButton>*/}
                                        </>}
                                    </div>
                                </div>
                            </SplitPane>
                        </div>
                    </SplitPane>
                </div>
                <div className={classes.rightSidebar}>
                    <StudioTabs orientation="topToBottom" items={rightSidebar} classes={{tab: classes.sidebarTab}}/>
                </div>
            </div>
            <div className={classes.statusBar}>
                {principalStatus}
                <div className={classes.filler}/>
                {/*{selectedResultsTab === ResultsTab.GRAPH &&*/}
                {/*<span>*/}
                {/*    Zoom: {zoom}%*/}
                {/*</span>}*/}
                {answerGraphStatus.vertexCount != null &&
                <div className={classes.resultsStatus}>
                    Vertices: {answerGraphStatus.vertexCount} | Edges: {answerGraphStatus.edgeCount} | {msToTime(answerGraphStatus.queryRunTime)}
                </div>}
            </div>
        </>
    );
}
