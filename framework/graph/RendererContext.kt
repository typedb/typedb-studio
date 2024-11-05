/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.graph

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.typedb.studio.framework.common.theme.Color
import com.typedb.studio.framework.common.theme.Typography

data class RendererContext(val drawScope: DrawScope, val theme: Color.GraphTheme, val typography: Typography.Theme)
