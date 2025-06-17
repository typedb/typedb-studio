/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { AfterViewInit, Component, OnInit } from "@angular/core";
import { SpinnerComponent } from "./framework/spinner/spinner.component";
import { INTERNAL_ERROR } from "./framework/util/strings";
import { AnalyticsService } from "./service/analytics.service";
import { GuardsCheckEnd, GuardsCheckStart, NavigationCancel, Router, RouterOutlet, Event as RouterEvent, NavigationEnd } from "@angular/router";
import { EMPTY, filter, of, switchMap } from "rxjs";
import { AsyncPipe } from "@angular/common";
import { AppData } from "./service/app-data.service";
import { DriverState } from "./service/driver-state.service";
import { SnackbarService } from "./service/snackbar.service";

@Component({
    selector: "ts-root", // eslint-disable-line @angular-eslint/component-selector
    templateUrl: "./root.component.html",
    styleUrls: ["root.component.scss"],
    imports: [RouterOutlet, SpinnerComponent, AsyncPipe]
})
export class RootComponent implements OnInit, AfterViewInit {
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

    constructor(private analytics: AnalyticsService, private router: Router, private appData: AppData, private driver: DriverState, private snackbar: SnackbarService) {
        this.informAnalyticsOnPageView(router, analytics);
    }

    private informAnalyticsOnPageView(router: Router, analytics: AnalyticsService) {
        router.events.pipe(filter((event: RouterEvent) => event instanceof NavigationEnd)).subscribe(() => {
            analytics.posthog.capturePageView();
            analytics.cio.page();
        });
    }

    ngOnInit() {
        const initialConnectionConfig = this.appData.connections.findStartupConnection();
        if (initialConnectionConfig) {
            this.driver.tryConnect(initialConnectionConfig).subscribe({
                next: (databases) => {
                    this.snackbar.info(`Connected to ${initialConnectionConfig.name}`);
                    this.initialised = true;
                },
                error: (err) => {
                    this.snackbar.infoPersistent(
                        `Failed to reconnect to '${initialConnectionConfig.name}'.\n`
                        + `Please reconnect manually via the 'Connect TypeDB server' page.`);
                    console.warn(err);
                    this.appData.connections.clearStartupConnection();
                    this.initialised = true;
                },
            });
        } else {
            this.initialised = true;
        }
    }

    ngAfterViewInit() {
        this.analytics.google.loadScriptTag();
    }
}
