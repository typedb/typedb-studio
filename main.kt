package com.vaticle.typedb.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
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
import androidx.compose.ui.window.Window
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
import com.vaticle.typedb.studio.OS.*
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.appearance.VisualiserTheme
import com.vaticle.typedb.studio.login.LoginScreen
import com.vaticle.typedb.studio.navigation.LoginScreenState
import com.vaticle.typedb.studio.navigation.Navigator
import com.vaticle.typedb.studio.navigation.WorkspaceScreenState
import com.vaticle.typedb.studio.ui.elements.StudioSnackbarHost
import com.vaticle.typedb.studio.workspace.WorkspaceScreen
import mu.KotlinLogging.logger
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.notExists

fun main() {

    val appData = AppData()
    if (appData.isWritable) setupFileLogging(appData)

    val log = logger {}
    appData.notWritableCause?.let {
        log.error(it) { "Unable to access app data. User preferences and history will be unavailable." }
    }

    application {
        val windowState: WindowState = rememberWindowState(placement = WindowPlacement.Maximized)

        fun onCloseRequest() {
            log.debug { "Closing TypeDB Studio" }
            exitApplication() // TODO: I think this is the wrong behaviour on MacOS
        }

        // TODO: we want undecorated (no title bar), but it seems to cause intermittent crashes on startup (see #40)
        //       Test if they occur when running the distribution, or only with bazel run :studio-bin-*
        Window(title = "TypeDB Studio", onCloseRequest = ::onCloseRequest, state = windowState /*undecorated = true*/) {
            var devicePixelRatio by remember { mutableStateOf(1F) }
            var titleBarHeight by remember { mutableStateOf(0F) }
            with(LocalDensity.current) { devicePixelRatio = 1.dp.toPx() }

            val scaffoldState = rememberScaffoldState()
            val snackbarHostState = scaffoldState.snackbarHostState

            val navigator = remember { Navigator(initialState = LoginScreenState()) }

            StudioTheme {
                Scaffold(modifier = Modifier.fillMaxSize()
                    .border(BorderStroke(1.dp, SolidColor(StudioTheme.colors.uiElementBorder)))
                    .onGloballyPositioned { coordinates ->
                        titleBarHeight = window.height - coordinates.size.height / devicePixelRatio
                    }) {

                    when (val screenState = navigator.activeScreenState) {
                        is LoginScreenState -> LoginScreen(form = screenState, navigator, snackbarHostState)
                        is WorkspaceScreenState -> WorkspaceScreen(workspace = screenState, navigator,
                            visualiserTheme = VisualiserTheme.Default, window, devicePixelRatio, titleBarHeight, snackbarHostState)
                    }

                    Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        StudioSnackbarHost(snackbarHostState)
                    }
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
        dataDir = when (OS.currentOS) {
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

enum class OS {
    WINDOWS,
    MAC,
    LINUX;

    companion object {
        val currentOS: OS
        get() {
            val osName = System.getProperty("os.name").lowercase(Locale.ENGLISH)
            return when {
                "mac" in osName || "darwin" in osName -> MAC
                "win" in osName -> WINDOWS
                else -> LINUX
            }
        }
    }
}
