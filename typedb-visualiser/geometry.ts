export interface Point {
    x: number;
    y: number;
}

export interface Line {
    from: Point;
    to: Point;
}

export interface Rect {
    x: number;
    y: number;
    w: number;
    h: number;
}

export interface Ellipse {
    x: number;
    y: number;
    /** half-width */
    hw: number;
    /** half-height */
    hh: number;
}

export type Polygon = Point[];

export function midpoint(line: Line): Point {
    return {x: (line.from.x + line.to.x) / 2, y: (line.from.y + line.to.y) / 2};
}

/*
 * line intercept math by Paul Bourke http://paulbourke.net/geometry/pointlineplane/
 * Determine the intersection point of two line segments
 * Return FALSE if the lines don't intersect
 */
export function lineIntersect(line1: Line, line2: Line) {
    const {x: x1, y: y1} = line1.from;
    const {x: x2, y: y2} = line1.to;
    const {x: x3, y: y3} = line2.from;
    const {x: x4, y: y4} = line2.to;

    // Check if any line has length 0
    if ((x1 === x2 && y1 === y2) || (x3 === x4 && y3 === y4)) return false;

    const denominator = ((y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1));

    // Check if lines are parallel
    if (denominator === 0) return false;

    let ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denominator;
    let ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denominator;

    // Check if the intersection of infinite-length lines is within these segments
    if (ua < 0 || ua > 1 || ub < 0 || ub > 1) return false;

    // Return a object with the x and y coordinates of the intersection
    let x = x1 + ua * (x2 - x1);
    let y = y1 + ua * (y2 - y1);

    return {x, y};
}

/*
 * Find intersection point of a line from `sourcePoint` to the centre of `targetRect`, with the edge of `targetRect`
 */
export function rectIncomingLineIntersect(sourcePoint: Point, targetRect: Rect) {
    const {x: px, y: py} = sourcePoint;
    const {x: rx, y: ry, w: rw, h: rh} = targetRect;

    const edgesToCheck: Line[] = [];
    const centre = {x: rx + rw/2, y: ry + rh/2};
    const topLeft = {x: rx, y: ry};
    const topRight = {x: rx+rw, y: ry};
    const bottomLeft = {x: rx, y: ry+rh};
    const bottomRight = {x: rx+rw, y: ry+rh};
    const incomingLine = {from: {x: px, y: py}, to: centre};

    if (px <= centre.x) edgesToCheck.push({from: topLeft, to: bottomLeft});
    else edgesToCheck.push({from: topRight, to: bottomRight});

    if (py <= centre.y) edgesToCheck.push({from: topLeft, to: topRight});
    else edgesToCheck.push({from: bottomLeft, to: bottomRight});

    for (const edge of edgesToCheck) {
        const intersection = lineIntersect(incomingLine, edge);
        if (intersection) return intersection;
    }

    return false;
}

/*
 * Find intersection point of a line from `sourcePoint` to the centre of `targetDiamond`, with the edge of `targetDiamond`
 */
export function diamondIncomingLineIntersect(sourcePoint: Point, targetDiamond: Rect) {
    const {x: px, y: py} = sourcePoint;
    const {x: dx, y: dy, w: dw, h: dh} = targetDiamond;

    let edgeToCheck: Line;
    const centre = {x: dx + dw/2, y: dy + dh/2};
    const centreLeft = {x: dx, y: dy + dh/2};
    const topCentre = {x: dx+dw/2, y: dy};
    const centreRight = {x: dx+dw, y: dy+dh/2};
    const bottomCentre = {x: dx+dw/2, y: dy+dh};
    const incomingLine = {from: {x: px, y: py}, to: centre};

    if (px <= centre.x && py <= centre.y) edgeToCheck = {from: centreLeft, to: topCentre};
    else if (px > centre.x && py <= centre.y) edgeToCheck = {from: topCentre, to: centreRight};
    else if (px > centre.x && py > centre.y) edgeToCheck = {from: centreRight, to: bottomCentre};
    else edgeToCheck = {from: bottomCentre, to: centreLeft};

    return lineIntersect(incomingLine, edgeToCheck);
}

/*
 * Find intersection point of a line from `sourcePoint` to the centre of `targetEllipse`, with the edge of `targetEllipse`
 */
export function ellipseIncomingLineIntersect(sourcePoint: Point, targetEllipse: Ellipse): Point {
    let {x: px, y: py} = sourcePoint;
    const {x, y, hw: a, hh: b} = targetEllipse; // ellipse has centre (x,y) and semiaxes of lengths [a,b]

    // translate structure to centre ellipse at origin
    px -= x;
    py -= y;

    // compute intersection points: +-(x0, y0)
    let x0 = (a * b * px) / Math.sqrt(a*a * py*py + b*b * px*px);
    let y0 = (a * b * py) / Math.sqrt(a*a * py*py + b*b * px*px);

    return {x: x0+x, y: y0+y};
}

export function arrowhead(line: Line): Polygon | null {
    // first compute normalised vector for the line
    let [dx, dy] = [line.to.x - line.from.x, line.to.y - line.from.y];
    let l = Math.sqrt(dx * dx + dy * dy);

    if (l === 0) return null; // if length is 0 - can't render arrows

    let [nx, ny] = [dx/l, dy/l]; // normal vector in the direction of the line with length 1
    let [arrowLength, arrowWidth] = [6, 3];
    let [ex, ey] = [line.from.x + nx * l, line.from.y + ny * l]; // arrow endpoint
    let [sx, sy] = [line.from.x + nx * (l - arrowLength), line.from.y + ny * (l - arrowLength)]; // wingtip offsets from line
    let [topX, topY] = [-ny, nx]; // orthogonal vector to the line vector

    return [{x: ex, y: ey},
        {x: sx + topX * arrowWidth, y: sy + topY * arrowWidth},
        {x: sx - topX * arrowWidth, y: sy - topY * arrowWidth}];
}
