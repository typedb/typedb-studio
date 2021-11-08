package com.vaticle.typedb.studio.common

import com.vaticle.typedb.studio.common.OS.*
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
