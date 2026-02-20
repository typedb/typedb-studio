/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AfterViewChecked, AfterViewInit, Component, ElementRef, EventEmitter, Input, OnDestroy, Output, ViewChild } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatButtonToggleModule } from "@angular/material/button-toggle";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatTableModule } from "@angular/material/table";
import { MatSortModule } from "@angular/material/sort";
import { Subscription } from "rxjs";
import { OutputState, OutputType } from "../../../service/chat-state.service";
import { RunOutputState } from "../../../service/query-page-state.service";

@Component({
    selector: "ts-chat-output",
    templateUrl: "./chat-output.component.html",
    styleUrls: ["./chat-output.component.scss"],
    imports: [
        FormsModule,
        ReactiveFormsModule,
        MatButtonModule,
        MatButtonToggleModule,
        MatFormFieldModule,
        MatInputModule,
        MatTableModule,
        MatSortModule,
    ],
})
export class ChatOutputComponent implements AfterViewInit, AfterViewChecked, OnDestroy {
    @Input({ required: true }) outputState!: OutputState;
    @Output() sendLogToAi = new EventEmitter<string>();
    @ViewChild("graphViewRef") graphViewRef?: ElementRef<HTMLElement>;

    outputTypes: OutputType[] = ["log", "table", "graph", "raw"];
    copied = false;
    aiSent = false;
    private outputTypeSub?: Subscription;
    private lastAttachedRun: RunOutputState | null = null;

    get currentRun(): RunOutputState | null {
        const { runs, selectedRunIndex } = this.outputState;
        if (selectedRunIndex < 0 || selectedRunIndex >= runs.length) return null;
        return runs[selectedRunIndex];
    }

    ngAfterViewInit() {
        this.attachCanvasIfNeeded();
        this.outputTypeSub = this.outputState.outputTypeControl.valueChanges.subscribe((value) => {
            if (value === "graph") {
                requestAnimationFrame(() => this.currentRun?.graph.resize());
            }
        });
    }

    ngAfterViewChecked() {
        this.attachCanvasIfNeeded();
    }

    private attachCanvasIfNeeded() {
        const run = this.currentRun;
        if (run && run !== this.lastAttachedRun && this.graphViewRef) {
            run.graph.attach(this.graphViewRef.nativeElement);
            this.lastAttachedRun = run;
            requestAnimationFrame(() => run.graph.resize());
        }
    }

    selectRun(index: number) {
        const { runs, selectedRunIndex } = this.outputState;
        if (index < 0 || index >= runs.length) return;
        if (index === selectedRunIndex) return;

        const oldRun = this.currentRun;
        if (oldRun) oldRun.graph.detach();

        this.outputState.selectedRunIndex = index;

        const newRun = this.currentRun;
        if (newRun && this.graphViewRef) {
            newRun.graph.attach(this.graphViewRef.nativeElement);
        }
    }

    closeRun(event: Event, index: number) {
        event.stopPropagation();
        const { runs } = this.outputState;
        if (index < 0 || index >= runs.length) return;

        const runToClose = runs[index];

        if (index === this.outputState.selectedRunIndex) {
            runToClose.graph.detach();
        }

        runToClose.graph.destroy();
        runs.splice(index, 1);

        if (runs.length === 0) {
            this.outputState.selectedRunIndex = -1;
        } else if (index < this.outputState.selectedRunIndex) {
            this.outputState.selectedRunIndex--;
        } else if (index === this.outputState.selectedRunIndex) {
            this.outputState.selectedRunIndex = Math.min(index, runs.length - 1);
            const newRun = this.currentRun;
            if (newRun && this.graphViewRef) {
                newRun.graph.attach(this.graphViewRef.nativeElement);
            }
        }
    }

    onCopyLogClick() {
        const run = this.currentRun;
        if (!run) return;
        navigator.clipboard.writeText(run.log.control.value);
        this.copied = true;
        setTimeout(() => this.copied = false, 3000);
    }

    onAiClick() {
        const run = this.currentRun;
        if (!run) return;
        this.sendLogToAi.emit(run.log.control.value);
        this.aiSent = true;
        setTimeout(() => this.aiSent = false, 3000);
    }

    ngOnDestroy() {
        this.outputTypeSub?.unsubscribe();
        for (const run of this.outputState.runs) {
            run.graph.detach();
        }
        this.lastAttachedRun = null;
    }
}
