import React from "react";
import { TypeDBVisualiser, TypeDBVisualiserData } from "../typedb-visualiser";
import { workspaceStyles } from "./workspace-styles";

const inferenceExampleGraph: TypeDBVisualiserData.Graph = {
    "vertices": [{
        "id": 1,
        "label": "teacher: Alice",
        "encoding": "entityType",
        "width": 156,
        "height": 32,
    }, {
        "id": 2,
        "label": "location",
        "encoding": "relationType",
        "width": 105,
        "height": 66,
    }, {
        "id": 3,
        "label": "country: UK",
        "encoding": "entityType",
        "width": 156,
        "height": 32,
    }, {
        "id": 4,
        "label": "location",
        "encoding": "relationType",
        "width": 105,
        "height": 66,
    }, {
        "id": 5,
        "label": "city: London",
        "encoding": "entityType",
        "width": 156,
        "height": 32,
    }, {
        "id": 6,
        "label": "location",
        "encoding": "relationType",
        "width": 105,
        "height": 66,
    }, {
        "id": 7,
        "label": "postgrad: Bob",
        "encoding": "entityType",
        "width": 156,
        "height": 32,
    }],
    "edges": [{
        "source": 2,
        "target": 1,
        "label": "located",
    }, {
        "source": 2,
        "target": 3,
        "label": "locating",
    }, {
        "source": 4,
        "target": 3,
        "label": "located",
        "highlight": "inferred",
    }, {
        "source": 4,
        "target": 5,
        "label": "locating",
        "highlight": "inferred",
    }, {
        "source": 6,
        "target": 5,
        "label": "located",
        "highlight": "inferred",
    }, {
        "source": 6,
        "target": 7,
        "label": "locating",
        "highlight": "inferred",
    }]
};

export const WorkspaceScreen: React.FC = () => {
    return (
        <TypeDBVisualiser data={inferenceExampleGraph} className={workspaceStyles().visualiser}/>
    );
}
