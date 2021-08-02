import * as d3 from "d3-force";
import * as PIXI from "pixi.js";
// @ts-ignore
import FontFaceObserver from "fontfaceobserver";
import { dynamicForceSimulation, ForceGraphSimulation, ForceGraphVertex } from "../d3-force-simulation";
import { TypeDBVisualiserTheme } from "../styles";
import { TypeDBVisualiserData } from "../data";
import { Viewport } from "pixi-viewport";
import { computeEdgeLabelMetrics, renderEdge, renderEdgeLabel, Renderer, renderVertex } from "./graph-renderer-common";

export interface RenderingStage {
    renderer: PIXI.Renderer;
    viewport: Viewport;
}

export function setupStage(container: HTMLElement, onZoom: (scale: number) => any): RenderingStage {
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
        .wheel() // NOTE: This wheel speed feels too fast with a Mac mouse and too slow with a Windows one - best to leave it as is
        .clampZoom({ minScale: .01, maxScale: 3 })
        .decelerate({ friction: .95 });

    viewport.on("zoomed", () => onZoom(viewport.scaled));

    return { renderer, viewport };
}

export interface TypeDBGraphSimulation {
    simulation: ForceGraphSimulation;
    add: (newObjects: { vertices: TypeDBVisualiserData.Vertex[], edges: TypeDBVisualiserData.Edge[] }) => void;
    destroy: () => void;
    clear: () => void;
}

// TODO: The purpose of this file isn't clear.
// TODO: Too much of this code is shared with fixed-container.ts, a refactor is required
export function renderDynamicGraph(viewport: Viewport, graphData: TypeDBVisualiserData.Graph, theme: TypeDBVisualiserTheme,
                                   onVertexClick: (vertex: ForceGraphVertex) => any, onFirstTick: () => any): TypeDBGraphSimulation {
    console.log("renderToViewport called")
    for (const child of viewport.children) child.destroy(true);
    viewport.removeChildren();
    const [width, height] = [viewport.screenWidth, viewport.screenHeight];
    // console.log([width, height]);
    // viewport.moveCenter(width / 2, height / 2);
    // viewport.position.set(width / 2, height / 2);
    // console.log([viewport.x, viewport.y]);
    // const nodeRadius = 150; // TODO: this should equal the link force
    // const nodeDensity = width * height / (nodeRadius * nodeRadius * Math.PI);
    // TODO: this code autoscales the visualiser - but it's inconsistent to do that and not also auto-center it
    //       and the centering code is not working!
    // viewport.scale.set(Math.max(0.01, Math.min(1, Math.sqrt(nodeDensity / graphData.vertices.length))));
    const vertices: Renderer.Vertex[] = graphData.vertices.map((d) => Object.assign({}, d));
    let dragged = false;

    const simulation = dynamicForceSimulation(vertices, graphData.edges.map((d) => Object.assign({}, d)), width, height);
    simulation.id = graphData.simulationID;
    const ubuntuMono = new FontFaceObserver("Ubuntu Mono") as { load: () => Promise<any> };
    let ticked = false;

    function onDragStart(this: any, evt: any) {
        viewport.plugins.pause('drag');
        // TODO: we should do this on the first movement, not on drag start
        const vertexGFX = evt.currentTarget as Renderer.VertexGFX;
        if (onVertexClick) onVertexClick(vertexGFX.vertex);
        // TODO: these checks should be merged into one once we're sure it's what we want to do
        if (vertices.length + simulation.edges.length > 5000) {
            simulation.force("link", null);
            simulation.force("charge", null);
            simulation.force("center", null);
        }
        simulation.alphaTarget(0.1).restart();
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

    function addVertex(vertex: Renderer.Vertex, useFallbackFont: boolean) {
        const boundDragMove = onDragMove.bind(vertex);
        const boundDragEnd = onDragEnd.bind(vertex);
        renderVertex(vertex, useFallbackFont, theme);

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
        vertex.gfx.vertex = vertex;

        viewport.addChild(vertex.gfx);
    }

    function addEdgeLabel(edge: Renderer.Edge, useFallbackFont: boolean) {
        renderEdgeLabel(edge, useFallbackFont, theme);
        viewport.addChildAt(edge.labelGFX, 0); // Ensure edge labels are rendered under vertices
    }

    const edgesGFX = new PIXI.Graphics();
    renderGraphElements();

    async function renderGraphElements() {
        let useFallbackFont = false;
        try {
            await ubuntuMono.load();
        } catch (e: any) {
            useFallbackFont = true;
        }

        viewport.addChild(edgesGFX);
        if (vertices.length + simulation.edges.length <= 5000) {
            simulation.edges.forEach(computeEdgeLabelMetrics);
            simulation.edges.forEach(edge => addEdgeLabel(edge, useFallbackFont));
        }
        vertices.forEach(v => addVertex(v, useFallbackFont));

        // Listen for tick events to render the nodes as they update in your Canvas or SVG.
        simulation.on("tick", onTick);
    }

    const onTick = () => {
        if (!ticked) {
            if (onFirstTick) onFirstTick();
            ticked = true;
        }

        vertices.forEach((vertex) => {
            let { x, y, gfx } = vertex;
            gfx.position.set(x, y);
        });

        for (let i = edgesGFX.children.length - 1; i >= 0; i--) {
            edgesGFX.children[i].destroy();
        }

        edgesGFX.clear();
        edgesGFX.removeChildren();
        if (vertices.length + simulation.edges.length <= 5000) {
            simulation.edges.forEach((edge) => {
                renderEdge(edge, edgesGFX, theme);
            });
        }
    }

    return {
        simulation,
        add: (async (newObject) => {
            // console.log(newObject.vertices);
            const [newVertices, newEdges] = [newObject.vertices, newObject.edges];
            vertices.push(...newVertices);
            simulation.nodes(vertices);
            simulation.force("link", null);
            simulation.edges.push(...newEdges);
            simulation.force("link", d3.forceLink(simulation.edges).id((d: any) => d.id).distance(120));

            let useFallbackFont = false;
            try {
                await ubuntuMono.load();
            } catch (e: any) {
                useFallbackFont = true;
            }

            edgesGFX.clear();
            if (vertices.length + simulation.edges.length <= 5000) {
                newEdges.forEach(computeEdgeLabelMetrics);
                newEdges.forEach(edge => addEdgeLabel(edge, useFallbackFont));
            } else {
                simulation.edges.forEach(edge => {
                    const labelGFX = (edge as Renderer.Edge).labelGFX;
                    if (labelGFX) {
                        labelGFX.destroy();
                        viewport.removeChild(labelGFX);
                        delete (edge as Renderer.Edge).labelGFX;
                    }
                });
            }
            newVertices.forEach(v => addVertex(v, useFallbackFont));
            simulation.alpha(0.5).restart();
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
