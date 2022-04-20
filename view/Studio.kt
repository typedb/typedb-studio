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

package com.vaticle.typedb.studio.view

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
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.common.Message
import com.vaticle.typedb.studio.state.AppDataManager
import com.vaticle.typedb.studio.view.browser.BrowserArea
import com.vaticle.typedb.studio.view.common.Context.LocalTitleBarHeight
import com.vaticle.typedb.studio.view.common.Context.LocalWindow
import com.vaticle.typedb.studio.view.common.KeyMapper
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Sentence
import com.vaticle.typedb.studio.view.common.Util.toDP
import com.vaticle.typedb.studio.view.common.component.Form.FormRowSpacer
import com.vaticle.typedb.studio.view.common.component.Form.SelectableText
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.component.Form.TextButton
import com.vaticle.typedb.studio.view.common.component.Frame
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.DIALOG_PADDING
import com.vaticle.typedb.studio.view.dialog.ConfirmationDialog
import com.vaticle.typedb.studio.view.dialog.ConnectionDialog
import com.vaticle.typedb.studio.view.dialog.DatabaseDialog
import com.vaticle.typedb.studio.view.dialog.ProjectDialog
import com.vaticle.typedb.studio.view.page.PageArea
import java.awt.Window
import java.awt.event.WindowEvent
import javax.swing.UIManager
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

object Studio {

    private val ERROR_WINDOW_WIDTH = 1000.dp
    private val ERROR_WINDOW_HEIGHT = 610.dp
    private val ERROR_WINDOW_CONTENT_PADDING = 10.dp
    private val ERROR_WINDOW_LABEL_WIDTH = 40.dp
    private val LOGGER = KotlinLogging.logger {}

    private var error: Throwable? by mutableStateOf(null)
    private var quit: Boolean by mutableStateOf(false)

    @OptIn(ExperimentalComposeUiApi::class)
    private object ExceptionHandler : WindowExceptionHandlerFactory {
        override fun exceptionHandler(window: Window) = WindowExceptionHandler {
            error = it
            window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
            throw it
        }
    }

    private fun getMainWindowTitle(): String {
        val pageName = GlobalState.resource.active?.fullName ?: GlobalState.project.current?.directory?.name ?: ""
        return Label.TYPEDB_STUDIO + " — " + pageName
    }

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            addShutdownHook()
            setConfigurations()
            Message.loadClasses()
            GlobalState.appData.initialise()
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

    private fun addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run(): Unit = runBlocking {
                LOGGER.info { Label.CLOSING_TYPEDB_STUDIO }
                GlobalState.connection.current?.close()
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
                CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides ExceptionHandler) {
                    window()
                }
            }
        }
    }

    private fun handleKeyEvent(event: KeyEvent, onClose: () -> Unit): Boolean {
        return if (event.type == KeyEventType.KeyUp) false
        else KeyMapper.CURRENT.map(event)?.let { executeCommand(it, onClose) } ?: false
    }

    private fun executeCommand(command: KeyMapper.Command, onClose: () -> Unit): Boolean {
        return when (command) {
            KeyMapper.Command.QUIT -> {
                onClose()
                true
            }
            else -> false
        }
    }

    @Composable
    private fun MainWindow(exitApplicationFn: () -> Unit) {
        fun confirmClose() = GlobalState.confirmation.submit(
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
            onPreviewKeyEvent = { handleKeyEvent(it, ::confirmClose) },
            onCloseRequest = { if (error != null) exitApplicationFn() else confirmClose() },
        ) {
            val density = LocalDensity.current.density
            var titleBarHeight by remember { mutableStateOf(0.dp) }
            val bgColor = Theme.colors.background1
            CompositionLocalProvider(LocalWindow provides window) {
                Column(Modifier.fillMaxSize().background(bgColor).onGloballyPositioned {
                    titleBarHeight = window.height.dp - toDP(it.size.height, density)
                }) {
                    CompositionLocalProvider(LocalTitleBarHeight provides titleBarHeight) {
                        Toolbar.Layout()
                        Separator.Horizontal()
                        Frame.Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            separator = Frame.SeparatorArgs(Separator.WEIGHT),
                            Frame.Pane(
                                id = BrowserArea.javaClass.name,
                                minSize = BrowserArea.MIN_WIDTH,
                                initSize = Either.first(BrowserArea.WIDTH)
                            ) { BrowserArea.Layout(it) },
                            Frame.Pane(
                                id = PageArea.javaClass.name,
                                minSize = PageArea.MIN_WIDTH,
                                initSize = Either.second(1f)
                            ) { PageArea.Layout() }
                        )
                        Separator.Horizontal()
                        StatusBar.Layout()
                    }
                }
                if (GlobalState.notification.queue.isNotEmpty()) NotificationArea.Layout()
                if (GlobalState.confirmation.isOpen) ConfirmationDialog.Layout()
                if (GlobalState.connection.connectServerDialog.isOpen) ConnectionDialog.ConnectServer()
                if (GlobalState.connection.manageDatabasesDialog.isOpen) DatabaseDialog.ManageDatabases()
                if (GlobalState.connection.selectDatabaseDialog.isOpen) DatabaseDialog.SelectDatabase()
                if (GlobalState.project.createItemDialog.isOpen) ProjectDialog.CreateProjectItem()
                if (GlobalState.project.openProjectDialog.isOpen) ProjectDialog.OpenProject()
                if (GlobalState.project.moveDirectoryDialog.isOpen) ProjectDialog.MoveDirectory()
                if (GlobalState.project.renameDirectoryDialog.isOpen) ProjectDialog.RenameDirectory()
                if (GlobalState.project.saveFileDialog.isOpen) ProjectDialog.SaveFile(window)
                if (GlobalState.project.renameFileDialog.isOpen) ProjectDialog.RenameFile()
            }
        }
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
            val contentColor = Theme.colors.error2
            val contentModifier = Modifier.fillMaxWidth().border(1.dp, Theme.colors.border)
                .background(Theme.colors.background0).padding(horizontal = ERROR_WINDOW_CONTENT_PADDING)

            fun exceptionText(): String =
                "${Label.TITLE}: ${exception.message}\n${Label.TRACE}: ${exception.stackTraceToString()}"

            CompositionLocalProvider(LocalWindow provides window) {
                Column(
                    modifier = Modifier.fillMaxSize().background(Theme.colors.background1).padding(DIALOG_PADDING),
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
                        TextButton(text = Label.COPY, onClick = { clipboard.setText(AnnotatedString(exceptionText())) })
                        FormRowSpacer()
                        TextButton(text = Label.QUIT, onClick = { quit = true; onClose() })
                        FormRowSpacer()
                        TextButton(text = Label.REOPEN, onClick = onClose)
                    }
                }
            }
        }
    }
}
