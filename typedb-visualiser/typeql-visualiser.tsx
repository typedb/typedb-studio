import React, {useEffect, useRef} from "react";
import {TypeQLGraph} from "./typeql-data";
import {runTypeQLForceGraph} from "./typeql-force-graph";
import {typeQLVisualiserStyles} from "./typeql-styles";

interface VisualiserProps {
    data: TypeQLGraph;
}

const TypeQLVisualiserPane: React.FC<VisualiserProps & { runForceGraph: (container: HTMLElement, data: TypeQLGraph) => { destroy: () => void } }> = ({data, runForceGraph}) => {
    const graphPaneRef: React.MutableRefObject<any> = useRef(null);
    const classes = typeQLVisualiserStyles();

    useEffect(() => {
        let destroyFn;

        if (graphPaneRef.current) {
            const { destroy } = runForceGraph(graphPaneRef.current, data);
            destroyFn = destroy;
        }

        return destroyFn;
    }, [data]);

    return <div ref={graphPaneRef} className={classes.graphPane} />;
}

export const TypeQLVisualiser: React.FC<VisualiserProps> = ({data}) => <TypeQLVisualiserPane data={data} runForceGraph={runTypeQLForceGraph}/>;

export const TypeQLVisualiserPixiJSLegacy: React.FC<VisualiserProps> = ({data}) => <TypeQLVisualiserPane data={data} runForceGraph={runTypeQLForceGraph}/>;
