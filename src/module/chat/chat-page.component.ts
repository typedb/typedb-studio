/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AfterViewInit, Component, ElementRef, NgZone, OnInit, ViewChild } from "@angular/core";
import { AsyncPipe, DatePipe } from "@angular/common";
import { FormControl, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { RouterLink } from "@angular/router";
import { MatButtonModule } from "@angular/material/button";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { MatMenuModule } from "@angular/material/menu";
import { MatDialog } from "@angular/material/dialog";
import { TextFieldModule } from "@angular/cdk/text-field";
import { ResizableDirective } from "@hhangular/resizable";
import { CodeEditorComponent } from "../../framework/code-editor/code-editor.component";
import { SpinnerComponent } from "../../framework/spinner/spinner.component";
import { ActionDurationPipe } from "../../framework/util/action-duration.pipe";
import { RichTooltipDirective } from "../../framework/tooltip/rich-tooltip.directive";
import { DriverAction, QueryRunAction, TransactionOperationAction, isQueryRun, isTransactionOperation } from "../../concept/action";
import { PageScaffoldComponent } from "../scaffold/page/page-scaffold.component";
import { SchemaToolWindowComponent } from "../schema/tool-window/schema-tool-window.component";
import { ChatMessageComponent, RunQueryEvent } from "./chat-message/chat-message.component";
import { ChatMessageData, ChatState, ConversationSummary } from "../../service/chat-state.service";
import { DriverState } from "../../service/driver-state.service";
import { HistoryWindowState } from "../../service/query-page-state.service";
import { AppData } from "../../service/app-data.service";
import { SnackbarService } from "../../service/snackbar.service";
import { DatabaseSelectDialogComponent } from "../database/select-dialog/database-select-dialog.component";

@Component({
    selector: "ts-chat-page",
    templateUrl: "./chat-page.component.html",
    styleUrls: ["./chat-page.component.scss"],
    imports: [
        AsyncPipe,
        DatePipe,
        RouterLink,
        FormsModule,
        ReactiveFormsModule,
        MatButtonModule,
        MatTooltipModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatMenuModule,
        TextFieldModule,
        ResizableDirective,
        PageScaffoldComponent,
        SchemaToolWindowComponent,
        ChatMessageComponent,
        CodeEditorComponent,
        SpinnerComponent,
        ActionDurationPipe,
        RichTooltipDirective,
    ],
})
export class ChatPageComponent implements OnInit, AfterViewInit {
    @ViewChild("messagesContainer") messagesContainer?: ElementRef<HTMLElement>;
    @ViewChild("promptInput") promptInput?: ElementRef<HTMLTextAreaElement>;

    conversationGroups: { label: string; conversations: ConversationSummary[] }[] = [];
    conversationRelativeTimes = new Map<string, string>();
    history: HistoryWindowState;
    showScrollToBottom = false;

    private historyIndex = -1;
    private historyDraft = "";
    private historyEntryControls = new Map<QueryRunAction, FormControl<string>>();

    constructor(
        public state: ChatState,
        public driver: DriverState,
        private appData: AppData,
        private dialog: MatDialog,
        private snackbar: SnackbarService,
        private zone: NgZone,
    ) {
        this.history = new HistoryWindowState(driver);
    }

    ngOnInit() {
        this.appData.viewState.setLastUsedTool("chat");

        this.state.conversations$.subscribe(convs => {
            this.conversationGroups = this.groupConversations(convs);
            this.conversationRelativeTimes = new Map(convs.map(c => [c.id, this.formatRelativeTime(c.updatedAt)]));
        });


        if (this.state.pendingMessage) {
            const message = this.state.pendingMessage;
            this.state.pendingMessage = null;
            setTimeout(() => this.onSendLogToAi(message));
        }
    }

    ngAfterViewInit() {
        this.promptInput?.nativeElement.focus();

        const container = this.messagesContainer?.nativeElement;
        if (container) {
            const updateScrollToBottom = () => {
                const distanceFromBottom = container.scrollHeight - container.scrollTop - container.clientHeight;
                const show = distanceFromBottom > 150;
                if (show !== this.showScrollToBottom) {
                    this.zone.run(() => this.showScrollToBottom = show);
                }
            };
            container.addEventListener('scroll', updateScrollToBottom, { passive: true });
            setTimeout(updateScrollToBottom);

            // When streaming ends, suppress smooth scrolling for the entire
            // DOM transition (filler removal + streaming-pair unwrap) so the
            // layout change doesn't cause a visible scroll animation.
            this.state.isProcessing$.subscribe(isProcessing => {
                if (!isProcessing) {
                    const fillers = container.querySelectorAll('.scroll-filler');
                    console.log('[stream-end] subscriber fired', {
                        scrollTop: container.scrollTop,
                        scrollHeight: container.scrollHeight,
                        clientHeight: container.clientHeight,
                        fillerCount: fillers.length,
                        hasStreamingPair: !!container.querySelector('.streaming-pair'),
                    });
                    const savedTop = container.scrollTop;
                    container.style.scrollBehavior = 'auto';
                    fillers.forEach(f => f.remove());
                    container.scrollTop = savedTop;
                    console.log('[stream-end] after cleanup', {
                        scrollTop: container.scrollTop,
                        scrollHeight: container.scrollHeight,
                    });
                    setTimeout(() => {
                        console.log('[stream-end] after setTimeout (before restore)', {
                            scrollTop: container.scrollTop,
                            scrollHeight: container.scrollHeight,
                            savedTop,
                            hasStreamingPair: !!container.querySelector('.streaming-pair'),
                        });
                        container.scrollTop = savedTop;
                        console.log('[stream-end] after setTimeout (after restore)', {
                            scrollTop: container.scrollTop,
                        });
                        container.style.scrollBehavior = '';
                        requestAnimationFrame(() => {
                            console.log('[stream-end] after rAF', {
                                scrollTop: container.scrollTop,
                                scrollHeight: container.scrollHeight,
                            });
                        });
                    });
                }
            });
        }
    }

    onScrollToBottom(): void {
        this.scrollToBottom();
    }

    private scrollToBottom(): void {
        if (this.messagesContainer) {
            const el = this.messagesContainer.nativeElement;
            el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
        }
    }

    private scheduleScrollToLastUserMessage(): void {
        setTimeout(() => this.scrollToLastUserMessage());
    }

    private scrollToLastUserMessage(): void {
        if (!this.messagesContainer) return;
        const container = this.messagesContainer.nativeElement;
        const userMessages = container.querySelectorAll('[data-message-id].user');
        const lastUserMsg = userMessages[userMessages.length - 1] as HTMLElement | undefined;
        if (lastUserMsg) {
            const containerRect = container.getBoundingClientRect();
            const msgRect = lastUserMsg.getBoundingClientRect();
            const msgAbsoluteTop = msgRect.top - containerRect.top + container.scrollTop;
            const maxVisibleHeight = 10 * 22; // ~10 lines at 22px line-height
            const targetScrollTop = msgRect.height > maxVisibleHeight
                ? msgAbsoluteTop + msgRect.height - maxVisibleHeight
                : msgAbsoluteTop;

            const maxScroll = container.scrollHeight - container.clientHeight;

            // Add scroll room inside the streaming-pair (whose content overflow
            // determines scrollHeight) so the target position is reachable
            if (targetScrollTop > maxScroll) {
                const streamingPair = container.querySelector('.streaming-pair');
                const target = streamingPair || container;
                const extraNeeded = Math.ceil(targetScrollTop - maxScroll) + 16;
                const filler = document.createElement('div');
                filler.className = 'scroll-filler';
                filler.style.height = `${extraNeeded}px`;
                filler.style.flexShrink = '0';
                target.appendChild(filler);
            }

            container.scrollTop = targetScrollTop;
        } else {
            container.scrollTop = container.scrollHeight;
        }
    }

    openSelectDatabaseDialog() {
        this.dialog.open(DatabaseSelectDialogComponent);
    }

    onSubmit(event: Event): void {
        event.preventDefault();
        if (this.handleSlashCommand()) return;
        if (this.state.isProcessing$.value) {
            this.state.cancelStream();
        }
        this.state.submitPrompt();
        this.scheduleScrollToLastUserMessage();
    }

    onInputKeyDownEnter(event: KeyboardEvent): void {
        if (!event.shiftKey) {
            event.preventDefault();
            if (this.handleSlashCommand()) {
                this.historyIndex = -1;
                this.historyDraft = "";
                return;
            }
            if (this.state.isProcessing$.value) {
                this.state.cancelStream();
            }
            this.state.submitPrompt();
            this.historyIndex = -1;
            this.historyDraft = "";
            this.scheduleScrollToLastUserMessage();
        }
    }

    private handleSlashCommand(): boolean {
        const prompt = this.state.promptControl.value.trim();
        if (prompt === "/clear") {
            this.state.promptControl.setValue("");
            this.clearChat();
            return true;
        }
        if (prompt === "/compact") {
            this.state.promptControl.setValue("");
            this.compactChat();
            return true;
        }
        return false;
    }

    onInputKeyDownArrow(event: KeyboardEvent): void {
        const textarea = event.target as HTMLTextAreaElement;
        const userMessages = this.state.messages$.value.filter(m => m.sender === "user");
        if (!userMessages.length) return;

        if (event.key === "ArrowUp" && textarea.selectionStart === 0 && textarea.selectionEnd === 0) {
            event.preventDefault();
            if (this.historyIndex === -1) {
                this.historyDraft = this.state.promptControl.value;
                this.historyIndex = userMessages.length - 1;
            } else if (this.historyIndex > 0) {
                this.historyIndex--;
            } else {
                return;
            }
            this.state.promptControl.setValue(this.extractMessageText(userMessages[this.historyIndex]));
            setTimeout(() => textarea.setSelectionRange(0, 0));
        } else if (event.key === "ArrowDown" && this.historyIndex >= 0) {
            const atEnd = textarea.selectionStart === textarea.value.length;
            if (!atEnd) return;
            event.preventDefault();
            if (this.historyIndex < userMessages.length - 1) {
                this.historyIndex++;
                this.state.promptControl.setValue(this.extractMessageText(userMessages[this.historyIndex]));
            } else {
                this.historyIndex = -1;
                this.state.promptControl.setValue(this.historyDraft);
            }
            setTimeout(() => textarea.setSelectionRange(0, 0));
        }
    }

    private extractMessageText(message: ChatMessageData): string {
        return message.content
            .filter(part => part.type === "text")
            .map(part => part.content)
            .join("\n");
    }

    onRunQuery(event: RunQueryEvent): void {
        this.state.executeQueryInMessage(event.messageId, event.blockIndex, event.query);
        setTimeout(() => {
            const el = this.messagesContainer?.nativeElement.querySelector(`[data-output-block="${event.messageId}-${event.blockIndex}"]`);
            el?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        });
    }

    onSendLogToAi(logText: string): void {
        this.state.promptControl.setValue(logText);
        this.state.submitPrompt();
        this.scheduleScrollToLastUserMessage();
    }

    getHistoryEntryControl(entry: QueryRunAction): FormControl<string> {
        let control = this.historyEntryControls.get(entry);
        if (!control) {
            control = new FormControl(entry.query, { nonNullable: true });
            this.historyEntryControls.set(entry, control);
        }
        return control;
    }

    runHistoryQuery(entry: QueryRunAction) {
        this.state.promptControl.setValue(entry.query);
        this.state.submitPrompt();
        setTimeout(() => this.scrollToBottom());
    }

    transactionOperationString(action: TransactionOperationAction) {
        switch (action.operation) {
            case "open": return "opened transaction";
            case "commit": return action.status === "error" ? "commit failed" : "committed";
            case "close": return "closed transaction";
        }
    }

    historyEntryErrorTooltip(entry: DriverAction) {
        if (!entry.result) return ``;
        else if ("err" in entry.result && !!entry.result.err?.message) return entry.result.err.message;
        else if ("message" in entry.result) return entry.result.message as string;
        else return entry.result.toString();
    }

    async copyHistoryEntryErrorTooltip(entry: DriverAction) {
        const tooltip = this.historyEntryErrorTooltip(entry);
        if (!tooltip) return;
        try {
            await navigator.clipboard.writeText(tooltip);
            this.snackbar.success("Error text copied", { duration: 2500 });
        } catch (e) {
            console.error('Failed to copy error text:', e);
        }
    }

    readonly isQueryRun = isQueryRun;
    readonly isTransactionOperation = isTransactionOperation;

    onStopStream(): void {
        this.state.cancelStream();
    }

    clearChat(): void {
        this.state.clearConversation();
    }

    compactChat(): void {
        this.state.compactConversation();
    }

    get selectedConversationTitle(): string {
        const convs = this.state.conversations$.value;
        const selectedId = this.state.selectedConversationId$.value;
        const selected = convs.find(c => c.id === selectedId);
        if (!selected) return "Past Conversations";
        const title = selected.title;
        if (title.length <= 30) return title;
        return title.substring(0, 30).trimEnd() + "...";
    }

    onNewConversation(): void {
        this.state.newConversation();
        setTimeout(() => this.promptInput?.nativeElement.focus());
    }

    formatRelativeTime(date: Date): string {
        const diffMs = Date.now() - date.getTime();
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);
        if (diffMins < 1) return "now";
        if (diffMins < 60) return `${diffMins}m`;
        if (diffHours < 24) return `${diffHours}h`;
        return `${diffDays}d`;
    }

    private groupConversations(conversations: ConversationSummary[]): { label: string; conversations: ConversationSummary[] }[] {
        const now = new Date();
        const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
        const startOfYesterday = startOfToday - 86400000;
        const startOfWeek = startOfToday - 7 * 86400000;
        const startOfMonth = startOfToday - 30 * 86400000;

        const buckets: { [key: string]: ConversationSummary[] } = {
            "Today": [], "Yesterday": [], "Past week": [], "Past month": [], "Older": [],
        };

        for (const conv of conversations) {
            const t = conv.updatedAt.getTime();
            if (t >= startOfToday) buckets["Today"].push(conv);
            else if (t >= startOfYesterday) buckets["Yesterday"].push(conv);
            else if (t >= startOfWeek) buckets["Past week"].push(conv);
            else if (t >= startOfMonth) buckets["Past month"].push(conv);
            else buckets["Older"].push(conv);
        }

        return Object.entries(buckets)
            .filter(([_, convs]) => convs.length > 0)
            .map(([label, convs]) => ({ label, conversations: convs }));
    }

    onSelectConversation(id: string): void {
        this.state.selectConversation(id);
    }

    onDeleteConversation(id: string): void {
        this.state.deleteConversation(id);
    }

    trackMessageById(index: number, message: { id: string }): string {
        return message.id;
    }
}
