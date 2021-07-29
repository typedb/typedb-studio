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
import AceEditor from "react-ace";
import { SplitPane } from "react-collapse-pane";
import { useHistory } from "react-router-dom";
import { SnackbarContext } from "../app";
import { StudioButton } from "../common/button/button";
import { StudioIconButton } from "../common/button/icon-button";
import { StudioSelect } from "../common/select/select";
import { StudioTable } from "../common/table/table";
import { StudioTabItem, StudioTabPanel, StudioTabs } from "../common/tabs/tabs";
import { useInterval } from "../common/use-interval";
import { ConceptData, ConceptMapData, MatchQueryRequest, MatchQueryResponse } from "../ipc/event-args";
import { routes } from "../router";
import { studioStyles } from "../styles/studio-styles";
import { TypeDBVisualiserData, ForceGraphVertex } from "../typedb-visualiser";
import { uuidv4 } from "../util/uuid";
import { AceTypeQL } from "./ace-typeql";
import { workspaceStyles } from "./workspace-styles";
import { databaseState, dbServerState, themeState } from "../state/state";
import TypeDBVisualiser from "../typedb-visualiser/react/TypeDBVisualiser";
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

type GraphNode = ConceptData & {nodeID: number};

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

export const WorkspaceScreen: React.FC = () => {
    const theme = themeState.use()[0];
    const classes = Object.assign({}, studioStyles({ theme }), workspaceStyles({ theme }));

    const [db, setDB] = databaseState.use();
    const [dbServer, setDBServer] = dbServerState.use();
    const [code, setCode] = React.useState("match $x sub thing;\noffset 0;\nlimit 1000;\n");
    const [answerGraph, setAnswerGraph] = React.useState<TypeDBVisualiserData.Graph>(null);
    const [visualiserData, setVisualiserData] = React.useState<TypeDBVisualiserData.Graph>(null);
    const [rawAnswers, setRawAnswers] = React.useState<ConceptMapData[]>(null);
    const [answerTable, setAnswerTable] = React.useState<AnswerTable>(null);
    const { setSnackbar } = React.useContext(SnackbarContext);
    const [principalStatus, setPrincipalStatus] = React.useState("Ready");
    const [zoom, setZoom] = React.useState("100");
    const [queryResult, setQueryResult] = React.useState<string>(null);
    const [queryRunning, setQueryRunning] = React.useState(false);
    const [queryRunTime, setQueryRunTime] = React.useState<string>(null);
    const [renderRunTime, setRenderRunTime] = React.useState<string>(null);
    const [queryStartTime, setQueryStartTime] = React.useState<number>(null);
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

    const runQuery = () => {
        const req: MatchQueryRequest = { db, query: code };
        ipcRenderer.send("match-query-request", req);
        setPrincipalStatus("Running Match query...");
        setQueryRunning(true);
        setQueryStartTime(Date.now());
        setQueryRunTime("00:00.000");
        setRenderRunTime(null);
        setQueryCancelled(false);
        addLogEntry(code);
    };

    const cancelQuery = () => {
        if (!queryRunning) return; // resolves race condition between resolving onClick and processing query response
        ipcRenderer.send("cancel-query-request");
        setPrincipalStatus("Ready");
        setQueryRunning(false);
        setQueryEndTime(Date.now());
        setTimeQuery(true);
        setQueryCancelled(true);
        setQueryResult("Cancelled");
        addLogEntry("Query cancelled by user");
    };

    const runOrCancelQuery = () => {
        if (!queryRunning) runQuery();
        else cancelQuery();
    };

    const signOut = () => {
        routerHistory.push(routes.login);
    };

    useInterval(() => {
        if (queryRunning) setQueryRunTime(msToTime(Date.now() - queryStartTime));
        else if (timeQuery) {
            setQueryRunTime(msToTime(queryEndTime - queryStartTime));
            setTimeQuery(false);
        }
    }, 40);

    const aceEditorRef = React.useRef<AceEditor>(null);
    const [selectedIndex, setSelectedIndex] = React.useState(0);
    const [selectedResultsTab, setSelectedResultsTab] = React.useState(ResultsTab.GRAPH);

    const switchResultsTab = (tab: ResultsTab) => {
        setSelectedResultsTab(tab);
        if (tab === ResultsTab.GRAPH) {
            setVisualiserData(answerGraph);
        }
    }

    React.useEffect(() => {
        const customMode = new AceTypeQL();
        aceEditorRef.current.editor.getSession().setMode(customMode as any);
    }, []);

    const formatLogDate = (date: Date) => moment(date).format("DD-MM-YY HH:mm:ss.SSS");
    const [resultsLog, setResultsLog] = React.useState(`${formatLogDate(new Date())} - Connected to database '${databaseState.use()[0]}'`);
    const addLogEntry = (entry: string) => {
        const lines = entry.split("\n");
        const formattedLines = [lines[0]].concat(lines.slice(1).map(line => " ".repeat(24) + line));
        setResultsLog(resultsLog + `\n\n${formatLogDate(new Date())} - ${formattedLines.join("\n").trim()}`);
    }

    React.useEffect(() => {
        const onMatchQueryResponse = (_event: IpcRendererEvent, res: MatchQueryResponse) => {
            // TODO: Concurrent responses may produce odd behaviour - can we correlate the event in the response
            //  to the one we sent in the request somehow?
            if (queryCancelled) return;
            setPrincipalStatus("Ready");
            setQueryRunning(false);
            setTimeQuery(true);
            setQueryEndTime(Date.now());
            setRenderRunTime("<<in progress>>");
            if (res.success) {
                setRawAnswers(res.answers);
                const answerCountString = `${res.answers.length} answer${res.answers.length !== 1 ? "s" : ""}`;
                setQueryResult(answerCountString);
                addLogEntry(answerCountString);
                const vertices: TypeDBVisualiserData.Vertex[] = [];
                const edges: TypeDBVisualiserData.Edge[] = [];

                let nextID = 1;
                const typeNodeIDs: {[label: string]: number} = {};
                const thingNodeIDs: {[iid: string]: number} = {};

                for (const conceptMap of res.answers) {
                    for (const varName in conceptMap) {
                        if (!conceptMap.hasOwnProperty(varName)) continue;
                        const concept = conceptMap[varName] as GraphNode;

                        if (concept.iid) {
                            const thingNodeID = thingNodeIDs[concept.iid];
                            if (thingNodeID == null) {
                                concept.nodeID = nextID;
                                thingNodeIDs[concept.iid] = nextID;
                            } else {
                                concept.nodeID = thingNodeID;
                                continue;
                            }
                        } else {
                            const typeNodeID = typeNodeIDs[concept.label];
                            if (typeNodeID == null) {
                                concept.nodeID = nextID;
                                typeNodeIDs[concept.label] = nextID;
                            } else {
                                concept.nodeID = typeNodeID;
                                continue;
                            }
                        }

                        const label = (concept.value != null
                            ? `${concept.type}:${concept.value instanceof Date ? moment(concept.value).format("DD-MM-YY HH:mm:ss") : concept.value.toString()}`
                            : (concept.label || concept.type)).slice(0, ["relation", "relationType"].includes(concept.encoding) ? 11 : 13);

                        vertices.push({
                            id: nextID,
                            width: ["relationType", "relation"].includes(concept.encoding) ? 120 : 110,
                            height: ["relationType", "relation"].includes(concept.encoding) ? 60 : 40,
                            label,
                            encoding: concept.encoding,
                        });
                        nextID++;
                    }
                }

                for (const conceptMap of res.answers) {
                    for (const varName in conceptMap) {
                        if (!conceptMap.hasOwnProperty(varName)) continue;
                        const concept = conceptMap[varName] as GraphNode;

                        if (concept.playsTypes) {
                            for (const roleType of concept.playsTypes) {
                                const relationTypeNodeID = typeNodeIDs[roleType.relation];
                                if (relationTypeNodeID != null) {
                                    edges.push({ source: relationTypeNodeID, target: concept.nodeID, label: roleType.role });
                                }
                            }
                        }

                        if (concept.ownsLabels) {
                            for (const attributeTypeLabel of concept.ownsLabels) {
                                const attributeTypeNodeID = typeNodeIDs[attributeTypeLabel];
                                if (attributeTypeNodeID != null) {
                                    edges.push({ source: concept.nodeID, target: attributeTypeNodeID, label: "owns" });
                                }
                            }
                        }

                        if (concept.playerInstances) {
                            for (const rolePlayer of concept.playerInstances) {
                                const rolePlayerNodeID = thingNodeIDs[rolePlayer.iid];
                                if (rolePlayerNodeID != null) {
                                    edges.push({ source: concept.nodeID, target: rolePlayerNodeID, label: rolePlayer.role });
                                }
                            }
                        }

                        if (concept.ownerIIDs) {
                            for (const ownerIID of concept.ownerIIDs) {
                                const ownerNodeID = thingNodeIDs[ownerIID];
                                if (ownerNodeID != null) {
                                    edges.push({ source: ownerNodeID, target: concept.nodeID, label: "has" });
                                }
                            }
                        }
                    }
                }

                setAnswerGraph({ vertices, edges });
                const simulationID = uuidv4();
                // TODO: We should also skip the Concept API calls on the backend if the Graph tab is inactive
                // TODO: PoC - delete when redundant
                // if (selectedResultsTab === ResultsTab.GRAPH) {
                //     setVisualiserData({ simulationID, vertices: vertices.slice(0, 50), edges: [] });
                //     setTimeout(() => {
                //         setVisualiserData({ simulationID, vertices, edges: [] });
                //     }, 1000);
                //     setTimeout(() => {
                //         setVisualiserData({ simulationID, vertices, edges });
                //     }, 2000);
                // } else {
                //     setVisualiserData({ simulationID, vertices: [], edges: [] });
                // }
                if (selectedResultsTab === ResultsTab.GRAPH) {
                    setVisualiserData({simulationID, vertices, edges});
                } else {
                    setVisualiserData({simulationID, vertices: [], edges: []});
                }

                if (res.answers) {
                    const headings = Object.keys(res.answers[0]);
                    const rows = res.answers.map(answer => {
                        const concepts = Object.values(answer);
                        return concepts.map(concept => {
                            // TODO: duplicated code
                            return concept.value != null
                                ? `${concept.type}:${concept.value instanceof Date ? moment(concept.value).format("DD-MM-YY HH:mm:ss") : concept.value.toString().slice(0, 100)}`
                                : (concept.label || concept.type);
                        });
                    });
                    setAnswerTable({ headings, rows, initialGridTemplateColumns: `40px ${"200px ".repeat(headings.length)}`.trim() });
                } else {
                    setAnswerTable(null); // We don't know what the column headings are if there are no answers
                }
            } else {
                setQueryResult("Error executing query");
                setSnackbar({ open: true, variant: "error", message: res.error });
                addLogEntry(res.error);
            }
        };

        ipcRenderer.on("match-query-response", onMatchQueryResponse);
        return () => {
            ipcRenderer.removeListener("match-query-response", onMatchQueryResponse);
        };
    }, [resultsLog, selectedResultsTab, queryCancelled]);

    const onRenderDone = () => {
        setRenderRunTime(msToTime(Date.now() - queryEndTime));
    }

    const computeWorkspaceSplitPaneInitialWidths = () => {
        const workspacePaneWidth = window.innerWidth - 56;
        const graphExplorerInitialWidth = 200;
        const queryPaneInitialWidth = workspacePaneWidth - 200;
        return [queryPaneInitialWidth, graphExplorerInitialWidth];
    }

    const loadConnectedAttributes = () => {
        let nextID = 10000; // TODO: compute from existing graph data
        const { vertices, edges } = answerGraph;

        for (const conceptMap of rawAnswers) {
            for (const varName in conceptMap) {
                if (!conceptMap.hasOwnProperty(varName)) continue;
                const concept = conceptMap[varName] as GraphNode;
                if (concept.label !== selectedVertex.label) continue;
                for (const attributeTypeLabel of concept.ownsLabels) {
                    // TODO: don't add if already in graph
                    vertices.push({
                        id: nextID,
                        width: 110,
                        height: 40,
                        label: attributeTypeLabel,
                        encoding: "attributeType",
                    });
                    edges.push({ source: concept.nodeID, target: nextID, label: "owns" });
                    nextID++;
                }
                // TODO: THIS CODE IS HORRIBLE AND MUST BE IMPROVED ASAP
                setAnswerGraph({ vertices, edges });
                setVisualiserData({ simulationID: visualiserData.simulationID, vertices, edges });
            }
        }
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
                                            <AceEditor ref={aceEditorRef} mode="text" theme="studio-dark" fontSize={"1rem"} value={code}
                                                       onChange={newValue => setCode(newValue)} width="100%" height="100%"/>
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
                                            <TypeDBVisualiser data={visualiserData} className={classes.visualiser}
                                                              theme={themeState.use()[0].visualiser} onVertexClick={setSelectedVertex}
                                                              onZoom={updateZoom} onFirstTick={onRenderDone}/>
                                        </StudioTabPanel>
                                        <StudioTabPanel index={2} selectedIndex={selectedResultsTab} className={clsx(classes.resultsTabPanel, classes.resultsTablePanel)}>
                                            {answerTable &&
                                            <StudioTable headings={[""].concat(answerTable.headings)} minCellWidth={40} sizing="resizable"
                                                         initialGridTemplateColumns={answerTable.initialGridTemplateColumns} className={classes.resultsTable}>
                                                {answerTable.rows.map((row, idx) => (
                                                    <tr>
                                                        <th>{idx + 1}</th>
                                                        {row.map(cell => <td><span>{cell}</span></td>)}
                                                    </tr>
                                                ))}
                                            </StudioTable>}
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
                                            <StudioButton size="smaller" type="primary" onClick={loadConnectedAttributes}>Load attribute ownerships</StudioButton>
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
                {queryRunTime &&
                <div className={classes.resultsStatus}>
                    {queryResult != null ? <>{queryResult} | Query {queryRunTime}</> : <>Query {queryRunTime}</>}
                    {renderRunTime && <> | Render {renderRunTime}</>}
                </div>}
            </div>
        </>
    );
}
