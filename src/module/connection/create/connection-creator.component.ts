/*
 * This unpublished material is proprietary to Vaticle.
 * All rights reserved. The methods and
 * techniques described herein are considered trade secrets
 * and/or confidential. Reproduction or distribution, in whole
 * or in part, is forbidden except by express written permission
 * of Vaticle.
 */

import { AsyncPipe, Location, NgTemplateOutlet } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";
import { Connection } from "../../../concept";
import { SpinnerComponent } from "../../../framework/spinner/spinner.component";
import { RichTooltipDirective } from "../../../framework/tooltip/rich-tooltip.directive";
import { AppDataService } from "../../../service/app-data.service";
import { PageScaffoldComponent, ResourceAvailability } from "../../scaffold/page/page-scaffold.component";
import { Router } from "@angular/router";
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidatorFn } from "@angular/forms";
import { BehaviorSubject } from "rxjs";
import { AnalyticsService } from "../../../service/analytics.service";
import { FormActionsComponent, FormComponent, FormInputComponent, FormOption, FormPasswordInputComponent, FormSelectComponent, FormToggleGroupComponent, requiredValidator } from "../../../framework/form";
import { CONNECTION_URI_PLACEHOLDER, parseConnectionUriOrNull } from "../connection";

const connectionUriValidator: ValidatorFn = (control: AbstractControl) => {
    if (parseConnectionUriOrNull(control.value)) return null;
    else return { errorText: `Enter a valid connection URI, or use advanced configuration` };
};

@Component({
    selector: "tp-connection-creator",
    templateUrl: "./connection-creator.component.html",
    styleUrls: ["./connection-creator.component.scss"],
    standalone: true,
    imports: [
        PageScaffoldComponent, AsyncPipe, MatFormFieldModule, MatSelectModule, MatTooltipModule,
        ReactiveFormsModule, FormInputComponent, FormComponent, FormActionsComponent,
        FormToggleGroupComponent, MatButtonModule, MatInputModule, RichTooltipDirective,
    ],
})
export class ConnectionCreatorComponent implements OnInit {

    readonly availability$ = new BehaviorSubject<ResourceAvailability>("ready");
    savedConnections = this.appData.connections.list();
    advancedConfigActiveOptions: FormOption<boolean>[] = [
        { value: false, viewValue: `Connection URI` },
        { value: true, viewValue: `Advanced config` },
    ];
    connectionUriRevealed = false;
    connectionUriPlaceholder = CONNECTION_URI_PLACEHOLDER;

    readonly form = this.formBuilder.group({
        name: ["", [(control: AbstractControl) => this.connectionNameUniqueValidator(control)]],
        advancedConfigActive: [this.appData.preferences.connection.showAdvancedConfigByDefault(), [requiredValidator]],
        uri: ["", [requiredValidator, connectionUriValidator]],
        saveConnectionDetails: [false, [requiredValidator]],
    });
    readonly isSubmitting$ = new BehaviorSubject(false);

    constructor(
        private formBuilder: FormBuilder, private router: Router, private appData: AppDataService,
        private analytics: AnalyticsService, private location: Location,
    ) {}

    ngOnInit() {
    }

    get advancedConfigActive() {
        return this.form.value.advancedConfigActive === true;
    }

    submit() {
        // this.clusterApi.deployCluster(cluster).subscribe({
        //     next: () => {
        //         this.analytics.google.reportAdConversion(cluster.machineType.isFree ? "deployFreeCluster" : "deployPaidCluster");
        //         this.router.navigate([clusterDetailsPath(cluster, this.org)], { queryParams: { [DIALOG]: CONNECT, [SETUP]: TRUE } });
        //     },
        //     error: () => {
        //         this.isSubmitting$.next(false);
        //     }
        // });
    }

    cancel() {
        this.location.back();
    }

    // isIdValid(id: string) {
    //     return !this.form.controls.id.validator?.call(this, new FormControl(id));
    // }

    private connectionNameUniqueValidator(control: AbstractControl<string>) {
        if (this.savedConnections.some(x => x.name === control.value)) {
            return { errorText: `A connection named '${control.value}' already exists` };
        } else return null;
    }
}
