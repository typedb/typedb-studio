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

package com.vaticle.typedb.studio.state.config

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.common.Property.OS.LINUX
import com.vaticle.typedb.studio.state.common.Property.OS.MAC
import com.vaticle.typedb.studio.state.common.Property.OS.WINDOWS
import java.lang.System.getProperty
import java.lang.System.getenv
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.notExists
import mu.KotlinLogging
import org.slf4j.LoggerFactory

object UserDataDirectory {

    private const val WIN_ENV_APP_DATA = "AppData"
    private const val UNIX_PROP_USER_HOME = "user.home"
    private const val MAC_DIR_LIBRARY = "Library"
    private const val MAC_DIR_APP_SUPPORT = "Application Support"
    private const val APP_DIR_TYPEDB_STUDIO = "TypeDB Studio"
    private const val APP_LOG = "typedb-studio.log"
    private val LOGGER = KotlinLogging.logger {}

    private val path: Path = when (currentOS()) {
        // Source: https://stackoverflow.com/a/16660314/2902555
        WINDOWS -> Path.of(getenv(WIN_ENV_APP_DATA), APP_DIR_TYPEDB_STUDIO)
        MAC -> Path.of(getProperty(UNIX_PROP_USER_HOME), MAC_DIR_LIBRARY, MAC_DIR_APP_SUPPORT, APP_DIR_TYPEDB_STUDIO)
        LINUX -> Path.of(getProperty(UNIX_PROP_USER_HOME), APP_DIR_TYPEDB_STUDIO)
    }

    private var logFile = path.resolve(APP_LOG).toFile()
    private var isWritable = false

    private fun currentOS(): Property.OS {
        val osName = getProperty("os.name").lowercase()
        return when {
            "mac" in osName || "darwin" in osName -> MAC
            "win" in osName -> WINDOWS
            else -> LINUX
        }
    }

    fun initialise() {
        try {
            if (path.notExists()) Files.createDirectory(path)
            isWritable = Files.isWritable(path)
            if (isWritable) initLogFileOutput()
        } catch (e: Exception) {
            isWritable = false;
        }
        if (!isWritable) LOGGER.error { "Unable to access app data. User preferences and history will be unavailable." }
    }

    private fun initLogFileOutput() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val fileAppender = RollingFileAppender<ILoggingEvent>().apply {
            context = loggerContext
            name = "LOGFILE"
            file = logFile.path
        }
        val encoder = PatternLayoutEncoder().apply {
            context = loggerContext
            pattern = "%date{ISO8601} [%thread] [%-5level] %logger{36} - %msg%n"
        }
        encoder.start()
        fileAppender.encoder = encoder
        val rollingPolicy = SizeAndTimeBasedRollingPolicy<ILoggingEvent>().apply {
            context = loggerContext
            setParent(fileAppender)
            fileNamePattern = "${logFile.path}-%d{yyyy-MM}.%i.log.gz"
            setMaxFileSize(FileSize.valueOf("50MB"))
            maxHistory = 60
            setTotalSizeCap(FileSize.valueOf("1GB"))
        }
        rollingPolicy.start()
        fileAppender.rollingPolicy = rollingPolicy
        fileAppender.start()
        val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        rootLogger.addAppender(fileAppender)
    }
}
