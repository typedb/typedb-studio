import { ForceGraphEdge, ForceGraphVertex } from "../d3-force-simulation";
import * as PIXI from "pixi.js";
import { defaultStyles, TypeDBVisualiserTheme } from "../styles";
import { arrowhead, diamondIncomingLineIntersect, Ellipse, ellipseIncomingLineIntersect, midpoint, Point, Rect,
    rectIncomingLineIntersect } from "../geometry";

export declare namespace Renderer {
    export type Edge = ForceGraphEdge & { labelGFX?: PIXI.Text };
    export type VertexGFX = PIXI.Graphics & { vertex?: Vertex };
    export type Vertex = ForceGraphVertex & { gfx?: VertexGFX };
}

const edgeLabelMetrics: {[label: string]: PIXI.TextMetrics} = {};
const edgeLabelStyle: Partial<PIXI.ITextStyle> = {
    fontSize: defaultStyles.edgeLabel.fontSize,
    fontFamily: defaultStyles.fontFamily,
};

export function renderVertex(vertex: Renderer.Vertex, useFallbackFont: boolean, theme: TypeDBVisualiserTheme) {
    vertex.gfx = new PIXI.Graphics();
    vertex.gfx.lineStyle(0);
    const colors = theme.colors.numeric;
    vertex.gfx.beginFill(colors[vertex.encoding]);

    // TODO: Maybe, instead of repeating this work for each vertex, we should do it once and create a shared image to reuse
    switch (vertex.encoding) {
        case "entity":
        case "entityType":
        case "thingType":
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

    renderVertexLabel(vertex, useFallbackFont, theme);
}

export function renderVertexLabel(vertex: Renderer.Vertex, useFallbackFont: boolean, theme: TypeDBVisualiserTheme) {
    const colors = theme.colors.numeric;
    const text1 = new PIXI.Text(vertex.label, {
        fontSize: defaultStyles.vertexLabel.fontSize,
        fontFamily: useFallbackFont ? defaultStyles.fontFamilyFallback : defaultStyles.fontFamily,
        fill: colors.vertexLabel,
    });
    text1.anchor.set(0.5);
    text1.resolution = window.devicePixelRatio * 2;
    vertex.gfx.addChild(text1);
}

export function renderEdge(edge: Renderer.Edge, edgesGFX: PIXI.Graphics, theme: TypeDBVisualiserTheme) {
    const [source, target] = [edge.source as Renderer.Vertex, edge.target as Renderer.Vertex];
    const [lineSource, lineTarget] = [edgeEndpoint(target, source), edgeEndpoint(source, target)];
    const colors = theme.colors.numeric;
    const edgeColor = edge.highlight ? colors[edge.highlight] : colors.edge;

    if (lineSource && lineTarget) {
        const { label } = edge;
        edgesGFX.lineStyle(1, edgeColor);
        const centrePoint = midpoint({ from: lineSource, to: lineTarget });
        const labelRect: Rect = {
            x: centrePoint.x - edgeLabelMetrics[label].width / 2 - 2,
            y: centrePoint.y - edgeLabelMetrics[label].height / 2 - 2,
            w: edgeLabelMetrics[label].width + 4,
            h: edgeLabelMetrics[label].height + 4,
        };
        if (edge.labelGFX) edge.labelGFX.position.set(centrePoint.x, centrePoint.y);
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

export function renderEdgeLabel(edge: Renderer.Edge, useFallbackFont: boolean, theme: TypeDBVisualiserTheme) {
    if (!edgeLabelMetrics[edge.label]) {
        const linkLabel = new PIXI.Text(edge.label, edgeLabelStyle);
        edgeLabelMetrics[edge.label] = PIXI.TextMetrics.measureText(edge.label, linkLabel.style as any);
    }

    const fontFamily = useFallbackFont ? defaultStyles.fontFamilyFallback : defaultStyles.fontFamily;

    const text1 = new PIXI.Text(edge.label, {
        fontSize: defaultStyles.edgeLabel.fontSize,
        fontFamily,
        fill: edge.highlight ? theme.colors.numeric[edge.highlight] : theme.colors.numeric.edge,
    });
    text1.anchor.set(0.5);
    text1.resolution = window.devicePixelRatio * 2;
    edge.labelGFX = text1;
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
        case "thingType":
            const targetRect: Rect = {
                x: target.x - target.width / 2 - 4, y: target.y - target.height / 2 - 4,
                w: target.width + 8, h: target.height + 8
            };
            if (["entity", "entityType", "thingType"].includes(target.encoding)) {
                return rectIncomingLineIntersect(source as Point, targetRect);
            } else {
                return diamondIncomingLineIntersect(source as Point, targetRect);
            }
        case "attribute":
        case "attributeType":
            const targetEllipse: Ellipse = { x: target.x, y: target.y, hw: target.width / 2 + 2, hh: target.height / 2 + 2 };
            return ellipseIncomingLineIntersect(source as Point, targetEllipse);
    }
}
