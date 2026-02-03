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
import { ChatState } from "../../service/chat-state.service";
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

    private shouldScrollToBottom = false;
    private lastMessageCount = 0;

    constructor(
        public state: ChatState,
        public driver: DriverState,
        private appData: AppData,
        private dialog: MatDialog,
    ) {}

    ngOnInit() {
        this.appData.viewState.setLastUsedTool("chat");

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
        }
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

    trackMessageById(index: number, message: { id: string }): string {
        return message.id;
    }
}
