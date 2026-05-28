/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AsyncPipe, Location } from "@angular/common";
import { Component, OnInit, ViewChild } from "@angular/core";
import { MatAutocompleteModule, MatAutocompleteTrigger } from "@angular/material/autocomplete";
import { MatButtonModule } from "@angular/material/button";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatDialog } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";
import { DriverParams, isApiErrorResponse, isBasicParams } from "@typedb/driver-http";
import { CONNECTION_STRING_PLACEHOLDER, ConnectionConfig, connectionString, parseConnectionStringOrNull } from "../../../concept/connection";
import { RichTooltipDirective } from "../../../framework/tooltip/rich-tooltip.directive";
import { INTERNAL_ERROR } from "../../../framework/util/strings";
import { ADDRESS, NAME, USERNAME } from "../../../framework/util/url-params";
import { AppData } from "../../../service/app-data.service";
import { DriverState } from "../../../service/driver-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { PageScaffoldComponent, ResourceAvailability } from "../../scaffold/page/page-scaffold.component";
import { ActivatedRoute, Router } from "@angular/router";
import { AbstractControl, FormBuilder, FormControl, ReactiveFormsModule, ValidatorFn } from "@angular/forms";
import { BehaviorSubject, combineLatest, filter, first, map, merge, Observable, startWith, tap } from "rxjs";
import { FormActionsComponent, FormComponent, FormInputComponent, FormOption, FormToggleGroupComponent, requiredValidator } from "../../../framework/form";
import { ConnectionStringEditorDialogComponent } from "./connection-string-editor-dialog/connection-string-editor-dialog.component";

const connectionStringValidator: ValidatorFn = (control: AbstractControl<string>) => {
    const params = parseConnectionStringOrNull(control.value);
    if (!params) return { errorText: `Format: typedb://username:password@address` };
    const addresses = isBasicParams(params) ? params.addresses : params.translatedAddresses.map(x => x.external);
    if (addresses.some(addr => !addr.startsWith(`http://`) && !addr.startsWith(`https://`))) {
        return { errorText: `Address must include http:// or https://` };
    }
    return null;
};

const addressValidator: ValidatorFn = (control: AbstractControl<string>) => {
    const value = control.value;
    if (!value.startsWith(`http://`) && !value.startsWith(`https://`)) {
        return { errorText: `Please specify http:// or https://` };
    }
    return null;
}

function addressHasPort(address: string): boolean {
    // Check for port format: http(s)://<address>:<port>
    // Match http(s):// followed by address content, then :port (digits)
    const portPattern = /^https?:\/\/.+:\d+/;
    return portPattern.test(address);
}

function isChromiumOrFirefox(): boolean {
    const ua = window.navigator.userAgent;
    return ua.includes("Chrome") || ua.includes("Chromium") || ua.includes("Firefox");
}

function isMixedContent(address: string): boolean {
    if (window.location.protocol !== "https:") return false;
    if (!address.startsWith("http://")) return false;
    if (isChromiumOrFirefox()) {
        try {
            const url = new URL(address);
            if (url.hostname === "localhost" || url.hostname === "127.0.0.1") return false;
        } catch { /* invalid URL, skip */ }
    }
    return true;
}


@Component({
    selector: "tp-connection-creator",
    templateUrl: "./connection-creator.component.html",
    styleUrls: ["./connection-creator.component.scss"],
    imports: [
        PageScaffoldComponent, AsyncPipe, MatFormFieldModule, MatSelectModule, MatTooltipModule,
        ReactiveFormsModule, FormInputComponent, FormComponent, FormActionsComponent,
        FormToggleGroupComponent, MatButtonModule, MatInputModule, RichTooltipDirective, MatCheckboxModule,
        MatAutocompleteModule,
    ]
})
export class ConnectionCreatorComponent {

    readonly availability$ = new BehaviorSubject<ResourceAvailability>("ready");
    savedConnections = this.appData.connections.list();
    advancedConfigActiveOptions: FormOption<boolean>[] = [
        { value: true, viewValue: `Use address and credentials` },
        { value: false, viewValue: `Use connection string` },
    ];
    connectionStringRevealed = false;
    connectionStringPlaceholder = CONNECTION_STRING_PLACEHOLDER;
    passwordRevealed = false;
    addressBlurred = false;
    connectionStringBlurred = false;

    readonly form = this.formBuilder.group({
        name: ["", []],
        advancedConfigActive: [this.appData.preferences.connection.showAdvancedConfigByDefault(), [requiredValidator]],
        url: ["", [requiredValidator, connectionStringValidator]],
        saveConnectionDetails: [false, [requiredValidator]],
    });
    // TODO: support multiple addresses
    readonly advancedForm = this.formBuilder.group({
        address: ["", [requiredValidator, addressValidator]],
        username: ["", [requiredValidator]],
        password: ["", [requiredValidator]],
    });
    readonly isSubmitting$ = new BehaviorSubject(false);
    private readonly recentAddressesRefresh$ = new BehaviorSubject<void>(undefined);
    readonly recentAddressSuggestions$: Observable<string[]> = merge(
        this.advancedForm.controls.address.valueChanges.pipe(startWith(this.advancedForm.controls.address.value)),
        this.recentAddressesRefresh$,
    ).pipe(
        map(() => {
            const recent = this.appData.recentAddresses.list();
            const query = (this.advancedForm.controls.address.value ?? "").trim().toLowerCase();
            if (!query) return recent;
            return recent.filter(addr => addr.toLowerCase().includes(query) && addr.toLowerCase() !== query);
        }),
    );

    constructor(
        private formBuilder: FormBuilder, private appData: AppData,
        private driver: DriverState, private snackbar: SnackbarService, private location: Location,
        private router: Router, route: ActivatedRoute, private dialog: MatDialog,
    ) {
        (window as any).connectionCreator = this;

        this.updateAdvancedConfigOnUrlChanges();
        this.updateUrlOnAdvancedConfigChanges();

        route.queryParamMap.pipe(first()).subscribe((params) => {
            if (params.get(NAME)) this.form.patchValue({ name: params.get(NAME) ?? `` });

            // Fall back to the active connection's address/username when the URL
            // params don't specify them — so re-opening the connect page from a
            // live session pre-fills with the current target.
            const current = this.driver.connection$.value;
            const currentAddress = current
                ? (isBasicParams(current.params) ? current.params.addresses[0] : current.params.translatedAddresses[0]?.external) ?? null
                : null;
            const currentUsername = current?.params.username ?? null;

            this.advancedForm.patchValue({
                address: params.get(ADDRESS) ?? currentAddress ?? ``,
                username: params.get(USERNAME) ?? currentUsername ?? ``,
            });
        });
    }

    private updateAdvancedConfigOnUrlChanges() {
        combineLatest([this.form.controls.url.valueChanges]).pipe(
            filter(([url]) => !this.form.controls.name.dirty && !!url),
            map(([url]) => parseConnectionStringOrNull(url!)),
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
            map((params) => connectionString(params)),
        ).subscribe(url => {
            this.form.patchValue({ url }, { emitEvent: false });
        });
    }

    get canSubmit() {
        return (this.form.dirty || this.advancedForm.dirty) && this.form.valid;
    }

    get addressWarnings(): string | null {
        const address = this.advancedForm.controls.address.value;
        if (!address || this.advancedForm.controls.address.invalid || !this.addressBlurred) return null;
        return this.buildWarnings(address);
    }

    get connectionStringWarnings(): string | null {
        const url = this.form.controls.url.value;
        if (!url || this.form.controls.url.invalid || !this.connectionStringBlurred) return null;
        const params = parseConnectionStringOrNull(url);
        if (!params) return null;
        const addresses = isBasicParams(params) ? params.addresses : params.translatedAddresses.map(x => x.external);
        if (addresses.length === 0) return null;
        return this.buildWarnings(addresses[0]);
    }

    private buildWarnings(address: string): string | null {
        const warnings: string[] = [];
        if (isMixedContent(address)) warnings.push("Many browsers block HTTP (insecure) connections. Consider setting up TLS or using TypeDB Studio Desktop.");
        if (/:1729\b/.test(address)) warnings.push("Port 1729 is the gRPC port - TypeDB Studio uses HTTP, by default on port 8000.");
        else if (!addressHasPort(address)) warnings.push("No port specified - will use default (80 for http, 443 for https).");
        return warnings.length > 0 ? warnings.join(" ") : null;
    }

    private buildConnectionConfigOrNull(): ConnectionConfig | null {
        if (this.form.invalid || !this.form.value.url) return null;
        const connectionParams = parseConnectionStringOrNull(this.form.value.url);
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
        if (!config) throw new Error(INTERNAL_ERROR);
        const usedAdvancedRoute = this.form.value.advancedConfigActive === true;
        const submittedAddress = this.advancedForm.controls.address.value;
        this.form.disable();
        this.driver.tryConnect(config).subscribe({
            next: () => {
                if (usedAdvancedRoute && submittedAddress) {
                    this.appData.recentAddresses.push(submittedAddress);
                }
                this.snackbar.success(`Connected to ${config.name}`);
                this.router.navigate([this.appData.viewState.lastUsedToolRoute()]).then((navigated) => {
                    if (!navigated) throw new Error(INTERNAL_ERROR);
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

    @ViewChild(MatAutocompleteTrigger) private addressAutocompleteTrigger?: MatAutocompleteTrigger;

    removeRecentAddress(address: string, event?: Event) {
        event?.stopPropagation();
        event?.preventDefault();
        this.appData.recentAddresses.remove(address);
        this.recentAddressesRefresh$.next();
        // Reposition the panel so it adapts to the new option count.
        this.addressAutocompleteTrigger?.updatePosition();
    }

    onAddressKeydown(event: KeyboardEvent) {
        if (event.key !== "Delete" && event.key !== "Backspace") return;
        const trigger = this.addressAutocompleteTrigger;
        if (!trigger?.panelOpen) return;
        const active = trigger.activeOption;
        if (!active) return;
        // Backspace would otherwise edit the input text; only intercept when an option is highlighted.
        this.removeRecentAddress(active.value, event);
    }

    openConnectionStringEditor() {
        const dialogRef = this.dialog.open(ConnectionStringEditorDialogComponent);
        dialogRef.afterClosed().subscribe((result: string | undefined) => {
            if (result) {
                this.form.patchValue({ url: result });
                this.form.controls.url.markAsDirty();
            }
        });
    }

    private connectionNameUniqueValidator(control: AbstractControl<string>) {
        if (this.savedConnections.some(x => x.name === control.value)) {
            return { errorText: `A connection named '${control.value}' already exists` };
        } else return null;
    }
}
