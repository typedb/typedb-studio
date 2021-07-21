import React from "react";
import { TypeDBVisualiserData } from "../data";
import { defaultTypeDBVisualiserTheme } from "../styles";
import { renderGraphPIXILegacy } from "../renderer/pixi-legacy-renderer";

export interface StaticVisualiserProps {
    data: TypeDBVisualiserData.Graph;
    className?: string;
}

const TypeDBStaticVisualiser: React.FC<StaticVisualiserProps> = ({data, className}) => {
    const graphPaneRef: React.MutableRefObject<any> = React.useRef(null);

    React.useEffect(() => {
        let destroyFn;

        if (graphPaneRef.current) {
            const { destroy } = renderGraphPIXILegacy(graphPaneRef.current, data, defaultTypeDBVisualiserTheme);
            destroyFn = destroy;
        }

        return destroyFn;
    }, [data]);

    return <div ref={graphPaneRef} className={className}/>;
};
export default TypeDBStaticVisualiser;
