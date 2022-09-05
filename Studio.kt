/*
 * Copyright (C) 2022 Vaticle
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
import com.vaticle.typedb.studio.framework.common.Util.toDP
import com.vaticle.typedb.studio.framework.common.WindowContext
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.common.theme.Theme.DIALOG_PADDING
import com.vaticle.typedb.studio.framework.material.Browsers
import com.vaticle.typedb.studio.framework.material.ConfirmationDialog
import com.vaticle.typedb.studio.framework.material.Form.FormHorizontalSpacer
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
import com.vaticle.typedb.studio.module.type.TypePage
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Message
import com.vaticle.typedb.studio.state.common.util.Sentence
import com.vaticle.typedb.studio.state.project.FileState
import com.vaticle.typedb.studio.state.schema.TypeState
import java.awt.Window
import java.awt.event.WindowEvent
import javax.swing.UIManager
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

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
        fun confirmClose() = StudioState.confirmation.submit(
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
            onPreviewKeyEvent = { handleKeyEvent(it, ::confirmClose) },
            onCloseRequest = { if (error != null) exitApplicationFn() else confirmClose() },
        ) {
            CompositionLocalProvider(LocalWindow provides window) {
                val windowContext = WindowContext.Compose(window)
                MainWindowContent(windowContext)
                CompositionLocalProvider(LocalWindowContext provides windowContext) {
                    Notifications.MayShowPopup()
                    ConfirmationDialog.MayShowDialog()
                    ServerDialog.MayShowDialogs()
                    DatabaseDialog.MayShowDialogs()
                    PreferenceDialog.MayShowDialogs()
                    ProjectDialog.MayShowDialogs(window)
                    TypeDialog.MayShowDialogs()
                }
            }
        }
    }

    @Composable
    private fun MainWindowContent(window: WindowContext) {
        var titleBarHeight by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current.density
        Column(Modifier.fillMaxSize().background(Theme.studio.backgroundMedium).onGloballyPositioned {
            titleBarHeight = window.height.dp - toDP(it.size.height, density)
        }) {
            CompositionLocalProvider(LocalWindowContext provides window) {
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
                            Pages.Layout(
                                enabled = StudioState.project.current != null,
                                onNewPage = { StudioState.project.tryCreateUntitledFile()?.tryOpen() }
                            ) {
                                when (it) {
                                    is FileState -> FilePage.create(it)
                                    is TypeState.Thing -> TypePage.create(it)
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
        val projectName = StudioState.project.current?.directory?.name
        val pageName = StudioState.pages.active?.windowTitle
        return Label.TYPEDB_STUDIO + ((pageName ?: projectName)?.let { " — $it" } ?: "")
    }

    private fun handleKeyEvent(event: KeyEvent, onClose: () -> Unit): Boolean {
        return if (event.type == KeyEventType.KeyUp) false
        else com.vaticle.typedb.studio.framework.common.KeyMapper.CURRENT.map(event)?.let {
            when (it) {
                com.vaticle.typedb.studio.framework.common.KeyMapper.Command.QUIT -> {
                    onClose()
                    true
                }
                else -> false
            }
        } ?: false
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
                val windowContext = WindowContext.Compose(window)
                CompositionLocalProvider(LocalWindowContext provides windowContext) {
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
                            FormHorizontalSpacer()
                            TextButton(text = Label.QUIT) { quit = true; onClose() }
                            FormHorizontalSpacer()
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
                StudioState.client.closeBlocking()
            }
        })
    }

    private fun setConfigurations() {
        // Enable anti-aliasing
        System.setProperty("awt.useSystemAAFontSettings", "on")
        System.setProperty("swing.aatext", "true")
        // Enable FileDialog to select "directories" on MacOS
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        // Enable native Windows UI style
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) // Set UI style for Windows
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
            StudioState.appData.initialise()
            while (!quit) {
                application { MainWindow(::exitApplication) }
                error?.let { exception ->
                    LOGGER.error(exception.message, exception)
                    application { ErrorWindow(exception) { error = null; exitApplication() } }
                }
            }
            exitProcess(0)
        } catch (exception: Exception) {
            LOGGER.error(exception.message, exception)
            exitProcess(1)
        }
    }
}
