/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnDestroy, Output, ViewChild } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatTableModule } from "@angular/material/table";
import { MatSortModule } from "@angular/material/sort";
import { Subscription } from "rxjs";
import { OutputState, OutputType } from "../../../service/chat-state.service";

@Component({
    selector: "ts-chat-output",
    templateUrl: "./chat-output.component.html",
    styleUrls: ["./chat-output.component.scss"],
    imports: [
        FormsModule,
        ReactiveFormsModule,
        MatButtonToggleModule,
        MatFormFieldModule,
        MatInputModule,
        MatTableModule,
        MatSortModule,
    ],
})
export class ChatOutputComponent implements AfterViewInit, OnDestroy {
    @Input({ required: true }) outputState!: OutputState;
    @Output() sendLogToAi = new EventEmitter<string>();
    @ViewChild("graphViewRef") graphViewRef?: ElementRef<HTMLElement>;

    outputTypes: OutputType[] = ["log", "table", "graph", "raw"];
    copied = false;
    aiSent = false;
    private outputTypeSub?: Subscription;

    ngAfterViewInit() {
        if (this.graphViewRef && this.outputState) {
            this.outputState.graph.canvasEl = this.graphViewRef.nativeElement;
        }
        this.outputTypeSub = this.outputState.outputTypeControl.valueChanges.subscribe((value) => {
            if (value === "graph") {
                requestAnimationFrame(() => this.outputState.graph.resize());
            }
        });
    }

    onCopyLogClick() {
        navigator.clipboard.writeText(this.outputState.log.control.value);
        this.copied = true;
        setTimeout(() => this.copied = false, 3000);
    }

    onAiClick() {
        this.sendLogToAi.emit(this.outputState.log.control.value);
        this.aiSent = true;
        setTimeout(() => this.aiSent = false, 3000);
    }

    ngOnDestroy() {
        this.outputTypeSub?.unsubscribe();
        this.outputState?.graph.destroy();
    }
}
