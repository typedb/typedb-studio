import { Viewport } from "pixi-viewport";
import React from "react";
import { TypeDBVisualiserData } from "../data";
import { defaultTypeDBVisualiserTheme, TypeDBVisualiserTheme } from "../styles";
import { renderToViewport, setupStage, TypeDBGraphSimulation } from "../renderer/viewport";

export interface VisualiserProps {
    data?: TypeDBVisualiserData.Graph;
    theme?: TypeDBVisualiserTheme;
    className?: string;
}

const TypeDBVisualiser: React.FC<VisualiserProps> = ({data, theme, className, }) => {
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
            const simulation = renderToViewport(viewport, data, theme || defaultTypeDBVisualiserTheme);
            destroyFn = simulation.destroy;
            simulationRef.current = simulation;
        }

        return destroyFn;
    }, [viewport, data]);

    return <div ref={htmlElementRef} className={className}/>;
};
export default TypeDBVisualiser;
