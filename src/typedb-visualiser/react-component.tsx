import React, {useEffect, useRef} from "react";
import { TypeDBVisualiserData } from "./data";
import { renderGraph } from "./pixi-renderer";
import { renderGraphPIXILegacy } from "./pixi-legacy-renderer";
import { defaultTypeDBVisualiserTheme, TypeDBVisualiserTheme } from "./styles";

export interface VisualiserProps {
    data: TypeDBVisualiserData.Graph;
    theme?: TypeDBVisualiserTheme;
    className?: string;
}

interface VisualiserCanvasProps extends VisualiserProps {
    render: (container: HTMLElement, data: TypeDBVisualiserData.Graph, theme?: TypeDBVisualiserTheme) => { destroy: () => void };
}

const TypeDBVisualiserCanvas: React.FC<VisualiserCanvasProps> = ({data, render, theme, className}) => {
    const graphPaneRef: React.MutableRefObject<any> = useRef(null);

    useEffect(() => {
        let destroyFn;

        if (graphPaneRef.current) {
            const { destroy } = render(graphPaneRef.current, data, theme || defaultTypeDBVisualiserTheme);
            destroyFn = destroy;
        }

        return destroyFn;
    }, [data]);

    return <div ref={graphPaneRef} className={className}/>;
}

export const TypeDBVisualiser: React.FC<VisualiserProps> = ({data, className, theme}) =>
    <TypeDBVisualiserCanvas data={data} render={renderGraph} theme={theme} className={className}/>;

export const TypeDBStaticVisualiser: React.FC<VisualiserProps> = ({data, className}) =>
    <TypeDBVisualiserCanvas data={data} render={renderGraphPIXILegacy} className={className}/>;
