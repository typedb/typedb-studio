/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AfterViewInit, Component, ElementRef, Input, OnDestroy, ViewChild } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatTableModule } from "@angular/material/table";
import { MatSortModule } from "@angular/material/sort";
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
    @ViewChild("graphViewRef") graphViewRef?: ElementRef<HTMLElement>;

    outputTypes: OutputType[] = ["log", "table", "graph", "raw"];

    ngAfterViewInit() {
        if (this.graphViewRef && this.outputState) {
            this.outputState.graph.canvasEl = this.graphViewRef.nativeElement;
        }
    }

    ngOnDestroy() {
        this.outputState?.graph.destroy();
    }
}
