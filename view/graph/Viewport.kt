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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import com.vaticle.typedb.studio.view.common.Util
import java.util.concurrent.atomic.AtomicBoolean

class Viewport constructor(private val graph: Graph) {
    var density: Float by mutableStateOf(1f); private set
    var physicalSize by mutableStateOf(DpSize.Zero); private set

    /** The world coordinates at the top-left corner of the viewport. */
    var worldCoordinates by mutableStateOf(Offset.Zero)
    private val physicalCenter get() = DpOffset(physicalSize.width / 2, physicalSize.height / 2)
    private var _scale by mutableStateOf(1f)
    var scale: Float
        get() = _scale
        set(value) {
            _scale = value.coerceIn(0.001f..10f)
        }
    var areInitialWorldCoordinatesSet = AtomicBoolean(false)

    fun updatePhysicalDimensions(size: Size, density: Float) {
        this.density = density
        physicalSize = Util.toDP(size, density)
    }

    fun alignWorldCenterWithPhysicalCenter() {
        worldCoordinates = Offset(-physicalCenter.x.value, -physicalCenter.y.value) / scale
    }

    fun findVertexAt(physicalPoint: Offset): Vertex? {
        val worldPoint = physicalPointToWorldPoint(physicalPoint)
        val nearestVertices = nearestVertices(worldPoint)
        return nearestVertices.find { it.geometry.intersects(worldPoint) }
    }

    private fun physicalPointToWorldPoint(physicalPoint: Offset): Offset {
        val transformOrigin = Offset(physicalSize.width.value, physicalSize.height.value) / 2f
        val scaledPhysicalPoint = physicalPoint / density

        // Let 'physical' be the physical position of a point in the viewport, 'origin' be the transform origin
        // of the viewport, 'world' be the position of 'physical' in the world, 'viewportPosition' be the world
        // offset at the top left corner of the viewport. Then:
        // physical = origin + scale * (world - viewportPosition - origin)
        // Rearranging this equation gives the result below:
        return (((scaledPhysicalPoint - transformOrigin) / scale) + transformOrigin) + worldCoordinates
    }

    private fun nearestVertices(worldPoint: Offset): Sequence<Vertex> {
        // TODO: once we have out-of-viewport detection, use it to make this function more performant on large graphs
        val vertexDistances: MutableMap<Vertex, Float> = mutableMapOf()
        graph.vertices.associateWithTo(vertexDistances) {
            (worldPoint - it.geometry.position).getDistanceSquared()
        }
        return sequence {
            while (vertexDistances.isNotEmpty()) {
                yield(vertexDistances.entries.minByOrNull { it.value }!!.key)
            }
        }.take(10)
    }
}