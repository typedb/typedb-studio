import { Viewport } from "pixi-viewport";
import React from "react";
import { TypeDBVisualiserData } from "../data";
import { defaultTypeDBVisualiserTheme, TypeDBVisualiserTheme } from "../styles";
import { renderToViewport, setupStage } from "../renderer/viewport";

export interface VisualiserProps {
    data?: TypeDBVisualiserData.Graph;
    theme?: TypeDBVisualiserTheme;
    className?: string;
}

const TypeDBVisualiser: React.FC<VisualiserProps> = ({data, className, theme}) => {
    const htmlElementRef: React.MutableRefObject<HTMLDivElement> = React.useRef(null);
    const [viewport, setViewport] = React.useState<Viewport>(null);

    React.useEffect(() => {
        if (!data) return;

        if (!htmlElementRef.current) throw new Error("Failed to start TypeDBVisualiser because its HTML container element could not be found");

        if (!viewport) {
            const renderingStage = setupStage(htmlElementRef.current);
            setViewport(renderingStage.viewport);
            return;
        }

        let destroyFn;

        if (htmlElementRef.current) {
            const { destroy } = renderToViewport(viewport, data, theme || defaultTypeDBVisualiserTheme);
            destroyFn = destroy;
        }

        return destroyFn;
    }, [viewport, data]);

    return <div ref={htmlElementRef} className={className}/>;
};
export default TypeDBVisualiser;
