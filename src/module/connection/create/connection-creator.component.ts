/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe, Location } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";
import { CONNECTION_URL_PLACEHOLDER, ConnectionConfig, parseConnectionUrlOrNull } from "../../../concept/connection";
import { RichTooltipDirective } from "../../../framework/tooltip/rich-tooltip.directive";
import { AppDataService } from "../../../service/app-data.service";
import { ConnectionStateService } from "../../../service/connection-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { PageScaffoldComponent, ResourceAvailability } from "../../scaffold/page/page-scaffold.component";
import { Router } from "@angular/router";
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidatorFn } from "@angular/forms";
import { BehaviorSubject, combineLatest, filter, map, startWith, switchMap, tap } from "rxjs";
import { FormActionsComponent, FormComponent, FormInputComponent, FormOption, FormToggleGroupComponent, requiredValidator } from "../../../framework/form";

const connectionUrlValidator: ValidatorFn = (control: AbstractControl) => {
    if (parseConnectionUrlOrNull(control.value)) return null;
    else return { errorText: `Enter a valid connection URL, or use advanced configuration` };
};

@Component({
    selector: "tp-config-creator",
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
        { value: false, viewValue: `Connection URL` },
        { value: true, viewValue: `Advanced config` },
    ];
    connectionUrlRevealed = false;
    connectionUrlPlaceholder = CONNECTION_URL_PLACEHOLDER;

    readonly form = this.formBuilder.group({
        name: ["", [(control: AbstractControl) => this.connectionNameUniqueValidator(control)]],
        advancedConfigActive: [this.appData.preferences.connection.showAdvancedConfigByDefault(), [requiredValidator]],
        url: ["", [requiredValidator, connectionUrlValidator]],
        saveConnectionDetails: [false, [requiredValidator]],
    });
    readonly isSubmitting$ = new BehaviorSubject(false);

    constructor(
        private formBuilder: FormBuilder, private appData: AppDataService,
        private connectionState: ConnectionStateService, private snackbar: SnackbarService, private location: Location,
        private router: Router,
    ) {}

    ngOnInit() {
        combineLatest([this.form.controls.url.valueChanges]).pipe(
            filter(([url]) => !this.form.controls.name.dirty && !!url),
            map(([url]) => parseConnectionUrlOrNull(url!)),
            filter(params => !!params),
            map(params => params!)
        ).subscribe((params) => {
            if (!this.form.controls.name.dirty) this.form.patchValue({ name: ConnectionConfig.autoName(params) });
        });
    }

    get advancedConfigActive() {
        return this.form.value.advancedConfigActive === true;
    }

    private buildConnectionConfigOrNull(): ConnectionConfig | null {
        if (this.form.invalid || !this.form.value.url) return null;
        const connectionParams = parseConnectionUrlOrNull(this.form.value.url);
        if (!connectionParams) return null;
        return new ConnectionConfig({
            name: this.form.value.name || ConnectionConfig.autoName(connectionParams),
            params: connectionParams,
            preferences: {
                autoReconnectOnAppStartup: false // TODO
            },
        });
    }

    submit() {
        const config = this.buildConnectionConfigOrNull();
        if (!config) throw `Internal error`;
        this.form.disable();
        this.connectionState.init(config).subscribe({
            next: () => {
                this.snackbar.success(`Connected`);
                this.router.navigate([this.lastUsedToolRoute()]).then((navigated) => {
                    if (!navigated) throw `Internal error`;
                });
            },
            error: (err) => {
                const msg = err?.message || err?.toString() || `Unknown error`;
                this.snackbar.errorPersistent(`Error: ${msg}\n`
                    + `Caused: Unable to connect to TypeDB server.\n`
                    + `Ensure the connection parameters are correct and the server is running.`);
                this.form.enable();
                this.isSubmitting$.next(false);
            },
        });
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

    private lastUsedToolRoute(): string {
        const lastUsedTool = this.appData.viewState.lastUsedTool();
        return lastUsedTool === "query" ? `/query` : `/explore`;
    }

    cancel() {
        this.location.back();
    }

    private connectionNameUniqueValidator(control: AbstractControl<string>) {
        if (this.savedConnections.some(x => x.name === control.value)) {
            return { errorText: `A connection named '${control.value}' already exists` };
        } else return null;
    }
}
