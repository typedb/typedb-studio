/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";
import { MarkdownComponent } from "ngx-markdown";
import { CodeEditorComponent } from "../../../framework/code-editor/code-editor.component";
import { SpinnerComponent } from "../../../framework/spinner/spinner.component";
import { ChatMessageData } from "../../../service/chat-state.service";
import { ChatOutputComponent } from "../chat-output/chat-output.component";

export interface RunQueryEvent {
    messageId: string;
    blockIndex: number;
    query: string;
}

@Component({
    selector: "ts-chat-message",
    templateUrl: "./chat-message.component.html",
    styleUrls: ["./chat-message.component.scss"],
    imports: [
        MarkdownComponent,
        CodeEditorComponent,
        SpinnerComponent,
        ChatOutputComponent,
    ],
})
export class ChatMessageComponent {
    @Input({ required: true }) message!: ChatMessageData;
    @Output() runQuery = new EventEmitter<RunQueryEvent>();
    @Output() sendLogToAi = new EventEmitter<string>();

    onRunClick(blockIndex: number, query: string): void {
        this.runQuery.emit({
            messageId: this.message.id,
            blockIndex,
            query,
        });
    }
}
