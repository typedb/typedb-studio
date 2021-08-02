import { ipcRenderer, IpcRendererEvent } from "electron";
import React from "react";
import { MatchQueryRequest, MatchQueryResponsePart } from "../ipc/event-args";
import { themeState } from "../state/state";
import { StudioTheme } from "../styles/theme";
import { TypeDBVisualiserData } from "../typedb-visualiser";
import TypeDBVisualiser from "../typedb-visualiser/react/TypeDBVisualiser";
import { workspaceStyles } from "./workspace-styles";

export interface QueryProps {
    time: number;
    text: string;
}

export interface AnswerGraphStatus {
    vertexCount: number;
    edgeCount: number;
    queryRunTime: number;
}

export interface QueryVisualiserProps {
    db: string;
    query: QueryProps;
    theme: StudioTheme;
    onStatus: (value: AnswerGraphStatus) => any;
}

export const QueryVisualiser: React.FC<QueryVisualiserProps> = ({db, query, theme, onStatus}) => {
    const classes = workspaceStyles({ theme });
    // const [rawAnswers, setRawAnswers] = React.useState<ConceptMapData[]>(null);
    const [visualiserData, setVisualiserData] = React.useState<TypeDBVisualiserData.Graph>(null);

    const runQuery = () => {
        const req: MatchQueryRequest = { db, query: query.text };
        ipcRenderer.send("match-query-request", req);
        // setPrincipalStatus("Running Match query...");
        // setQueryRunning(true);
        // setQueryStartTime(Date.now());
        // setQueryRunTime("00:00.000");
        // setRenderRunTime(null);
        // setQueryCancelled(false);
        // setAnswerGraph({ simulationID: null, vertices: [], edges: [] });
        setVisualiserData({ simulationID: null, vertices: [], edges: [] });
        // setRawAnswers([]);
        // setAnswerTable(null);
        // addLogEntry(code);
    };

    React.useEffect(() => {
        if (query) runQuery();
    }, [query]);

    React.useEffect(() => {
        const onReceiveMatchQueryResponsePart = (_event: IpcRendererEvent, res: MatchQueryResponsePart) => {
            // TODO: Concurrent responses may produce odd behaviour - can we correlate the event in the response
            //  to the one we sent in the request somehow?
            // if (queryCancelled) return;

            if (res.done) {
                // setPrincipalStatus("Ready");
                // setQueryRunning(false);
                // setTimeQuery(true);
                // setQueryEndTime(Date.now());
            }

            // setRenderRunTime("<<in progress>>");
            if (res.success) {
                setVisualiserData(res.graph);
                onStatus({
                    vertexCount: res.graph.vertices.length,
                    edgeCount: res.graph.edges.length,
                    queryRunTime: res.executionTime,
                });
                // TODO: There must be a more efficient way of doing this
                // if (rawAnswers) {
                //     const headings = Object.keys(rawAnswers[0]);
                //     const rows = rawAnswers.map(answer => {
                //         const concepts = Object.values(answer);
                //         return concepts.map(concept => {
                //             // TODO: duplicated code
                //             return concept.value != null
                //                 ? `${concept.type}:${concept.value instanceof Date ? moment(concept.value).format("DD-MM-YY HH:mm:ss") : concept.value.toString().slice(0, 100)}`
                //                 : (concept.label || concept.type);
                //         });
                //     });
                //     // TODO: this setting of initialGridTemplateColumns is suspect
                //     setAnswerTable({ headings, rows, initialGridTemplateColumns: `40px ${"200px ".repeat(headings.length)}`.trim() });
                // } else {
                //     setAnswerTable(null); // We don't know what the column headings are if there are no answers
                // }
            } else {
                // setQueryResult("Error executing query");
                // setSnackbar({ open: true, variant: "error", message: res.error });
                // addLogEntry(res.error);
            }
        };

        ipcRenderer.on("match-query-response-part", onReceiveMatchQueryResponsePart);
        return () => {
            ipcRenderer.removeListener("match-query-response-part", onReceiveMatchQueryResponsePart);
        };
    }, [/*resultsLog, selectedResultsTab, queryCancelled, rawAnswers, */visualiserData]);

    // const loadConnectedAttributes = () => {
    //     const { vertices, edges } = visualiserData;
    //
    //     for (const conceptMap of rawAnswers) {
    //         for (const varName in conceptMap) {
    //             if (!conceptMap.hasOwnProperty(varName)) continue;
    //             const concept = conceptMap[varName] as GraphNode;
    //             if (concept.label !== selectedVertex.label) continue;
    //             for (const attributeTypeLabel of concept.ownsLabels) {
    //                 // TODO: don't add if already in graph
    //                 vertices.push({
    //                     id: graphElementIDs.nextID,
    //                     width: 110,
    //                     height: 40,
    //                     label: attributeTypeLabel,
    //                     encoding: "attributeType",
    //                 });
    //                 graphElementIDs.types[attributeTypeLabel] = graphElementIDs.nextID;
    //                 graphElementIDs.nextID++;
    //                 edges.push({ id: graphElementIDs.nextID, source: concept.nodeID, target: graphElementIDs.nextID, label: "owns" });
    //                 graphElementIDs.nextID++;
    //             }
    //             // TODO: answerGraph and visualiserData are usually identical unless the graph tab is not selected,
    //             //  but this isn't intuitive at all
    //             setAnswerGraph({ simulationID: visualiserData.simulationID, vertices, edges });
    //             setVisualiserData({ simulationID: visualiserData.simulationID, vertices, edges });
    //             setGraphElementIDs(graphElementIDs);
    //         }
    //     }
    // }

    return (
        <TypeDBVisualiser data={visualiserData} className={classes.visualiser} theme={themeState.use()[0].visualiser}
                          onVertexClick={() => null} onZoom={() => null} onFirstTick={() => null}/>
    );
}
