import * as PIXI from "pixi.js";
// @ts-ignore
import FontFaceObserver from "fontfaceobserver";
import { TypeDBVisualiserData } from "../data";
import { stickyForceSimulation } from "../d3-force-simulation";
import { TypeDBVisualiserTheme } from "../styles";
import { renderEdge, renderEdgeLabel, Renderer, renderVertex } from "./graph-renderer-common";

export function renderStaticGraph(container: HTMLElement, graphData: TypeDBVisualiserData.Graph, theme: TypeDBVisualiserTheme) {
    const [width, height] = [container.offsetWidth, container.offsetHeight];
    const edges: Renderer.Edge[] = graphData.edges.map((d) => Object.assign({}, d));
    const vertices: Renderer.Vertex[] = graphData.vertices.map((d) => Object.assign({}, d));
    let dragged = false;

    const app = new PIXI.Application({ width, height, antialias: !0, backgroundAlpha: 0, resolution: window.devicePixelRatio });
    container.innerHTML = "";
    container.appendChild(app.view);

    const simulation = stickyForceSimulation(vertices, edges, width, height);
    const ubuntuMono = new FontFaceObserver("Ubuntu Mono") as { load: () => Promise<any> };

    function onDragStart(this: any, evt: any) {
        simulation.alphaTarget(0.3).restart();
        this.isDown = true;
        this.eventData = evt.data;
        this.alpha = 0.75;
        this.dragging = true;
    }

    function onDragEnd(this: any, gfx: any) {
        simulation.alphaTarget(0);
        gfx.alpha = 1;
        gfx.dragging = false;
        gfx.isOver = false;
        gfx.eventData = null;
        delete this.fx;
        delete this.fy;
    }

    function onDragMove(this: any, gfx: any) {
        if (gfx.dragging) {
            dragged = true;
            const newPosition = gfx.eventData.getLocalPosition(gfx.parent);
            this.fx = newPosition.x;
            this.fy = newPosition.y;
        }
    }

    vertices.forEach((vertex) => {
        const boundDragMove = onDragMove.bind(vertex);
        const boundDragEnd = onDragEnd.bind(vertex);
        renderVertex(vertex, ubuntuMono, theme);

        vertex.gfx
            .on('click', (e: Event) => {
                if (!dragged) {
                    e.stopPropagation();
                }
                dragged = false;
            })
            .on('mousedown', onDragStart)
            .on('mouseup', () => boundDragEnd(vertex.gfx))
            .on('mouseupoutside', () => boundDragEnd(vertex.gfx))
            .on('mousemove', () => boundDragMove(vertex.gfx));
        vertex.gfx.interactive = true;
        vertex.gfx.buttonMode = true;

        app.stage.addChild(vertex.gfx);
    });

    const edgesGFX = new PIXI.Graphics();
    app.stage.addChild(edgesGFX);

    const onTick = () => {
        vertices.forEach((vertex) => {
            let { x, y, gfx } = vertex;
            gfx.position.set(x, y);
        });

        for (let i = edgesGFX.children.length - 1; i >= 0; i--) {
            edgesGFX.children[i].destroy();
        }

        edgesGFX.clear();
        edgesGFX.removeChildren();
        edges.forEach((edge) => {
            renderEdge(edge, edgesGFX, theme);
        });
    }

    edges.forEach(async (edge) => {
        await renderEdgeLabel(edge, ubuntuMono, theme);
        app.stage.addChild(edge.labelGFX);
    });

    // Listen for tick events to render the nodes as they update in your Canvas or SVG.
    simulation.on("tick", onTick);

    return {
        destroy: () => {
            simulation.stop();
            vertices.forEach((vertex) => {
                vertex.gfx.clear();
            });
            edgesGFX.clear();
        }
    };
}
