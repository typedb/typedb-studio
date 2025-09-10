/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe, Location } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";
import { DriverParams, isApiErrorResponse, isBasicParams } from "typedb-driver-http";
import { CONNECTION_URL_PLACEHOLDER, ConnectionConfig, connectionUrl, parseConnectionUrlOrNull } from "../../../concept/connection";
import { RichTooltipDirective } from "../../../framework/tooltip/rich-tooltip.directive";
import { INTERNAL_ERROR } from "../../../framework/util/strings";
import { ADDRESS, NAME, USERNAME } from "../../../framework/util/url-params";
import { AppData } from "../../../service/app-data.service";
import { DriverState } from "../../../service/driver-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { PageScaffoldComponent, ResourceAvailability } from "../../scaffold/page/page-scaffold.component";
import { ActivatedRoute, Router } from "@angular/router";
import { AbstractControl, FormBuilder, FormControl, ReactiveFormsModule, ValidatorFn } from "@angular/forms";
import { BehaviorSubject, combineLatest, filter, first, map, tap } from "rxjs";
import { FormActionsComponent, FormComponent, FormInputComponent, FormOption, FormToggleGroupComponent, requiredValidator } from "../../../framework/form";

const connectionUrlValidator: ValidatorFn = (control: AbstractControl<string>) => {
    if (parseConnectionUrlOrNull(control.value)) return null;
    else return { errorText: `Format: typedb://username:password@address` };
};

const addressValidator: ValidatorFn = (control: AbstractControl<string>) => {
    if (control.value.startsWith(`http://`) || control.value.startsWith(`https://`)) return null;
    else return { errorText: `Please specify http:// or https://` };
}

@Component({
    selector: "tp-connection-creator",
    templateUrl: "./connection-creator.component.html",
    styleUrls: ["./connection-creator.component.scss"],
    imports: [
        PageScaffoldComponent, AsyncPipe, MatFormFieldModule, MatSelectModule, MatTooltipModule,
        ReactiveFormsModule, FormInputComponent, FormComponent, FormActionsComponent,
        FormToggleGroupComponent, MatButtonModule, MatInputModule, RichTooltipDirective, MatCheckboxModule,
    ]
})
export class ConnectionCreatorComponent {

    readonly availability$ = new BehaviorSubject<ResourceAvailability>("ready");
    savedConnections = this.appData.connections.list();
    advancedConfigActiveOptions: FormOption<boolean>[] = [
        { value: true, viewValue: `Use address and credentials` },
        { value: false, viewValue: `Use connection URL` },
    ];
    connectionUrlRevealed = true;
    connectionUrlPlaceholder = CONNECTION_URL_PLACEHOLDER;
    passwordRevealed = false;

    readonly form = this.formBuilder.group({
        name: ["", [requiredValidator]],
        advancedConfigActive: [this.appData.preferences.connection.showAdvancedConfigByDefault(), [requiredValidator]],
        url: ["", [requiredValidator, connectionUrlValidator]],
        saveConnectionDetails: [false, [requiredValidator]],
    });
    // TODO: support multiple addresses
    readonly advancedForm = this.formBuilder.group({
        address: ["", [requiredValidator, addressValidator]],
        username: ["", [requiredValidator]],
        password: ["", [requiredValidator]],
    });
    readonly isSubmitting$ = new BehaviorSubject(false);

    constructor(
        private formBuilder: FormBuilder, private appData: AppData,
        private driver: DriverState, private snackbar: SnackbarService, private location: Location,
        private router: Router, route: ActivatedRoute,
    ) {
        (window as any).connectionCreator = this;

        this.updateAdvancedConfigOnUrlChanges();
        this.updateUrlOnAdvancedConfigChanges();

        route.queryParamMap.pipe(first()).subscribe((params) => {
            if (params.get(NAME)) this.form.patchValue({ name: params.get(NAME) ?? `` });

            this.advancedForm.patchValue({
                address: params.get(ADDRESS) ?? ``,
                username: params.get(USERNAME) ?? ``,
            });
        });
    }

    private updateAdvancedConfigOnUrlChanges() {
        combineLatest([this.form.controls.url.valueChanges]).pipe(
            filter(([url]) => !this.form.controls.name.dirty && !!url),
            map(([url]) => parseConnectionUrlOrNull(url!)),
            filter(params => !!params),
            map(params => params!)
        ).subscribe((params) => {
            this.advancedForm.patchValue({
                address: isBasicParams(params) ? params.addresses[0] : params.translatedAddresses[0].external,
                username: params.username,
                password: params.password,
            }, { emitEvent: false });
        });
    }

    private updateUrlOnAdvancedConfigChanges() {
        this.advancedForm.valueChanges.pipe(
            map((value) => {
                const params: DriverParams = {
                    addresses: [value.address || ""],
                    username: value.username || "",
                    password: value.password || "",
                };
                return params;
            }),
            tap((params) => {
                if (!params.addresses[0]?.length || !params.username) return;
            }),
            map((params) => connectionUrl(params)),
        ).subscribe(url => {
            this.form.patchValue({ url }, { emitEvent: false });
        });
    }

    get canSubmit() {
        return (this.form.dirty || this.advancedForm.dirty) && this.form.valid;
    }

    private buildConnectionConfigOrNull(): ConnectionConfig | null {
        if (this.form.invalid || !this.form.value.url) return null;
        const connectionParams = parseConnectionUrlOrNull(this.form.value.url);
        if (!connectionParams) return null;
        return new ConnectionConfig({
            name: this.form.value.name || ConnectionConfig.autoName(connectionParams),
            params: connectionParams,
            preferences: {
                isStartupConnection: true
            },
        });
    }

    submit() {
        const config = this.buildConnectionConfigOrNull();
        if (!config) throw INTERNAL_ERROR;
        this.form.disable();
        this.driver.tryConnect(config).subscribe({
            next: () => {
                this.snackbar.success(`Connected to ${config.name}`);
                this.router.navigate([this.appData.viewState.lastUsedToolRoute()]).then((navigated) => {
                    if (!navigated) throw INTERNAL_ERROR;
                });
            },
            error: (err) => {
                if (typeof err === "object" && "customError" in err) {
                    this.snackbar.errorPersistent(`${err.customError}`);
                } else {
                    if (isApiErrorResponse(err)) {
                        this.snackbar.errorPersistent(`Error: ${err.err.message}`);
                    } else {
                        const msg = err?.message ?? err?.toString() ?? `Unknown error`;
                        this.snackbar.errorPersistent(`Error: ${msg}\n`
                            + `Unable to connect to TypeDB server '${config.name}'.\n`
                            + `Ensure the parameters are correct, the server is running, and its version is at least TypeDB 3.3.0.`);
                    }
                }
                this.form.enable();
                this.isSubmitting$.next(false);
            },
        });
    }

    cancel() {
        this.router.navigate(["/"]);
    }

    private connectionNameUniqueValidator(control: AbstractControl<string>) {
        if (this.savedConnections.some(x => x.name === control.value)) {
            return { errorText: `A connection named '${control.value}' already exists` };
        } else return null;
    }
}
