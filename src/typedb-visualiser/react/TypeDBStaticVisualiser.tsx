import React from "react";
import { TypeDBVisualiserData } from "../data";
import { defaultTypeDBVisualiserTheme } from "../styles";
import { renderToFixedContainer } from "../renderer/fixed-container";

export interface StaticVisualiserProps {
    data: TypeDBVisualiserData.Graph;
    className?: string;
}

const TypeDBStaticVisualiser: React.FC<StaticVisualiserProps> = ({data, className}) => {
    const graphPaneRef: React.MutableRefObject<any> = React.useRef(null);

    React.useEffect(() => {
        let destroyFn;

        if (graphPaneRef.current) {
            const { destroy } = renderToFixedContainer(graphPaneRef.current, data, defaultTypeDBVisualiserTheme);
            destroyFn = destroy;
        }

        return destroyFn;
    }, [data]);

    return <div ref={graphPaneRef} className={className}/>;
};
export default TypeDBStaticVisualiser;
