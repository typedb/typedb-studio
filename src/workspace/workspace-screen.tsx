import React, { useEffect, useState } from "react";
import { SplitPane } from "react-collapse-pane";
import { TypeDBVisualiserData } from "../typedb-visualiser";
import { workspaceStyles } from "./workspace-styles";
import { SessionType, TransactionType, Type } from "typedb-client";
import { databaseState, themeState, typeDBClientState } from "../state/typedb-client";
import TypeDBVisualiser from "../typedb-visualiser/react/TypeDBVisualiser";
import { Controlled as CodeMirror, IControlledCodeMirror } from "react-codemirror2";

export const WorkspaceScreen: React.FC = () => {
    const classes = workspaceStyles({ theme: themeState.use()[0] });

    const client = typeDBClientState.use()[0];
    const db = databaseState.use()[0];
    const [code, setCode] = React.useState("match $x sub thing;");
    const [data, setData] = useState<TypeDBVisualiserData.Graph>({vertices: [], edges: []});

    useEffect(() => {
        async function loadSchema() {
            console.log(new Date(), "Loading schema...");
            let session, tx;
            try {
                session = await client.session(db, SessionType.DATA);
                tx = await session.transaction(TransactionType.READ);
                const conceptMaps = await tx.query.match("match $x sub thing;").collect();
                console.log(new Date(), "Creating graph from Grabl schema");
                const vertices: TypeDBVisualiserData.Vertex[] = [];
                let nextID = 1;
                for (const conceptMap of conceptMaps) {
                    const type = conceptMap.get("x") as Type;
                    const encoding = type.isEntityType() ? "entityType" : type.isRelationType() ? "relationType" : "attributeType";
                    vertices.push({
                        id: nextID,
                        width: 150,
                        height: encoding === "relationType" ? 75 : 60,
                        label: type.label.name,
                        encoding,
                    });
                    nextID++;
                }
                setData({vertices, edges: []});
                console.log(new Date(), "Rendering Grabl schema...");
            } finally {
                session.close();
            }
        }

        loadSchema();
    }, []);

    const onBeforeCodeChange: IControlledCodeMirror["onBeforeChange"] = (_editor, _data, value) => {
        setCode(value);
    };

    return (
        <div>
            <div className={classes.appBar}></div>
            <SplitPane split="horizontal" initialSizes={[1, 2]}>
                <div className={classes.codeEditor}>
                    <CodeMirror onBeforeChange={onBeforeCodeChange} value={code} options={{mode: "typeql"}}/>
                </div>
                <TypeDBVisualiser data={data} className={classes.visualiser} theme={themeState.use()[0].visualiser}/>
            </SplitPane>
        </div>
    );
}
