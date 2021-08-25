package com.vaticle.typedb.studio.visualiser

import androidx.compose.ui.graphics.Color
import com.vaticle.typedb.studio.db.VertexEncoding
import com.vaticle.typedb.studio.db.VertexEncoding.*

private val Red = Color(0xFFF66B65)
private val Gold = Color(0xFFEBC53D)
private val Yellow = Color(0xFFFFE4A7)
private val Green = Color(0xFF02DAC9)
private val SkyBlue = Color(0xFF92E4FC)
private val Blue = Color(0xFF7BA0FF)
private val Pink = Color(0xFFFFA9E8)
private val Purple = Color(0xFFE69CFF)
private val DeepPurple = Color(0xFF0E053F)
private val Black = Color(0xFF09022F)
private val White = Color(0xFFFFFFFF)

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
            background = DeepPurple,
            vertex = mapOf(
                Pair(THING_TYPE, Pink),
                Pair(ENTITY_TYPE, Pink),
                Pair(RELATION_TYPE, Yellow),
                Pair(ATTRIBUTE_TYPE, Blue),
                Pair(ENTITY, Purple),
                Pair(RELATION, Gold),
                Pair(ATTRIBUTE, SkyBlue),
            ),
            edge = Blue,
            inferred = Green,
            error = Red,
            vertexLabel = Black,
        )
    }
}
