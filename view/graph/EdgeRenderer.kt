/*
 * Copyright (C) 2021 Vaticle
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

package com.vaticle.typedb.studio.view.graph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.Stroke
import com.vaticle.typedb.studio.view.common.geometry.Geometry
import com.vaticle.typedb.studio.view.common.geometry.Geometry.normalisedAngle
import com.vaticle.typedb.studio.view.common.geometry.Geometry.radToDeg
import com.vaticle.typedb.studio.view.common.theme.Color
import kotlin.math.abs
import kotlin.math.atan2

class EdgeRenderer(private val graphArea: GraphArea, private val ctx: RendererContext) {

    companion object {
        private const val BACKGROUND_ALPHA = .25f
        private const val ARROWHEAD_LENGTH = 6f
        private const val ARROWHEAD_WIDTH = 3f
    }

    private val viewport = graphArea.viewport
    val density = graphArea.viewport.density
    private val edgeLabelSizes = graphArea.edgeLabelSizes

    fun draw(edges: Iterable<Edge>, detailed: Boolean) {
        for ((colorCode, edgeGroup) in EdgesByColorCode(edges, graphArea)) {
            draw(edgeGroup, detailed, colorCode.toColor(ctx.theme))
        }
    }

    private fun draw(edges: Iterable<Edge>, detailed: Boolean, color: androidx.compose.ui.graphics.Color) {
        if (detailed) {
            val (curvedEdges, straightEdges) = edges.partition { it.geometry.isCurved }
            drawLines(straightEdges, true, color)
            curvedEdges.forEach { drawCurvedEdge(it, color) }
        } else {
            drawLines(edges, false, color)
        }
    }

    private fun drawLines(edges: Iterable<Edge>, detailed: Boolean, color: androidx.compose.ui.graphics.Color) {
        ctx.drawScope.drawPoints(edgeCoordinates(edges, detailed), PointMode.Lines, color, density)
    }

    private fun drawCurvedEdge(edge: Edge, color: androidx.compose.ui.graphics.Color) {
        val curveMidpoint = edge.geometry.curveMidpoint!!
        val arc = Geometry.arcThroughPoints(edge.source.geometry.position, curveMidpoint, edge.target.geometry.position)
        if (arc != null) {
            drawCurvedEdge(edge, color, arc, curveMidpoint)
        } else {
            // the 3 points are almost collinear, so fall back to straight lines
            drawLines(listOf(edge), true, color)
        }
    }

    private fun drawCurvedEdge(
        edge: Edge, color: androidx.compose.ui.graphics.Color, fullArc: Geometry.Arc, labelPosition: Offset
    ) {
        val source = edge.source.geometry
        val target = edge.target.geometry
        val labelRect = labelRect(edge, labelPosition) ?: return
        val arcStartAngle = source.curvedEdgeEndAngle(fullArc) ?: return
        val arcEndAngle = target.curvedEdgeEndAngle(fullArc) ?: return

        // Once we have the arc angle at the label's position, we can split the arc into 2 segments,
        // using them to identify the angles where the segments go into the label
        val arcLabelAngle = atan2(labelPosition.y - fullArc.center.y, labelPosition.x - fullArc.center.x)
            .radToDeg().normalisedAngle()

        // The "full arc" runs between the midpoints of the two vertices;
        // the "major arc" runs between the points where the final arcs will enter the two vertices
        val majorArc = MajorArc(arcStartAngle, arcLabelAngle, arcEndAngle)
        drawCurveSegment1(fullArc, majorArc, labelRect, color, edge)
        drawCurveSegment2(fullArc, majorArc, labelRect, color, edge)
    }

    class MajorArc(val start: Float, val label: Float, val end: Float)

    private fun drawCurveSegment1(
        fullArc: Geometry.Arc, majorArc: MajorArc, labelRect: Rect,
        color: androidx.compose.ui.graphics.Color, edge: Edge
    ) {
        // Find where the first arc segment goes into the label
        // There should be precisely one intersection point since the arc ends inside the rectangle
        val sweepAngleUnclipped = Geometry.sweepAngle(from = majorArc.start, to = majorArc.label, fullArc.direction)
        val arcSegmentUnclipped = Geometry.Arc(fullArc.topLeft, fullArc.size, majorArc.start, sweepAngleUnclipped)
        val labelIntersectAngle = Geometry.rectArcIntersectAngles(arcSegmentUnclipped, labelRect).firstOrNull() ?: return

        val sweepAngle = Geometry.sweepAngle(from = majorArc.start, to = labelIntersectAngle, fullArc.direction)
        when {
            abs(sweepAngle) < 180 -> with(viewport) {
                ctx.drawScope.drawArc(
                    color = color, startAngle = majorArc.start, sweepAngle = sweepAngle, useCenter = false,
                    topLeft = fullArc.topLeft.toViewport(), size = fullArc.size.toViewport(), style = Stroke(density)
                )
            }
            // If sweep angle > 180, most likely the label has reached an awkward spot to draw an arc through,
            // so we fall back to a straight line segment
            else -> PrettyEdgeCoordinates(edge, this).arrowSegment1(edge.source, labelRect)?.let {
                ctx.drawScope.drawPoints(it.toList(), PointMode.Lines, color, density)
            }
        }
    }

    private fun drawCurveSegment2(
        fullArc: Geometry.Arc, majorArc: MajorArc, labelRect: Rect,
        color: androidx.compose.ui.graphics.Color, edge: Edge
    ) {
        val sweepAngleUnclipped = Geometry.sweepAngle(from = majorArc.label, to = majorArc.end, fullArc.direction)
        val arcSegmentUnclipped = Geometry.Arc(fullArc.topLeft, fullArc.size, majorArc.label, sweepAngleUnclipped)
        val labelIntersectAngle = Geometry.rectArcIntersectAngles(arcSegmentUnclipped, labelRect).firstOrNull() ?: return

        val sweepAngle = Geometry.sweepAngle(from = labelIntersectAngle, to = majorArc.end, fullArc.direction)
        when {
            abs(sweepAngle) < 180 -> {
                with(viewport) {
                    ctx.drawScope.drawArc(
                        color = color, startAngle = labelIntersectAngle, sweepAngle = sweepAngle, useCenter = false,
                        topLeft = fullArc.topLeft.toViewport(), size = fullArc.size.toViewport(),
                        style = Stroke(density)
                    )
                }
                curveArrowhead(fullArc, majorArc)?.toList()?.forEach {
                    ctx.drawScope.drawLine(color, it.from, it.to, density)
                }
            }
            else -> PrettyEdgeCoordinates(edge, this).arrowSegment2(labelRect, edge.target)?.let {
                ctx.drawScope.drawPoints(it.toList(), PointMode.Lines, color, density)
            }
        }
    }

    private fun curveArrowhead(fullArc: Geometry.Arc, majorArc: MajorArc): Pair<Geometry.Line, Geometry.Line>? {
        val arrowTarget = fullArc.offsetAtAngle(majorArc.end)
        val approachAngle = when (fullArc.direction) {
            Geometry.AngularDirection.Clockwise -> (majorArc.end - 1).normalisedAngle()
            Geometry.AngularDirection.CounterClockwise -> (majorArc.end + 1).normalisedAngle()
        }
        val arrowSource = fullArc.offsetAtAngle(approachAngle)
        val lines = Geometry.arrowhead(arrowSource, arrowTarget, ARROWHEAD_LENGTH, ARROWHEAD_WIDTH)
        return with(viewport) { lines?.let { Pair(it.first.toViewport(), it.second.toViewport()) } }
    }

    private fun edgeCoordinates(edges: Iterable<Edge>, detailed: Boolean): List<Offset> {
        return if (detailed) edges.flatMap { prettyEdgeCoordinates(it) }
        else edges.flatMap { simpleEdgeCoordinates(it) }
    }

    private fun simpleEdgeCoordinates(edge: Edge): Iterable<Offset> {
        val source = edge.source.geometry.position
        val target = edge.target.geometry.position
        return when (val arrowTarget = edge.target.geometry.edgeEndpoint(source)) {
            null -> line(source, target)
            else -> listOfNotNull(line(source, arrowTarget), arrowhead(source, arrowTarget)).flatten()
        }
    }

    private fun prettyEdgeCoordinates(edge: Edge): Iterable<Offset> {
        return PrettyEdgeCoordinates(edge, this).get()
    }

    fun line(source: Offset, target: Offset): Iterable<Offset> {
        return with(viewport) { listOf(source.toViewport(), target.toViewport()) }
    }

    private fun arrowhead(source: Offset, target: Offset): Iterable<Offset>? {
        return with(viewport) {
            val lines = Geometry.arrowhead(source, target, ARROWHEAD_LENGTH, ARROWHEAD_WIDTH)
            lines?.toList()?.flatMap { listOf(it.from.toViewport(), it.to.toViewport()) }
        }
    }

    fun labelRect(edge: Edge, position: Offset): Rect? {
        val labelSize = edgeLabelSizes[edge.label]
        return labelSize?.let {
            Rect(
                Offset(position.x - it.width.value / 2 - 2, position.y - it.height.value / 2 - 2),
                Size(it.width.value + 4, it.height.value + 4)
            )
        }
    }

    private enum class EdgeColorCode {
        Regular,
        Background,
        Inferred,
        InferredBackground;

        companion object {
            fun of(edge: Edge, graphArea: GraphArea): EdgeColorCode {
                val isInferred = edge is Edge.Inferrable && edge.isInferred
                val isBackground = with(graphArea.interactions) { edge.isBackground }
                return when {
                    isInferred && isBackground -> InferredBackground
                    isBackground -> Background
                    isInferred -> Inferred
                    else -> Regular
                }
            }
        }

        fun toColor(theme: Color.GraphTheme) = when (this) {
            Regular -> theme.edge
            Background -> theme.edge.copy(alpha = BACKGROUND_ALPHA)
            Inferred -> theme.inferred
            InferredBackground -> theme.inferred.copy(alpha = BACKGROUND_ALPHA)
        }
    }

    private class EdgesByColorCode(
        edges: Iterable<Edge>, graphArea: GraphArea
    ) : Iterable<Map.Entry<EdgeColorCode, List<Edge>>> {

        private val _map = EdgeColorCode.values().associateWith { mutableListOf<Edge>() }
        val map: Map<EdgeColorCode, List<Edge>> get() = _map

        init {
            synchronized(edges) { edges.forEach { _map[EdgeColorCode.of(it, graphArea)]!! += it } }
        }

        override fun iterator(): Iterator<Map.Entry<EdgeColorCode, List<Edge>>> {
            return map.iterator()
        }
    }

    private class PrettyEdgeCoordinates(val edge: Edge, private val renderer: EdgeRenderer) {

        private val labelRect = renderer.labelRect(
            edge, Geometry.midpoint(edge.source.geometry.position, edge.target.geometry.position)
        )

        fun get(): Iterable<Offset> {
            val arrowSegment1 = labelRect?.let { arrowSegment1(edge.source, it) }
            val arrowSegment2 = labelRect?.let { arrowSegment2(it, edge.target) }
            return listOfNotNull(arrowSegment1, arrowSegment2).flatten()
        }

        fun arrowSegment1(sourceVertex: Vertex, targetRect: Rect): Iterable<Offset>? {
            val source = sourceVertex.geometry.edgeEndpoint(targetRect.center)
            val target = source?.let { Geometry.rectIncomingLineIntersect(it, targetRect) } ?: return null
            return renderer.line(source, target)
        }

        fun arrowSegment2(sourceRect: Rect, targetVertex: Vertex): Iterable<Offset>? {
            val target = targetVertex.geometry.edgeEndpoint(sourceRect.center)
            val source = target?.let { Geometry.rectIncomingLineIntersect(it, sourceRect) } ?: return null
            return listOfNotNull(renderer.line(source, target), renderer.arrowhead(source, target)).flatten()
        }
    }
}