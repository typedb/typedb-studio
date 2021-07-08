import * as PIXI from "pixi.js";
// @ts-ignore
import FontFaceObserver from "fontfaceobserver";
import {
    arrowhead,
    diamondIncomingLineIntersect, Ellipse,
    ellipseIncomingLineIntersect,
    midpoint,
    Rect,
    rectIncomingLineIntersect
} from "./geometry";
import { ForceGraphEdge, ForceGraphVertex, stickyForceSimulation } from "./d3-force-simulation";
import { defaultColors, defaultStyles } from "./styles";
import { TypeDBVisualiserData } from "./data";

const edgeLabelMetrics: {[label: string]: PIXI.TextMetrics} = {};
const edgeLabelStyle: Partial<PIXI.ITextStyle> = {
    fontSize: defaultStyles.edgeLabel.fontSize,
    fontFamily: defaultStyles.fontFamily,
};

export declare namespace Renderer {
    export type Edge = ForceGraphEdge & { gfx?: PIXI.Graphics };
    export type Vertex = ForceGraphVertex & { gfx?: PIXI.Graphics };
}

export function renderVertex(vertex: Renderer.Vertex, fontFace: { load: () => Promise<any> }) {
    vertex.gfx = new PIXI.Graphics();
    vertex.gfx.lineStyle(0);
    vertex.gfx.beginFill(defaultColors[vertex.encoding]);

    switch (vertex.encoding) {
        case "entity":
        case "entityType":
            vertex.gfx.drawRoundedRect(-vertex.width / 2, -vertex.height / 2, vertex.width, vertex.height, 3);
            vertex.gfx.hitArea = new PIXI.RoundedRectangle(-vertex.width / 2, -vertex.height / 2, vertex.width, vertex.height, 3);
            break;
        case "relation":
        case "relationType":
            const leftArc = { x: 8, y: 4, r: 6 };
            const topArc = { x: 6, y: 3, r: 6 };
            vertex.gfx.moveTo(-vertex.width / 2 + leftArc.x, -leftArc.y);
            vertex.gfx.lineTo(-topArc.x, -vertex.height / 2 + topArc.y);
            vertex.gfx.arcTo(0, -vertex.height / 2, topArc.x, -vertex.height / 2 + topArc.y, topArc.r);
            vertex.gfx.lineTo(vertex.width / 2 - leftArc.x, -leftArc.y);
            vertex.gfx.arcTo(vertex.width / 2, 0, vertex.width / 2 - leftArc.x, leftArc.y, leftArc.r);
            vertex.gfx.lineTo(topArc.x, vertex.height / 2 - topArc.y);
            vertex.gfx.arcTo(0, vertex.height / 2, -topArc.x, vertex.height / 2 - topArc.y, topArc.r);
            vertex.gfx.lineTo(-vertex.width / 2 + leftArc.x, leftArc.y);
            vertex.gfx.arcTo(-vertex.width / 2, 0, -vertex.width / 2 + leftArc.x, -leftArc.y, leftArc.r);
            vertex.gfx.hitArea = new PIXI.Polygon([
                new PIXI.Point(-vertex.width / 2, 0),
                new PIXI.Point(0, -vertex.height / 2),
                new PIXI.Point(vertex.width / 2, 0),
                new PIXI.Point(0, vertex.height / 2)
            ]);
            break;
        case "attribute":
        case "attributeType":
            vertex.gfx.drawEllipse(0, 0, vertex.width / 2, vertex.height / 2);
            vertex.gfx.hitArea = new PIXI.Ellipse(0, 0, vertex.width / 2, vertex.height / 2);
            break;
    }
    vertex.gfx.endFill();

    fontFace.load().then(() => {
        renderVertexLabel(vertex, false);
    }, () => {
        renderVertexLabel(vertex, true);
    });
}

export function renderVertexLabel(vertex: Renderer.Vertex, useFallbackFont: boolean) {
    const text1 = new PIXI.Text(vertex.label, {
        fontSize: defaultStyles.vertexLabel.fontSize,
        fontFamily: useFallbackFont ? defaultStyles.fontFamilyFallback : defaultStyles.fontFamily,
        fill: defaultColors.vertexLabel,
    });
    text1.anchor.set(0.5);
    text1.resolution = window.devicePixelRatio * 2;
    vertex.gfx.addChild(text1);
}

export function renderEdge(edge: Renderer.Edge, edgesGFX: PIXI.Graphics) {
    const [source, target] = [edge.source as Renderer.Vertex, edge.target as Renderer.Vertex];
    const [lineSource, lineTarget] = [edgeEndpoint(target, source), edgeEndpoint(source, target)];
    const edgeColor = edge.highlight ? defaultColors[edge.highlight] : defaultColors.edge;

    if (lineSource && lineTarget) {
        const { label } = edge;
        edgesGFX.lineStyle(1, edgeColor);
        // Draw edge label
        const centrePoint = midpoint({ from: lineSource, to: lineTarget });
        const edgeLabel = new PIXI.Text(label, edgeLabelStyle);
        edgeLabel.style.fill = edgeColor
        edgeLabel.resolution = window.devicePixelRatio * 2;
        edgeLabel.anchor.set(0.5);
        edgeLabel.position.set(centrePoint.x, centrePoint.y);
        edgesGFX.addChild(edgeLabel);

        // Draw line parts
        if (!edgeLabelMetrics[edge.label]) {
            const linkLabel = new PIXI.Text(edge.label, edgeLabelStyle);
            edgeLabelMetrics[edge.label] = PIXI.TextMetrics.measureText(edge.label, linkLabel.style as any);
        }
        const labelRect: Rect = {
            x: centrePoint.x - edgeLabelMetrics[label].width / 2 - 2,
            y: centrePoint.y - edgeLabelMetrics[label].height / 2 - 2,
            w: edgeLabelMetrics[label].width + 4,
            h: edgeLabelMetrics[label].height + 4,
        };
        edgesGFX.moveTo(lineSource.x, lineSource.y);
        const linePart1Target = rectIncomingLineIntersect(lineSource, labelRect);
        if (linePart1Target) edgesGFX.lineTo(linePart1Target.x, linePart1Target.y);
        const linePart2Source = rectIncomingLineIntersect(lineTarget, labelRect);
        if (linePart2Source) {
            edgesGFX.moveTo(linePart2Source.x, linePart2Source.y);
            edgesGFX.lineTo(lineTarget.x, lineTarget.y);
        }

        // Draw arrowhead
        const arrow = arrowhead({ from: lineSource, to: lineTarget });
        if (arrow) {
            edgesGFX.moveTo(arrow[0].x, arrow[0].y);
            edgesGFX.beginFill(edgeColor);
            const points: PIXI.Point[] = [];
            for (const pt of arrow) points.push(new PIXI.Point(pt.x, pt.y));
            edgesGFX.drawPolygon(points);
            edgesGFX.endFill();
        }
    }
}

/*
 * Find the endpoint of an edge drawn from `source` to `target`
 */
export function edgeEndpoint(source: Renderer.Vertex, target: Renderer.Vertex): false | {x: number, y: number} {
    switch (target.encoding) {
        case "entity":
        case "relation":
        case "entityType":
        case "relationType":
            const targetRect: Rect = {
                x: target.x - target.width / 2 - 4, y: target.y - target.height / 2 - 4,
                w: target.width + 8, h: target.height + 8
            };
            if (["entity", "entityType"].includes(target.encoding)) {
                return rectIncomingLineIntersect(source, targetRect);
            } else {
                return diamondIncomingLineIntersect(source, targetRect);
            }
        case "attribute":
        case "attributeType":
            const targetEllipse: Ellipse = { x: target.x, y: target.y, hw: target.width / 2 + 2, hh: target.height / 2 + 2 };
            return ellipseIncomingLineIntersect(source, targetEllipse);
    }
}

export function renderGraph(container: HTMLElement, graphData: TypeDBVisualiserData.Graph) {
    const [width, height] = [container.offsetWidth, container.offsetHeight];
    const edges: Renderer.Edge[] = graphData.edges.map((d) => Object.assign({}, d));
    const vertices: Renderer.Vertex[] = graphData.vertices.map((d) => Object.assign({}, d));
    let dragged = false;

    const app = new PIXI.Application({ width, height, antialias: !0,
        backgroundColor: defaultColors.background, backgroundAlpha: 0, resolution: window.devicePixelRatio });
    container.innerHTML = "";
    container.appendChild(app.view);

    // TODO: We should reintroduce pixi-viewport when we stop using pixi.js-legacy
    // const viewport = new Viewport({
    //     screenWidth: width,
    //     screenHeight: height,
    //     worldWidth: width,
    //     worldHeight: height,
    //     passiveWheel: false,
    //
    //     interaction: app.renderer.plugins.interaction // the interaction module is important for wheel to work properly when renderer.view is placed or scaled
    // });

    // app.stage.addChild(viewport);

    // activate plugins
    // viewport.drag({ factor: .33 })
    //     .pinch({ factor: .5 })
    //     .clampZoom({ minScale: .4, maxScale: 2.5 })
    //     .wheel()
    //     .decelerate();

    const simulation = stickyForceSimulation(vertices, edges, width, height);
    const ubuntuMono = new FontFaceObserver("Ubuntu Mono") as { load: () => Promise<any> };

    function onDragStart(this: any, evt: any) {
        // viewport.plugins.pause('drag');
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
        // viewport.plugins.resume('drag');
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
        renderVertex(vertex, ubuntuMono);

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
            renderEdge(edge, edgesGFX);
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
