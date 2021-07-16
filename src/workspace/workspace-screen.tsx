import React, { useEffect, useState } from "react";
import { TypeDBVisualiserData } from "../typedb-visualiser";
import { workspaceStyles } from "./workspace-styles";
import { SessionType } from "typedb-client/api/connection/TypeDBSession";
import { Type } from "typedb-client/api/concept/type/Type";
import { databaseState, themeState, typeDBClientState } from "../state/typedb-client";
import TypeDBVisualiser from "../typedb-visualiser/react/TypeDBVisualiser";
import { TransactionType } from "typedb-client/api/connection/TypeDBTransaction";

export const WorkspaceScreen: React.FC = () => {
    console.log("Rendering workspace screen")
    const client = typeDBClientState.use()[0];
    const db = databaseState.use()[0];
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

    return client ? <TypeDBVisualiser data={data} className={workspaceStyles().visualiser} theme={themeState.use()[0].visualiser}/> : <h1>Hello World</h1>;
}
