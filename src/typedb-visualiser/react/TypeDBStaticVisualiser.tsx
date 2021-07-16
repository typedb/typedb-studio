import React from "react";
import { TypeDBVisualiserCanvas, VisualiserProps } from "./canvas";
import { renderGraphPIXILegacy } from "../renderer/pixi-legacy-renderer";

const TypeDBStaticVisualiser: React.FC<VisualiserProps> = ({data, className}) =>
    <TypeDBVisualiserCanvas data={data} render={renderGraphPIXILegacy} className={className}/>;

export default TypeDBStaticVisualiser;
