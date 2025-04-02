/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { AsyncPipe } from "@angular/common";
import { Component, Inject } from "@angular/core";
import { FormBuilder, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { Subject } from "rxjs";
import { FormActionsComponent, FormComponent, FormInputComponent, ModalComponent, patternValidator, requiredValidator } from "typedb-platform-framework";
import { Cluster } from "../../../concept/cluster";
import { clusterDetailsPath } from "../../../routing/resource-paths";
import { ClusterApi } from "../../../service/cluster/cluster-api.service";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { ApplicationState } from "../../../service/application-state.service";
import { DialogResult } from "../../../framework/modal/dialog-result";
import { SnackbarService } from "../../../service/snackbar.service";
import { idPattern, idPatternErrorText } from "typedb-web-common/lib";
import { Router } from "@angular/router";

export type ClusterEditDialogData = { cluster: Cluster };

@Component({
    selector: "tp-cluster-update-dialog",
    templateUrl: "./cluster-update-dialog.component.html",
    standalone: true,
    imports: [
        ModalComponent, AsyncPipe, FormInputComponent, FormActionsComponent, FormsModule, ReactiveFormsModule,
        FormComponent, MatFormFieldModule, MatInputModule
    ],
})
export class ConnectionEditorComponent {
    readonly isSubmitting$ = new Subject<boolean>();
    readonly form = this.formBuilder.nonNullable.group({
        id: [this.data.cluster.id, [patternValidator(idPattern, idPatternErrorText), requiredValidator]],
    });

    constructor(
        private dialogRef: MatDialogRef<ConnectionEditorComponent, DialogResult>, private formBuilder: FormBuilder,
        @Inject(MAT_DIALOG_DATA) private data: ClusterEditDialogData, private clusterApi: ClusterApi,
        private app: ApplicationState, private router: Router, private snackbar: SnackbarService,
    ) {
    }

    submit() {
        if (this.form.value.id !== this.data.cluster.id) {
            this.clusterApi.updateCluster({ uuid: this.data.cluster.uuid, id: this.form.value.id }).subscribe({
                next: () => {
                    this.close("ok");
                    this.router.navigate([clusterDetailsPath({ id: this.form.value.id!, project: this.data.cluster.project }, this.app.requireCurrentOrg())], { replaceUrl: true });
                    this.snackbar.success(`Cluster updated successfully`);
                },
                error: () => {
                    this.isSubmitting$.next(false);
                },
            });
        } else {
            this.close();
        }
    }

    close(result?: DialogResult) {
        this.dialogRef.close(result);
    }
}
