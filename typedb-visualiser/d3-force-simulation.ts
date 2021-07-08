/*
 Source: https://levelup.gitconnected.com/creating-a-force-graph-using-react-d3-and-pixijs-95616051aba
 */

import * as d3 from "d3-force";
import { TypeDBVisualiserData } from "./data";

export type ForceGraphVertex = d3.SimulationNodeDatum & TypeDBVisualiserData.Vertex;
export type ForceGraphEdge = d3.SimulationLinkDatum<ForceGraphVertex> & TypeDBVisualiserData.Edge;

export function stickyForceSimulation(vertices: ForceGraphVertex[], edges: ForceGraphEdge[], width: number, height: number) {
    return d3.forceSimulation(vertices)
        .force("link", d3.forceLink(edges) // This force provides links between nodes
            .id((d: any) => d.id) // This sets the node id accessor to the specified function. If not specified, will default to the index of a node.
            .distance(function (d: any) {
                const source = {
                    x: width * d.source.x / 100,
                    y: height * d.source.y / 100,
                },
                target = {
                    x: width * d.target.x / 100,
                    y: height * d.target.y / 100,
                };

                return Math.sqrt(Math.pow(source.x - target.x, 2) + Math.pow(source.y - target.y, 2));
            })
        )
        .force("charge", d3.forceManyBody().strength(-100)) // This adds repulsion (if it's negative) between nodes.
        .force("x", d3.forceX().x((d: any) => width * d.x / 100).strength(1))
        .force("y", d3.forceY().y((d: any) => height * d.y / 100).strength(1))
        .velocityDecay(0.8);
}
