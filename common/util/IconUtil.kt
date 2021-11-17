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

package com.vaticle.typedb.studio.common.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import java.io.InputStream

object IconUtil {

    private const val DATABASE_ICON_PNG = "icons/database.png"
    private const val DATABASE_ICON_SVG = "icons/database.svg"

    private val databasePngExists: Boolean = ClassLoader.getSystemResource(DATABASE_ICON_PNG) != null
    private val databaseSvgInputStream: InputStream? = ClassLoader.getSystemResourceAsStream(DATABASE_ICON_SVG)


    @Composable
    fun DatabaseIcon() {
        // The Blueprint database icon appears blurred when scaled to our icon size, so we use a custom version
        // The SVG version looks better on high density displays, the PNG version looks better on low density ones

        val pixelDensity = LocalDensity.current.density
        when {
            pixelDensity <= 1f -> {
                if (databasePngExists) {
                    Image(
                        painter = painterResource(DATABASE_ICON_PNG), contentDescription = "Database",
                        modifier = Modifier.graphicsLayer(scaleX = pixelDensity, scaleY = pixelDensity)
                    )
                } else {
                    Text("", Modifier.width(14.dp))
                }
            }
            else -> {
                if (databaseSvgInputStream != null) {
                    Image(
                        painter = loadSvgPainter(databaseSvgInputStream, LocalDensity.current),
                        contentDescription = "Database",
                        modifier = Modifier.graphicsLayer(scaleX = 14f / 12f, scaleY = 14f / 12f)
                    )
                } else {
                    Text("", Modifier.width(14.dp))
                }
            }
        }
    }
}
