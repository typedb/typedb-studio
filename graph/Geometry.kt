package com.vaticle.graph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import kotlin.math.sqrt

data class Line(val from: Offset, val to: Offset)

data class Ellipse(val x: Float, val y: Float, /** half-width */ val hw: Float, /** half-height */ val hh: Float)

fun midpoint(from: Offset, to: Offset): Offset {
    return Offset((from.x + to.x) / 2, (from.y + to.y) / 2)
}

/**
 * line intercept math by Paul Bourke http://paulbourke.net/geometry/pointlineplane/
 *
 * Determine the intersection point of two line segments
 *
 * Return null if the lines don't intersect
 */
fun lineIntersect(line1: Line, line2: Line): Offset? {
    val x1 = line1.from.x; val y1 = line1.from.y
    val x2 = line1.to.x; val y2 = line1.to.y
    val x3 = line2.from.x; val y3 = line2.from.y
    val x4 = line2.to.x; val y4 = line2.to.y

    // Check if any line has length 0
    if ((x1 == x2 && y1 == y2) || (x3 == x4 && y3 == y4)) return null

    val denominator: Float = ((y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1))

    // Check if lines are parallel
    if (denominator == 0F) return null

    val ua: Float = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denominator
    val ub: Float = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denominator

    // Check if the intersection of infinite-length lines is within these segments
    if (ua < 0 || ua > 1 || ub < 0 || ub > 1) return null

    // Return an object with the x and y coordinates of the intersection
    val x: Float = x1 + ua * (x2 - x1)
    val y: Float = y1 + ua * (y2 - y1)
    return Offset(x, y)
}

/**
 * Find intersection point of a line from `sourcePoint` to the centre of `rect`, with the edge of `rect`
 */
fun rectIncomingLineIntersect(sourcePoint: Offset, rect: Rect): Offset? {
    val incomingLine = Line(from = Offset(sourcePoint.x, sourcePoint.y), to = rect.center)
    val edgesToCheck: MutableList<Line> = mutableListOf()

    edgesToCheck +=
        if (sourcePoint.x <= rect.center.x) Line(from = rect.topLeft, to = rect.bottomLeft)
        else Line(from = rect.topRight, to = rect.bottomRight)

    edgesToCheck +=
        if (sourcePoint.y <= rect.center.y) Line(from = rect.topLeft, to = rect.topRight)
        else Line(from = rect.bottomLeft, to = rect.bottomRight)

    for (edge in edgesToCheck) {
        val intersection: Offset? = lineIntersect(line1 = incomingLine, line2 = edge)
        if (intersection != null) return intersection
    }

    return null
}

/**
 * Find intersection point of a line from `sourcePoint` to the centre of `diamond`, with the edge of `diamond`
 */
fun diamondIncomingLineIntersect(sourcePoint: Offset, diamond: Rect): Offset? {
    val px = sourcePoint.x; val py = sourcePoint.y
    val incomingLine = Line(from = Offset(px, py), to = diamond.center)

    val edgeToCheck: Line =
        if (px <= diamond.center.x && py <= diamond.center.y) Line(from = diamond.centerLeft, to = diamond.topCenter)
        else if (px > diamond.center.x && py <= diamond.center.y) Line(from = diamond.topCenter, to = diamond.centerRight)
        else if (px > diamond.center.x && py > diamond.center.y) Line(from = diamond.centerRight, to = diamond.bottomCenter)
        else Line(from = diamond.bottomCenter, to = diamond.centerLeft)

    return lineIntersect(incomingLine, edgeToCheck)
}

/**
 * Find intersection point of a line from `sourcePoint` through the centre of `ellipse`, with the edge of `ellipse`
 */
fun ellipseIncomingLineIntersect(sourcePoint: Offset, ellipse: Ellipse): Offset {
    var px = sourcePoint.x; var py = sourcePoint.y
    val x = ellipse.x; val y = ellipse.y
    val a = ellipse.hw; val b = ellipse.hh // ellipse has centre (x,y) and semiaxes of lengths [a,b]

    // translate structure to centre ellipse at origin
    px -= x
    py -= y

    // compute intersection points: +-(x0, y0)
    val x0 = (a * b * px) / sqrt(a*a * py*py + b*b * px*px)
    val y0 = (a * b * py) / sqrt(a*a * py*py + b*b * px*px)

    return Offset(x0+x, y0+y)
}

fun arrowhead(from: Offset, to: Offset, arrowLength: Float, arrowWidth: Float): Path? {
    // first compute normalised vector for the line
    val d = to - from
    val len = sqrt(d.x*d.x + d.y*d.y)

    if (len == 0F) return null; // if length is 0 - can't render arrows

    val n = d / len // normal vector in the direction of the line with length 1
    val s = Offset(from.x + n.x * (len - arrowLength), from.y + n.y * (len - arrowLength)) // wingtip offsets from line
    val top = Offset(-n.y, n.x) // orthogonal vector to the line vector

    return Path().apply {
        moveTo(to.x, to.y)
        lineTo(s.x + top.x * arrowWidth, s.y + top.y * arrowWidth)
        lineTo(s.x - top.x * arrowWidth, s.y - top.y * arrowWidth)
        close()
    }
}
