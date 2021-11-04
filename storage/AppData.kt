package com.vaticle.typedb.studio.storage

import com.vaticle.typedb.studio.common.platform.OS.*
import com.vaticle.typedb.studio.common.platform.currentOS
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.notExists

class AppData {
    private val dataDir: Path

    var logFile: File
        private set

    val isWritable: Boolean
        get() = notWritableCause == null

    var notWritableCause: Exception? = null
        private set

    init {
        val appName = "TypeDB Studio"
        dataDir = when (currentOS()) {
            // https://stackoverflow.com/a/16660314/2902555
            WINDOWS -> Path.of(System.getenv("AppData"), appName)
            MAC -> Path.of(System.getProperty("user.home"), "Library", "Application Support", appName)
            LINUX -> Path.of(System.getProperty("user.home"), appName)
        }
        logFile = dataDir.resolve("typedb-studio.log").toFile()
        try {
            if (dataDir.notExists()) Files.createDirectory(dataDir)
            // test that we have write access
            val testPath = dataDir.resolve("test.txt")
            Files.deleteIfExists(testPath)
            Files.createFile(testPath)
            Files.delete(testPath)
        } catch (e: Exception) {
            notWritableCause = e
        }
    }
}
