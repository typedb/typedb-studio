/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import com.typedb.studio.service.common.util.Message.System.Companion.APP_DATA_DIR_DISABLED
import com.typedb.studio.service.common.util.Message.System.Companion.DATA_DIR_NOT_WRITABLE
import com.typedb.studio.service.common.util.Message.System.Companion.UNEXPECTED_ERROR_APP_DATA_DIR
import com.typedb.studio.service.common.util.Property
import com.typedb.studio.service.common.util.Property.OS.LINUX
import com.typedb.studio.service.common.util.Property.OS.MACOS
import com.typedb.studio.service.common.util.Property.OS.WINDOWS
import java.lang.System.getProperty
import java.lang.System.getenv
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.exists
import kotlin.io.path.notExists
import mu.KotlinLogging
import org.slf4j.LoggerFactory

class DataService {

    companion object {
        private val DATA_DIR: Path = when (Property.OS.Current) {
            WINDOWS -> Path.of(getenv("AppData")).resolve("TypeDB Studio")
            MACOS, LINUX -> Path.of(getProperty("user.home")).resolve(".typedb-studio")
        }
        private var LOG_DIR = DATA_DIR.resolve("log")
        private var LOG_FILE = LOG_DIR.resolve("typedb-studio.log").toFile()
        private var PROPERTIES_DIR = DATA_DIR.resolve("properties")
        private var PROPERTIES_FILE = PROPERTIES_DIR.resolve("typedb-studio.properties").toFile()
        private val LOGGER = KotlinLogging.logger {}
    }

    inner class Project {

        private val PROJECT_PATH = "project.path"

        var path: Path?
            get() = properties?.getProperty(PROJECT_PATH)?.let { Path.of(it) }
            set(value) = value?.let { setProperty(PROJECT_PATH, it.toString()) } ?: Unit
    }

    inner class Connection {

        private val CONNECTION_SERVER = "connection.server"
        private val CONNECTION_CORE_ADDRESS = "connection.core_address"
        private val CONNECTION_CLOUD_ADDRESSES = "connection.cloud_addresses"
        private val CONNECTION_CLOUD_ADDRESS_TRANSLATION = "connection.cloud_address_translation"
        private val CONNECTION_USE_CLOUD_ADDRESS_TRANSLATION = "connection.use_cloud_address_translation"
        private val CONNECTION_USERNAME = "connection.username"
        private val CONNECTION_TLS_ENABLED = "connection.tls_enabled"
        private val CONNECTION_CA_CERTIFICATE = "connection.ca_certificate"
        private val CONNECTION_ADVANCED_CONFIG_SELECTED = "connection.advanced_config_selected"

        var server: Property.Server?
            get() = properties?.getProperty(CONNECTION_SERVER)?.let { Property.Server.of(it) }
            set(value) = value?.let { setProperty(CONNECTION_SERVER, it.displayName) } ?: Unit
        var coreAddress: String?
            get() = properties?.getProperty(CONNECTION_CORE_ADDRESS)
            set(value) = value?.let { setProperty(CONNECTION_CORE_ADDRESS, it) } ?: Unit
        var cloudAddresses: List<String>?
            get() = properties?.getProperty(CONNECTION_CLOUD_ADDRESSES)
                ?.split(",")?.filter { it.isNotBlank() }
            set(value) = value?.let { setProperty(CONNECTION_CLOUD_ADDRESSES, it.joinToString(",")) } ?: Unit
        var cloudAddressTranslation: List<Pair<String, String>>?
            get() = properties?.getProperty(CONNECTION_CLOUD_ADDRESS_TRANSLATION)
                ?.split(",")?.filter { it.contains("=") }?.map { it.split("=", limit = 2) }?.map{ it[0] to it[1] }
            set(value) = value
                ?.let { setProperty(CONNECTION_CLOUD_ADDRESS_TRANSLATION, it.map { pair -> "${pair.first}=${pair.second}" } .joinToString(",")) } ?: Unit
        var useCloudAddressTranslation: Boolean?
            get() = properties?.getProperty(CONNECTION_USE_CLOUD_ADDRESS_TRANSLATION)?.toBooleanStrictOrNull()
            set(value) = value?.let { setProperty(CONNECTION_USE_CLOUD_ADDRESS_TRANSLATION, it.toString()) } ?: Unit
        var username: String?
            get() = properties?.getProperty(CONNECTION_USERNAME)
            set(value) = value?.let { setProperty(CONNECTION_USERNAME, it) } ?: Unit
        var tlsEnabled: Boolean?
            get() = properties?.getProperty(CONNECTION_TLS_ENABLED)?.toBooleanStrictOrNull()
            set(value) = value?.let { setProperty(CONNECTION_TLS_ENABLED, it.toString()) } ?: Unit
        var caCertificate: String?
            get() = properties?.getProperty(CONNECTION_CA_CERTIFICATE)
            set(value) = value?.let { setProperty(CONNECTION_CA_CERTIFICATE, it) } ?: Unit
        var advancedConfigSelected: Boolean?
            get() = properties?.getProperty(CONNECTION_ADVANCED_CONFIG_SELECTED)?.toBooleanStrictOrNull()
            set(value) = value?.let { setProperty(CONNECTION_ADVANCED_CONFIG_SELECTED, it.toString()) } ?: Unit
    }

    inner class Preferences {
        private val AUTO_SAVE = "editor.auto-save"
        private val IGNORED_PATHS = "project.ignored-paths"
        private val MATCH_QUERY_LIMIT = "query.match-limit"
        private val TRANSACTION_TIMEOUT_MINS = "query.transaction-timeout-mins"
        private val GRAPH_OUTPUT = "graph.output" // TODO: add _ENABLED to be symmetric, when we break backwards compatibility
        private val DIAGNOSTICS_REPORTING_ENABLED = "diagnostics.reporting.enabled"

        var autoSave: Boolean?
            get() = properties?.getProperty(AUTO_SAVE)?.toBoolean()
            set(value) = setProperty(AUTO_SAVE, value.toString())

        var ignoredPaths: List<String>?
            get() = properties?.getProperty(IGNORED_PATHS)?.split(',')?.map { it.trim() }
            set(value) = setProperty(IGNORED_PATHS, value!!.joinToString(","))

        var getQueryLimit: Long?
            get() = properties?.getProperty(MATCH_QUERY_LIMIT)?.toLong()
            set(value) = setProperty(MATCH_QUERY_LIMIT, value!!.toString())

        var transactionTimeoutMins: Long?
            get() = properties?.getProperty(TRANSACTION_TIMEOUT_MINS)?.toLong()
            set(value) = setProperty(TRANSACTION_TIMEOUT_MINS, value!!.toString())

        var graphOutputEnabled: Boolean?
            get() = properties?.getProperty(GRAPH_OUTPUT)?.toBoolean()
            set(value) = setProperty(GRAPH_OUTPUT, value.toString())

        var diagnosticsReportingEnabled: Boolean?
            get() = properties?.getProperty(DIAGNOSTICS_REPORTING_ENABLED)?.toBoolean()
            set(value) = setProperty(DIAGNOSTICS_REPORTING_ENABLED, value.toString())
    }

    var properties: Properties? by mutableStateOf(null)
    var project = Project()
    var connection = Connection()
    var preferences = Preferences()

    private fun setProperty(key: String, value: String) {
        properties?.setProperty(key, value)
        properties?.store(PROPERTIES_FILE.outputStream(), null)
    }

    fun initialise() {
        var isEnabled = false
        try {
            if (DATA_DIR.notExists()) Files.createDirectory(DATA_DIR)
            if (Files.isWritable(DATA_DIR)) {
                initPropertiesFile()
                initLogFile()
                isEnabled = true
            } else {
                LOGGER.error { DATA_DIR_NOT_WRITABLE.message(DATA_DIR) }
            }
        } catch (e: Exception) {
            LOGGER.error { UNEXPECTED_ERROR_APP_DATA_DIR.message(e.message) }
            LOGGER.error { e }
            isEnabled = false
        } finally {
            if (!isEnabled) LOGGER.error { APP_DATA_DIR_DISABLED }
        }
    }

    private fun initPropertiesFile() {
        if (!PROPERTIES_DIR.exists()) Files.createDirectories(PROPERTIES_DIR)
        if (!PROPERTIES_FILE.exists()) Files.createFile(PROPERTIES_FILE.toPath())
        properties = Properties().also { it.load(PROPERTIES_FILE.inputStream()) }
    }

    private fun initLogFile() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val fileAppender = RollingFileAppender<ILoggingEvent>().apply {
            context = loggerContext
            name = "LOGFILE"
            file = LOG_FILE.path
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
            fileNamePattern = "${LOG_FILE.path}-%d{yyyy-MM}.%i.log.gz"
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
