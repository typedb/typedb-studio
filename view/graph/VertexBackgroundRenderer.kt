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

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import com.vaticle.typedb.client.api.concept.thing.Thing
import kotlin.math.sqrt

sealed class VertexBackgroundRenderer(
    private val vertex: Vertex,
    private val graphArea: GraphArea,
    protected val ctx: RendererContext
) {
    companion object {
        private const val CORNER_RADIUS = 5f
        private const val HOVERED_ALPHA = .675f
        private const val BACKGROUND_ALPHA = .25f
        private const val HOVERED_BACKGROUND_ALPHA = .175f

        fun of(vertex: Vertex, graphArea: GraphArea, ctx: RendererContext): VertexBackgroundRenderer =
            when (vertex) {
                is Vertex.Type.Entity, is Vertex.Type.Thing, is Vertex.Thing.Entity -> Entity(
                    vertex,
                    graphArea,
                    ctx
                )
                is Vertex.Type.Relation, is Vertex.Thing.Relation -> Relation(vertex, graphArea, ctx)
                is Vertex.Type.Attribute, is Vertex.Thing.Attribute -> Attribute(vertex, graphArea, ctx)
            }
    }

    private val baseColor = ctx.theme.vertex.let { colors ->
        when (vertex) {
            is Vertex.Thing.Attribute -> colors.attribute
            is Vertex.Thing.Entity -> colors.entity
            is Vertex.Thing.Relation -> colors.relation
            is Vertex.Type.Attribute -> colors.attributeType
            is Vertex.Type.Entity -> colors.entityType
            is Vertex.Type.Relation -> colors.relationType
            is Vertex.Type.Thing -> colors.thingType
        }
    }

    // Logically, if the vertex is dragged, it should also be hovered; however, this is not always true
    // because the vertex takes some time to "catch up" to the pointer. So check both conditions.
    private val alpha = with(graphArea.interactions) {
        when {
            vertex.isHovered && vertex.isBackground -> HOVERED_BACKGROUND_ALPHA
            vertex.isBackground -> BACKGROUND_ALPHA
            vertex.isHovered -> HOVERED_ALPHA
            else -> 1f
        }
    }
    protected val color = baseColor.copy(alpha)
    private val density = graphArea.viewport.density
    protected val rect = vertex.geometry.let {
        Rect(
            (it.position - graphArea.viewport.worldCoordinates) * density
                    - Offset(it.size.width * density / 2, it.size.height * density / 2),
            it.size * density
        )
    }

    protected val cornerRadius get() = CornerRadius(CORNER_RADIUS * density)

    protected fun getHighlight(): Highlight? = when {
        isInHoveredExplanationTree() -> Highlight.of(ctx.theme.explanation.copy(alpha), density * 1.5f, this)
        isInferred() -> Highlight.of(ctx.theme.inferred.copy(alpha), density, this)
        else -> null
    }

    protected open fun getHighlightRect(highlightWidth: Float) = Rect(
        rect.topLeft - Offset(highlightWidth, highlightWidth),
        Size(rect.size.width + highlightWidth * 2, rect.size.height + highlightWidth * 2)
    )

    private fun isInHoveredExplanationTree(): Boolean {
        return graphArea.graph.reasoning.explanationsByVertex[vertex]
            ?.any { it in graphArea.interactions.hoveredVertexExplanations } ?: false
    }

    private fun isInferred() = vertex.concept is Thing && vertex.concept.isInferred

    abstract fun draw()

    class Highlight private constructor(
        val color: androidx.compose.ui.graphics.Color, val width: Float, val rect: Rect
    ) {
        companion object {
            fun of(color: androidx.compose.ui.graphics.Color, width: Float, renderer: VertexBackgroundRenderer)
                    : Highlight {
                return Highlight(color, width, renderer.getHighlightRect(width))
            }
        }
    }

    class Entity(vertex: Vertex, graphArea: GraphArea, ctx: RendererContext) :
        VertexBackgroundRenderer(vertex, graphArea, ctx) {

        override fun draw() {
            getHighlight()?.let {
                ctx.drawScope.drawRoundRect(it.color, it.rect.topLeft, it.rect.size, cornerRadius)
            }
            ctx.drawScope.drawRoundRect(color, rect.topLeft, rect.size, cornerRadius)
        }
    }

    class Relation(vertex: Vertex, graphArea: GraphArea, ctx: RendererContext) :
        VertexBackgroundRenderer(vertex, graphArea, ctx) {

        // We start with a square of width n and transform it into a rhombus
        private val n = (rect.height / sqrt(2.0)).toFloat()
        private val baseShape = Rect(offset = rect.center - Offset(n / 2, n / 2), size = Size(n, n))

        override fun getHighlightRect(highlightWidth: Float) = Rect(
            baseShape.topLeft - Offset(highlightWidth, highlightWidth),
            Size(baseShape.size.width + highlightWidth * 2, baseShape.size.height + highlightWidth * 2)
        )

        override fun draw() {
            with(ctx.drawScope) {
                withTransform({
                    scale(scaleX = rect.width / rect.height, scaleY = 1f, pivot = rect.center)
                    rotate(degrees = 45f, pivot = rect.center)
                }) {
                    getHighlight()?.let { drawRoundRect(it.color, it.rect.topLeft, it.rect.size, cornerRadius) }
                    drawRoundRect(color, baseShape.topLeft, baseShape.size, cornerRadius)
                }
            }
        }
    }

    class Attribute(vertex: Vertex, graphArea: GraphArea, ctx: RendererContext) :
        VertexBackgroundRenderer(vertex, graphArea, ctx) {

        override fun draw() {
            getHighlight()?.let { ctx.drawScope.drawOval(it.color, it.rect.topLeft, it.rect.size) }
            ctx.drawScope.drawOval(color, rect.topLeft, rect.size)
        }
    }
}