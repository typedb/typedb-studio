import * as d3 from "d3-force";
import * as PIXI from "pixi.js";
// @ts-ignore
import FontFaceObserver from "fontfaceobserver";
import { dynamicForceSimulation, ForceGraphEdge, ForceGraphVertex } from "../d3-force-simulation";
import { TypeDBVisualiserTheme } from "../styles";
import { TypeDBVisualiserData } from "../data";
import { Viewport } from "pixi-viewport";
import { renderEdge, renderEdgeLabel, Renderer, renderVertex } from "./graph-renderer";

export interface RenderingStage {
    renderer: PIXI.Renderer;
    viewport: Viewport;
}

export function setupStage(container: HTMLElement): RenderingStage {
    const [width, height] = [container.offsetWidth, container.offsetHeight];

    const renderer = new PIXI.Renderer({ width, height, antialias: !0, backgroundAlpha: 0, resolution: window.devicePixelRatio });
    container.innerHTML = "";
    container.appendChild(renderer.view);
    const stage = new PIXI.Container();

    const ticker = new PIXI.Ticker();
    ticker.add(() => {
        renderer.render(stage);
    }, PIXI.UPDATE_PRIORITY.LOW);
    ticker.start();

    const viewport = new Viewport({
        screenWidth: width,
        screenHeight: height,
        worldWidth: width,
        worldHeight: height,
        passiveWheel: false,

        interaction: renderer.plugins.interaction // the interaction module is important for wheel to work properly when renderer.view is placed or scaled
    });
    stage.addChild(viewport);

    // activate plugins
    viewport.drag()
        .pinch({ factor: 1 })
        .wheel({ percent: -.5 })
        .clampZoom({ minScale: .01, maxScale: 3 })
        .decelerate({ friction: .95 });

    return { renderer, viewport };
}

export interface TypeDBGraphSimulation {
    simulation: d3.Simulation<ForceGraphVertex, ForceGraphEdge>;
    add: (newObjects: { vertices: TypeDBVisualiserData.Vertex[], edges: TypeDBVisualiserData.Edge[] }) => void;
    destroy: () => void;
    clear: () => void;
}

// TODO: The purpose of this file isn't clear.
// TODO: Too much of this code is shared with fixed-container.ts, a refactor is required
export function renderToViewport(viewport: Viewport, graphData: TypeDBVisualiserData.Graph, theme: TypeDBVisualiserTheme): TypeDBGraphSimulation {
    console.log("renderToViewport called")
    viewport.removeChildren();
    const [width, height] = [viewport.screenWidth, viewport.screenHeight];
    let edges: Renderer.Edge[] = graphData.edges.map((d) => Object.assign({}, d));
    const vertices: Renderer.Vertex[] = graphData.vertices.map((d) => Object.assign({}, d));
    let dragged = false;

    const simulation = dynamicForceSimulation(vertices, edges, width, height);
    simulation.id = graphData.simulationID;
    const ubuntuMono = new FontFaceObserver("Ubuntu Mono") as { load: () => Promise<any> };

    function onDragStart(this: any, evt: any) {
        viewport.plugins.pause('drag');
        simulation.alphaTarget(0.15).restart();
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
        viewport.plugins.resume('drag');
    }

    function onDragMove(this: any, gfx: any) {
        if (gfx.dragging) {
            dragged = true;
            const newPosition = gfx.eventData.getLocalPosition(gfx.parent);
            this.fx = newPosition.x;
            this.fy = newPosition.y;
        }
    }

    function addVertex(vertex: Renderer.Vertex) {
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

        viewport.addChild(vertex.gfx);
    }

    vertices.forEach(v => addVertex(v));

    const edgesGFX = new PIXI.Graphics();
    viewport.addChild(edgesGFX);

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

    async function addEdgeLabel(edge: Renderer.Edge) {
        await renderEdgeLabel(edge, ubuntuMono, theme);
        viewport.addChild(edge.labelGFX);
    }

    edges.forEach(addEdgeLabel);

    // Listen for tick events to render the nodes as they update in your Canvas or SVG.
    simulation.on("tick", onTick);

    return {
        simulation,
        add: (newObject => {
            console.log(newObject.vertices);
            const [newVertices, newEdges] = [newObject.vertices, newObject.edges];
            vertices.push(...newVertices);
            simulation.nodes(vertices);
            simulation.force("link", null);
            edges = newEdges;
            simulation.force("link", d3.forceLink(edges).id((d: any) => d.id).distance(120));

            newVertices.forEach(addVertex);
            edgesGFX.clear();
            edges.forEach(addEdgeLabel);
            simulation.restart();
        }),
        destroy: () => {
            console.log("destroying")
            simulation.stop();
            // vertices.forEach((vertex) => {
            //     vertex.gfx.clear();
            // });
            edgesGFX.clear();
        },
        clear: () => {
            console.log("clearing")
            simulation.stop();
            vertices.forEach((vertex) => {
                vertex.gfx.clear();
            });
            edgesGFX.clear();
        },
    };
}
