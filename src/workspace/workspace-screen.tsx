import { faCog } from "@fortawesome/free-solid-svg-icons/faCog";
import { faDatabase } from "@fortawesome/free-solid-svg-icons/faDatabase";
import { faFolderOpen } from "@fortawesome/free-solid-svg-icons/faFolderOpen";
import { faPlay } from "@fortawesome/free-solid-svg-icons/faPlay";
import { faProjectDiagram } from "@fortawesome/free-solid-svg-icons/faProjectDiagram";
import { faSave } from "@fortawesome/free-solid-svg-icons/faSave";
import { faShapes } from "@fortawesome/free-solid-svg-icons/faShapes";
import { faSignOutAlt } from "@fortawesome/free-solid-svg-icons/faSignOutAlt";
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
import { ConceptData, MatchQueryRequest, MatchQueryResponse } from "../ipc/event-args";
import { routes } from "../router";
import { studioStyles } from "../styles/studio-styles";
import { TypeDBVisualiserData } from "../typedb-visualiser";
import { AceTypeQL } from "./ace-typeql";
import { workspaceStyles } from "./workspace-styles";
import { databaseState, dbServerState, themeState } from "../state/state";
import TypeDBVisualiser from "../typedb-visualiser/react/TypeDBVisualiser";
import moment from "moment";

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

export const WorkspaceScreen: React.FC = () => {
    const theme = themeState.use()[0];
    const classes = Object.assign({}, studioStyles({ theme }), workspaceStyles({ theme }));

    const [db, setDB] = databaseState.use();
    const [dbServer, setDBServer] = dbServerState.use();
    const [code, setCode] = React.useState("match $x sub thing;\noffset 0;\nlimit 1000;\n");
    const [data, setData] = React.useState<TypeDBVisualiserData.Graph>(null);
    const { setSnackbar } = React.useContext(SnackbarContext);
    const [principalStatus, setPrincipalStatus] = React.useState("Ready");
    const [queryResult, setQueryResult] = React.useState<string>(null);
    const [queryRunning, setQueryRunning] = React.useState(false);
    const [queryRunTime, setQueryRunTime] = React.useState<string>(null);
    const [queryStartTime, setQueryStartTime] = React.useState<number>(null);
    const [queryEndTime, setQueryEndTime] = React.useState<number>(null);
    const [timeQuery, setTimeQuery] = React.useState(false);
    const routerHistory = useHistory();

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

    const runQuery = async () => {
        const req: MatchQueryRequest = { db, query: code };
        ipcRenderer.send("match-query-request", req);
        setPrincipalStatus("Running Match query...");
        setQueryRunning(true);
        setQueryStartTime(Date.now());
        setQueryRunTime("00:00.000");
        addLogEntry(code);
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
    const [selectedResultsTabIndex, setSelectedResultsTabIndex] = React.useState(1);

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
            setPrincipalStatus("Ready");
            setQueryRunning(false);
            setTimeQuery(true);
            setQueryEndTime(Date.now());
            if (res.success) {
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

                        vertices.push({
                            id: nextID,
                            width: 110,
                            height: concept.encoding === "relationType" ? 60 : 40,
                            label: concept.value != null ? `${concept.type}:${concept.value.toString().slice(0, 100)}` : (concept.label || concept.type),
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

                setData({vertices, edges });
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
    }, [resultsLog]);

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
                <StudioIconButton size="smaller" onClick={runQuery}>
                    <FontAwesomeIcon icon={faPlay}/>
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
                                        <StudioTabPanel index={0} selectedIndex={selectedResultsTab} className={clsx(classes.resultsTabPanel, classes.resultsLogPanel)}>
                                            <pre>{resultsLog}</pre>
                                        </StudioTabPanel>
                                        <StudioTabPanel index={1} selectedIndex={selectedResultsTab} className={classes.resultsTabPanel}>
                                            <TypeDBVisualiser data={visualiserData} className={classes.visualiser} theme={themeState.use()[0].visualiser}/>
                                        </StudioTabPanel>
                                        <StudioTabPanel index={2} selectedIndex={selectedResultsTab} className={clsx(classes.resultsTabPanel, classes.resultsTablePanel)}>
                                            Food for humans goes here
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
                                        <StudioTable headings={["Property", "Value"]} minCellWidth={50} className={classes.graphExplorerTable}>
                                            <tr>
                                                <td><span>Type</span></td>
                                                <td><span>person</span></td>
                                            </tr>
                                            <tr>
                                                <td><span>Encoding</span></td>
                                                <td><span>entity</span></td>
                                            </tr>
                                            <tr>
                                                <td><span>Internal ID</span></td>
                                                <td><span>123b548m5656vbc4nb430gh3453d</span></td>
                                            </tr>
                                        </StudioTable>
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
                {queryResult != null ? <>{queryResult} | {queryRunTime}</> : queryRunTime != null ? queryRunTime : ""}
            </div>
        </>
    );
}
