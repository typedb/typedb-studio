import * as PIXI from "pixi.js";
// @ts-ignore
import FontFaceObserver from "fontfaceobserver";
import { dynamicForceSimulation } from "../d3-force-simulation";
import { TypeDBVisualiserTheme } from "../styles";
import { TypeDBVisualiserData } from "../data";
import { Viewport } from "pixi-viewport";
import { renderEdge, Renderer, renderVertex } from "./renderer-common";

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
        .pinch({ factor: .5 })
        .wheel({ percent: -.66 })
        .clampZoom({ minScale: .1, maxScale: 3 })
        .decelerate({ friction: .95 });

    return { renderer, viewport };
}

export function renderGraph(viewport: Viewport, graphData: TypeDBVisualiserData.Graph, theme: TypeDBVisualiserTheme) {
    viewport.removeChildren();
    const [width, height] = [viewport.screenWidth, viewport.screenHeight];
    const edges: Renderer.Edge[] = graphData.edges.map((d) => Object.assign({}, d));
    const vertices: Renderer.Vertex[] = graphData.vertices.map((d) => Object.assign({}, d));
    let dragged = false;

    const simulation = dynamicForceSimulation(vertices, edges, width, height);
    const ubuntuMono = new FontFaceObserver("Ubuntu Mono") as { load: () => Promise<any> };

    function onDragStart(this: any, evt: any) {
        viewport.plugins.pause('drag');
        simulation.alphaTarget(0.3).restart();
        this.isDown = true;
        this.eventData = evt.data;
        this.alpha = 0.75;
        this.dragging = true;
    }

    function onDragEnd(this: any, gfx: any) {
        gfx.alpha = 1;
        gfx.dragging = false;
        gfx.isOver = false;
        gfx.eventData = null;
        delete this.fx;
        delete this.fy;
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

        viewport.addChild(vertex.gfx);
    });

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
