package com.vaticle.typedb.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.appearance.VisualiserTheme
import com.vaticle.typedb.studio.login.LoginScreen
import com.vaticle.typedb.studio.routing.CoreLoginRoute
import com.vaticle.typedb.studio.routing.LoginRoute
import com.vaticle.typedb.studio.routing.Router
import com.vaticle.typedb.studio.routing.WorkspaceRoute
import com.vaticle.typedb.studio.storage.AppData
import com.vaticle.typedb.studio.ui.elements.StudioSnackbarHost
import com.vaticle.typedb.studio.workspace.WorkspaceScreen
import mu.KotlinLogging.logger
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory

@Composable
fun Studio(onCloseRequest: () -> Unit) {

    val windowState: WindowState = rememberWindowState(placement = WindowPlacement.Maximized)
    var titleBarHeight by remember { mutableStateOf(0F) }
    val scaffoldState = rememberScaffoldState()
    val snackbarHostState = scaffoldState.snackbarHostState
    val router = remember { Router(initialRoute = CoreLoginRoute()) }
    val pixelDensity = LocalDensity.current.density

    // TODO: we want undecorated (no title bar), but it seems to cause intermittent crashes on startup (see #40)
    //       Test if they occur when running the distribution, or only with bazel run :studio-bin-*
    androidx.compose.ui.window.Window(
        title = "TypeDB Studio",
        onCloseRequest = onCloseRequest,
        state = windowState /*undecorated = true*/
    ) {
        StudioTheme {
            Scaffold(modifier = Modifier.fillMaxSize()
                .border(BorderStroke(1.dp, SolidColor(StudioTheme.colors.uiElementBorder)))
                .onGloballyPositioned { coordinates ->
                    // used to translate from screen coordinates to window coordinates in the visualiser
                    titleBarHeight = window.height - coordinates.size.height / pixelDensity
                }) {

                when (val currentRoute = router.currentRoute) {
                    is LoginRoute -> LoginScreen(routeData = currentRoute, router, snackbarHostState)
                    is WorkspaceRoute -> WorkspaceScreen(
                        routeData = currentRoute, router,
                        visualiserTheme = VisualiserTheme.Default, window, titleBarHeight, snackbarHostState
                    )
                }

                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    StudioSnackbarHost(snackbarHostState)
                }
            }
        }
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

        Studio(onCloseRequest = ::onCloseRequest)
    }
}
