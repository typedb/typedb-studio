/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AfterViewChecked, AfterViewInit, Component, ElementRef, OnInit, ViewChild } from "@angular/core";
import { AsyncPipe } from "@angular/common";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
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
import { PageScaffoldComponent } from "../scaffold/page/page-scaffold.component";
import { SchemaToolWindowComponent } from "../schema/tool-window/schema-tool-window.component";
import { ChatMessageComponent, RunQueryEvent } from "./chat-message/chat-message.component";
import { ChatMessageData, ChatState, ConversationSummary } from "../../service/chat-state.service";
import { DriverState } from "../../service/driver-state.service";
import { AppData } from "../../service/app-data.service";
import { DatabaseSelectDialogComponent } from "../database/select-dialog/database-select-dialog.component";

@Component({
    selector: "ts-chat-page",
    templateUrl: "./chat-page.component.html",
    styleUrls: ["./chat-page.component.scss"],
    imports: [
        AsyncPipe,
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
    ],
})
export class ChatPageComponent implements OnInit, AfterViewInit, AfterViewChecked {
    @ViewChild("messagesContainer") messagesContainer?: ElementRef<HTMLElement>;
    @ViewChild("promptInput") promptInput?: ElementRef<HTMLTextAreaElement>;

    conversationGroups: { label: string; conversations: ConversationSummary[] }[] = [];

    private shouldScrollToBottom = false;
    private lastMessageCount = 0;
    private historyIndex = -1;
    private historyDraft = "";

    constructor(
        public state: ChatState,
        public driver: DriverState,
        private appData: AppData,
        private dialog: MatDialog,
    ) {}

    ngOnInit() {
        this.appData.viewState.setLastUsedTool("chat");

        this.state.conversations$.subscribe(convs => {
            this.conversationGroups = this.groupConversations(convs);
        });

        // Auto-scroll when new messages arrive
        this.state.messages$.subscribe(messages => {
            if (messages.length > this.lastMessageCount) {
                this.shouldScrollToBottom = true;
            }
            this.lastMessageCount = messages.length;
        });
    }

    ngAfterViewInit() {
        this.promptInput?.nativeElement.focus();
    }

    ngAfterViewChecked() {
        if (this.shouldScrollToBottom && this.messagesContainer) {
            this.scrollToBottom();
            this.shouldScrollToBottom = false;
        }
    }

    private scrollToBottom(): void {
        if (this.messagesContainer) {
            const el = this.messagesContainer.nativeElement;
            el.scrollTop = el.scrollHeight;
        }
    }

    openSelectDatabaseDialog() {
        this.dialog.open(DatabaseSelectDialogComponent);
    }

    onSubmit(event: Event): void {
        event.preventDefault();
        this.state.submitPrompt();
    }

    onInputKeyDownEnter(event: KeyboardEvent): void {
        if (!event.shiftKey) {
            event.preventDefault();
            this.state.submitPrompt();
            this.historyIndex = -1;
            this.historyDraft = "";
        }
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
