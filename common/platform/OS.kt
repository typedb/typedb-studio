package com.vaticle.typedb.studio.common.platform

import com.vaticle.typedb.studio.common.platform.OS.*
import java.util.Locale

enum class OS {
    WINDOWS,
    MAC,
    LINUX
}

fun currentOS(): OS {
    val osName = System.getProperty("os.name").lowercase(Locale.ENGLISH)
    return when {
        "mac" in osName || "darwin" in osName -> MAC
        "win" in osName -> WINDOWS
        else -> LINUX
    }
}
