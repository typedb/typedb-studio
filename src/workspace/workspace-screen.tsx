import { ipcRenderer } from "electron";
import React from "react";
import AceEditor from "react-ace";
import { SplitPane } from "react-collapse-pane";
import { SnackbarContext } from "../app";
import { StudioButton } from "../common/button/button";
import { StudioSelect } from "../common/select/select";
import { StudioTabItem, StudioTabPanel, StudioTabs } from "../common/tabs/tabs";
import { useInterval } from "../common/use-interval";
import { MatchQueryRequest, MatchQueryResponse } from "../ipc/event-args";
import { studioStyles } from "../styles/studio-styles";
import { TypeDBVisualiserData } from "../typedb-visualiser";
import { AceTypeQL } from "./ace-typeql";
import { workspaceStyles } from "./workspace-styles";
import { databaseState, dbServerState, themeState } from "../state/state";
import TypeDBVisualiser from "../typedb-visualiser/react/TypeDBVisualiser";

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

export const WorkspaceScreen: React.FC = () => {
    const theme = themeState.use()[0];
    const classes = Object.assign({}, studioStyles({ theme }), workspaceStyles({ theme }));

    const [db, setDB] = databaseState.use();
    const [dbServer, setDBServer] = dbServerState.use();
    const [code, setCode] = React.useState("match $x sub thing;\noffset 0;\nlimit 1000;\n");
    const [data, setData] = React.useState<TypeDBVisualiserData.Graph>(null);
    const { setSnackbar } = React.useContext(SnackbarContext);
    const [principalStatus, setPrincipalStatus] = React.useState("Ready");
    const [answerCount, setAnswerCount] = React.useState<number>(null);
    const [queryRunning, setQueryRunning] = React.useState(false);
    const [queryRunTime, setQueryRunTime] = React.useState<string>(null);
    const [queryStartTime, setQueryStartTime] = React.useState<number>(null);
    const [queryEndTime, setQueryEndTime] = React.useState<number>(null);
    const [timeQuery, setTimeQuery] = React.useState(false);

    const tabs: StudioTabItem[] = [{
        name: "Query1.tql",
        key: "0",
    }];

    async function runQuery() {
        const req: MatchQueryRequest = { db, query: code };
        ipcRenderer.send("match-query-request", req);
        setPrincipalStatus("Running Match query...");
        setQueryRunning(true);
        setQueryStartTime(Date.now());
        setQueryRunTime("00:00.000");
    }

    useInterval(() => {
        if (queryRunning) setQueryRunTime(msToTime(Date.now() - queryStartTime));
        else if (timeQuery) {
            setQueryRunTime(msToTime(queryEndTime - queryStartTime));
            setTimeQuery(false);
        }
    }, 40);

    const aceEditorRef = React.useRef<AceEditor>(null);
    const [selectedIndex, setSelectedIndex] = React.useState(0);

    React.useEffect(() => {
        const customMode = new AceTypeQL();
        aceEditorRef.current.editor.getSession().setMode(customMode as any);
    }, []);

    React.useEffect(() => {
        ipcRenderer.on("match-query-response", ((_event, res: MatchQueryResponse) => {
            setPrincipalStatus("Ready");
            setQueryRunning(false);
            setTimeQuery(true);
            setQueryEndTime(Date.now());
            if (res.success) {
                setAnswerCount(res.answers.length);
                const vertices: TypeDBVisualiserData.Vertex[] = [];
                let nextID = 1;
                for (const conceptMap of res.answers) {
                    for (const varName in conceptMap) {
                        if (!conceptMap.hasOwnProperty(varName)) continue;
                        const concept = conceptMap[varName];
                        vertices.push({
                            id: nextID,
                            width: 150,
                            height: concept.encoding === "relationType" ? 75 : 60,
                            label: concept.value ? `${concept.type}:${concept.value}` : (concept.label || concept.type),
                            encoding: concept.encoding,
                        });
                        nextID++;
                    }
                }
                setData({vertices, edges: []});
            } else {
                setAnswerCount(null);
                setSnackbar({ open: true, variant: "error", message: res.error });
            }
        }));
    }, []);

    return (
        <>
            <div className={classes.appBar}>
                <StudioSelect value={db} setValue={setDB} variant="filled">
                    {dbServer.dbs.map(db => <option value={db}>{db}</option>)}
                </StudioSelect>
            </div>
            <div className={classes.querySplitPane}>
                <SplitPane split="horizontal" initialSizes={[4, 7]}>
                    <div className={classes.editorPane}>
                        <StudioTabs selectedIndex={selectedIndex} setSelectedIndex={setSelectedIndex} items={tabs}
                                    classes={{ root: classes.editorTabs, tabGroup: classes.editorTabGroup, tab: classes.editorTab }}
                                    showAddButton>
                            <StudioTabPanel index={0} selectedIndex={selectedIndex} className={classes.editorTabPanel}>
                                <AceEditor ref={aceEditorRef} mode="text" theme="studio-dark" fontSize={"1rem"} value={code}
                                           onChange={newValue => setCode(newValue)} width="100%" height="100%"/>
                            </StudioTabPanel>
                        </StudioTabs>
                        <div className={classes.actionsBar}>
                            <StudioButton size="small" type="primary" onClick={runQuery}>▶️ Run</StudioButton>
                        </div>
                    </div>
                    <div className={classes.resultsPane}>
                        <TypeDBVisualiser data={data} className={classes.visualiser} theme={themeState.use()[0].visualiser}/>
                        <div className={classes.statusBar}>
                            {principalStatus}
                            <div className={classes.filler}/>
                            {answerCount != null ? <>{answerCount} answer{answerCount !== 1 && "s"} | {queryRunTime}</> : queryRunTime != null ? queryRunTime : ""}
                        </div>
                    </div>
                </SplitPane>
            </div>
        </>
    );
}
