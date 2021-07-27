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
    className?: string;
}

const TypeDBVisualiser: React.FC<VisualiserProps> = ({data, theme, onVertexClick, className }) => {
    const htmlElementRef: React.MutableRefObject<HTMLDivElement> = React.useRef(null);
    const simulationRef: React.MutableRefObject<TypeDBGraphSimulation> = React.useRef(null);
    const [viewport, setViewport] = React.useState<Viewport>(null);

    React.useEffect(() => {
        if (!data) return;

        if (!htmlElementRef.current) throw new Error("Failed to start TypeDBVisualiser because its HTML container element could not be found");

        if (!viewport) {
            const renderingStage = setupStage(htmlElementRef.current);
            setViewport(renderingStage.viewport);
            return;
        }

        if (simulationRef.current && simulationRef.current.simulation.id === data.simulationID) {
            const currentVertexIDs = new Set(simulationRef.current.simulation.nodes().map(x => x.id));
            const newVertices = data.vertices.filter(x => !currentVertexIDs.has(x.id));
            simulationRef.current.add({
                vertices: newVertices,
                edges: data.edges,
            });
            return simulationRef.current.destroy;
        }

        let destroyFn;

        if (htmlElementRef.current) {
            const simulation = renderDynamicGraph(viewport, data, theme || defaultTypeDBVisualiserTheme, onVertexClick);
            destroyFn = simulation.destroy;
            simulationRef.current = simulation;
        }

        return destroyFn;
    }, [viewport, data]);

    return <div ref={htmlElementRef} className={className}/>;
};
export default TypeDBVisualiser;
