/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component, inject } from "@angular/core";
import { MatDialogRef } from "@angular/material/dialog";
import { MatInputModule } from "@angular/material/input";
import { MatButtonModule } from "@angular/material/button";
import { ModalComponent } from "../../../framework/modal";
import { DriverState } from "../../../service/driver-state.service";
import { CodeSnippetComponent } from "../../../framework/code-snippet/code-snippet.component";
import { filter, map, Observable, tap } from "rxjs";
import { isOkResponse } from "@typedb/driver-http";

@Component({
    selector: "ts-schema-text-dialog",
    templateUrl: "./schema-text-dialog.component.html",
    styleUrls: ["./schema-text-dialog.component.scss"],
    imports: [
        ModalComponent, AsyncPipe, MatInputModule, CodeSnippetComponent, MatButtonModule
    ]
})
export class SchemaTextDialogComponent {

    private readonly dialogRef = inject(MatDialogRef<SchemaTextDialogComponent>);
    private readonly driver = inject(DriverState);

    readonly title = `Full schema text [${this.driver.requireDatabase().name}]`;

    copied = false;
    private schemaText = "";

    schemaTextSnippet$: Observable<CodeSnippetComponent["snippet"]> = this.driver.getDatabaseSchemaText().pipe(
        filter((res) => isOkResponse(res)),
        tap((res) => { this.schemaText = res.ok; }),
        map((res) => ({ language: "typeql", code: res.ok }))
    );

    copySchemaText() {
        navigator.clipboard.writeText(this.schemaText);
        this.copied = true;
        setTimeout(() => this.copied = false, 1500);
    }

    close() {
        this.dialogRef.close();
    }
}
