/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe } from "@angular/common";
import { Component } from "@angular/core";
import { FormBuilder, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatDialogRef } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { Database } from "@samuel-butcher-typedb/typedb-http-driver";
import { map, Observable, Subject } from "rxjs";
import { FormActionsComponent, FormComponent, FormOption, FormSelectComponent, requiredValidator } from "../../../framework/form";
import { ModalComponent } from "../../../framework/modal";
import { DriverState } from "../../../service/driver-state.service";
import { SnackbarService } from "../../../service/snackbar.service";

@Component({
    selector: "ts-database-select-dialog",
    templateUrl: "./database-select-dialog.component.html",
    styleUrls: ["./database-select-dialog.component.scss"],
    imports: [
        ModalComponent, AsyncPipe, FormsModule, ReactiveFormsModule, MatFormFieldModule,
        MatInputModule, FormComponent, FormActionsComponent, FormSelectComponent,
    ]
})
export class DatabaseSelectDialogComponent {

    readonly isSubmitting$ = new Subject<boolean>();
    readonly databases$: Observable<FormOption<Database>[] | null> = this.driver.databaseList$.pipe(map(x => x == null ? null : x.map(x =>({
        value: x,
        viewValue: x.name,
    }))));
    readonly form = this.formBuilder.group({
        database: [null as Database | null, [requiredValidator]],
    });
    errorLines: string[] = [];

    constructor(
        private dialogRef: MatDialogRef<DatabaseSelectDialogComponent>,
        private formBuilder: FormBuilder, private snackbar: SnackbarService, private driver: DriverState,
    ) {
    }

    submit() {
        const database = this.form.value.database!;
        this.driver.selectDatabase(database);
        this.close();
        this.snackbar.info(`Now using database '${database.name}'`);
    }

    close() {
        this.dialogRef.close();
    }
}
