/*
 * Copyright (C) 2022 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.vaticle.typedb.studio.framework.common.geometry

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.vaticle.typedb.studio.framework.common.geometry.Geometry.AngularDirection.Clockwise
import com.vaticle.typedb.studio.framework.common.geometry.Geometry.AngularDirection.CounterClockwise
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// TODO: why is this class here and not in //framework/graph? Do we plan on using it for other components?
object Geometry {

    data class Ray(val origin: Offset, val directionVector: Offset) {
        @Stable
        val isVertical: Boolean
            get() = directionVector.x == 0f

        @Stable
        val gradient: Float
            get() = if (!isVertical) directionVector.y / directionVector.x else Float.POSITIVE_INFINITY

        @Stable
        val yIntercept: Float
            // Let origin be (x0,y0) and directionVector be (x',y'), then the ray equation is (x,y) = (x0+vx',y0+vy')
            // At the y-intercept, x0+vx'=0 so v=-x0/x'. Substituting this in gives y = y0 - x0*y'/x'
            get() = if (!isVertical) origin.y - (origin.x * directionVector.y) / directionVector.x else Float.NaN
    }

    data class Line(val from: Offset, val to: Offset) {
        @Stable
        fun toRay(): Ray = Ray(origin = from, directionVector = to - from)
    }

    data class Circle(val x: Float, val y: Float, val r: Float)

    data class Ellipse(
        val x: Float, val y: Float,
        /** half-width */
        val hw: Float,
        /** half-height */
        val hh: Float
    )

    /** Convert radians to degrees */
    fun Float.radToDeg(): Float {
        return (this * 180 / PI).toFloat()
    }

    /** Convert degrees to radians */
    fun Float.degToRad(): Float {
        return (this * PI / 180).toFloat()
    }

    data class Arc(val topLeft: Offset, val size: Size, val startAngle: Float, val sweepAngle: Float) {
        @Stable
        val center: Offset
            get() = topLeft + Offset(x = size.width / 2, y = size.height / 2)

        @Stable
        val direction: AngularDirection
            get() = if (sweepAngle > 0) Clockwise else CounterClockwise

        @Stable
        val endAngle: Float
            get() = (startAngle + sweepAngle).normalisedAngle()

        @Stable
        fun offsetAtAngle(angle: Float): Offset {
            return center + Offset(cos(angle.degToRad()) * size.width / 2, sin(angle.degToRad()) * size.height / 2)
        }

        @Stable
        fun toCircle(): Circle {
            if (size.height == 0f || size.width / size.height !in 0.999f..1.001f) {
                throw IllegalStateException("toCircle: width and height are different, so this arc is not circular")
            }
            return Circle(x = center.x, y = center.y, r = size.height / 2f)
        }
    }

    enum class AngularDirection {
        Clockwise,
        CounterClockwise
    }

    fun midpoint(from: Offset, to: Offset): Offset {
        return Offset((from.x + to.x) / 2, (from.y + to.y) / 2)
    }

    fun rayIntersect(ray1: Ray, ray2: Ray): Offset? {
        return lineIntersect(
            line1 = Line(ray1.origin, ray1.origin + ray1.directionVector),
            line2 = Line(ray2.origin, ray2.origin + ray2.directionVector), infiniteLength = true
        )
    }

    /**
     * line intercept math by Paul Bourke http://paulbourke.net/geometry/pointlineplane/
     *
     * Determine the intersection point of two line segments
     *
     * Return null if the lines don't intersect
     */
    fun lineIntersect(line1: Line, line2: Line, infiniteLength: Boolean = false): Offset? {
        val x1 = line1.from.x;
        val y1 = line1.from.y
        val x2 = line1.to.x;
        val y2 = line1.to.y
        val x3 = line2.from.x;
        val y3 = line2.from.y
        val x4 = line2.to.x;
        val y4 = line2.to.y

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
     * Return all roots of an equation of the form ax^2 + bx + c = 0, where a is nonzero
     */
    fun quadraticRoots(a: Float, b: Float, c: Float): Set<Float> {
        if (a == 0f) throw IllegalArgumentException("quadraticRoots: a must be nonzero")
        val discriminant = b * b - 4f * a * c
        return when {
            discriminant > 0 -> {
                val sqrtDiscriminant = sqrt(discriminant)
                setOf((-b + sqrtDiscriminant) / (2f * a), (-b - sqrtDiscriminant) / (2f * a))
            }
            discriminant < 0 -> emptySet()
            else -> setOf(-b / (2f * a))
        }
    }

    /**
     * Find intersection points of a ray and a circle
     */
    fun rayCircleIntersect(ray: Ray, circle: Circle): Set<Offset> {
        val (a, b, r) = listOf(circle.x, circle.y, circle.r)
        return when (ray.isVertical) {
            false -> {
                // The equations of the ray and circle are (x - a)^2 + (y - b)^2 = r^2 and y = mx + c
                // Substituting y = mx + c into (x - a)^2 + (y - b)^2 = r^2 gives (x-a)^2 + (mx+c-b)^2 = r^2
                // Rearranging gives the quadratic: (m^2 + 1)(x^2) + (2(mc-mb-a))x + (a^2 + b^2 + c^2 - r^2 - 2bc) = 0
                val m = ray.gradient
                val c = ray.yIntercept
                val xValues = quadraticRoots(
                    a = m * m + 1,
                    b = 2 * (m * c - m * b - a),
                    c = (a * a + b * b + c * c - r * r - 2 * b * c)
                )
                xValues.map { x -> Offset(x, m * x + c) }.toSet()
            }
            true -> {
                // For a vertical ray, x is just a constant, so we need only rearrange (x - a)^2 + (y - b)^2 = r^2
                // into a quadratic function of y
                // This yields y^2 - 2by + (b^2 + (x-a)^2 - r^2) = 0
                val x = ray.origin.x
                val yValues = quadraticRoots(a = 1f, b = -2 * b, c = b * b + (x - a) * (x - a) - r * r)
                yValues.map { y -> Offset(x, y) }.toSet()
            }
        }
    }

    fun min(a: Float, b: Float): Float = a.coerceAtMost(b)

    fun max(a: Float, b: Float): Float = a.coerceAtLeast(b)

    /** Given an angle, return its normalised value in the range [0..360) */
    fun Float.normalisedAngle(): Float {
        return (this + 7200) % 360
    }

    /**
     * Check if this angle lies within an arc defined by the given start and sweep angles
     */
    fun Float.isInArcSweep(startAngle: Float, sweepAngle: Float): Boolean {
        val normalisedAngle = this.normalisedAngle()
        val normalisedStartAngle = startAngle.normalisedAngle()
        val normalisedEndAngle = (startAngle + sweepAngle).normalisedAngle()
        return when (sweepAngle > 0) {
            true -> {
                when (normalisedStartAngle < normalisedEndAngle) {
                    true -> normalisedAngle in normalisedStartAngle..normalisedEndAngle
                    false -> normalisedAngle > normalisedStartAngle || normalisedAngle < normalisedEndAngle
                }
            }
            false -> {
                when (normalisedStartAngle > normalisedEndAngle) {
                    true -> normalisedAngle in normalisedEndAngle..normalisedStartAngle
                    false -> normalisedAngle > normalisedEndAngle || normalisedAngle < normalisedStartAngle
                }
            }
        }
    }

    /**
     * Compute the sweep angle of an arc between two polar-coordinate angles in a specified angular direction
     */
    fun sweepAngle(from: Float, to: Float, direction: AngularDirection): Float {
        val fromAngle = from.normalisedAngle()
        val toAngle = to.normalisedAngle()
        return when (direction) {
            Clockwise -> if (toAngle > fromAngle) toAngle - fromAngle else 360 + toAngle - fromAngle
            CounterClockwise -> if (toAngle < fromAngle) toAngle - fromAngle else -360 + toAngle - fromAngle
        }
    }

    /**
     * Find intersection angles of an arc with a line
     */
    fun lineArcIntersectAngles(line: Line, arc: Arc): List<Float> {
        val ray = line.toRay()
        val circle = arc.toCircle()
        val rayCircleIntersections = rayCircleIntersect(ray, circle)

        val lineRect = Rect(
            left = min(line.from.x, line.to.x) - 1f, top = min(line.from.y, line.to.y) - 1f,
            right = max(line.from.x, line.to.x) + 1f, bottom = max(line.from.y, line.to.y) + 1f
        )
        return rayCircleIntersections
            .filter { lineRect.contains(it) }
            .map { atan2(y = it.y - circle.y, x = it.x - circle.x).radToDeg().normalisedAngle() }
            .filter { it.isInArcSweep(arc.startAngle, arc.sweepAngle) }
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

    fun Rect.lines(): List<Line> = listOf(
        Line(bottomLeft, topLeft), Line(topLeft, topRight),
        Line(topRight, bottomRight), Line(bottomRight, bottomLeft)
    )

    /**
     * Find intersection angles of an arc with the edges of `rect`
     */
    fun rectArcIntersectAngles(arc: Arc, rect: Rect): List<Float> {
        return rect.lines().flatMap { lineArcIntersectAngles(line = it, arc = arc) }
    }

    /**
     * Find intersection point of a line from `sourcePoint` to the centre of `diamond`, with the edge of `diamond`
     */
    fun diamondIncomingLineIntersect(sourcePoint: Offset, diamond: Rect): Offset? {
        val px = sourcePoint.x;
        val py = sourcePoint.y
        val incomingLine = Line(from = Offset(px, py), to = diamond.center)

        val edgeToCheck: Line =
            if (px <= diamond.center.x && py <= diamond.center.y) Line(diamond.centerLeft, diamond.topCenter)
            else if (px > diamond.center.x && py <= diamond.center.y) Line(diamond.topCenter, diamond.centerRight)
            else if (px > diamond.center.x && py > diamond.center.y) Line(diamond.centerRight, diamond.bottomCenter)
            else Line(diamond.bottomCenter, diamond.centerLeft)

        return lineIntersect(incomingLine, edgeToCheck)
    }

    fun Rect.diamondLines(): List<Line> = listOf(
        Line(centerLeft, topCenter), Line(topCenter, centerRight),
        Line(centerRight, bottomCenter), Line(bottomCenter, centerLeft)
    )

    /**
     * Find intersection angles of an arc with the edges of `diamond`
     */
    fun diamondArcIntersectAngles(arc: Arc, diamond: Rect): List<Float> {
        return diamond.diamondLines().flatMap { lineArcIntersectAngles(line = it, arc = arc) }
    }

    /**
     * Find intersection point of a line from `sourcePoint` through the centre of `ellipse`, with the edge of `ellipse`
     */
    fun ellipseIncomingLineIntersect(sourcePoint: Offset, ellipse: Ellipse): Offset? {
        var px = sourcePoint.x;
        var py = sourcePoint.y
        val x = ellipse.x;
        val y = ellipse.y
        val a = ellipse.hw;
        val b = ellipse.hh // ellipse has centre (x,y) and semiaxes of lengths [a,b]

        // translate structure to centre ellipse at origin
        px -= x
        py -= y

        val denominator = sqrt(a*a * py*py + b*b * px*px)
        if (denominator == 0F) return null

        // compute intersection points: +-(x0, y0)
        val x0 = (a * b * px) / denominator
        val y0 = (a * b * py) / denominator

        return Offset(x0 + x, y0 + y)
    }

    /**
     * Determine if 3 points are collinear or effectively collinear
     */
    fun collinear(point1: Offset, point2: Offset, point3: Offset): Boolean {
        val (x1, x2, x3) = listOf(point1.x, point2.x, point3.x)
        val (y1, y2, y3) = listOf(point1.y, point2.y, point3.y)
        val tolerance = abs(maxOf(x1 - x2, x1 - x3, x2 - x3, y1 - y2, y1 - y3, y2 - y3)) * 0.00001
        return abs((x2 - x1) * (y3 - y1) - (x3 - x1) * (y2 - y1)) < tolerance
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
        val centrePoint = rayIntersect(ray1, ray2) ?: throw IllegalStateException(
            "arcThroughPoints: rayIntersect unexpectedly returned null! The arguments were $ray1, $ray2"
        )
        val radius = (centrePoint - point1).getDistance()
        val topLeft = centrePoint - Offset(x = radius, y = radius)
        val size = Size(radius * 2, radius * 2)

        // Now determine if we want a clockwise or counter-clockwise arc from point 1
        // (t1, t2, t3) are the angles in polar coordinates of tangents at (point1, point2, point3)
        val point1FromCentre = point1 - centrePoint
        val t1 = atan2(point1FromCentre.y, point1FromCentre.x).radToDeg()
        val point2FromCentre = point2 - centrePoint
        val t2 = atan2(point2FromCentre.y, point2FromCentre.x).radToDeg()
        val point3FromCentre = point3 - centrePoint
        val t3 = atan2(point3FromCentre.y, point3FromCentre.x).radToDeg()

        val direction = if ((t2 in t1..t3) || (t3 in t2..t1) || (t1 in t3..t2)) Clockwise else CounterClockwise
        return Arc(topLeft = topLeft, size = size, startAngle = t1, sweepAngle = sweepAngle(t1, t3, direction))
    }

    fun arrowhead(from: Offset, to: Offset, arrowLength: Float, arrowWidth: Float): Pair<Line, Line>? {
        // first compute normalised vector for the line
        val d = to - from
        val len = sqrt(d.x * d.x + d.y * d.y)

        if (len == 0F) return null; // if length is 0 - can't render arrows

        val n = d / len // normal vector in the direction of the line with length 1
        val s =
            Offset(from.x + n.x * (len - arrowLength), from.y + n.y * (len - arrowLength)) // wingtip offsets from line
        val top = Offset(-n.y, n.x) // orthogonal vector to the line vector

        return Pair(
            Line(from = Offset(s.x + top.x * arrowWidth, s.y + top.y * arrowWidth), to = to),
            Line(from = Offset(s.x - top.x * arrowWidth, s.y - top.y * arrowWidth), to = to)
        )
    }
}
