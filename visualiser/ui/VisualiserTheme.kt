package com.vaticle.typedb.studio.visualiser.ui

import com.vaticle.typedb.studio.db.VertexEncoding
import com.vaticle.typedb.studio.db.VertexEncoding.*
import com.vaticle.typedb.studio.visualiser.ui.VisualiserColor.*

enum class VisualiserColor(val argb: Long) {
    RED(0xFFF66B65),
    GOLD(0xFFEBC53D),
    YELLOW(0xFFFFE4A7),
    GREEN(0xFF02DAC9),
    SKY_BLUE(0xFF92E4FC),
    BLUE(0xFF7BA0FF),
    PINK(0xFFFFA9E8),
    PURPLE(0xFFE69CFF),
    DEEP_PURPLE(0xFF0E053F),
    BLACK(0xFF09022F),
    WHITE(0xFFFFFFFF),
}

data class VisualiserTheme(
    val background: VisualiserColor,
    val vertex: Map<VertexEncoding, VisualiserColor>,
    val edge: VisualiserColor,
    val inferred: VisualiserColor,
    val error: VisualiserColor,
    val vertexLabel: VisualiserColor,
) {
    companion object {
        val DEFAULT = VisualiserTheme(
            background = DEEP_PURPLE,
            vertex = mapOf(
                Pair(THING_TYPE, PINK),
                Pair(ENTITY_TYPE, PINK),
                Pair(RELATION_TYPE, YELLOW),
                Pair(ATTRIBUTE_TYPE, BLUE),
                Pair(ENTITY, PURPLE),
                Pair(RELATION, GOLD),
                Pair(ATTRIBUTE, SKY_BLUE),
            ),
            edge = BLUE,
            inferred = GREEN,
            error = RED,
            vertexLabel = BLACK,
        )
    }
}
