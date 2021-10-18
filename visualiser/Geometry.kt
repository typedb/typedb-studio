package com.vaticle.typedb.studio.visualiser

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import java.lang.IllegalStateException
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

data class Ray(val origin: Offset, val directionVector: Offset)

data class Line(val from: Offset, val to: Offset)

data class Ellipse(val x: Float, val y: Float, /** half-width */ val hw: Float, /** half-height */ val hh: Float)

data class Arc(val topLeft: Offset, val size: Size, val startAngle: Float, val sweepAngle: Float)

fun midpoint(from: Offset, to: Offset): Offset {
    return Offset((from.x + to.x) / 2, (from.y + to.y) / 2)
}

fun rayIntersect(ray1: Ray, ray2: Ray): Offset? {
    return lineIntersect(line1 = Line(ray1.origin, ray1.origin + ray1.directionVector),
        line2 = Line(ray2.origin, ray2.origin + ray2.directionVector), infiniteLength = true)
}

/**
 * line intercept math by Paul Bourke http://paulbourke.net/geometry/pointlineplane/
 *
 * Determine the intersection point of two line segments
 *
 * Return null if the lines don't intersect
 */
fun lineIntersect(line1: Line, line2: Line, infiniteLength: Boolean = false): Offset? {
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
    if (!infiniteLength && (ua < 0 || ua > 1 || ub < 0 || ub > 1)) return null

    // Return an object with the x and y coordinates of the intersection
    val x: Float = x1 + ua * (x2 - x1)
    val y: Float = y1 + ua * (y2 - y1)
    return Offset(x, y)
}

/**
 * Find intersection point of a line and an ellipse
 */
fun lineArcIntersect(line: Line, arc: Arc): Collection<Offset> {
    var px = line.to.x - line.from.x; var py = line.to.y - line.from.y
    val x = arc.topLeft.x + arc.size.width / 2; val y = arc.topLeft.y + arc.size.height / 2
    val a = arc.size.width / 2; val b = arc.size.height // ellipse has centre (x,y) and semiaxes of lengths [a,b]

    // translate structure to centre ellipse at origin
    px -= x
    py -= y

    // compute intersection points: +-(x0, y0)
    val x0 = (a * b * px) / sqrt(a*a * py*py + b*b * px*px)
    val y0 = (a * b * py) / sqrt(a*a * py*py + b*b * px*px)

    val lineEllipseIntersects = listOf(Offset(x0+x, y0+y))
    return lineEllipseIntersects.filter {
        // TODO
        return@filter true
    }
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

/**
 * Determine if 3 points are collinear or effectively collinear
 */
fun collinear(point1: Offset, point2: Offset, point3: Offset): Boolean {
    val (x1, x2, x3) = listOf(point1.x, point2.x, point3.x)
    val (y1, y2, y3) = listOf(point1.y, point2.y, point3.y)
    val tolerance = abs(maxOf(x1-x2, x1-x3, x2-x3, y1-y2, y1-y3, y2-y3)) * 0.00001
    return abs((x2-x1) * (y3-y1) - (x3-x1) * (y2-y1)) < tolerance
}

/**
 * Given a vector, compute its (undirected) normal vector
 */
fun normalVector(vector: Offset): Offset {
    return Offset(vector.y, -vector.x) // n.b: equivalently (-vector.y, vector.x) would be a normal vector
}

/**
 * Find the position, size, start + sweep angles of an arc originating from point 1, passing through points 2 and 3
 * Return 'null' if the points are collinear or effectively collinear
 * Method to compute the circle from https://stackoverflow.com/a/4420986/2902555
 */
fun arcThroughPoints(point1: Offset, point2: Offset, point3: Offset): Arc? {
    if (collinear(point1, point2, point3)) return null

    // Draw lines from point1-point2 and point2-point3, bisect them and obtain their normals
    val lineMidpoint1 = midpoint(point1, point2)
    val lineMidpoint2 = midpoint(point2, point3)
    val normalVector1 = normalVector(point2 - point1)
    val normalVector2 = normalVector(point3 - point2)

    // The arc through all 3 points forms part of a circle centred at the intersection of these normals
    val ray1 = Ray(lineMidpoint1, normalVector1)
    val ray2 = Ray(lineMidpoint2, normalVector2)
    val centrePoint = rayIntersect(ray1, ray2)
        ?: throw IllegalStateException("arcThroughPoints: rayIntersect unexpectedly returned null! The arguments were $ray1, $ray2")
    val radius = (centrePoint - point1).getDistance()
    val topLeft = centrePoint - Offset(x = radius, y = radius)
    val size = Size(radius * 2, radius * 2)

    // (t1, t2, t3) are the angles in polar coordinates of tangents at (point1, point2, point3)
    val point1FromCentre = point1 - centrePoint
    val t1 = (atan2(point1FromCentre.y, point1FromCentre.x) * 180 / PI).toFloat()
    val point2FromCentre = point2 - centrePoint
    val t2 = (atan2(point2FromCentre.y, point2FromCentre.x) * 180 / PI).toFloat()
    val point3FromCentre = point3 - centrePoint
    val t3 = (atan2(point3FromCentre.y, point3FromCentre.x) * 180 / PI).toFloat()

    // Now determine if we want a clockwise or counter-clockwise arc from point 1
    return when ((t2 in t1..t3) || (t3 in t2..t1) || (t1 in t3..t2)) {
        // We need sweepAngle > 0 if and only if the arc is going clockwise
        true -> Arc(topLeft = topLeft, size = size, startAngle = t1, sweepAngle = if (t3 >= t1) t3 - t1 else t3 - t1 + 360)
        false -> Arc(topLeft = topLeft, size = size, startAngle = t1, sweepAngle = if (t1 >= t3) t3 - t1 else t3 - t1 - 360)
    }
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
