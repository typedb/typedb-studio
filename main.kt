package com.vaticle.typedb.studio

import androidx.compose.ui.window.application
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import com.vaticle.typedb.studio.storage.AppData
import mu.KotlinLogging.logger
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory

fun main() {

    val appData = AppData()
    if (appData.isWritable) setupFileLogging(appData)

    val log = logger {}
    appData.notWritableCause?.let {
        log.error(it) { "Unable to access app data. User preferences and history will be unavailable." }
    }

    application {
        fun onCloseRequest() {
            log.debug { "Closing TypeDB Studio" }
            exitApplication() // TODO: I think this is the wrong behaviour on MacOS
        }

        MainWindow(onCloseRequest = ::onCloseRequest)
    }
}

fun setupFileLogging(appData: AppData) {
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val fileAppender = RollingFileAppender<ILoggingEvent>().apply {
        context = loggerContext
        name = "LOGFILE"
        file = appData.logFile.path
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
        fileNamePattern = "${appData.logFile.path}-%d{yyyy-MM}.%i.log.gz"
        setMaxFileSize(FileSize.valueOf("50MB"))
        maxHistory = 60
        setTotalSizeCap(FileSize.valueOf("1GB"))
    }
    rollingPolicy.start()
    fileAppender.rollingPolicy = rollingPolicy
    fileAppender.start()
    val rootLogger = LoggerFactory.getLogger(ROOT_LOGGER_NAME) as Logger
    rootLogger.addAppender(fileAppender)
}
