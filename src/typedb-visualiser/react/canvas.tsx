import React, {useEffect, useRef} from "react";
import { TypeDBVisualiserData } from "../data";
import { defaultTypeDBVisualiserTheme, TypeDBVisualiserTheme } from "../styles";

export interface VisualiserProps {
    data: TypeDBVisualiserData.Graph;
    theme?: TypeDBVisualiserTheme;
    className?: string;
}

interface VisualiserCanvasProps extends VisualiserProps {
    render: (container: HTMLElement, data: TypeDBVisualiserData.Graph, theme?: TypeDBVisualiserTheme) => { destroy: () => void };
}

export const TypeDBVisualiserCanvas: React.FC<VisualiserCanvasProps> = ({data, render, theme, className}) => {
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
