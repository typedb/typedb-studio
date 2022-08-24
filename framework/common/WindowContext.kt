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

package com.vaticle.typedb.studio.framework.common

import androidx.compose.ui.awt.ComposeWindow

sealed interface WindowContext {
    var height: Int
    var width: Int
    var x: Int
    var y: Int

    class Test(override var height: Int, override var width: Int, override var x: Int, override var y: Int): WindowContext

    class Compose(val window: ComposeWindow): WindowContext {
        override var height: Int
            get() = window.size.height
            set(value) { value }

        override var width: Int
            get() = window.size.height
            set(value) {value}

        override var x: Int
            get() = window.x
            set(value) {value}

        override var y: Int
            get() = window.y
            set(value) {value}
    }
}