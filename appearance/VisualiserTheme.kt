package com.vaticle.typedb.studio.appearance

import androidx.compose.ui.graphics.Color
import com.vaticle.typedb.studio.data.VertexEncoding
import com.vaticle.typedb.studio.data.VertexEncoding.*

private val Gold = Color(0xFFEBC53D)
private val SkyBlue = Color(0xFF92E4FC)
private val Blue = Color(0xFF7BA0FF)
private val Purple = Color(0xFFE69CFF)
private val Black = Color(0xFF09022F)

data class VisualiserTheme(
    val background: Color,
    val vertex: Map<VertexEncoding, Color>,
    val edge: Color,
    val inferred: Color,
    val error: Color,
    val vertexLabel: Color,
) {
    companion object {
        val Default = VisualiserTheme(
            background = VaticlePalette.Purple0,
            vertex = mapOf(
                THING_TYPE to VaticlePalette.Pink2,
                ENTITY_TYPE to VaticlePalette.Pink2,
                RELATION_TYPE to VaticlePalette.Yellow2,
                ATTRIBUTE_TYPE to Blue,
                ENTITY to Purple,
                RELATION to Gold,
                ATTRIBUTE to SkyBlue,
            ),
            edge = Blue,
            inferred = VaticlePalette.Green,
            error = VaticlePalette.Red1,
            vertexLabel = Black,
        )
    }
}
