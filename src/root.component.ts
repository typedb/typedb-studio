/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, OnInit, ViewContainerRef } from "@angular/core";
import { SpinnerComponent } from "./framework/spinner/spinner.component";
import { AnalyticsService } from "./service/analytics.service";
import { GuardsCheckEnd, GuardsCheckStart, NavigationCancel, Router, RouterOutlet, Event as RouterEvent, NavigationEnd } from "@angular/router";
import { EMPTY, filter, of, switchMap } from "rxjs";
import { AsyncPipe } from "@angular/common";
import { AppData } from "./service/app-data.service";
import { DriverState } from "./service/driver-state.service";
import { StartupMessage, StartupMessageService } from "./service/startup-message.service";
import { SnackbarService } from "./service/snackbar.service";
import { ThemeService } from "./service/theme.service";

@Component({
    selector: "ts-root", // eslint-disable-line @angular-eslint/component-selector
    templateUrl: "./root.component.html",
    styleUrls: ["root.component.scss"],
    imports: [RouterOutlet, SpinnerComponent, AsyncPipe]
})
export class RootComponent implements OnInit {
    routeIsLoading$ = this.router.events.pipe(
        switchMap((event) => {
            if (event instanceof GuardsCheckStart) {
                return of(true);
            } else if (event instanceof GuardsCheckEnd || event instanceof NavigationCancel) {
                return of(false);
            } else {
                return EMPTY;
            }
        })
    );
    initialised = false;

    constructor(public vcRef: ViewContainerRef, private analytics: AnalyticsService, private router: Router, private appData: AppData, private driver: DriverState, private snackbar: SnackbarService, private theme: ThemeService, private startupMessage: StartupMessageService) {
        this.informAnalyticsOnPageView(router, analytics);
    }

    private informAnalyticsOnPageView(router: Router, analytics: AnalyticsService) {
        router.events.pipe(filter((event: RouterEvent) => event instanceof NavigationEnd)).subscribe(() => {
            analytics.posthog.capturePageView();
            analytics.cio.page();
        });
    }

    ngOnInit() {
        const message = this.startupMessage.consume();
        if (message) this.showStartupMessage(message);

        const initialConnectionConfig = this.appData.connections.findStartupConnection();
        if (initialConnectionConfig) {
            this.driver.tryConnect(initialConnectionConfig).subscribe({
                next: (databases) => {
                    this.snackbar.info(`Connected to ${initialConnectionConfig.name}`);
                    this.initialised = true;
                },
                error: (err) => {
                    console.warn(err);
                    this.appData.connections.clearStartupConnection();
                    this.initialised = true;
                },
            });
        } else {
            this.initialised = true;
        }
    }

    private showStartupMessage(message: StartupMessage) {
        const who = message.username ? `'${message.username}'` : `user`;
        if (message.kind === "password-changed") {
            this.snackbar.success(`Password changed for ${who}. Please log in with the new password.`);
        } else if (message.kind === "user-deleted") {
            this.snackbar.success(`User ${who} deleted.`);
        } else if (message.kind === "signed-out") {
            this.snackbar.info(`You have been signed out.`);
        }
    }

    ngAfterViewInit() {
        this.analytics.google.loadScriptTag();
    }
}
