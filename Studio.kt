/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.framework.common.Context.LocalTitleBarHeight
import com.vaticle.typedb.studio.framework.common.Context.LocalWindow
import com.vaticle.typedb.studio.framework.common.Context.LocalWindowContext
import com.vaticle.typedb.studio.framework.common.KeyMapper
import com.vaticle.typedb.studio.framework.common.Util.toDP
import com.vaticle.typedb.studio.framework.common.WindowContext
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.common.theme.Theme.DIALOG_PADDING
import com.vaticle.typedb.studio.framework.material.Browsers
import com.vaticle.typedb.studio.framework.material.ConfirmationDialog
import com.vaticle.typedb.studio.framework.material.Form.RowSpacer
import com.vaticle.typedb.studio.framework.material.Form.SelectableText
import com.vaticle.typedb.studio.framework.material.Form.Text
import com.vaticle.typedb.studio.framework.material.Form.TextButton
import com.vaticle.typedb.studio.framework.material.Frame
import com.vaticle.typedb.studio.framework.material.Notifications
import com.vaticle.typedb.studio.framework.material.Pages
import com.vaticle.typedb.studio.framework.material.Separator
import com.vaticle.typedb.studio.module.StatusBar
import com.vaticle.typedb.studio.module.Toolbar
import com.vaticle.typedb.studio.module.connection.DatabaseDialog
import com.vaticle.typedb.studio.module.connection.ServerDialog
import com.vaticle.typedb.studio.module.preference.PreferenceDialog
import com.vaticle.typedb.studio.module.project.FilePage
import com.vaticle.typedb.studio.module.project.ProjectBrowser
import com.vaticle.typedb.studio.module.project.ProjectDialog
import com.vaticle.typedb.studio.module.type.TypeBrowser
import com.vaticle.typedb.studio.module.type.TypeDialog
import com.vaticle.typedb.studio.module.type.TypeEditor
import com.vaticle.typedb.studio.module.user.UpdateDefaultPasswordDialog
import com.vaticle.typedb.studio.resources.version.Version
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Message
import com.vaticle.typedb.studio.service.common.util.Sentence
import com.vaticle.typedb.studio.service.project.FileState
import com.vaticle.typedb.studio.service.schema.ThingTypeState
import io.sentry.Sentry
import io.sentry.protocol.User
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.awt.Window
import java.awt.event.WindowEvent
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*
import javax.swing.UIManager
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import java.lang.RuntimeException

object Studio {

    private const val VATICLE_BOT_ICON = "resources/icons/vaticle/vaticle-bot-32px.png"
    private val ERROR_WINDOW_WIDTH = 1000.dp
    private val ERROR_WINDOW_HEIGHT = 610.dp
    private val ERROR_WINDOW_CONTENT_PADDING = 10.dp
    private val ERROR_WINDOW_LABEL_WIDTH = 40.dp
    private val LOGGER = KotlinLogging.logger {}

    private var error: Throwable? by mutableStateOf(null)
    private var quit: Boolean by mutableStateOf(false)

    private val browsers = listOf(
        ProjectBrowser(true, 1),
        TypeBrowser(true, 2),
        // RuleBrowser(false, 3),
        // UserBrowser(false, 4),
        // RoleBrowser(false, 5),
    )

    @OptIn(ExperimentalComposeUiApi::class)
    private object ExceptionHandler : WindowExceptionHandlerFactory {
        override fun exceptionHandler(window: Window) = WindowExceptionHandler {
            error = it
            window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
            throw it
        }
    }

    @Composable
    private fun MainWindow(exitApplicationFn: () -> Unit) {
        fun confirmClose() = Service.confirmation.submit(
            title = Label.CONFIRM_QUITTING_APPLICATION,
            message = Sentence.CONFIRM_QUITING_APPLICATION,
            onConfirm = { quit = true; exitApplicationFn() }
            // TODO: we don't want to call exitApplication() for MacOS
        )

        // TODO: we want no title bar, by passing undecorated=true, but it seems to cause intermittent crashes on startup
        //       (see #40). Test if they occur when running the distribution, or only with bazel run :studio-bin-*
        Window(
            title = getMainWindowTitle(),
            state = rememberWindowState(WindowPlacement.Maximized),
            icon = painterResource(VATICLE_BOT_ICON),
            onPreviewKeyEvent = { handlePreviewKeyEvent(it, ::confirmClose) },
            onKeyEvent = { handleKeyEvent(it) },
            onCloseRequest = { if (error != null) exitApplicationFn() else confirmClose() },
        ) {
            CompositionLocalProvider(LocalWindow provides window) {
                val windowCtx = WindowContext.Compose(window)
                MainWindowContent(windowCtx)
                CompositionLocalProvider(LocalWindowContext provides windowCtx) {
                    Notifications.MayShowPopup()
                    ConfirmationDialog.MayShowDialog()
                    ServerDialog.MayShowDialogs()
                    DatabaseDialog.MayShowDialogs()
                    PreferenceDialog.MayShowDialogs()
                    ProjectDialog.MayShowDialogs(window)
                    TypeDialog.MayShowDialogs()
                    UpdateDefaultPasswordDialog.MayShowDialogs()
                }
            }
        }
    }

    @Composable
    private fun MainWindowContent(windowCtx: WindowContext) {
        var titleBarHeight by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current.density
        Column(Modifier.fillMaxSize().background(Theme.studio.backgroundMedium).onGloballyPositioned {
            titleBarHeight = windowCtx.height.dp - toDP(it.size.height, density)
        }) {
            CompositionLocalProvider(LocalWindowContext provides windowCtx) {
                CompositionLocalProvider(LocalTitleBarHeight provides titleBarHeight) {
                    Toolbar.Layout()
                    Separator.Horizontal()
                    Frame.Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        separator = Frame.SeparatorArgs(Separator.WEIGHT),
                        Frame.Pane(
                            id = Browsers.javaClass.name,
                            minSize = Browsers.MIN_WIDTH,
                            initSize = Either.first(Browsers.DEFAULT_WIDTH)
                        ) { Browsers.Layout(browsers, it, Browsers.Position.LEFT) },
                        Frame.Pane(
                            id = Pages.javaClass.name,
                            minSize = Pages.MIN_WIDTH,
                            initSize = Either.second(1f)
                        ) {
                            Pages.Layout(enabled = Service.project.current != null) {
                                when (it) {
                                    is FileState -> FilePage.create(it)
                                    is ThingTypeState<*, *> -> TypeEditor.create(it)
                                    else -> throw IllegalStateException("Unrecognised pageable type")
                                }
                            }
                        }
                    )
                    Separator.Horizontal()
                    StatusBar.Layout()
                }
            }
        }
    }

    private fun getMainWindowTitle(): String {
        val projectName = Service.project.current?.directory?.name
        val pageName = Service.pages.active?.windowTitle
        return Label.TYPEDB_STUDIO + ((pageName ?: projectName)?.let { " — $it" } ?: "")
    }

    private fun handlePreviewKeyEvent(event: KeyEvent, onClose: () -> Unit): Boolean = handleEvent(event) {
        when (it) {
            KeyMapper.Command.QUIT -> {
                onClose()
                true
            }
            else -> false
        }
    }

    private fun handleKeyEvent(event: KeyEvent): Boolean = handleEvent(event) {
        when (it) {
            KeyMapper.Command.NEW_PAGE -> {
                Service.project.tryCreateUntitledFile()?.tryOpen()
                true
            }
            else -> false
        }
    }

    private fun handleEvent(event: KeyEvent, function: (KeyMapper.Command) -> Boolean): Boolean {
        return if (event.type == KeyEventType.KeyUp) false
        else KeyMapper.CURRENT.map(event)?.let { function(it) } ?: false
    }

    @Composable
    private fun ErrorWindow(exception: Throwable, onClose: () -> Unit) {
        Window(
            title = Label.TYPEDB_STUDIO_APPLICATION_ERROR,
            onCloseRequest = { onClose() },
            state = rememberWindowState(
                placement = WindowPlacement.Floating,
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(ERROR_WINDOW_WIDTH, ERROR_WINDOW_HEIGHT),
            )
        ) {
            val clipboard = LocalClipboardManager.current
            val labelModifier = Modifier.width(ERROR_WINDOW_LABEL_WIDTH)
            val labelStyle = Theme.typography.body1.copy(fontWeight = FontWeight.Bold)
            val contentColor = Theme.studio.errorStroke
            val contentModifier = Modifier.fillMaxWidth().border(1.dp, Theme.studio.border)
                .background(Theme.studio.backgroundDark).padding(horizontal = ERROR_WINDOW_CONTENT_PADDING)

            fun exceptionText(): String =
                "${Label.TITLE}: ${exception.message}\n${Label.TRACE}: ${exception.stackTraceToString()}"

            CompositionLocalProvider(LocalWindow provides window) {
                CompositionLocalProvider(LocalWindowContext provides WindowContext.Compose(window)) {
                    Column(
                        modifier = Modifier.fillMaxSize().background(Theme.studio.backgroundMedium)
                            .padding(DIALOG_PADDING),
                        verticalArrangement = Arrangement.spacedBy(DIALOG_PADDING)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(value = "${Label.TITLE}:", modifier = labelModifier, textStyle = labelStyle)
                            exception.message?.let { SelectableText(value = it, color = contentColor) }
                        }
                        Row(Modifier.weight(1f)) {
                            Text(value = "${Label.TRACE}:", modifier = labelModifier, textStyle = labelStyle)
                            SelectableText(
                                value = exception.stackTraceToString(), color = contentColor, modifier = contentModifier
                            )
                        }
                        Row {
                            Spacer(Modifier.weight(1f))
                            TextButton(text = Label.COPY) { clipboard.setText(AnnotatedString(exceptionText())) }
                            RowSpacer()
                            TextButton(text = Label.QUIT) { quit = true; onClose() }
                            RowSpacer()
                            TextButton(text = Label.REOPEN, onClick = onClose)
                        }
                    }
                }
            }
        }
    }

    private fun addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run(): Unit = runBlocking {
                LOGGER.info { Label.CLOSING_TYPEDB_STUDIO }
                Service.driver.close()
            }
        })
    }

    private fun setConfigurations() {
        // Enable anti-aliasing
        System.setProperty("awt.useSystemAAFontSettings", "on")
        System.setProperty("swing.aatext", "true")
        // Enable native Windows UI style
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) // Set UI style for Windows
    }

    private fun configureDiagnostics(enabled: Boolean) {
        val releaseName = "TypeDB Studio@" + Version.VERSION;
        Sentry.init { options ->
            options.setDsn("https://9c327cb98a925974587f98adb192a89b@o4506315929812992.ingest.sentry.io/4506355166806016")
            options.setEnableTracing(true)
            options.setSendDefaultPii(false)
            options.setRelease(releaseName)
            options.setEnabled(enabled)
        }
        val user = User();
        user.setUsername(userID());
        Sentry.setUser(user);
    }

    private fun userID(): String {
        return try {
            val mac = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).hardwareAddress
            val macHash = MessageDigest.getInstance("SHA-256").digest(mac)
            val truncatedHash = Arrays.copyOfRange(macHash, 0, 8)
            String.format("%X", ByteBuffer.wrap(truncatedHash).long)
        } catch (e: java.lang.Exception) {
            "_0"
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun application(window: @Composable ApplicationScope.() -> Unit) {
        androidx.compose.ui.window.application(exitProcessOnExit = false) {
            Theme.Material {
                CompositionLocalProvider(
                        LocalWindowExceptionHandlerFactory provides ExceptionHandler
                ) { window() }
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            addShutdownHook()
            setConfigurations()
            Message.loadClasses()
            Service.data.initialise()
            configureDiagnostics(Service.preference.diagnosticsReportingEnabled);
            while (!quit) {
                application { MainWindow(::exitApplication) }
                error?.let { exception ->
                    Sentry.captureException(exception)
                    LOGGER.error(exception.message, exception)
                    application { ErrorWindow(exception) { error = null; exitApplication() } }
                }
            }
            exitProcess(0)
        } catch (exception: Exception) {
            Sentry.captureException(exception)
            LOGGER.error(exception.message, exception)
            exitProcess(1)
        }
    }
}
