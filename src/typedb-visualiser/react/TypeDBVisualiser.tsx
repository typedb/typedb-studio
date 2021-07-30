import { Viewport } from "pixi-viewport";
import React from "react";
import { ForceGraphVertex } from "../d3-force-simulation";
import { TypeDBVisualiserData } from "../data";
import { defaultTypeDBVisualiserTheme, TypeDBVisualiserTheme } from "../styles";
import { renderDynamicGraph, setupStage, TypeDBGraphSimulation } from "../renderer/dynamic-renderer";

export interface VisualiserProps {
    data?: TypeDBVisualiserData.Graph;
    theme?: TypeDBVisualiserTheme;
    onVertexClick?: (vertex: ForceGraphVertex) => any;
    onZoom?: (scale: number) => any;
    onFirstTick?: () => any;
    className?: string;
}

const TypeDBVisualiser: React.FC<VisualiserProps> = ({data, theme, onVertexClick, onZoom, onFirstTick, className }) => {
    const htmlElementRef: React.MutableRefObject<HTMLDivElement> = React.useRef(null);
    const simulationRef: React.MutableRefObject<TypeDBGraphSimulation> = React.useRef(null);
    const [viewport, setViewport] = React.useState<Viewport>(null);

    React.useEffect(() => {
        // TODO: This is a bit of a hack - setupStage should be called on first render, but if we do that,
        //       the viewport will have the wrong size because of SplitPane's resizing.
        //       To fix this issue we need to resize the viewport when its container is resized
        if (!data) return;

        if (!htmlElementRef.current) throw new Error("Failed to start TypeDBVisualiser because its HTML container element could not be found");

        if (!viewport) {
            const renderingStage = setupStage(htmlElementRef.current, onZoom);
            setViewport(renderingStage.viewport);
            return;
        }

        if (simulationRef.current && simulationRef.current.simulation.id === data.simulationID) {
            const currentVertexIDs = new Set(simulationRef.current.simulation.nodes().map(x => x.id));
            const currentEdgeIDs = new Set(simulationRef.current.simulation.edges.map(x => x.id));
            const newVertices = data.vertices.filter(x => !currentVertexIDs.has(x.id));
            const newEdges = data.edges.filter(x => !currentEdgeIDs.has(x.id));
            console.log([newVertices, newEdges]);
            simulationRef.current.add({
                vertices: newVertices,
                edges: newEdges,
            });
            return simulationRef.current.destroy;
        }

        let destroyFn;

        if (htmlElementRef.current) {
            const simulation = renderDynamicGraph(viewport, data, theme || defaultTypeDBVisualiserTheme, onVertexClick, onFirstTick);
            destroyFn = simulation.destroy;
            simulationRef.current = simulation;
        }

        return destroyFn;
    }, [viewport, data]);

    return <div ref={htmlElementRef} className={className}/>;
};
export default TypeDBVisualiser;
