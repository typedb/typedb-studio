/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe, DatePipe } from "@angular/common";
import { AfterViewChecked, Component, ElementRef, ViewChild } from "@angular/core";
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatSortModule } from "@angular/material/sort";
import { MatTableModule } from "@angular/material/table";
import { MatTreeModule } from "@angular/material/tree";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatMenuModule } from "@angular/material/menu";
import { TextFieldModule } from '@angular/cdk/text-field';
import { CodeSnippetComponent } from "../../framework/code-snippet/code-snippet.component";
import { SpinnerComponent } from "../../framework/spinner/spinner.component";
import { AIAssistToolWindowState } from "../../service/ai-assist-tool-window-state.service";
import { DriverState } from "../../service/driver-state.service";

@Component({
    selector: "ts-ai-assist-tool-window",
    templateUrl: "ai-assist-tool-window.component.html",
    styleUrls: ["ai-assist-tool-window.component.scss"],
    standalone: true,
    imports: [
        CommonModule, AsyncPipe, DatePipe, FormsModule, ReactiveFormsModule, MatFormFieldModule,
        MatInputModule, MatButtonModule, MatIconModule, MatTooltipModule, MatProgressSpinnerModule,
        TextFieldModule, SpinnerComponent, CodeSnippetComponent
    ]
})
export class AIAssistToolWindowComponent {

    @ViewChild('messagesContainer') private messagesContainer!: ElementRef<HTMLDivElement>;

    constructor(public state: AIAssistToolWindowState, public driver: DriverState) {
        this.state.messages$.subscribe(() => {
            setTimeout(() => {
                this.scrollToBottom();
            });
        });
    }

    onInputKeyDownEnter(event: KeyboardEvent) {
        if (event.shiftKey) return;
        this.onSubmit(event);
    }

    onSubmit(event: Event) {
        event.preventDefault();
        if (this.state.promptControl.value && !this.state.isProcessing$.value && this.driver.database$.value) {
            this.state.submitPrompt();
        }
    }

    private scrollToBottom(): void {
        try {
            this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
        } catch (err) {
            console.error('Error scrolling to bottom:', err);
        }
    }
}
