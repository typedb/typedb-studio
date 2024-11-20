/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.graph

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import com.typedb.driver.api.concept.thing.Thing
import kotlin.math.sqrt

sealed class VertexBackgroundRenderer(
    protected val vertex: Vertex,
    private val graphArea: GraphArea,
    protected val ctx: RendererContext
) {
    companion object {
        private const val CORNER_RADIUS = 5f
        private const val HOVERED_ALPHA = 1f
        private const val BACKGROUND_ALPHA = .25f
        private const val HOVERED_BACKGROUND_ALPHA = .25f

        fun of(vertex: Vertex, graphArea: GraphArea, ctx: RendererContext): VertexBackgroundRenderer = when (vertex) {
            is Vertex.Type.Entity, is Vertex.Type.Thing, is Vertex.Thing.Entity -> Entity(vertex, graphArea, ctx)
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

    class Highlight private constructor(val color: Color, val width: Float, val rect: Rect) {
        companion object {
            fun of(color: Color, width: Float, renderer: VertexBackgroundRenderer): Highlight {
                return Highlight(color, width, renderer.getHighlightRect(width))
            }
        }
    }

    protected fun withOpaqueBackground(color: Color, ctx: RendererContext, fn: (Color) -> Unit) {
        // we want vertices to appear "faded", not "semi-transparent"
        if (color.alpha < 1f) fn(ctx.theme.background)
        fn(color)
    }

    class Entity(vertex: Vertex, graphArea: GraphArea, ctx: RendererContext) :
        VertexBackgroundRenderer(vertex, graphArea, ctx) {

        override fun draw() {
            getHighlight()?.let {
                withOpaqueBackground(it.color, ctx) { color ->
                    ctx.drawScope.drawRoundRect(color, it.rect.topLeft, it.rect.size, cornerRadius)
                }
            }
            withOpaqueBackground(color, ctx) { ctx.drawScope.drawRoundRect(it, rect.topLeft, rect.size, cornerRadius) }
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
                if (vertex.geometry.isVisiblyCollapsed || !vertex.geometry.contentOverflowsBaseShape) {
                    withTransform({
                        scale(scaleX = rect.width / rect.height, scaleY = 1f, pivot = rect.center)
                        rotate(degrees = 45f, pivot = rect.center)
                    }) {
                        getHighlight()?.let {
                            withOpaqueBackground(it.color, ctx) { color ->
                                drawRoundRect(color, it.rect.topLeft, it.rect.size, cornerRadius)
                            }
                        }
                        withOpaqueBackground(color, ctx) {
                            drawRoundRect(it, baseShape.topLeft, baseShape.size, cornerRadius)
                        }
                    }
                } else {
                    getHighlight()?.let {
                        withOpaqueBackground(it.color, ctx) { color ->
                            ctx.drawScope.drawRoundRect(color, it.rect.topLeft, it.rect.size, CornerRadius(8f))
                        }
                    }
                    withOpaqueBackground(color, ctx) {
                        ctx.drawScope.drawRoundRect(it, rect.topLeft, rect.size, CornerRadius(8f))
                    }
                }
            }
        }
    }

    class Attribute(vertex: Vertex, graphArea: GraphArea, ctx: RendererContext) :
        VertexBackgroundRenderer(vertex, graphArea, ctx) {

        // TODO: too much duplication
        override fun draw() {
            if (vertex.geometry.isVisiblyCollapsed || !vertex.geometry.contentOverflowsBaseShape) {
                getHighlight()?.let {
                    withOpaqueBackground(it.color, ctx) { color ->
                        ctx.drawScope.drawOval(color, it.rect.topLeft, it.rect.size)
                    }
                }
                withOpaqueBackground(color, ctx) { ctx.drawScope.drawOval(it, rect.topLeft, rect.size) }
            } else {
                getHighlight()?.let {
                    withOpaqueBackground(it.color, ctx) { color ->
                        ctx.drawScope.drawRoundRect(color, it.rect.topLeft, it.rect.size, CornerRadius(8f))
                    }
                }
                withOpaqueBackground(color, ctx) {
                    ctx.drawScope.drawRoundRect(it, rect.topLeft, rect.size, CornerRadius(8f))
                }
            }
        }
    }
}
