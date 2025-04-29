import MultiGraph from "graphology";
import { ApiResponse, isApiErrorResponse, QueryResponse } from "../../typedb-driver/response";
import {constructGraphFromRowsResult} from "../graph.js";
import {convertLogicalGraphWith} from "../visualisation.js";
import {StudioConverter} from "./converter.js";
import {StudioConverterStructureParameters, StudioConverterStyleParameters} from "./config.js";

export class StudioVisualiser {
    private graph: MultiGraph;
    private styleParameters: StudioConverterStyleParameters;
    private structureParameters: StudioConverterStructureParameters;
    constructor(graph: MultiGraph, styleParameters: StudioConverterStyleParameters, structureParameters: StudioConverterStructureParameters) {
        this.graph = graph;
        this.styleParameters = styleParameters;
        this.structureParameters = structureParameters;
    }

    handleQueryResult(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) return;

        if (res.ok.answerType == "conceptRows" && res.ok.queryStructure != null) {
            let converter = new StudioConverter(this.graph, res.ok.queryStructure, false, this.structureParameters, this.styleParameters);
            let logicalGraph = constructGraphFromRowsResult(res.ok); // In memory, not visualised
            this.graph.clear();
            convertLogicalGraphWith(logicalGraph, converter);
        }
    }

    handleExplorationQueryResult(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) return;

        if (res.ok.answerType == "conceptRows" && res.ok.queryStructure != null) {
            let converter = new StudioConverter(this.graph, res.ok.queryStructure, true, this.structureParameters, this.styleParameters);
            let logicalGraph = constructGraphFromRowsResult(res.ok); // In memory, not visualised
            convertLogicalGraphWith(logicalGraph, converter);
        }
    }
}
