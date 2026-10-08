/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, EventEmitter, Output } from "@angular/core";
import { AsyncPipe, DatePipe } from "@angular/common";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatDialog } from "@angular/material/dialog";
import { StaticCodeComponent } from "../../../framework/code-editor/static-code.component";
import { QueryTextDialogComponent } from "../../../framework/query-text-dialog/query-text-dialog.component";
import { PersistedSavedQuery } from "../../../service/app-data.service";
import { SavedQueriesState } from "../../../service/saved-queries-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { SaveQueryDialogComponent, SaveQueryDialogData } from "../save-query-dialog/save-query-dialog.component";

@Component({
    selector: "ts-saved-queries-pane",
    templateUrl: "./saved-queries-pane.component.html",
    styleUrls: ["./saved-queries-pane.component.scss"],
    imports: [AsyncPipe, DatePipe, MatTooltipModule, StaticCodeComponent],
})
export class SavedQueriesPaneComponent {
    @Output() runSavedQuery = new EventEmitter<PersistedSavedQuery>();
    @Output() openSavedQuery = new EventEmitter<PersistedSavedQuery>();

    constructor(
        public state: SavedQueriesState,
        private dialog: MatDialog,
        private snackbar: SnackbarService,
    ) {}

    trackById(_: number, entry: PersistedSavedQuery): string { return entry.id; }

    onRun(entry: PersistedSavedQuery) {
        this.runSavedQuery.emit(entry);
    }

    onViewFull(entry: PersistedSavedQuery) {
        this.dialog.open(QueryTextDialogComponent, {
            data: { query: entry.query },
            width: "800px",
        });
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
