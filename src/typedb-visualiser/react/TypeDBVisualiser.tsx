import React from "react";
import { TypeDBVisualiserCanvas, VisualiserProps } from "./canvas";
import { renderGraph } from "../renderer/pixi-renderer";

const TypeDBVisualiser: React.FC<VisualiserProps> = ({data, className, theme}) =>
    <TypeDBVisualiserCanvas data={data} render={renderGraph} theme={theme} className={className}/>;

export default TypeDBVisualiser;
