import React, {useEffect, useRef} from "react";
import { TypeDBVisualiserData } from "./data";
import { renderGraph } from "./pixi-renderer";
import { renderGraphPIXILegacy } from "./pixi-legacy-renderer";

export interface VisualiserProps {
    data: TypeDBVisualiserData.Graph;
    className?: string;
}

interface VisualiserCanvasProps extends VisualiserProps {
    render: (container: HTMLElement, data: TypeDBVisualiserData.Graph) => { destroy: () => void };
}

const TypeDBVisualiserCanvas: React.FC<VisualiserCanvasProps> = ({data, render, className}) => {
    const graphPaneRef: React.MutableRefObject<any> = useRef(null);

    useEffect(() => {
        let destroyFn;

        if (graphPaneRef.current) {
            const { destroy } = render(graphPaneRef.current, data);
            destroyFn = destroy;
        }

        return destroyFn;
    }, [data]);

    return <div ref={graphPaneRef} className={className}/>;
}

export const TypeDBVisualiser: React.FC<VisualiserProps> = ({data, className}) =>
    <TypeDBVisualiserCanvas data={data} render={renderGraph} className={className}/>;

export const TypeDBStaticVisualiser: React.FC<VisualiserProps> = ({data, className}) =>
    <TypeDBVisualiserCanvas data={data} render={renderGraphPIXILegacy} className={className}/>;
