/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, EventEmitter, Output } from "@angular/core";
import { AsyncPipe, DatePipe } from "@angular/common";
import { FormControl } from "@angular/forms";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatDialog } from "@angular/material/dialog";
import { CodeEditorComponent } from "../../../framework/code-editor/code-editor.component";
import { PersistedSavedQuery } from "../../../service/app-data.service";
import { SavedQueriesState } from "../../../service/saved-queries-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { SaveQueryDialogComponent, SaveQueryDialogData } from "../save-query-dialog/save-query-dialog.component";

@Component({
    selector: "ts-saved-queries-pane",
    templateUrl: "./saved-queries-pane.component.html",
    styleUrls: ["./saved-queries-pane.component.scss"],
    imports: [AsyncPipe, DatePipe, MatTooltipModule, CodeEditorComponent],
})
export class SavedQueriesPaneComponent {
    @Output() runSavedQuery = new EventEmitter<PersistedSavedQuery>();
    @Output() openSavedQuery = new EventEmitter<PersistedSavedQuery>();

    /** Stable per-entry FormControl so the CodeMirror editor doesn't unmount
     *  when entries re-shuffle on save/delete. Keyed by saved-query id. */
    private entryControls = new Map<string, FormControl<string>>();

    constructor(
        public state: SavedQueriesState,
        private dialog: MatDialog,
        private snackbar: SnackbarService,
    ) {}

    trackById(_: number, entry: PersistedSavedQuery): string { return entry.id; }

    getEntryControl(entry: PersistedSavedQuery): FormControl<string> {
        let control = this.entryControls.get(entry.id);
        if (!control) {
            control = new FormControl(entry.query, { nonNullable: true });
            this.entryControls.set(entry.id, control);
        } else if (control.value !== entry.query) {
            // Persisted query was edited externally — keep editor in sync.
            control.setValue(entry.query, { emitEvent: false });
        }
        return control;
    }

    onRun(entry: PersistedSavedQuery) {
        this.runSavedQuery.emit(entry);
    }

    onOpen(entry: PersistedSavedQuery) {
        this.openSavedQuery.emit(entry);
    }

    onRename(entry: PersistedSavedQuery) {
        const ref = this.dialog.open(SaveQueryDialogComponent, {
            data: { suggestedName: entry.name } as SaveQueryDialogData,
            width: "400px",
        });
        ref.afterClosed().subscribe((newName: string | undefined) => {
            if (newName && newName !== entry.name) {
                this.state.rename(entry.id, newName);
                this.snackbar.success("Query renamed");
            }
        });
    }

    onDelete(entry: PersistedSavedQuery) {
        this.state.remove(entry.id);
        this.snackbar.success(`Deleted "${entry.name}"`);
    }
}
